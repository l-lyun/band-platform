package band.platform.global.security;

public record JwtAuthenticationPrincipal(
	Long userId,
	String loginId
) {
}
