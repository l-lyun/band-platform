package band.platform.domain.user.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Arrays;

import jakarta.servlet.http.Cookie;

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
import band.platform.domain.user.service.UserPasswordResetService;
import band.platform.domain.user.service.UserSocialLoginService;
import band.platform.domain.user.service.UserSignupService;
import band.platform.domain.user.service.UserTokenService;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.config.PublicEndpoints;
import band.platform.global.security.cookie.PasswordResetTokenCookieFactory;
import band.platform.global.security.cookie.RefreshTokenCookieFactory;

class UserControllerTest {

	private MockMvc mockMvc;

	private UserSignupService userSignupService;
	private UserLoginService userLoginService;
	private UserTokenService userTokenService;
	private UserPasswordResetService userPasswordResetService;
	private RefreshTokenCookieFactory refreshTokenCookieFactory;
	private PasswordResetTokenCookieFactory passwordResetTokenCookieFactory;

	@BeforeEach
	void setUp() {
		userSignupService = mock(UserSignupService.class);
		userLoginService = mock(UserLoginService.class);
		userTokenService = mock(UserTokenService.class);
		userPasswordResetService = mock(UserPasswordResetService.class);
		refreshTokenCookieFactory = refreshTokenCookieFactory();
		passwordResetTokenCookieFactory = passwordResetTokenCookieFactory();
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(
				userSignupService,
				userLoginService,
				userTokenService,
				userPasswordResetService,
				mock(UserSocialLoginService.class),
				refreshTokenCookieFactory,
				passwordResetTokenCookieFactory
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

	@Test
	@DisplayName("비밀번호 재설정 코드 요청이 유효하면 서비스를 호출하고 성공 응답을 반환한다")
	void requestPasswordResetCode() throws Exception {
		mockMvc.perform(post("/api/users/password-reset/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetRequestCodeRequest("bandmaster", "bandmaster@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."));

		verify(userPasswordResetService).request("bandmaster", "bandmaster@example.com");
	}

	@Test
	@DisplayName("비밀번호 재설정 코드 검증이 유효하면 서비스를 호출하고 재설정 토큰을 JSON으로 반환하지 않는다")
	void verifyPasswordResetCode() throws Exception {
		when(userPasswordResetService.verify("bandmaster", "bandmaster@example.com", "123456"))
			.thenReturn("reset-token");

		mockMvc.perform(post("/api/users/password-reset/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetVerifyCodeRequest("bandmaster", "bandmaster@example.com", "123456")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("passwordResetToken=reset-token")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Path=/api/users/password-reset")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=600")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Lax")))
			.andExpect(jsonPath("$.data.resetToken").doesNotExist())
			.andExpect(jsonPath("$.data.token").doesNotExist());

		verify(userPasswordResetService).verify("bandmaster", "bandmaster@example.com", "123456");
	}

	@Test
	@DisplayName("비밀번호 재설정 완료 요청이 유효하면 재설정 토큰 경계값으로 서비스를 호출하고 성공 응답을 반환한다")
	void completePasswordReset() throws Exception {
		mockMvc.perform(post("/api/users/password-reset/complete")
				.cookie(new Cookie("passwordResetToken", "reset-token"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetCompleteRequest("newPassword123!")))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("passwordResetToken=")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Path=/api/users/password-reset")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
			.andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Lax")))
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."));

		verify(userPasswordResetService).complete("reset-token", "newPassword123!");
	}

	@Test
	@DisplayName("비밀번호 재설정 코드 요청 이메일 형식이 아니면 E01 에러 응답을 반환한다")
	void requestPasswordResetCodeInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/users/password-reset/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetRequestCodeRequest("bandmaster", "invalid-email")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("비밀번호 재설정 대상 계정이 없어도 성공 응답을 반환한다")
	void requestPasswordResetCodeUnknownAccount() throws Exception {
		mockMvc.perform(post("/api/users/password-reset/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetRequestCodeRequest("unknown", "unknown@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."));

		verify(userPasswordResetService).request("unknown", "unknown@example.com");
	}

	@Test
	@DisplayName("비밀번호 재설정 인증 코드가 올바르지 않으면 A07 에러 응답을 반환한다")
	void verifyPasswordResetCodeInvalidCode() throws Exception {
		when(userPasswordResetService.verify(eq("bandmaster"), eq("bandmaster@example.com"), eq("000000")))
			.thenThrow(new BusinessException(ErrorCode.AUTH_CODE_INVALID));

		mockMvc.perform(post("/api/users/password-reset/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetVerifyCodeRequest("bandmaster", "bandmaster@example.com", "000000")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("A07"))
			.andExpect(jsonPath("$.message").value("인증 코드가 올바르지 않습니다."));
	}

	@Test
	@DisplayName("비밀번호 재설정 대상 계정이 없어도 코드 검증은 A07 에러 응답을 반환한다")
	void verifyPasswordResetCodeUnknownAccount() throws Exception {
		when(userPasswordResetService.verify(eq("unknown"), eq("unknown@example.com"), eq("123456")))
			.thenThrow(new BusinessException(ErrorCode.AUTH_CODE_INVALID));

		mockMvc.perform(post("/api/users/password-reset/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetVerifyCodeRequest("unknown", "unknown@example.com", "123456")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("A07"))
			.andExpect(jsonPath("$.message").value("인증 코드가 올바르지 않습니다."));
	}

	@Test
	@DisplayName("비밀번호 재설정 인증 코드가 6자리를 초과하면 E01 에러 응답을 반환한다")
	void verifyPasswordResetCodeTooLong() throws Exception {
		mockMvc.perform(post("/api/users/password-reset/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetVerifyCodeRequest("bandmaster", "bandmaster@example.com", "1234567")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("비밀번호 재설정 인증 코드가 6자리보다 짧으면 E01 에러 응답을 반환한다")
	void verifyPasswordResetCodeTooShort() throws Exception {
		mockMvc.perform(post("/api/users/password-reset/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetVerifyCodeRequest("bandmaster", "bandmaster@example.com", "12345")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("비밀번호 재설정 인증 코드가 숫자가 아니면 E01 에러 응답을 반환한다")
	void verifyPasswordResetCodeNonNumeric() throws Exception {
		mockMvc.perform(post("/api/users/password-reset/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetVerifyCodeRequest("bandmaster", "bandmaster@example.com", "ABC123")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("비밀번호 재설정 인증 코드가 만료되면 A08 에러 응답을 반환한다")
	void verifyPasswordResetCodeExpiredCode() throws Exception {
		when(userPasswordResetService.verify(eq("bandmaster"), eq("bandmaster@example.com"), eq("123456")))
			.thenThrow(new BusinessException(ErrorCode.AUTH_CODE_EXPIRED));

		mockMvc.perform(post("/api/users/password-reset/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetVerifyCodeRequest("bandmaster", "bandmaster@example.com", "123456")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("A08"))
			.andExpect(jsonPath("$.message").value("인증 코드가 만료되었습니다."));
	}

	@Test
	@DisplayName("비밀번호 재설정 인증 코드 최대 시도를 초과하면 A07 에러 응답을 반환한다")
	void verifyPasswordResetCodeMaxAttemptsExceeded() throws Exception {
		when(userPasswordResetService.verify(eq("bandmaster"), eq("bandmaster@example.com"), eq("999999")))
			.thenThrow(new BusinessException(ErrorCode.AUTH_CODE_INVALID));

		mockMvc.perform(post("/api/users/password-reset/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetVerifyCodeRequest("bandmaster", "bandmaster@example.com", "999999")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("A07"))
			.andExpect(jsonPath("$.message").value("인증 코드가 올바르지 않습니다."));
	}

	@Test
	@DisplayName("비밀번호 재설정 토큰이 올바르지 않으면 A06 에러 응답을 반환한다")
	void completePasswordResetInvalidResetToken() throws Exception {
		doThrow(new BusinessException(ErrorCode.AUTH_TOKEN_INVALID))
			.when(userPasswordResetService)
			.complete(eq("invalid-reset-token"), eq("newPassword123!"));

		mockMvc.perform(post("/api/users/password-reset/complete")
				.cookie(new Cookie("passwordResetToken", "invalid-reset-token"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetCompleteRequest("newPassword123!")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value("A06"))
			.andExpect(jsonPath("$.message").value("인증 토큰이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("새 비밀번호 정책이 유효하지 않으면 E01 에러 응답을 반환한다")
	void completePasswordResetInvalidNewPasswordPolicy() throws Exception {
		mockMvc.perform(post("/api/users/password-reset/complete")
				.cookie(new Cookie("passwordResetToken", "reset-token"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(passwordResetCompleteRequest("short")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("비밀번호 재설정 API는 공개 POST 엔드포인트에 포함된다")
	void passwordResetPublicEndpoint() {
		assertTrue(Arrays.asList(PublicEndpoints.USER_POST_ENDPOINTS).contains("/api/users/password-reset/**"));
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

	private String passwordResetRequestCodeRequest(String loginId, String email) {
		return """
			{
				"loginId": "%s",
				"email": "%s"
			}
			""".formatted(loginId, email);
	}

	private String passwordResetVerifyCodeRequest(String loginId, String email, String code) {
		return """
			{
				"loginId": "%s",
				"email": "%s",
				"code": "%s"
			}
			""".formatted(loginId, email, code);
	}

	private String passwordResetCompleteRequest(String newPassword) {
		return """
			{
				"newPassword": "%s"
			}
			""".formatted(newPassword);
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
