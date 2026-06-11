package band.platform.domain.user.repository;

import java.time.Duration;

public interface UserLoginLockRepository {

	boolean existsLock(String loginId);

	int increaseFailureCountAndLockIfThresholdReached(
		String loginId,
		int maxFailureCount,
		Duration failureTtl,
		Duration lockTtl
	);

	void deleteFailureCountAndLock(String loginId);

}
