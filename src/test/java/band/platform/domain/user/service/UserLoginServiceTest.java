package band.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.FindLoginIdRequest;
import band.platform.domain.user.dto.FindLoginIdResponse;
import band.platform.domain.user.dto.UserLoginRequest;
import band.platform.domain.user.dto.UserLoginResponse;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@SpringBootTest
@Transactional
class UserLoginServiceTest {

	private static final String RAW_PASSWORD = "password123!";

	@Autowired
	private UserLoginService userLoginService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("로그인 아이디와 비밀번호가 일치하면 회원 식별 정보를 반환한다")
	void login() {
		User user = saveUser("bandmaster", "bandmaster@example.com", RAW_PASSWORD);

		UserLoginResponse response = userLoginService.login(new UserLoginRequest("bandmaster", RAW_PASSWORD));

		assertThat(response.id()).isEqualTo(user.getId());
		assertThat(response.loginId()).isEqualTo("bandmaster");
		assertThat(response.email()).isEqualTo("bandmaster@example.com");
	}

	@Test
	@DisplayName("존재하지 않는 로그인 아이디이면 A03 예외를 던진다")
	void unknownLoginId() {
		assertThatThrownBy(() -> userLoginService.login(new UserLoginRequest("unknown", RAW_PASSWORD)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS)
			);
	}

	@Test
	@DisplayName("존재하지 않는 로그인 아이디여도 더미 비밀번호 검증을 수행한다")
	void unknownLoginIdVerifiesDummyPassword() {
		UserRepository userRepository = mock(UserRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		UserLoginService loginService = new UserLoginService(userRepository, passwordEncoder);
		when(userRepository.findByLoginId("unknown")).thenReturn(Optional.empty());
		when(passwordEncoder.matches(eq(RAW_PASSWORD), anyString())).thenReturn(false);

		assertThatThrownBy(() -> loginService.login(new UserLoginRequest("unknown", RAW_PASSWORD)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS)
			);

		verify(passwordEncoder).matches(eq(RAW_PASSWORD), anyString());
	}

	@Test
	@DisplayName("비밀번호가 일치하지 않으면 A03 예외를 던진다")
	void wrongPassword() {
		saveUser("bandmaster", "bandmaster@example.com", RAW_PASSWORD);

		assertThatThrownBy(() -> userLoginService.login(new UserLoginRequest("bandmaster", "wrongpass123!")))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS)
			);
	}

	@Test
	@DisplayName("탈퇴한 회원이면 A03 예외를 던진다")
	void withdrawnUser() {
		saveWithdrawnUser("bandmaster", "bandmaster@example.com", RAW_PASSWORD);

		assertThatThrownBy(() -> userLoginService.login(new UserLoginRequest("bandmaster", RAW_PASSWORD)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS)
			);
	}

	@Test
	@DisplayName("BCrypt 72바이트를 초과하는 비밀번호이면 E01 예외를 던진다")
	void passwordByteLengthExceeded() {
		String password = "가".repeat(25);

		assertThatThrownBy(() -> userLoginService.login(new UserLoginRequest("bandmaster", password)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT)
			);
	}

	@Test
	@DisplayName("가입된 활성 회원 이메일이면 로그인 아이디를 반환한다")
	void findLoginId() {
		saveUser("bandmaster", "bandmaster@example.com", RAW_PASSWORD);

		FindLoginIdResponse response = userLoginService.findLoginId(
			new FindLoginIdRequest("bandmaster@example.com")
		);

		assertThat(response.loginId()).isEqualTo("bandmaster");
	}

	@Test
	@DisplayName("가입되지 않은 이메일이면 A03 예외를 던진다")
	void findLoginIdUnknownEmail() {
		assertThatThrownBy(() -> userLoginService.findLoginId(
			new FindLoginIdRequest("unknown@example.com")
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS)
			);
	}

	@Test
	@DisplayName("탈퇴한 회원 이메일이면 A03 예외를 던진다")
	void findLoginIdWithdrawnUserEmail() {
		saveWithdrawnUser("bandmaster", "bandmaster@example.com", RAW_PASSWORD);

		assertThatThrownBy(() -> userLoginService.findLoginId(
			new FindLoginIdRequest("bandmaster@example.com")
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS)
			);
	}

	private User saveUser(String loginId, String email, String rawPassword) {
		return userRepository.save(User.createLocalUser(
			"김김김",
			loginId,
			passwordEncoder.encode(rawPassword),
			email,
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			"img",
			true,
			true
		));
	}

	private void saveWithdrawnUser(String loginId, String email, String rawPassword) {
		User user = saveUser(loginId, email, rawPassword);
		ReflectionTestUtils.setField(user, "status", UserStatus.WITHDRAWN);
	}

}
