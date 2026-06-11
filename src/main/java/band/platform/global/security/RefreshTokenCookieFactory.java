package band.platform.global.security;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;

@Component
public class RefreshTokenCookieFactory {

	private final String cookieName;
	private final boolean secure;
	private final String sameSite;

	public RefreshTokenCookieFactory(
		@Value("${security.jwt.refresh-cookie-name}") String cookieName,
		@Value("${security.jwt.refresh-cookie-secure}") boolean secure,
		@Value("${security.jwt.refresh-cookie-same-site}") String sameSite
	) {
		this.cookieName = cookieName;
		this.secure = secure;
		this.sameSite = sameSite;
	}

	public ResponseCookie create(String refreshToken, long maxAgeSeconds) {
		return baseCookie()
			.value(refreshToken)
			.maxAge(maxAgeSeconds)
			.build();
	}

	public ResponseCookie delete() {
		return baseCookie()
			.value("")
			.maxAge(0)
			.build();
	}

	public Optional<String> extract(Cookie[] cookies) {
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(cookie -> cookieName.equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(value -> value != null && !value.isBlank())
			.findFirst();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie() {
		return ResponseCookie.from(cookieName)
			.httpOnly(true)
			.secure(secure)
			.path("/api/auth")
			.sameSite(sameSite);
	}

}
