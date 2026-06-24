package band.platform.global.security.config;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import band.platform.global.security.handler.JsonAccessDeniedHandler;
import band.platform.global.security.handler.JsonAuthenticationEntryPoint;
import band.platform.global.security.jwt.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		JsonAuthenticationEntryPoint authenticationEntryPoint,
		JsonAccessDeniedHandler accessDeniedHandler,
		JwtAuthenticationFilter jwtAuthenticationFilter
	) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.cors(Customizer.withDefaults())
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.authorizeHttpRequests(authorization -> authorization
				.requestMatchers(HttpMethod.POST, PublicEndpoints.USER_POST_ENDPOINTS).permitAll()
				.requestMatchers(HttpMethod.POST, PublicEndpoints.AUTH_POST_ENDPOINTS).permitAll()
				.requestMatchers(HttpMethod.OPTIONS, PublicEndpoints.OPTIONS_ENDPOINTS).permitAll()
				.requestMatchers(PublicEndpoints.ERROR_ENDPOINTS).permitAll()
				.anyRequest().authenticated()
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(
		@Value("${security.cors.allowed-origins}") String allowedOrigins
	) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(parseAllowedOrigins(allowedOrigins));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private List<String> parseAllowedOrigins(String allowedOrigins) {
		return Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isBlank())
			.toList();
	}

}
