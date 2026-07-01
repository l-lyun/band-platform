package band.platform.domain.user.repository;

import java.time.Duration;
import java.util.Optional;

import band.platform.domain.user.social.SocialPendingSignup;

public interface SocialPendingSignupRepository {

	void save(String token, SocialPendingSignup pendingSignup, Duration ttl);

	Optional<SocialPendingSignup> consume(String token);
}
