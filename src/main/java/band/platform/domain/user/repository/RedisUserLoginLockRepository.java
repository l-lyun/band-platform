package band.platform.domain.user.repository;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisUserLoginLockRepository implements UserLoginLockRepository {

	private static final String LOGIN_FAILURE_KEY_PREFIX = "login:failure:";
	private static final String LOGIN_LOCK_KEY_PREFIX = "login:lock:";
	private static final String LOCK_VALUE = "LOCKED";
	static final String RECORD_FAILURE_SCRIPT_TEXT = """
		local failureKey = KEYS[1]
		local lockKey = KEYS[2]
		local maxFailureCount = tonumber(ARGV[1])
		local failureTtlSeconds = tonumber(ARGV[2])
		local lockTtlSeconds = tonumber(ARGV[3])
		local lockValue = ARGV[4]

		if redis.call('EXISTS', lockKey) == 1 then
			return maxFailureCount
		end

		local failureCount = redis.call('INCR', failureKey)
		if failureCount == 1 then
			redis.call('EXPIRE', failureKey, failureTtlSeconds)
		end

		if failureCount >= maxFailureCount then
			redis.call('DEL', failureKey)
			redis.call('SET', lockKey, lockValue, 'EX', lockTtlSeconds)
			return maxFailureCount
		end

		return failureCount
		""";
	private static final RedisScript<Long> RECORD_FAILURE_SCRIPT = RedisScript.of(RECORD_FAILURE_SCRIPT_TEXT, Long.class);

	private final StringRedisTemplate redisTemplate;

	@Override
	public boolean existsLock(String loginId) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(loginLockKey(loginId)));
	}

	@Override
	public int increaseFailureCountAndLockIfThresholdReached(
		String loginId,
		int maxFailureCount,
		Duration failureTtl,
		Duration lockTtl
	) {
		Long failureCount = redisTemplate.execute(
			RECORD_FAILURE_SCRIPT,
			List.of(loginFailureKey(loginId), loginLockKey(loginId)),
			String.valueOf(maxFailureCount),
			String.valueOf(failureTtl.toSeconds()),
			String.valueOf(lockTtl.toSeconds()),
			LOCK_VALUE
		);
		if (failureCount == null) {
			throw new IllegalStateException("Redis login failure script result is null.");
		}

		return failureCount.intValue();
	}

	@Override
	public void deleteFailureCountAndLock(String loginId) {
		redisTemplate.delete(loginFailureKey(loginId));
		redisTemplate.delete(loginLockKey(loginId));
	}

	private static String loginFailureKey(String loginId) {
		return LOGIN_FAILURE_KEY_PREFIX + requireLoginId(loginId);
	}

	private static String loginLockKey(String loginId) {
		return LOGIN_LOCK_KEY_PREFIX + requireLoginId(loginId);
	}

	private static String requireLoginId(String loginId) {
		if (!StringUtils.hasText(loginId)) {
			throw new IllegalArgumentException("loginId must not be blank.");
		}
		return loginId;
	}

}
