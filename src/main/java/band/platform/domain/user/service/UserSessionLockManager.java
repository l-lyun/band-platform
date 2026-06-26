package band.platform.domain.user.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

@Component
public class UserSessionLockManager {

	private final ConcurrentHashMap<Long, UserLock> locks = new ConcurrentHashMap<>();

	public void withLock(Long userId, Runnable operation) {
		withLock(userId, () -> {
			operation.run();
			return null;
		});
	}

	public <T> T withLock(Long userId, Supplier<T> operation) {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(operation, "operation must not be null");

		UserLock userLock = locks.compute(userId, (key, lock) -> {
			if (lock == null) {
				return new UserLock();
			}
			lock.retain();
			return lock;
		});

		userLock.lock();
		try {
			return operation.get();
		} finally {
			userLock.unlock();
			locks.computeIfPresent(userId, (key, lock) -> {
				if (lock != userLock) {
					return lock;
				}
				return lock.release() == 0 ? null : lock;
			});
		}
	}

	private static class UserLock {

		private final ReentrantLock lock = new ReentrantLock();
		private int references = 1;

		private void retain() {
			references += 1;
		}

		private int release() {
			references -= 1;
			return references;
		}

		private void lock() {
			lock.lock();
		}

		private void unlock() {
			lock.unlock();
		}
	}
}
