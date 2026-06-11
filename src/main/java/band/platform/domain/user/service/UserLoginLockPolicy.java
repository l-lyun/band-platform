package band.platform.domain.user.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import band.platform.domain.user.repository.UserLoginLockRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserLoginLockPolicy {

	public static final int MAX_LOGIN_FAILURE_COUNT = 5;
	public static final Duration LOGIN_FAILURE_TTL = Duration.ofHours(24);
	public static final Duration LOGIN_LOCK_TTL = Duration.ofHours(24);

	private final UserLoginLockRepository userLoginLockRepository;

	public boolean isLocked(String loginId) {
		return userLoginLockRepository.existsLock(loginId);
	}

	public LoginFailureResult recordFailure(String loginId) {
		int failureCount = userLoginLockRepository.increaseFailureCountAndLockIfThresholdReached(
			loginId,
			MAX_LOGIN_FAILURE_COUNT,
			LOGIN_FAILURE_TTL,
			LOGIN_LOCK_TTL
		);

		return new LoginFailureResult(failureCount, failureCount >= MAX_LOGIN_FAILURE_COUNT);
	}

	public void clear(String loginId) {
		userLoginLockRepository.deleteFailureCountAndLock(loginId);
	}

	public record LoginFailureResult(int failureCount, boolean locked) {

	}

}
