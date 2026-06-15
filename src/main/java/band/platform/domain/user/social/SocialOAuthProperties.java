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
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "security.oauth2")
@Getter
@Setter
public class SocialOAuthProperties {

	private Duration stateTtl = Duration.ofMinutes(10);
	private Map<SocialProvider, Provider> providers = new EnumMap<>(SocialProvider.class);

	public Provider provider(SocialProvider provider) {
		if (provider == null || provider == SocialProvider.LOCAL) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED);
		}
		Provider properties = providers.get(provider);
		if (properties == null) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
		return properties;
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
