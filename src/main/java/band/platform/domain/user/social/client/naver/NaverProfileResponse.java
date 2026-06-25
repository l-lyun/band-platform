package band.platform.domain.user.social.client.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialUserInfo;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@JsonIgnoreProperties(ignoreUnknown = true)
record NaverProfileResponse(
	@JsonProperty("resultcode")
	String resultCode,
	String message,
	Response response
) {

	SocialUserInfo toSocialUserInfo() {
		if (response == null) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_USER_INFO_INVALID);
		}
		return new SocialUserInfo(
			SocialProvider.NAVER,
			response.id(),
			response.email(),
			response.name(),
			response.profileImage()
		);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Response(
		String id,
		String email,
		String name,
		@JsonProperty("profile_image")
		String profileImage
	) {
	}

}
