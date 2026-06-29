package band.platform.domain.user.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class PasswordResetSecureValueGenerator {

	private static final int CODE_BOUND = 1_000_000;
	private static final int TOKEN_BYTES = 32;

	private final SecureRandom secureRandom;

	public PasswordResetSecureValueGenerator() {
		this(new SecureRandom());
	}

	PasswordResetSecureValueGenerator(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
	}

	public String generateCode() {
		return "%06d".formatted(secureRandom.nextInt(CODE_BOUND));
	}

	public String generateToken() {
		byte[] tokenBytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(tokenBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}
}
