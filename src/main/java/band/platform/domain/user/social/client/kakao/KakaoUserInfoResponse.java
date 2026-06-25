package band.platform.domain.user.social.client.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialUserInfo;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoUserInfoResponse(
	Long id,
	@JsonProperty("kakao_account")
	KakaoAccount kakaoAccount
) {

	SocialUserInfo toSocialUserInfo() {
		if (id == null) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_USER_INFO_INVALID);
		}

		return new SocialUserInfo(
			SocialProvider.KAKAO,
			String.valueOf(id),
			email(),
			name(),
			profileImageUrl()
		);
	}

	private String email() {
		if (kakaoAccount == null) {
			return null;
		}
		return kakaoAccount.email();
	}

	private String name() {
		if (kakaoAccount == null) {
			return null;
		}
		if (kakaoAccount.name() != null) {
			return kakaoAccount.name();
		}
		if (kakaoAccount.profile() == null) {
			return null;
		}
		return kakaoAccount.profile().nickname();
	}

	private String profileImageUrl() {
		if (kakaoAccount == null || kakaoAccount.profile() == null) {
			return null;
		}
		return kakaoAccount.profile().profileImageUrl();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record KakaoAccount(
		String email,
		String name,
		Profile profile
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Profile(
		String nickname,
		@JsonProperty("profile_image_url")
		String profileImageUrl
	) {
	}

}
