package band.platform.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import band.platform.domain.user.dto.FindLoginIdResponse;
import band.platform.domain.user.dto.TokenResponse;
import band.platform.domain.user.dto.UserLoginResponse;
import band.platform.domain.user.dto.UserSignupResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.service.UserLoginService;
import band.platform.domain.user.service.UserSignupService;
import band.platform.domain.user.service.UserTokenService;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.cookie.RefreshTokenCookieFactory;

class UserControllerTest {

	private MockMvc mockMvc;

	private UserSignupService userSignupService;
	private UserLoginService userLoginService;
	private UserTokenService userTokenService;
	private RefreshTokenCookieFactory refreshTokenCookieFactory;

	@BeforeEach
	void setUp() {
		userSignupService = mock(UserSignupService.class);
		userLoginService = mock(UserLoginService.class);
		userTokenService = mock(UserTokenService.class);
		refreshTokenCookieFactory = refreshTokenCookieFactory();
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(
				userSignupService,
				userLoginService,
				userTokenService,
				refreshTokenCookieFactory
			))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@Test
	@DisplayName("회원가입 요청이 유효하면 201 응답과 생성된 회원 정보를 반환한다")
	void signup() throws Exception {
		when(userSignupService.signup(any()))
			.thenReturn(new UserSignupResponse(1L, "bandmaster", "bandmaster@example.com"));

		mockMvc.perform(post("/api/users/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupRequest("bandmaster", "bandmaster@example.com", true)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value(201))
			.andExpect(jsonPath("$.message").value("리소스가 생성되었습니다."))
			.andExpect(jsonPath("$.data.id").isNumber())
			.andExpect(jsonPath("$.data.loginId").value("bandmaster"))
			.andExpect(jsonPath("$.data.email").value("bandmaster@example.com"));
	}

	@Test
	@DisplayName("중복 로그인 아이디로 회원가입하면 U01 에러 응답을 반환한다")
	void duplicateLoginId() throws Exception {
		when(userSignupService.signup(any()))
			.thenThrow(new BusinessException(ErrorCode.USER_LOGIN_ID_DUPLICATED));

		mockMvc.perform(post("/api/users/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupRequest("bandmaster", "other@example.com", true)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.code").value("U01"))
			.andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."));
	}

	@Test
	@DisplayName("개인정보 필수 동의가 아니면 E01 에러 응답을 반환한다")
	void privacyPolicyNotAgreed() throws Exception {
		mockMvc.perform(post("/api/users/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupRequest("bandmaster", "bandmaster@example.com", false)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("로그인 요청이 유효하면 200 응답과 회원 식별 정보를 반환한다")
	void login() throws Exception {
		when(userLoginService.login(any()))
			.thenReturn(new UserLoginResponse(1L, "bandmaster", "bandmaster@example.com", null, null, 0));
		when(userTokenService.issue(1L, "bandmaster"))
			.thenReturn(new UserTokenIssueResult(
				TokenResponse.bearer("access-token", 1800),
				"refresh-token",
				1209600
			));

		mockMvc.perform(post("/api/users/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("bandmaster", "password123!")))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=refresh-token")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.id").isNumber())
			.andExpect(jsonPath("$.data.loginId").value("bandmaster"))
			.andExpect(jsonPath("$.data.email").value("bandmaster@example.com"))
			.andExpect(jsonPath("$.data.accessToken").value("access-token"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.expiresIn").value(1800));
	}

	@Test
	@DisplayName("로그인 아이디나 비밀번호가 올바르지 않으면 A03 에러 응답을 반환한다")
	void invalidCredentials() throws Exception {
		when(userLoginService.login(any()))
			.thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

		mockMvc.perform(post("/api/users/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("bandmaster", "wrongpass123!")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value("A03"))
			.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
	}

	@Test
	@DisplayName("가입된 이메일로 아이디 찾기를 요청하면 UserLoginService 결과를 반환한다")
	void findLoginId() throws Exception {
		when(userLoginService.findLoginId(any()))
			.thenReturn(new FindLoginIdResponse("bandmaster"));

		mockMvc.perform(post("/api/users/find-login-id")
				.contentType(MediaType.APPLICATION_JSON)
				.content(findLoginIdRequest("bandmaster@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.loginId").value("bandmaster"));

		verify(userLoginService).findLoginId(any());
	}

	@Test
	@DisplayName("가입되지 않은 이메일로 아이디 찾기를 요청하면 A03 에러 응답을 반환한다")
	void findLoginIdUnknownEmail() throws Exception {
		when(userLoginService.findLoginId(any()))
			.thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

		mockMvc.perform(post("/api/users/find-login-id")
				.contentType(MediaType.APPLICATION_JSON)
				.content(findLoginIdRequest("unknown@example.com")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value("A03"))
			.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
	}

	@Test
	@DisplayName("이메일 형식이 아니면 아이디 찾기 요청은 E01 에러 응답을 반환한다")
	void findLoginIdInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/users/find-login-id")
				.contentType(MediaType.APPLICATION_JSON)
				.content(findLoginIdRequest("invalid-email")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("로그인 비밀번호가 BCrypt 72바이트를 초과하면 E01 에러 응답을 반환한다")
	void passwordByteLengthExceeded() throws Exception {
		mockMvc.perform(post("/api/users/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("bandmaster", "가".repeat(25))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	private String signupRequest(String loginId, String email, boolean privacyPolicyAgreed) {
		return """
			{
				"name": "김김김",
				"loginId": "%s",
				"password": "password123!",
				"email": "%s",
				"description": "자기소개",
				"opened": false,
				"phoneNumber": "01012345678",
				"gender": "MALE",
				"profileImg": "img",
				"privacyPolicyAgreed": %s,
				"marketingPolicyAgreed": true
			}
			""".formatted(loginId, email, privacyPolicyAgreed);
	}

	private String loginRequest(String loginId, String password) {
		return """
			{
				"loginId": "%s",
				"password": "%s"
			}
			""".formatted(loginId, password);
	}

	private String findLoginIdRequest(String email) {
		return """
			{
				"email": "%s"
			}
			""".formatted(email);
	}

	private RefreshTokenCookieFactory refreshTokenCookieFactory() {
		RefreshTokenCookieFactory refreshTokenCookieFactory = new RefreshTokenCookieFactory();
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "cookieName", "refreshToken");
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "secure", false);
		ReflectionTestUtils.setField(refreshTokenCookieFactory, "sameSite", "Lax");
		return refreshTokenCookieFactory;
	}

}
