package band.platform.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import band.platform.domain.user.dto.SocialLoginRequest;
import band.platform.domain.user.dto.SocialLoginResponse;
import band.platform.domain.user.dto.SocialLoginStartRequest;
import band.platform.domain.user.dto.SocialLoginStartResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.entity.SocialAccount;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.SocialAccountRepository;
import band.platform.domain.user.social.SocialAuthorizationCode;
import band.platform.domain.user.social.SocialLoginClient;
import band.platform.domain.user.social.SocialLoginClientResolver;
import band.platform.domain.user.social.SocialOAuthProperties;
import band.platform.domain.user.social.SocialOAuthState;
import band.platform.domain.user.social.SocialOAuthStateService;
import band.platform.domain.user.social.SocialUserInfo;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSocialLoginService {

	private final SocialLoginClientResolver socialLoginClientResolver;
	private final SocialOAuthStateService socialOAuthStateService;
	private final SocialOAuthProperties socialOAuthProperties;
	private final SocialAccountRepository socialAccountRepository;
	private final UserTokenService userTokenService;

	public SocialLoginStartResponse start(SocialLoginStartRequest request) {
		SocialOAuthState oauthState = socialOAuthStateService.issue(request.provider());
		SocialOAuthProperties.Provider providerProperties = socialOAuthProperties.provider(request.provider());
		return new SocialLoginStartResponse(
			request.provider(),
			authorizationUrl(providerProperties, oauthState),
			oauthState.state()
		);
	}

	@Transactional
	public UserSocialLoginResult signIn(SocialLoginRequest request) {
		SocialOAuthState oauthState = socialOAuthStateService.consume(request.provider(), request.state())
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_OAUTH_STATE_INVALID));
		SocialAuthorizationCode authorizationCode = new SocialAuthorizationCode(
			request.provider(),
			request.code(),
			request.state(),
			request.redirectUri(),
			oauthState.nonce()
		);

		SocialLoginClient socialLoginClient = socialLoginClientResolver.resolve(request.provider());
		SocialUserInfo socialUserInfo = socialLoginClient.fetchUserInfo(authorizationCode);

		return socialAccountRepository
			.findByProviderAndProviderSubject(socialUserInfo.provider(), socialUserInfo.providerSubject())
			.map(socialAccount -> linkedLogin(socialAccount, socialUserInfo))
			.orElseGet(() -> signupRequired(socialUserInfo));
	}

	private UserSocialLoginResult linkedLogin(SocialAccount socialAccount, SocialUserInfo socialUserInfo) {
		User user = socialAccount.getUser();
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}

		UserTokenIssueResult tokenIssueResult = userTokenService.issue(user.getId(), user.getLoginId());
		return new UserSocialLoginResult(
			SocialLoginResponse.linked(
				socialUserInfo.provider(),
				socialUserInfo.email(),
				socialUserInfo.name(),
				socialUserInfo.profileImageUrl(),
				tokenIssueResult.tokenResponse()
			),
			tokenIssueResult
		);
	}

	private UserSocialLoginResult signupRequired(SocialUserInfo socialUserInfo) {
		return new UserSocialLoginResult(
			SocialLoginResponse.signupRequired(
				socialUserInfo.provider(),
				socialUserInfo.email(),
				socialUserInfo.name(),
				socialUserInfo.profileImageUrl()
			),
			null
		);
	}

	private String authorizationUrl(SocialOAuthProperties.Provider providerProperties, SocialOAuthState oauthState) {
		UriComponentsBuilder builder = UriComponentsBuilder
			.fromUriString(providerProperties.getAuthorizationUri())
			.queryParam("response_type", "code")
			.queryParam("client_id", providerProperties.getClientId())
			.queryParam("redirect_uri", providerProperties.getRedirectUri())
			.queryParam("state", oauthState.state());

		if (!providerProperties.getScopes().isEmpty()) {
			builder.queryParam("scope", String.join(" ", providerProperties.getScopes()));
		}
		if (providerProperties.isOpenId()) {
			builder.queryParam("nonce", oauthState.nonce());
		}

		return builder.build().encode().toUriString();
	}

}
