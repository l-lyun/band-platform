package band.platform.domain.user.controller;

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
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.RefreshTokenCookieFactory;

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
	@DisplayName("리프레시 토큰 쿠키가 있으면 토큰을 회전하고 새 쿠키를 내려준다")
	void reissue() throws Exception {
		when(userTokenService.reissue("old-refresh-token"))
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
	@DisplayName("리프레시 토큰 쿠키가 없으면 A06 에러 응답을 반환한다")
	void reissueWithoutCookie() throws Exception {
		mockMvc.perform(post("/api/auth/reissue"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("A06"));
	}

	@Test
	@DisplayName("로그아웃하면 리프레시 토큰을 삭제하고 쿠키를 만료한다")
	void logout() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
				.cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-token")))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken=")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")));

		verify(userTokenService).logout("refresh-token");
	}

	private RefreshTokenCookieFactory refreshTokenCookieFactory() {
		RefreshTokenCookieFactory refreshTokenCookieFactory = new RefreshTokenCookieFactory();
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "cookieName", "refreshToken");
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "secure", false);
		return refreshTokenCookieFactory;
	}

}
