package band.platform.domain.user.social.client.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ProviderRestClientConfig {

	@Bean
	RestClient socialProviderRestClient(RestClient.Builder restClientBuilder) {
		return ProviderRestClientFactory.create(restClientBuilder);
	}
}
