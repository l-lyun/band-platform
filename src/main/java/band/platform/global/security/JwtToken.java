package band.platform.global.security;

public record JwtToken(
	String value,
	long expiresIn
) {
}
