package band.platform.domain.user.social;

import org.springframework.util.StringUtils;

import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

public record SocialOAuthState(String state, String nonce) {

	public SocialOAuthState {
		if (!StringUtils.hasText(state)) {
			throw new BusinessException(ErrorCode.AUTH_OAUTH_STATE_INVALID);
		}
		if (!StringUtils.hasText(nonce)) {
			throw new BusinessException(ErrorCode.AUTH_OAUTH_STATE_INVALID);
		}
	}

}
