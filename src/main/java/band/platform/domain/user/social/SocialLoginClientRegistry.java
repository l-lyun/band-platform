package band.platform.domain.user.social;

import java.util.Map;
import java.util.Optional;

import band.platform.domain.user.entity.SocialProvider;

public record SocialLoginClientRegistry(Map<SocialProvider, SocialLoginClient> clients) {

	public SocialLoginClientRegistry {
		clients = Map.copyOf(clients);
	}

	public Optional<SocialLoginClient> findByProvider(SocialProvider provider) {
		return Optional.ofNullable(clients.get(provider));
	}

}
