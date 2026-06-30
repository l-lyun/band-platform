package band.platform.domain.user.social.client.apple;

import org.springframework.util.StringUtils;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialUserInfo;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

public record AppleIdTokenClaims(
	String subject,
	String email
) {

	public AppleIdTokenClaims {
		if (!StringUtils.hasText(subject)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		}
	}

	public SocialUserInfo toSocialUserInfo() {
		return new SocialUserInfo(SocialProvider.APPLE, subject, email, null, null);
	}
}
