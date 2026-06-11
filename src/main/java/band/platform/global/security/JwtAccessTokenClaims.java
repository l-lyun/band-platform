package band.platform.global.security;

public record JwtAccessTokenClaims(
	Long userId,
	String loginId
) {
}
