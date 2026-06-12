package band.platform.domain.user.dto;

public record TokenResponse(
	String accessToken,
	String tokenType,
	long expiresIn
) {

	private static final String BEARER_TYPE = "Bearer";

	public static TokenResponse bearer(String accessToken, long expiresIn) {
		return new TokenResponse(accessToken, BEARER_TYPE, expiresIn);
	}

}
