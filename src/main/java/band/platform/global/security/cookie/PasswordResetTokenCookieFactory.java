package band.platform.global.security.cookie;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenCookieFactory {

	public static final String COOKIE_NAME = "passwordResetToken";
	private static final String COOKIE_PATH = "/api/users/password-reset";

	@Value("${security.password-reset.token-ttl:10m}")
	private Duration tokenTtl;

	@Value("${security.jwt.refresh-cookie-secure}")
	private boolean secure;

	@Value("${security.jwt.refresh-cookie-same-site}")
	private String sameSite;

	public ResponseCookie create(String resetToken) {
		return baseCookie()
			.value(resetToken)
			.maxAge(tokenTtl)
			.build();
	}

	public ResponseCookie delete() {
		return baseCookie()
			.value("")
			.maxAge(0)
			.build();
	}

	private ResponseCookie.ResponseCookieBuilder baseCookie() {
		return ResponseCookie.from(COOKIE_NAME)
			.httpOnly(true)
			.secure(secure)
			.path(COOKIE_PATH)
			.sameSite(sameSite);
	}

}
