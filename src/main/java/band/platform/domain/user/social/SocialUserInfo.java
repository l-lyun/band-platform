package band.platform.domain.user.social;

import org.springframework.util.StringUtils;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

public record SocialUserInfo(
	SocialProvider provider,
	String providerSubject,
	String email,
	String name,
	String profileImageUrl
) {

	public SocialUserInfo {
		requireSocialProvider(provider);
		if (!StringUtils.hasText(providerSubject)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_USER_INFO_INVALID);
		}
	}

	private static void requireSocialProvider(SocialProvider provider) {
		if (provider == null || provider == SocialProvider.LOCAL) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED);
		}
	}

}
