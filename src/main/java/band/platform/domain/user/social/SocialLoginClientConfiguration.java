package band.platform.domain.user.social;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@Configuration(proxyBeanMethods = false)
class SocialLoginClientConfiguration {

	@Bean
	RestClient.Builder socialProviderRestClientBuilder() {
		return RestClient.builder();
	}

	@Bean
	SocialLoginClientRegistry socialLoginClientRegistry(List<SocialLoginClient> clients) {
		return new SocialLoginClientRegistry(toClientMap(clients));
	}

	private Map<SocialProvider, SocialLoginClient> toClientMap(List<SocialLoginClient> clients) {
		Map<SocialProvider, SocialLoginClient> clientMap = new EnumMap<>(SocialProvider.class);
		for (SocialLoginClient client : clients) {
			SocialProvider provider = client.provider();
			if (provider == null || provider == SocialProvider.LOCAL) {
				throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
			}
			if (clientMap.put(provider, client) != null) {
				throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
			}
		}
		return clientMap;
	}

}
