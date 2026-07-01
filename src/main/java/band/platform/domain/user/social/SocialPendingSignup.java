package band.platform.domain.user.social;

import band.platform.domain.user.entity.SocialProvider;

public record SocialPendingSignup(
	SocialProvider provider,
	String providerSubject,
	String email,
	String name,
	String profileImageUrl
) {

	public static SocialPendingSignup from(SocialUserInfo socialUserInfo) {
		return new SocialPendingSignup(
			socialUserInfo.provider(),
			socialUserInfo.providerSubject(),
			socialUserInfo.email(),
			socialUserInfo.name(),
			socialUserInfo.profileImageUrl()
		);
	}

	public SocialUserInfo toSocialUserInfo() {
		return new SocialUserInfo(provider, providerSubject, email, name, profileImageUrl);
	}

}
