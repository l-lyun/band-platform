package band.platform.domain.user.service;

import band.platform.domain.user.dto.SocialLoginResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;

public record UserSocialLoginResult(
	SocialLoginResponse response,
	UserTokenIssueResult tokenIssueResult
) {
}
