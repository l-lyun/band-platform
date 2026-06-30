package band.platform.domain.user.social.client.apple;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.http.HttpStatus;
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
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppleSocialLoginClient implements SocialLoginClient {

	private static final String GRANT_TYPE = "authorization_code";
	private static final String INVALID_GRANT = "invalid_grant";
	private static final String INVALID_CLIENT = "invalid_client";

	private final RestClient restClient;
	private final SocialOAuthProperties socialOAuthProperties;
	private final AppleClientSecretGenerator clientSecretGenerator;
	private final AppleIdTokenVerifier idTokenVerifier;

	@Override
	public SocialProvider provider() {
		return SocialProvider.APPLE;
	}

	@Override
	public SocialUserInfo fetchUserInfo(SocialAuthorizationCode authorizationCode) {
		if (authorizationCode.provider() != SocialProvider.APPLE) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}

		SocialOAuthProperties.Provider properties = providerProperties();
		AppleTokenResponse tokenResponse = requestToken(properties, authorizationCode);
		AppleIdTokenClaims claims = idTokenVerifier.verifyClaims(
			idToken(tokenResponse),
			properties,
			authorizationCode.nonce()
		);
		return claims.toSocialUserInfo();
	}

	private SocialOAuthProperties.Provider providerProperties() {
		try {
			return socialOAuthProperties.provider(SocialProvider.APPLE);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
	}

	private AppleTokenResponse requestToken(
		SocialOAuthProperties.Provider properties,
		SocialAuthorizationCode authorizationCode
	) {
		try {
			return restClient.post()
				.uri(tokenUri(properties))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(tokenRequest(properties, authorizationCode))
				.retrieve()
				.body(AppleTokenResponse.class);
		} catch (RestClientException exception) {
			throw toProviderException(exception);
		}
	}

	private MultiValueMap<String, String> tokenRequest(
		SocialOAuthProperties.Provider properties,
		SocialAuthorizationCode authorizationCode
	) {
		MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
		tokenRequest.add("grant_type", GRANT_TYPE);
		tokenRequest.add("client_id", properties.getClientId());
		tokenRequest.add("client_secret", clientSecretGenerator.generate(properties));
		tokenRequest.add("redirect_uri", authorizationCode.redirectUri());
		tokenRequest.add("code", authorizationCode.code());
		return tokenRequest;
	}

	private URI tokenUri(SocialOAuthProperties.Provider properties) {
		try {
			return URI.create(properties.getTokenUri());
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
	}

	private String idToken(AppleTokenResponse tokenResponse) {
		if (tokenResponse == null || !StringUtils.hasText(tokenResponse.idToken())) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		}
		return tokenResponse.idToken();
	}

	private BusinessException toProviderException(RestClientException exception) {
		if (exception instanceof RestClientResponseException responseException) {
			return toProviderResponseException(responseException);
		}
		if (exception instanceof ResourceAccessException) {
			return new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		}
		return new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
	}

	private BusinessException toProviderResponseException(RestClientResponseException exception) {
		if (exception.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
			return new BusinessException(tokenErrorCode(exception));
		}
		if (exception.getStatusCode().is5xxServerError()) {
			return new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		}
		return new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
	}

	private ErrorCode tokenErrorCode(RestClientResponseException exception) {
		String error = tokenError(exception);
		if (INVALID_GRANT.equals(error)) {
			return ErrorCode.AUTH_CODE_INVALID;
		}
		if (INVALID_CLIENT.equals(error)) {
			return ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID;
		}
		return ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID;
	}

	private String tokenError(RestClientResponseException exception) {
		try {
			AppleTokenErrorResponse response = exception.getResponseBodyAs(AppleTokenErrorResponse.class);
			return response == null ? null : response.error();
		} catch (RestClientException | IllegalStateException exceptionToIgnore) {
			return null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record AppleTokenErrorResponse(String error) {
	}
}
