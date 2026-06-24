package band.platform.global.security.jwt;

public record JwtRefreshTokenClaims(
	Long userId,
	String loginId,
	String tokenId
) {
}
