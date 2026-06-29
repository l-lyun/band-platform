package band.platform.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import band.platform.domain.user.dto.TokenResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.service.UserTokenService;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.cookie.RefreshTokenCookieFactory;
import jakarta.servlet.http.HttpServletRequest;

class AuthControllerTest {

	private MockMvc mockMvc;

	private UserTokenService userTokenService;

	@BeforeEach
	void setUp() {
		userTokenService = mock(UserTokenService.class);
		RefreshTokenCookieFactory refreshTokenCookieFactory = refreshTokenCookieFactory();
		mockMvc = MockMvcBuilders
			.standaloneSetup(new AuthController(userTokenService, refreshTokenCookieFactory))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("서비스의 재발급 결과를 새 쿠키와 응답 바디로 변환한다")
	void reissue() throws Exception {
		when(userTokenService.reissue(any(HttpServletRequest.class)))
			.thenReturn(new UserTokenIssueResult(
				TokenResponse.bearer("new-access-token", 1800),
				"new-refresh-token",
				1209600
			));

		mockMvc.perform(post("/api/auth/reissue")
				.cookie(new jakarta.servlet.http.Cookie("refreshToken", "old-refresh-token")))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken=new-refresh-token")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("HttpOnly")))
			.andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").value(1800));
	}

	@Test
	@DisplayName("서비스의 A06 예외를 공통 에러 응답으로 변환한다")
	void reissueWithoutCookie() throws Exception {
		when(userTokenService.reissue(any(HttpServletRequest.class)))
			.thenThrow(new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));

		mockMvc.perform(post("/api/auth/reissue"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("A06"));
	}

	@Test
	@DisplayName("로그아웃 요청을 서비스에 위임하고 쿠키를 만료한다")
	void logout() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
				.cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-token")))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken=")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")));

		verify(userTokenService).logout(any(HttpServletRequest.class));
	}

	@Test
	@DisplayName("서비스가 예외를 던지지 않으면 로그아웃 쿠키를 만료한다")
	void logoutWhenServiceDoesNotThrow() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
				.cookie(new jakarta.servlet.http.Cookie("refreshToken", "invalid-refresh-token")))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken=")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")));

		verify(userTokenService).logout(any(HttpServletRequest.class));
	}

	@Test
	@DisplayName("로그아웃 중 내부 오류가 발생하면 E04 에러 응답을 반환한다")
	void logoutWithInternalError() throws Exception {
		doThrow(new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR))
			.when(userTokenService).logout(any(HttpServletRequest.class));

		mockMvc.perform(post("/api/auth/logout")
				.cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-token")))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("E04"));

		verify(userTokenService).logout(any(HttpServletRequest.class));
	}

	private RefreshTokenCookieFactory refreshTokenCookieFactory() {
		RefreshTokenCookieFactory refreshTokenCookieFactory = new RefreshTokenCookieFactory();
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "cookieName", "refreshToken");
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "secure", false);
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "sameSite", "Lax");
		return refreshTokenCookieFactory;
	}

}
