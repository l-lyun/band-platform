package band.platform.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import band.platform.domain.user.dto.SocialLoginRequest;
import band.platform.domain.user.dto.SocialLoginResponse;
import band.platform.domain.user.dto.SocialLoginStartRequest;
import band.platform.domain.user.dto.SocialLoginStartResponse;
import band.platform.domain.user.dto.SocialSignupRequest;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.entity.SocialAccount;
import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.SocialAccountRepository;
import band.platform.domain.user.repository.UserRepository;
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
	private final UserRepository userRepository;
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
		SocialUserInfo socialUserInfo = fetchSocialUserInfo(
			request.provider(),
			request.code(),
			request.state(),
			request.redirectUri()
		);

		return socialAccountRepository
			.findByProviderAndProviderSubject(socialUserInfo.provider(), socialUserInfo.providerSubject())
			.map(socialAccount -> linkedLogin(socialAccount, socialUserInfo))
			.orElseGet(() -> signupRequired(socialUserInfo));
	}

	@Transactional
	public UserSocialLoginResult signup(SocialSignupRequest request) {
		SocialUserInfo socialUserInfo = fetchSocialUserInfo(
			request.provider(),
			request.code(),
			request.state(),
			request.redirectUri()
		);

		validateProviderEmail(socialUserInfo);
		validateProviderAccountNotLinked(socialUserInfo);
		validatePrivacyPolicyAgreement(request.privacyPolicyAgreed());

		User user = userRepository.findByEmail(socialUserInfo.email())
			.map(existingUser -> existingLinkTarget(existingUser, socialUserInfo.provider(), request.linkExistingAccount()))
			.orElseGet(() -> createSocialUser(request, socialUserInfo));

		socialAccountRepository.save(SocialAccount.connect(
			user,
			socialUserInfo.provider(),
			socialUserInfo.providerSubject()
		));

		UserTokenIssueResult tokenIssueResult = userTokenService.issue(user.getId(), tokenSubject(user));
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

	private SocialUserInfo fetchSocialUserInfo(
		SocialProvider provider,
		String code,
		String state,
		String redirectUri
	) {
		SocialOAuthState oauthState = socialOAuthStateService.consume(provider, state)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_OAUTH_STATE_INVALID));
		SocialAuthorizationCode authorizationCode = new SocialAuthorizationCode(
			provider,
			code,
			state,
			redirectUri,
			oauthState.nonce()
		);

		SocialLoginClient socialLoginClient = socialLoginClientResolver.resolve(provider);
		return socialLoginClient.fetchUserInfo(authorizationCode);
	}

	private UserSocialLoginResult linkedLogin(SocialAccount socialAccount, SocialUserInfo socialUserInfo) {
		User user = socialAccount.getUser();
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}

		UserTokenIssueResult tokenIssueResult = userTokenService.issue(user.getId(), tokenSubject(user));
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

	private void validateProviderAccountNotLinked(SocialUserInfo socialUserInfo) {
		if (socialAccountRepository.existsByProviderAndProviderSubject(
			socialUserInfo.provider(),
			socialUserInfo.providerSubject()
		)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_ACCOUNT_ALREADY_LINKED);
		}
	}

	private void validateProviderEmail(SocialUserInfo socialUserInfo) {
		if (socialUserInfo.email() == null || socialUserInfo.email().isBlank()) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_USER_INFO_INVALID);
		}
	}

	private void validatePrivacyPolicyAgreement(Boolean privacyPolicyAgreed) {
		if (!Boolean.TRUE.equals(privacyPolicyAgreed)) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
	}

	private User existingLinkTarget(User user, SocialProvider provider, boolean linkExistingAccount) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}
		if (!linkExistingAccount) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_ACCOUNT_LINK_REQUIRED);
		}
		if (socialAccountRepository.existsByUserAndProvider(user, provider)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_ACCOUNT_ALREADY_LINKED);
		}
		return user;
	}

	private User createSocialUser(SocialSignupRequest request, SocialUserInfo socialUserInfo) {
		return userRepository.save(User.createSocialUser(
			socialUserInfo.name(),
			socialUserInfo.email(),
			request.phoneNumber(),
			socialUserInfo.provider(),
			request.privacyPolicyAgreed(),
			request.marketingPolicyAgreed()
		));
	}

	private String tokenSubject(User user) {
		if (user.getLoginId() != null && !user.getLoginId().isBlank()) {
			return user.getLoginId();
		}
		return user.getEmail();
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
