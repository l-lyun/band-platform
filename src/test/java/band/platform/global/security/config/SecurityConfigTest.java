package band.platform.global.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

	private final SecurityConfig securityConfig = new SecurityConfig();

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

}
