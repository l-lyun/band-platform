package band.platform.domain.user.repository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisUserPasswordResetRepository implements UserPasswordResetRepository {

	private static final String PASSWORD_RESET_CODE_KEY_PREFIX = "auth:password-reset:code:";
	private static final String PASSWORD_RESET_TOKEN_KEY_PREFIX = "auth:password-reset:token:";
	private static final String CODE_HASH_FIELD = "codeHash";
	private static final String ATTEMPTS_FIELD = "attempts";

	private final StringRedisTemplate redisTemplate;

	@Override
	public void saveCode(Long userId, String codeHash, Duration ttl) {
		String key = codeKey(userId);
		redisTemplate.boundHashOps(key)
			.putAll(Map.of(
				CODE_HASH_FIELD, codeHash,
				ATTEMPTS_FIELD, "0"
			));
		redisTemplate.expire(key, ttl);
	}

	@Override
	public Optional<PasswordResetCode> findCode(Long userId) {
		Map<Object, Object> values = codeOperations(codeKey(userId)).entries();
		if (values.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(decodeCode(values));
	}

	@Override
	public Optional<PasswordResetCode> incrementCodeAttempts(Long userId) {
		String key = codeKey(userId);
		if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
			return Optional.empty();
		}

		BoundHashOperations<String, Object, Object> operations = codeOperations(key);
		Long attempts = operations.increment(ATTEMPTS_FIELD, 1);
		Object codeHash = operations.get(CODE_HASH_FIELD);
		if (codeHash == null) {
			redisTemplate.delete(key);
			return Optional.empty();
		}
		if (String.valueOf(codeHash).isBlank() || attempts == null) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
		}

		Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
		if (ttlSeconds == null || ttlSeconds <= 0) {
			redisTemplate.delete(key);
			return Optional.empty();
		}

		return Optional.of(new PasswordResetCode(String.valueOf(codeHash), Math.toIntExact(attempts)));
	}

	@Override
	public void deleteCode(Long userId) {
		redisTemplate.delete(codeKey(userId));
	}

	@Override
	public void saveToken(String tokenHash, Long userId, Duration ttl) {
		redisTemplate.opsForValue().set(tokenKey(tokenHash), String.valueOf(userId), ttl);
	}

	@Override
	public Optional<Long> consumeToken(String tokenHash) {
		String userId = redisTemplate.opsForValue().getAndDelete(tokenKey(tokenHash));
		if (userId == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(Long.valueOf(userId));
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
		}
	}

	private PasswordResetCode decodeCode(Map<Object, Object> values) {
		Object codeHash = values.get(CODE_HASH_FIELD);
		Object attempts = values.get(ATTEMPTS_FIELD);
		if (codeHash == null || String.valueOf(codeHash).isBlank() || attempts == null) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
		}
		try {
			return new PasswordResetCode(String.valueOf(codeHash), Integer.parseInt(String.valueOf(attempts)));
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
		}
	}

	private BoundHashOperations<String, Object, Object> codeOperations(String key) {
		return redisTemplate.boundHashOps(key);
	}

	private String codeKey(Long userId) {
		return PASSWORD_RESET_CODE_KEY_PREFIX + userId;
	}

	private String tokenKey(String tokenHash) {
		return PASSWORD_RESET_TOKEN_KEY_PREFIX + tokenHash;
	}
}
