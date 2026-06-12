package band.platform.domain.user.repository;

import java.time.Duration;

public interface UserRefreshTokenRepository {

	void save(Long userId, String tokenId, Duration ttl);

	boolean rotate(Long userId, String oldTokenId, String newTokenId, Duration ttl);

	void delete(Long userId, String tokenId);

}
