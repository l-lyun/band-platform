package band.platform.global.security.jwt;

public record JwtAuthenticationPrincipal(
	Long userId,
	String loginId
) {
}
