package band.platform.global.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import band.platform.domain.user.controller.UserController;
import band.platform.domain.user.controller.UserInterestController;
import band.platform.domain.user.controller.UserProfileController;
import band.platform.domain.user.service.UserInterestService;
import band.platform.domain.user.service.UserLoginService;
import band.platform.domain.user.service.UserPasswordResetService;
import band.platform.domain.user.service.UserProfileService;
import band.platform.domain.user.service.UserSignupService;
import band.platform.domain.user.service.UserSocialLoginService;
import band.platform.domain.user.service.UserTokenService;
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.cookie.PasswordResetTokenCookieFactory;
import band.platform.global.security.cookie.RefreshTokenCookieFactory;
import band.platform.global.security.handler.JsonAccessDeniedHandler;
import band.platform.global.security.handler.JsonAuthenticationEntryPoint;
import band.platform.global.security.handler.SecurityErrorResponseWriter;
import band.platform.global.security.jwt.JwtAuthenticationFilter;
import band.platform.global.security.jwt.JwtTokenProvider;
import tools.jackson.databind.ObjectMapper;

@SpringJUnitWebConfig(classes = SecurityConfigTest.TestConfig.class)
@TestPropertySource(properties = {
	"security.cors.allowed-origins=http://localhost:3000,http://localhost:5173",
	"security.jwt.access-token-ttl-seconds=1800",
	"security.jwt.refresh-token-ttl-seconds=1209600",
	"security.jwt.refresh-cookie-secure=false"
})
class SecurityConfigTest {

	@Autowired
	private SecurityConfig securityConfig;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private Filter springSecurityFilterChain;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.webAppContextSetup(webApplicationContext)
			.addFilters(springSecurityFilterChain)
			.build();
	}

	@Test
	@DisplayName("비밀번호를 BCrypt로 단방향 암호화한다")
	void passwordEncoder() {
		PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

		String encodedPassword = passwordEncoder.encode("password123!");

		assertThat(encodedPassword).isNotEqualTo("password123!");
		assertThat(passwordEncoder.matches("password123!", encodedPassword)).isTrue();
	}

	@Test
	@DisplayName("토큰 인증 API에 필요한 CORS 기본 정책을 제공한다")
	void corsConfigurationSource() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/users");

		CorsConfiguration configuration = securityConfig
			.corsConfigurationSource("http://localhost:3000,http://localhost:5173")
			.getCorsConfiguration(request);

		assertThat(configuration).isNotNull();
		assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:3000", "http://localhost:5173");
		assertThat(configuration.getAllowedOriginPatterns()).isNull();
		assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
		assertThat(configuration.getAllowedHeaders()).contains("Authorization", "Content-Type", "Accept");
		assertThat(configuration.getExposedHeaders()).contains("Authorization");
		assertThat(configuration.getAllowCredentials()).isTrue();
	}

	@Test
	@DisplayName("소셜 로그인 시작 POST는 익명 요청이 인증 차단이 아니라 컨트롤러 검증까지 도달한다")
	void socialAuthorizationPublicPostReachesControllerValidation() throws Exception {
		mockMvc.perform(post("/api/users/social/authorization")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("E01"));
	}

	@Test
	@DisplayName("소셜 로그인 POST는 익명 요청이 인증 차단이 아니라 컨트롤러 검증까지 도달한다")
	void socialSignInPublicPostReachesControllerValidation() throws Exception {
		mockMvc.perform(post("/api/users/social/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("E01"));
	}

	@Test
	@DisplayName("소셜 가입 POST는 익명 요청이 인증 차단이 아니라 컨트롤러 검증까지 도달한다")
	void socialSignupPublicPostReachesControllerValidation() throws Exception {
		mockMvc.perform(post("/api/users/social/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("E01"));
	}

	@Test
	@DisplayName("공개 엔드포인트가 아닌 API 익명 요청은 A01로 차단된다")
	void privateEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/users/private"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("A01"));
	}

	@Test
	@DisplayName("프로필 조회 API 익명 요청은 A01로 차단된다")
	void profileReadRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/users/me/profile"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("A01"));
	}

	@Test
	@DisplayName("프로필 수정 API 익명 요청은 A01로 차단된다")
	void profileUpdateRequiresAuthentication() throws Exception {
		mockMvc.perform(put("/api/users/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("A01"));
	}

	@Test
	@DisplayName("관심사 조회 API 익명 요청은 A01로 차단된다")
	void interestReadRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/users/me/interests"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("A01"));
	}

	@Test
	@DisplayName("관심사 수정 API 익명 요청은 A01로 차단된다")
	void interestUpdateRequiresAuthentication() throws Exception {
		mockMvc.perform(put("/api/users/me/interests")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("A01"));
	}

	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	@Import({SecurityConfig.class, GlobalExceptionHandler.class})
	static class TestConfig {

		@Bean
		static ConversionService conversionService() {
			return ApplicationConversionService.getSharedInstance();
		}

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		SecurityErrorResponseWriter securityErrorResponseWriter(ObjectMapper objectMapper) {
			return new SecurityErrorResponseWriter(objectMapper);
		}

		@Bean
		JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint(SecurityErrorResponseWriter errorResponseWriter) {
			return new JsonAuthenticationEntryPoint(errorResponseWriter);
		}

		@Bean
		JsonAccessDeniedHandler jsonAccessDeniedHandler(SecurityErrorResponseWriter errorResponseWriter) {
			return new JsonAccessDeniedHandler(errorResponseWriter);
		}

		@Bean
		JwtTokenProvider jwtTokenProvider() {
			return mock(JwtTokenProvider.class);
		}

		@Bean
		JwtAuthenticationFilter jwtAuthenticationFilter(
			JwtTokenProvider jwtTokenProvider,
			SecurityErrorResponseWriter errorResponseWriter
		) {
			return new JwtAuthenticationFilter(jwtTokenProvider, errorResponseWriter);
		}

		@Bean
		UserController userController(
			UserSignupService userSignupService,
			UserLoginService userLoginService,
			UserTokenService userTokenService,
			UserPasswordResetService userPasswordResetService,
			UserSocialLoginService userSocialLoginService,
			RefreshTokenCookieFactory refreshTokenCookieFactory,
			PasswordResetTokenCookieFactory passwordResetTokenCookieFactory
		) {
			return new UserController(
				userSignupService,
				userLoginService,
				userTokenService,
				userPasswordResetService,
				userSocialLoginService,
				refreshTokenCookieFactory,
				passwordResetTokenCookieFactory
			);
		}

		@Bean
		UserProfileController userProfileController(UserProfileService userProfileService) {
			return new UserProfileController(userProfileService);
		}

		@Bean
		UserInterestController userInterestController(UserInterestService userInterestService) {
			return new UserInterestController(userInterestService);
		}

		@Bean
		UserSignupService userSignupService() {
			return mock(UserSignupService.class);
		}

		@Bean
		UserLoginService userLoginService() {
			return mock(UserLoginService.class);
		}

		@Bean
		UserTokenService userTokenService() {
			return mock(UserTokenService.class);
		}

		@Bean
		UserPasswordResetService userPasswordResetService() {
			return mock(UserPasswordResetService.class);
		}

		@Bean
		UserSocialLoginService userSocialLoginService() {
			return mock(UserSocialLoginService.class);
		}

		@Bean
		UserProfileService userProfileService() {
			return mock(UserProfileService.class);
		}

		@Bean
		UserInterestService userInterestService() {
			return mock(UserInterestService.class);
		}

		@Bean
		RefreshTokenCookieFactory refreshTokenCookieFactory() {
			RefreshTokenCookieFactory refreshTokenCookieFactory = new RefreshTokenCookieFactory();
			ReflectionTestUtils.setField(refreshTokenCookieFactory, "cookieName", "refreshToken");
			ReflectionTestUtils.setField(refreshTokenCookieFactory, "secure", false);
			ReflectionTestUtils.setField(refreshTokenCookieFactory, "sameSite", "Lax");
			return refreshTokenCookieFactory;
		}

		@Bean
		PasswordResetTokenCookieFactory passwordResetTokenCookieFactory() {
			PasswordResetTokenCookieFactory passwordResetTokenCookieFactory = new PasswordResetTokenCookieFactory();
			ReflectionTestUtils.setField(passwordResetTokenCookieFactory, "tokenTtl", Duration.ofMinutes(10));
			ReflectionTestUtils.setField(passwordResetTokenCookieFactory, "secure", false);
			ReflectionTestUtils.setField(passwordResetTokenCookieFactory, "sameSite", "Lax");
			return passwordResetTokenCookieFactory;
		}

	}

}
