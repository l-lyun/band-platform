package band.platform.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import band.platform.global.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	private final JwtTokenProvider jwtTokenProvider;
	private final SecurityErrorResponseWriter errorResponseWriter;

	public JwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		SecurityErrorResponseWriter errorResponseWriter
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.errorResponseWriter = errorResponseWriter;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			JwtAccessTokenClaims claims = jwtTokenProvider.parseAccessToken(authorizationHeader.substring(BEARER_PREFIX.length()));
			JwtAuthenticationPrincipal principal = new JwtAuthenticationPrincipal(claims.userId(), claims.loginId());
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER"))
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (BusinessException exception) {
			SecurityContextHolder.clearContext();
			errorResponseWriter.write(response, exception.getErrorCode());
		}
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			return matches(path, PublicEndpoints.OPTIONS_ENDPOINTS);
		}
		if (HttpMethod.POST.matches(request.getMethod())) {
			return matches(path, PublicEndpoints.USER_POST_ENDPOINTS)
				|| matches(path, PublicEndpoints.AUTH_POST_ENDPOINTS);
		}
		return matches(path, PublicEndpoints.ERROR_ENDPOINTS);
	}

	private boolean matches(String path, String[] patterns) {
		for (String pattern : patterns) {
			if (PATH_MATCHER.match(pattern, path)) {
				return true;
			}
		}
		return false;
	}

}
