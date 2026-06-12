package band.platform.domain.user.dto;

public record UserTokenIssueResult(
	TokenResponse tokenResponse,
	String refreshToken,
	long refreshTokenMaxAgeSeconds
) {
}
