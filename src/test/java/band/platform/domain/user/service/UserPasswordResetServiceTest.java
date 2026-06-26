package band.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserPasswordResetRepository;
import band.platform.domain.user.repository.UserRefreshTokenRepository;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class UserPasswordResetServiceTest {

	private static final Long USER_ID = 1L;
	private static final String LOGIN_ID = "bandmaster";
	private static final String EMAIL = "bandmaster@example.com";
	private static final String RESET_CODE = "123456";
	private static final String RESET_TOKEN = "raw-reset-token";
	private static final String NEW_PASSWORD = "newPassword123!";
	private static final Duration CODE_TTL = Duration.ofMinutes(5);
	private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserPasswordResetRepository passwordResetRepository;

	@Mock
	private UserRefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordResetSecureValueGenerator secureValueGenerator;

	private PasswordEncoder passwordEncoder;
	private CapturingPasswordResetMailSender mailSender;
	private RecordingUserSessionLockManager userSessionLockManager;
	private UserPasswordResetService service;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		passwordEncoder = new BCryptPasswordEncoder();
		mailSender = new CapturingPasswordResetMailSender();
		PasswordResetProperties properties = new PasswordResetProperties();
		properties.setCodeTtl(CODE_TTL);
		properties.setTokenTtl(TOKEN_TTL);
		properties.setMaxAttempts(3);
		userSessionLockManager = new RecordingUserSessionLockManager();
		service = new UserPasswordResetService(
			userRepository,
			passwordResetRepository,
			refreshTokenRepository,
			passwordEncoder,
			secureValueGenerator,
			properties,
			mailSender,
			userSessionLockManager
		);
	}

	@Test
	@DisplayName("회원 정보가 일치하면 재설정 코드를 해시로 저장하고 이메일 발송 경계에 전달한다")
	void request() {
		User user = user();
		when(userRepository.findByLoginIdAndEmailAndStatus(LOGIN_ID, EMAIL, UserStatus.ACTIVE))
			.thenReturn(Optional.of(user));
		when(secureValueGenerator.generateCode()).thenReturn(RESET_CODE);
		ArgumentCaptor<String> codeHash = ArgumentCaptor.forClass(String.class);

		service.request(LOGIN_ID, EMAIL);

		verify(passwordResetRepository).saveCode(eq(USER_ID), codeHash.capture(), eq(CODE_TTL));
		assertThat(passwordEncoder.matches(RESET_CODE, codeHash.getValue())).isTrue();
		assertThat(codeHash.getValue()).isNotEqualTo(RESET_CODE);
		assertThat(mailSender.email).isEqualTo(EMAIL);
		assertThat(mailSender.code).isEqualTo(RESET_CODE);
	}

	@Test
	@DisplayName("회원 정보가 일치하지 않으면 재설정 코드를 발급하지 않는다")
	void requestUnknownUser() {
		when(userRepository.findByLoginIdAndEmailAndStatus(LOGIN_ID, EMAIL, UserStatus.ACTIVE))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.request(LOGIN_ID, EMAIL))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
		verify(passwordResetRepository, never()).saveCode(any(), anyString(), any());
	}

	@Test
	@DisplayName("탈퇴한 회원은 재설정 코드 발급 대상에서 제외한다")
	void requestWithdrawnUser() {
		when(userRepository.findByLoginIdAndEmailAndStatus(LOGIN_ID, EMAIL, UserStatus.ACTIVE))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.request(LOGIN_ID, EMAIL))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
		verify(passwordResetRepository, never()).saveCode(any(), anyString(), any());
	}

	@Test
	@DisplayName("재설정 코드가 일치하면 코드 소비와 토큰 저장을 원자적으로 요청한다")
	void verifyCode() {
		User user = user();
		String codeHash = passwordEncoder.encode(RESET_CODE);
		when(userRepository.findByLoginIdAndEmailAndStatus(LOGIN_ID, EMAIL, UserStatus.ACTIVE))
			.thenReturn(Optional.of(user));
		when(passwordResetRepository.findCode(USER_ID))
			.thenReturn(Optional.of(new UserPasswordResetRepository.PasswordResetCode(codeHash, 0)));
		when(secureValueGenerator.generateToken()).thenReturn(RESET_TOKEN);
		when(passwordResetRepository.consumeCodeAndSaveToken(
			USER_ID,
			UserPasswordResetService.sha256(RESET_TOKEN),
			TOKEN_TTL
		)).thenReturn(true);

		String resetToken = service.verify(LOGIN_ID, EMAIL, RESET_CODE);

		assertThat(resetToken).isEqualTo(RESET_TOKEN);
		verify(passwordResetRepository).consumeCodeAndSaveToken(
			USER_ID,
			UserPasswordResetService.sha256(RESET_TOKEN),
			TOKEN_TTL
		);
		verify(passwordResetRepository, never()).deleteCode(USER_ID);
		verify(passwordResetRepository, never()).saveToken(anyString(), any(), any());
	}

	@Test
	@DisplayName("동시에 이미 소비된 재설정 코드를 검증하면 토큰을 반환하지 않고 만료 예외를 던진다")
	void verifyCodeAlreadyConsumed() {
		String codeHash = passwordEncoder.encode(RESET_CODE);
		when(userRepository.findByLoginIdAndEmailAndStatus(LOGIN_ID, EMAIL, UserStatus.ACTIVE))
			.thenReturn(Optional.of(user()));
		when(passwordResetRepository.findCode(USER_ID))
			.thenReturn(Optional.of(new UserPasswordResetRepository.PasswordResetCode(codeHash, 0)));
		when(secureValueGenerator.generateToken()).thenReturn(RESET_TOKEN);
		when(passwordResetRepository.consumeCodeAndSaveToken(
			USER_ID,
			UserPasswordResetService.sha256(RESET_TOKEN),
			TOKEN_TTL
		)).thenReturn(false);

		assertThatThrownBy(() -> service.verify(LOGIN_ID, EMAIL, RESET_CODE))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_CODE_EXPIRED)
			);
		verify(passwordResetRepository, never()).saveToken(anyString(), any(), any());
	}

	@Test
	@DisplayName("재설정 코드가 없으면 만료 예외를 던진다")
	void verifyExpiredCode() {
		when(userRepository.findByLoginIdAndEmailAndStatus(LOGIN_ID, EMAIL, UserStatus.ACTIVE))
			.thenReturn(Optional.of(user()));
		when(passwordResetRepository.findCode(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.verify(LOGIN_ID, EMAIL, RESET_CODE))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_CODE_EXPIRED)
			);
	}

	@Test
	@DisplayName("재설정 코드가 일치하지 않으면 실패 횟수를 증가시키고 A07 예외를 던진다")
	void verifyWrongCode() {
		String codeHash = passwordEncoder.encode(RESET_CODE);
		when(userRepository.findByLoginIdAndEmailAndStatus(LOGIN_ID, EMAIL, UserStatus.ACTIVE))
			.thenReturn(Optional.of(user()));
		when(passwordResetRepository.findCode(USER_ID))
			.thenReturn(Optional.of(new UserPasswordResetRepository.PasswordResetCode(codeHash, 0)));
		when(passwordResetRepository.incrementCodeAttempts(USER_ID))
			.thenReturn(Optional.of(new UserPasswordResetRepository.PasswordResetCode(codeHash, 1)));

		assertThatThrownBy(() -> service.verify(LOGIN_ID, EMAIL, "000000"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_CODE_INVALID)
			);
		verify(passwordResetRepository).incrementCodeAttempts(USER_ID);
		verify(passwordResetRepository, never()).saveToken(anyString(), any(), any());
	}

	@Test
	@DisplayName("재설정 코드 실패 횟수가 한도에 도달하면 코드를 삭제한다")
	void verifyMaxAttempts() {
		String codeHash = passwordEncoder.encode(RESET_CODE);
		when(userRepository.findByLoginIdAndEmailAndStatus(LOGIN_ID, EMAIL, UserStatus.ACTIVE))
			.thenReturn(Optional.of(user()));
		when(passwordResetRepository.findCode(USER_ID))
			.thenReturn(Optional.of(new UserPasswordResetRepository.PasswordResetCode(codeHash, 2)));
		when(passwordResetRepository.incrementCodeAttempts(USER_ID))
			.thenReturn(Optional.of(new UserPasswordResetRepository.PasswordResetCode(codeHash, 3)));

		assertThatThrownBy(() -> service.verify(LOGIN_ID, EMAIL, "000000"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_CODE_INVALID)
			);
		verify(passwordResetRepository).deleteCode(USER_ID);
	}

	@Test
	@DisplayName("탈퇴한 회원은 재설정 코드 검증 대상에서 제외한다")
	void verifyWithdrawnUser() {
		when(userRepository.findByLoginIdAndEmailAndStatus(LOGIN_ID, EMAIL, UserStatus.ACTIVE))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.verify(LOGIN_ID, EMAIL, RESET_CODE))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
		verify(passwordResetRepository, never()).findCode(any());
	}

	@Test
	@DisplayName("재설정 토큰을 소비한 뒤 새 비밀번호로 변경하고 리프레시 토큰을 무효화한다")
	void complete() {
		User user = user();
		when(passwordResetRepository.consumeToken(UserPasswordResetService.sha256(RESET_TOKEN))).thenReturn(Optional.of(USER_ID));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		service.complete(RESET_TOKEN, NEW_PASSWORD);

		assertThat(passwordEncoder.matches(NEW_PASSWORD, user.getPassword())).isTrue();
		verify(refreshTokenRepository).deleteAll(USER_ID);
		assertThat(userSessionLockManager.lockedUserIds).containsExactly(USER_ID);
	}

	@Test
	@DisplayName("재설정 토큰이 없으면 비밀번호를 변경하지 않고 A06 예외를 던진다")
	void completeInvalidToken() {
		when(passwordResetRepository.consumeToken(UserPasswordResetService.sha256(RESET_TOKEN))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.complete(RESET_TOKEN, NEW_PASSWORD))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID)
			);
		verify(refreshTokenRepository, never()).deleteAll(any());
	}

	@Test
	@DisplayName("새 비밀번호가 8자보다 짧으면 재설정 토큰을 소비하지 않는다")
	void completePasswordTooShort() {
		assertThatThrownBy(() -> service.complete(RESET_TOKEN, "short7!"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT)
			);
		verify(passwordResetRepository, never()).consumeToken(anyString());
	}

	@Test
	@DisplayName("새 비밀번호가 15자를 넘으면 재설정 토큰을 소비하지 않는다")
	void completePasswordTooLong() {
		assertThatThrownBy(() -> service.complete(RESET_TOKEN, "password12345678"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT)
			);
		verify(passwordResetRepository, never()).consumeToken(anyString());
	}

	@Test
	@DisplayName("BCrypt 제한을 넘는 새 비밀번호이면 재설정 토큰을 소비하지 않는다")
	void completePasswordByteLengthExceeded() {
		String password = "가".repeat(15) + "a".repeat(28);

		assertThatThrownBy(() -> service.complete(RESET_TOKEN, password))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT)
			);
		verify(passwordResetRepository, never()).consumeToken(anyString());
	}

	private User user() {
		User user = User.createLocalUser(
			"김김김",
			LOGIN_ID,
			passwordEncoder.encode("oldPassword123!"),
			EMAIL,
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			"img",
			true,
			true
		);
		ReflectionTestUtils.setField(user, "id", USER_ID);
		return user;
	}

	private static class CapturingPasswordResetMailSender implements PasswordResetMailSender {
		private String email;
		private String code;

		@Override
		public void sendPasswordResetCode(String email, String code) {
			this.email = email;
			this.code = code;
		}
	}

	private static class RecordingUserSessionLockManager extends UserSessionLockManager {

		private final List<Long> lockedUserIds = new ArrayList<>();

		@Override
		public <T> T withLock(Long userId, Supplier<T> operation) {
			lockedUserIds.add(userId);
			return super.withLock(userId, operation);
		}
	}
}
