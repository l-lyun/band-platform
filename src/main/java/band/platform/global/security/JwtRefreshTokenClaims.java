package band.platform.global.security;

public record JwtRefreshTokenClaims(
	Long userId,
	String loginId,
	String tokenId
) {
}
