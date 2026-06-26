package band.platform.domain.user.social.client.naver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialAuthorizationCode;
import band.platform.domain.user.social.SocialLoginClient;
import band.platform.domain.user.social.SocialOAuthProperties;
import band.platform.domain.user.social.SocialUserInfo;
import band.platform.domain.user.social.client.common.ProviderRestClientFactory;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@Component
public class NaverSocialLoginClient implements SocialLoginClient {

	private static final String GRANT_TYPE = "authorization_code";
	private static final String BEARER = "Bearer ";

	private final RestClient restClient;
	private final SocialOAuthProperties socialOAuthProperties;

	@Autowired
	public NaverSocialLoginClient(RestClient.Builder restClientBuilder, SocialOAuthProperties socialOAuthProperties) {
		this(ProviderRestClientFactory.create(restClientBuilder), socialOAuthProperties);
	}

	NaverSocialLoginClient(RestClient restClient, SocialOAuthProperties socialOAuthProperties) {
		this.restClient = restClient;
		this.socialOAuthProperties = socialOAuthProperties;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.NAVER;
	}

	@Override
	public SocialUserInfo fetchUserInfo(SocialAuthorizationCode authorizationCode) {
		if (authorizationCode.provider() != SocialProvider.NAVER) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}

		SocialOAuthProperties.Provider properties = providerProperties();
		NaverTokenResponse tokenResponse = requestAccessToken(properties, authorizationCode);
		String accessToken = accessToken(tokenResponse);
		return requestUserInfo(properties, accessToken).toSocialUserInfo();
	}

	private SocialOAuthProperties.Provider providerProperties() {
		try {
			SocialOAuthProperties.Provider properties = socialOAuthProperties.provider(SocialProvider.NAVER);
			if (!properties.hasClientSecret()) {
				throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
			}
			return properties;
		} catch (BusinessException exception) {
			throw exception;
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
	}

	private NaverTokenResponse requestAccessToken(
		SocialOAuthProperties.Provider properties,
		SocialAuthorizationCode authorizationCode
	) {
		try {
			MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
			tokenRequest.add("grant_type", GRANT_TYPE);
			tokenRequest.add("client_id", properties.getClientId());
			tokenRequest.add("client_secret", properties.getClientSecret());
			tokenRequest.add("redirect_uri", authorizationCode.redirectUri());
			tokenRequest.add("code", authorizationCode.code());
			tokenRequest.add("state", authorizationCode.state());

			return restClient.post()
				.uri(properties.getTokenUri())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(tokenRequest)
				.retrieve()
				.body(NaverTokenResponse.class);
		} catch (RestClientResponseException exception) {
			throw toProviderException(exception);
		} catch (ResourceAccessException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
	}

	private String accessToken(NaverTokenResponse tokenResponse) {
		if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		}
		return tokenResponse.accessToken();
	}

	private NaverProfileResponse requestUserInfo(SocialOAuthProperties.Provider properties, String accessToken) {
		try {
			NaverProfileResponse profileResponse = restClient.get()
				.uri(properties.getUserInfoUri())
				.header("Authorization", BEARER + accessToken)
				.retrieve()
				.body(NaverProfileResponse.class);
			if (profileResponse == null) {
				throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
			}
			return profileResponse;
		} catch (BusinessException exception) {
			throw exception;
		} catch (RestClientResponseException exception) {
			throw toProviderException(exception);
		} catch (ResourceAccessException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		} catch (RestClientException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
	}

	private BusinessException toProviderException(RestClientResponseException exception) {
		if (exception.getStatusCode().isSameCodeAs(org.springframework.http.HttpStatus.BAD_REQUEST)) {
			return new BusinessException(ErrorCode.AUTH_CODE_INVALID);
		}
		if (exception.getStatusCode().is5xxServerError()) {
			return new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		}
		return new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
	}

}
