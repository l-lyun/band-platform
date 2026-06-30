package band.platform.domain.user.social.client.apple;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record AppleTokenResponse(
	@JsonProperty("id_token")
	String idToken
) {
}
