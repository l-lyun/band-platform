package band.platform.domain.user.repository;

import java.time.Duration;
import java.util.Optional;

import band.platform.domain.user.social.SocialOAuthState;

public interface SocialOAuthStateRepository {

	void save(SocialOAuthState oauthState, Duration ttl);

	Optional<SocialOAuthState> consume(String state);
}
