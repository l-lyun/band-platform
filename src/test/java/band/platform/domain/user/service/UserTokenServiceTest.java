package band.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.repository.UserRefreshTokenRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.security.jwt.JwtTokenProvider;
import tools.jackson.databind.ObjectMapper;

class UserTokenServiceTest {

	private static final Long USER_ID = 1L;
	private static final String LOGIN_ID = "bandmaster";
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

	@Mock
	private UserRefreshTokenRepository userRefreshTokenRepository;

	private RecordingUserSessionLockManager userSessionLockManager;
	private UserTokenService userTokenService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		JwtTokenProvider jwtTokenProvider = createJwtTokenProvider();
		userSessionLockManager = new RecordingUserSessionLockManager();
		userTokenService = new UserTokenService(jwtTokenProvider, userRefreshTokenRepository, userSessionLockManager);
	}

	@Test
	@DisplayName("로그인 성공 시 액세스 토큰과 리프레시 토큰을 발급하고 리프레시 토큰 식별자를 저장한다")
	void issue() {
		UserTokenIssueResult result = userTokenService.issue(USER_ID, LOGIN_ID);

		assertThat(result.tokenResponse().accessToken()).isNotBlank();
		assertThat(result.tokenResponse().tokenType()).isEqualTo("Bearer");
		assertThat(result.tokenResponse().expiresIn()).isEqualTo(1800);
		assertThat(result.refreshToken()).isNotBlank();
		assertThat(result.refreshTokenMaxAgeSeconds()).isEqualTo(1209600);
		verify(userRefreshTokenRepository).save(eq(USER_ID), any(), eq(REFRESH_TOKEN_TTL));
		assertThat(userSessionLockManager.lockedUserIds).containsExactly(USER_ID);
	}

	@Test
	@DisplayName("리프레시 토큰 재발급 시 기존 토큰을 새 토큰으로 회전한다")
	void reissue() {
		UserTokenIssueResult loginResult = userTokenService.issue(USER_ID, LOGIN_ID);
		userSessionLockManager.clear();
		when(userRefreshTokenRepository.rotate(eq(USER_ID), any(), any(), eq(REFRESH_TOKEN_TTL)))
			.thenReturn(true);

		UserTokenIssueResult reissueResult = userTokenService.reissue(loginResult.refreshToken());

		assertThat(reissueResult.tokenResponse().accessToken()).isNotBlank();
		assertThat(reissueResult.refreshToken()).isNotEqualTo(loginResult.refreshToken());
		verify(userRefreshTokenRepository).rotate(eq(USER_ID), any(), any(), eq(REFRESH_TOKEN_TTL));
		assertThat(userSessionLockManager.lockedUserIds).containsExactly(USER_ID);
	}

	@Test
	@DisplayName("이미 회전된 리프레시 토큰이면 A06 예외를 던진다")
	void reissueRotatedToken() {
		UserTokenIssueResult loginResult = userTokenService.issue(USER_ID, LOGIN_ID);
		userSessionLockManager.clear();
		when(userRefreshTokenRepository.rotate(eq(USER_ID), any(), any(), eq(REFRESH_TOKEN_TTL)))
			.thenReturn(false);

		assertThatThrownBy(() -> userTokenService.reissue(loginResult.refreshToken()))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID)
			);
		assertThat(userSessionLockManager.lockedUserIds).containsExactly(USER_ID);
	}

	@Test
	@DisplayName("로그아웃 시 리프레시 토큰 식별자를 삭제한다")
	void logout() {
		UserTokenIssueResult loginResult = userTokenService.issue(USER_ID, LOGIN_ID);
		userSessionLockManager.clear();

		userTokenService.logout(loginResult.refreshToken());

		verify(userRefreshTokenRepository).delete(eq(USER_ID), any());
		assertThat(userSessionLockManager.lockedUserIds).containsExactly(USER_ID);
	}

	private JwtTokenProvider createJwtTokenProvider() {
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
			new ObjectMapper(),
			Clock.fixed(Instant.parse("2026-06-11T00:00:00Z"), ZoneOffset.UTC)
		);
		ReflectionTestUtils.setField(jwtTokenProvider, "issuer", "band-platform");
		ReflectionTestUtils.setField(jwtTokenProvider, "secret", "test-secret-key-for-jwt-token-provider");
		ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenTtlSeconds", 1800L);
		ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenTtlSeconds", 1209600L);
		return jwtTokenProvider;
	}

	private static class RecordingUserSessionLockManager extends UserSessionLockManager {

		private final List<Long> lockedUserIds = new ArrayList<>();

		@Override
		public <T> T withLock(Long userId, Supplier<T> operation) {
			lockedUserIds.add(userId);
			return super.withLock(userId, operation);
		}

		private void clear() {
			lockedUserIds.clear();
		}
	}
}
