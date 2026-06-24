package band.platform.global.security.jwt;

public record JwtToken(
	String value,
	long expiresIn
) {
}
