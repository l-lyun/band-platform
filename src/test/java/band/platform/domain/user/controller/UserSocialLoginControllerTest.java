package band.platform.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import band.platform.domain.user.dto.SocialLoginResponse;
import band.platform.domain.user.dto.SocialLoginStartResponse;
import band.platform.domain.user.dto.TokenResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.service.UserLoginService;
import band.platform.domain.user.service.UserPasswordResetService;
import band.platform.domain.user.service.UserSignupService;
import band.platform.domain.user.service.UserSocialLoginResult;
import band.platform.domain.user.service.UserSocialLoginService;
import band.platform.domain.user.service.UserTokenService;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.cookie.PasswordResetTokenCookieFactory;
import band.platform.global.security.cookie.RefreshTokenCookieFactory;

class UserSocialLoginControllerTest {

	private MockMvc mockMvc;

	private UserSocialLoginService userSocialLoginService;

	@BeforeEach
	void setUp() {
		userSocialLoginService = mock(UserSocialLoginService.class);

		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(
				mock(UserSignupService.class),
				mock(UserLoginService.class),
				mock(UserTokenService.class),
				mock(UserPasswordResetService.class),
				userSocialLoginService,
				refreshTokenCookieFactory(),
				passwordResetTokenCookieFactory()
			))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("소셜 로그인 시작 요청이면 authorization URL과 state를 반환한다")
	void socialAuthorizationContract() throws Exception {
		when(userSocialLoginService.start(any()))
			.thenReturn(new SocialLoginStartResponse(
				SocialProvider.NAVER,
				"https://nid.naver.com/oauth2.0/authorize?response_type=code&state=oauth-state",
				"oauth-state"
			));

		mockMvc.perform(post("/api/users/social/authorization")
				.contentType(MediaType.APPLICATION_JSON)
				.content(socialLoginStartRequest("NAVER")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.provider").value("NAVER"))
			.andExpect(jsonPath("$.data.authorizationUrl").value("https://nid.naver.com/oauth2.0/authorize?response_type=code&state=oauth-state"))
			.andExpect(jsonPath("$.data.state").value("oauth-state"))
			.andExpect(jsonPath("$.data.providerSubject").doesNotExist())
			.andExpect(jsonPath("$.data.accessToken").doesNotExist());
	}

	@Test
	@DisplayName("소셜 로그인 시작 provider가 없으면 E01 에러 응답을 반환한다")
	void socialAuthorizationMissingProvider() throws Exception {
		mockMvc.perform(post("/api/users/social/authorization")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("소셜 로그인 연결 계정이면 토큰을 포함한 단일 응답 스키마를 반환한다")
	void socialSignInLinkedUserContract() throws Exception {
		when(userSocialLoginService.signIn(any()))
			.thenReturn(new UserSocialLoginResult(
				SocialLoginResponse.linked(
					SocialProvider.NAVER,
					"bandmaster@example.com",
					"김밴드",
					"https://example.com/profile.png",
					TokenResponse.bearer("access-token", 1800)
				),
				new UserTokenIssueResult(
					TokenResponse.bearer("access-token", 1800),
					"refresh-token",
					1209600
				)
			));

		mockMvc.perform(post("/api/users/social/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(socialLoginRequest("NAVER", "authorization-code", "oauth-state", "http://localhost:3000/oauth/naver")))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=refresh-token")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.signupRequired").value(false))
			.andExpect(jsonPath("$.data.provider").value("NAVER"))
			.andExpect(jsonPath("$.data.email").value("bandmaster@example.com"))
			.andExpect(jsonPath("$.data.name").value("김밴드"))
			.andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.png"))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").value(1800))
			.andExpect(jsonPath("$.data.providerSubject").doesNotExist());
	}

	@Test
	@DisplayName("소셜 로그인 미연결 계정이면 토큰 없이 가입 필요 응답을 반환한다")
	void socialSignInSignupRequiredContract() throws Exception {
		when(userSocialLoginService.signIn(any()))
			.thenReturn(new UserSocialLoginResult(
				SocialLoginResponse.signupRequired(
					SocialProvider.KAKAO,
					"bandmaster@example.com",
					"김밴드",
					"https://example.com/profile.png"
				),
				null
			));

		mockMvc.perform(post("/api/users/social/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(socialLoginRequest("KAKAO", "authorization-code", "oauth-state", "http://localhost:3000/oauth/kakao")))
			.andExpect(status().isOk())
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.signupRequired").value(true))
			.andExpect(jsonPath("$.data.provider").value("KAKAO"))
			.andExpect(jsonPath("$.data.email").value("bandmaster@example.com"))
			.andExpect(jsonPath("$.data.name").value("김밴드"))
			.andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/profile.png"))
			.andExpect(jsonPath("$.data.accessToken").value(org.hamcrest.Matchers.nullValue()))
			.andExpect(jsonPath("$.data.tokenType").value(org.hamcrest.Matchers.nullValue()))
			.andExpect(jsonPath("$.data.expiresIn").value(org.hamcrest.Matchers.nullValue()))
			.andExpect(jsonPath("$.data.providerSubject").doesNotExist());
	}

	@Test
	@DisplayName("소셜 로그인 state가 올바르지 않으면 서비스의 A10 에러를 그대로 반환한다")
	void socialSignInInvalidState() throws Exception {
		when(userSocialLoginService.signIn(any()))
			.thenThrow(new BusinessException(ErrorCode.AUTH_OAUTH_STATE_INVALID));

		mockMvc.perform(post("/api/users/social/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(socialLoginRequest("NAVER", "authorization-code", "invalid-state", "http://localhost:3000/oauth/naver")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("A10"))
			.andExpect(jsonPath("$.message").value("소셜 로그인 상태 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("소셜 로그인 제공자 통신 실패는 서비스의 A15 에러를 그대로 반환한다")
	void socialSignInProviderUnavailable() throws Exception {
		when(userSocialLoginService.signIn(any()))
			.thenThrow(new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE));

		mockMvc.perform(post("/api/users/social/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(socialLoginRequest("NAVER", "authorization-code", "oauth-state", "http://localhost:3000/oauth/naver")))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.status").value(502))
			.andExpect(jsonPath("$.code").value("A15"))
			.andExpect(jsonPath("$.message").value("소셜 로그인 제공자와 통신할 수 없습니다."));
	}

	@Test
	@DisplayName("소셜 로그인 필수 요청 값이 없거나 공백이면 E01 에러 응답을 반환한다")
	void socialSignInMissingRequiredFields() throws Exception {
		mockMvc.perform(post("/api/users/social/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"code": " ",
						"state": "",
						"redirectUri": " "
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("소셜 로그인 provider는 enum 이름 대문자만 허용한다")
	void socialSignInRejectsLowercaseProvider() throws Exception {
		mockMvc.perform(post("/api/users/social/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(socialLoginRequest("naver", "authorization-code", "oauth-state", "http://localhost:3000/oauth/naver")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	private String socialLoginRequest(String provider, String code, String state, String redirectUri) {
		return """
			{
				"provider": "%s",
				"code": "%s",
				"state": "%s",
				"redirectUri": "%s"
			}
			""".formatted(provider, code, state, redirectUri);
	}

	private String socialLoginStartRequest(String provider) {
		return """
			{
				"provider": "%s"
			}
			""".formatted(provider);
	}

	private RefreshTokenCookieFactory refreshTokenCookieFactory() {
		RefreshTokenCookieFactory refreshTokenCookieFactory = new RefreshTokenCookieFactory();
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "cookieName", "refreshToken");
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "secure", false);
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "sameSite", "Lax");
		return refreshTokenCookieFactory;
	}

	private PasswordResetTokenCookieFactory passwordResetTokenCookieFactory() {
		PasswordResetTokenCookieFactory passwordResetTokenCookieFactory = new PasswordResetTokenCookieFactory();
		ReflectionTestUtils.setField(passwordResetTokenCookieFactory, "tokenTtl", Duration.ofMinutes(10));
		ReflectionTestUtils.setField(passwordResetTokenCookieFactory, "secure", false);
		ReflectionTestUtils.setField(passwordResetTokenCookieFactory, "sameSite", "Lax");
		return passwordResetTokenCookieFactory;
	}

}
