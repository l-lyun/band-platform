package band.platform.domain.user.social.client.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoTokenResponse(
	@JsonProperty("access_token")
	String accessToken,
	@JsonProperty("refresh_token")
	String refreshToken,
	@JsonProperty("token_type")
	String tokenType,
	@JsonProperty("expires_in")
	Long expiresIn
) {
}
