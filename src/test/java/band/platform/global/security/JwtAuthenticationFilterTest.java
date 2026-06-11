package band.platform.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

class JwtAuthenticationFilterTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-11T00:00:00Z"), ZoneOffset.UTC);

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JwtTokenProvider jwtTokenProvider = createJwtTokenProvider();
	private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
		jwtTokenProvider,
		new SecurityErrorResponseWriter(objectMapper)
	);

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("유효한 액세스 토큰이면 인증 정보를 SecurityContext에 저장한다")
	void validAccessToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		String accessToken = jwtTokenProvider.issueAccessToken(1L, "bandmaster").value();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

		jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		assertThat(principal).isEqualTo(new JwtAuthenticationPrincipal(1L, "bandmaster"));
	}

	@Test
	@DisplayName("잘못된 액세스 토큰이면 A06 에러 응답을 반환한다")
	void invalidAccessToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

		jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).contains("\"code\":\"A06\"");
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	private JwtTokenProvider createJwtTokenProvider() {
		JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(objectMapper, FIXED_CLOCK);
		ReflectionTestUtils.setField(jwtTokenProvider, "issuer", "band-platform");
		ReflectionTestUtils.setField(jwtTokenProvider, "secret", "test-secret-key-for-jwt-token-provider");
		ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenTtlSeconds", 1800L);
		ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenTtlSeconds", 1209600L);
		return jwtTokenProvider;
	}

}
