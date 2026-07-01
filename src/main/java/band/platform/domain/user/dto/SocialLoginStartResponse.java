package band.platform.domain.user.dto;

import band.platform.domain.user.entity.SocialProvider;

public record SocialLoginStartResponse(
	SocialProvider provider,
	String authorizationUrl,
	String state
) {

}
