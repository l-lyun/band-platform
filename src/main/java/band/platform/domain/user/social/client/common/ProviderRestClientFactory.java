package band.platform.domain.user.social.client.common;

import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class ProviderRestClientFactory {

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	private ProviderRestClientFactory() {
	}

	public static RestClient create(RestClient.Builder builder) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(TIMEOUT);
		requestFactory.setReadTimeout(TIMEOUT);
		return builder.requestFactory(requestFactory).build();
	}

}
