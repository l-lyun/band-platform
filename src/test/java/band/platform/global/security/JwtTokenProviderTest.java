package band.platform.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import tools.jackson.databind.ObjectMapper;

class JwtTokenProviderTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-11T00:00:00Z"), ZoneOffset.UTC);

	private final JwtTokenProvider jwtTokenProvider = createJwtTokenProvider();

	@Test
	@DisplayName("액세스 토큰을 발급하고 회원 식별 클레임을 검증한다")
	void accessToken() {
		JwtToken accessToken = jwtTokenProvider.issueAccessToken(1L, "bandmaster");

		JwtAccessTokenClaims claims = jwtTokenProvider.parseAccessToken(accessToken.value());

		assertThat(accessToken.expiresIn()).isEqualTo(1800);
		assertThat(claims.userId()).isEqualTo(1L);
		assertThat(claims.loginId()).isEqualTo("bandmaster");
	}

	@Test
	@DisplayName("리프레시 토큰을 발급하고 RTR 식별자를 검증한다")
	void refreshToken() {
		JwtToken refreshToken = jwtTokenProvider.issueRefreshToken(1L, "bandmaster", "token-id");

		JwtRefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken.value());

		assertThat(refreshToken.expiresIn()).isEqualTo(1209600);
		assertThat(claims.userId()).isEqualTo(1L);
		assertThat(claims.loginId()).isEqualTo("bandmaster");
		assertThat(claims.tokenId()).isEqualTo("token-id");
	}

	@Test
	@DisplayName("액세스 토큰을 리프레시 토큰으로 사용하면 A06 예외를 던진다")
	void wrongTokenType() {
		JwtToken accessToken = jwtTokenProvider.issueAccessToken(1L, "bandmaster");

		assertThatThrownBy(() -> jwtTokenProvider.parseRefreshToken(accessToken.value()))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID)
			);
	}

	@Test
	@DisplayName("서명이 변조된 토큰이면 A06 예외를 던진다")
	void tamperedToken() {
		JwtToken accessToken = jwtTokenProvider.issueAccessToken(1L, "bandmaster");
		String tamperedToken = accessToken.value() + "x";

		assertThatThrownBy(() -> jwtTokenProvider.parseAccessToken(tamperedToken))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID)
			);
	}

	private static JwtTokenProvider createJwtTokenProvider() {
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(new ObjectMapper(), FIXED_CLOCK);
		ReflectionTestUtils.setField(jwtTokenProvider, "issuer", "band-platform");
		ReflectionTestUtils.setField(jwtTokenProvider, "secret", "test-secret-key-for-jwt-token-provider");
		ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenTtlSeconds", 1800L);
		ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenTtlSeconds", 1209600L);
		return jwtTokenProvider;
	}

}
