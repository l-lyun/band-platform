package band.platform.domain.user.repository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
	private static final String VALUE_DELIMITER = ":";
	private static final int HASH_INDEX = 0;
	private static final int ATTEMPTS_INDEX = 1;
	private static final int CODE_VALUE_PARTS = 2;

	private final StringRedisTemplate redisTemplate;

	@Override
	public void saveCode(Long userId, String codeHash, Duration ttl) {
		redisTemplate.opsForValue().set(codeKey(userId), encodeCode(codeHash, 0), ttl);
	}

	@Override
	public Optional<PasswordResetCode> findCode(Long userId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(codeKey(userId)))
			.map(this::decodeCode);
	}

	@Override
	public Optional<PasswordResetCode> incrementCodeAttempts(Long userId) {
		String key = codeKey(userId);
		String value = redisTemplate.opsForValue().get(key);
		if (value == null) {
			return Optional.empty();
		}

		PasswordResetCode code = decodeCode(value);
		PasswordResetCode updatedCode = new PasswordResetCode(code.codeHash(), code.attempts() + 1);
		Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
		if (ttlSeconds == null || ttlSeconds <= 0) {
			redisTemplate.delete(key);
			return Optional.empty();
		}

		redisTemplate.opsForValue().set(key, encodeCode(updatedCode.codeHash(), updatedCode.attempts()), Duration.ofSeconds(ttlSeconds));
		return Optional.of(updatedCode);
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

	private String encodeCode(String codeHash, int attempts) {
		return codeHash + VALUE_DELIMITER + attempts;
	}

	private PasswordResetCode decodeCode(String value) {
		String[] parts = value.split(VALUE_DELIMITER, CODE_VALUE_PARTS);
		if (parts.length != CODE_VALUE_PARTS || parts[HASH_INDEX].isBlank()) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
		}
		try {
			return new PasswordResetCode(parts[HASH_INDEX], Integer.parseInt(parts[ATTEMPTS_INDEX]));
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
		}
	}

	private String codeKey(Long userId) {
		return PASSWORD_RESET_CODE_KEY_PREFIX + userId;
	}

	private String tokenKey(String tokenHash) {
		return PASSWORD_RESET_TOKEN_KEY_PREFIX + tokenHash;
	}
}
