package band.platform.domain.user.social;

import band.platform.domain.user.entity.SocialProvider;

public interface SocialLoginClient {

	SocialProvider provider();

	SocialUserInfo fetchUserInfo(SocialAuthorizationCode authorizationCode);
}
