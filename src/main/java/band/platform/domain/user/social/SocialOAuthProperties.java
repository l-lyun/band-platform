package band.platform.domain.user.social;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "security.oauth2")
@Getter
@Setter
public class SocialOAuthProperties {

	private Duration stateTtl = Duration.ofMinutes(10);
	private Map<SocialProvider, Provider> providers = new EnumMap<>(SocialProvider.class);

	@PostConstruct
	void validateBoundProviders() {
		providers.forEach(SocialOAuthProperties::validateProvider);
	}

	public Provider provider(SocialProvider provider) {
		if (provider == null || provider == SocialProvider.LOCAL) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED);
		}
		Provider properties = providers.get(provider);
		if (properties == null) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
		validateProvider(provider, properties);
		return properties;
	}

	public void setProviders(Map<SocialProvider, Provider> providers) {
		if (providers == null) {
			this.providers = new EnumMap<>(SocialProvider.class);
			return;
		}

		EnumMap<SocialProvider, Provider> validatedProviders = new EnumMap<>(SocialProvider.class);
		for (Map.Entry<SocialProvider, Provider> entry : providers.entrySet()) {
			validateProvider(entry.getKey(), entry.getValue());
			validatedProviders.put(entry.getKey(), entry.getValue());
		}
		this.providers = validatedProviders;
	}

	private static void validateProvider(SocialProvider provider, Provider properties) {
		if (provider == null || provider == SocialProvider.LOCAL || properties == null) {
			throw new IllegalArgumentException("OAuth provider configuration is invalid.");
		}
		requireText(properties.clientId, provider, "client-id");
		requireText(properties.redirectUri, provider, "redirect-uri");
		requireText(properties.authorizationUri, provider, "authorization-uri");
		requireText(properties.tokenUri, provider, "token-uri");

		if (provider == SocialProvider.NAVER || provider == SocialProvider.KAKAO) {
			requireText(properties.userInfoUri, provider, "user-info-uri");
		}
		if (provider == SocialProvider.APPLE) {
			requireText(properties.jwkSetUri, provider, "jwk-set-uri");
			requireText(properties.teamId, provider, "team-id");
			requireText(properties.keyId, provider, "key-id");
			requireText(properties.privateKey, provider, "private-key");
		}
	}

	private static void requireText(String value, SocialProvider provider, String propertyName) {
		if (!StringUtils.hasText(value)) {
			throw new IllegalArgumentException(provider.name().toLowerCase() + "." + propertyName + " must not be blank.");
		}
	}

	@Getter
	@Setter
	public static class Provider {
		private String clientId;
		private String clientSecret;
		private String redirectUri;
		private String authorizationUri;
		private String tokenUri;
		private String userInfoUri;
		private String jwkSetUri;
		private String teamId;
		private String keyId;
		private String privateKey;
		private List<String> scopes = List.of();
		private boolean openId;

		public boolean hasClientSecret() {
			return StringUtils.hasText(clientSecret);
		}
	}

}
