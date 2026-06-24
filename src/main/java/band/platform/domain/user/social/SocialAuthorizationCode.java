package band.platform.domain.user.social;

import org.springframework.util.StringUtils;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

public record SocialAuthorizationCode(
	SocialProvider provider,
	String code,
	String state,
	String redirectUri,
	String nonce
) {

	public SocialAuthorizationCode(
		SocialProvider provider,
		String code,
		String state,
		String redirectUri
	) {
		this(provider, code, state, redirectUri, null);
	}

	public SocialAuthorizationCode {
		requireSocialProvider(provider);
		requireText(code, ErrorCode.AUTH_CODE_INVALID);
		requireText(state, ErrorCode.AUTH_OAUTH_STATE_INVALID);
		requireText(redirectUri, ErrorCode.AUTH_REDIRECT_URI_INVALID);
		requireTextIfPresent(nonce, ErrorCode.AUTH_OAUTH_STATE_INVALID);
	}

	private static void requireSocialProvider(SocialProvider provider) {
		if (provider == null || provider == SocialProvider.LOCAL) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED);
		}
	}

	private static void requireText(String value, ErrorCode errorCode) {
		if (!StringUtils.hasText(value)) {
			throw new BusinessException(errorCode);
		}
	}

	private static void requireTextIfPresent(String value, ErrorCode errorCode) {
		if (value != null && !StringUtils.hasText(value)) {
			throw new BusinessException(errorCode);
		}
	}

}
