package band.platform.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import band.platform.domain.user.dto.UserLoginResponse;
import band.platform.domain.user.dto.UserSignupResponse;
import band.platform.domain.user.service.UserLoginService;
import band.platform.domain.user.service.UserSignupService;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.error.GlobalExceptionHandler;

class UserControllerTest {

	private MockMvc mockMvc;

	private UserSignupService userSignupService;
	private UserLoginService userLoginService;

	@BeforeEach
	void setUp() {
		userSignupService = mock(UserSignupService.class);
		userLoginService = mock(UserLoginService.class);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserController(userSignupService, userLoginService))
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
			.thenReturn(new UserLoginResponse(1L, "bandmaster", "bandmaster@example.com"));

		mockMvc.perform(post("/api/users/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("bandmaster", "password123!")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.id").isNumber())
			.andExpect(jsonPath("$.data.loginId").value("bandmaster"))
			.andExpect(jsonPath("$.data.email").value("bandmaster@example.com"));
	}

	@Test
	@DisplayName("로그인 아이디나 비밀번호가 올바르지 않으면 A03 에러 응답을 반환한다")
	void invalidCredentials() throws Exception {
		when(userLoginService.login(any()))
			.thenThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

		mockMvc.perform(post("/api/users/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequest("bandmaster", "wrongPassword123!")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value("A03"))
			.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
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

}
