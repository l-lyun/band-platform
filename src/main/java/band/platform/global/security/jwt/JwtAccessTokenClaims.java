package band.platform.global.security.jwt;

public record JwtAccessTokenClaims(
	Long userId,
	String loginId
) {
}
