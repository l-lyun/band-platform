package band.platform.domain.user.repository;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class RedisUserRefreshTokenRepository implements UserRefreshTokenRepository {

	private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
	private static final String REFRESH_TOKEN_VALUE = "ACTIVE";
	static final String ROTATE_REFRESH_TOKEN_SCRIPT_TEXT = """
		local oldKey = KEYS[1]
		local newKey = KEYS[2]
		local tokenValue = ARGV[1]
		local ttlSeconds = tonumber(ARGV[2])

		if redis.call('EXISTS', oldKey) == 0 then
			return 0
		end

		redis.call('DEL', oldKey)
		redis.call('SET', newKey, tokenValue, 'EX', ttlSeconds)
		return 1
		""";
	private static final RedisScript<Long> ROTATE_REFRESH_TOKEN_SCRIPT =
		RedisScript.of(ROTATE_REFRESH_TOKEN_SCRIPT_TEXT, Long.class);

	private final StringRedisTemplate redisTemplate;

	public RedisUserRefreshTokenRepository(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(Long userId, String tokenId, Duration ttl) {
		redisTemplate.opsForValue()
			.set(refreshTokenKey(userId, tokenId), REFRESH_TOKEN_VALUE, ttl);
	}

	@Override
	public boolean rotate(Long userId, String oldTokenId, String newTokenId, Duration ttl) {
		Long result = redisTemplate.execute(
			ROTATE_REFRESH_TOKEN_SCRIPT,
			List.of(refreshTokenKey(userId, oldTokenId), refreshTokenKey(userId, newTokenId)),
			REFRESH_TOKEN_VALUE,
			String.valueOf(ttl.toSeconds())
		);

		return Long.valueOf(1).equals(result);
	}

	@Override
	public void delete(Long userId, String tokenId) {
		redisTemplate.delete(refreshTokenKey(userId, tokenId));
	}

	private String refreshTokenKey(Long userId, String tokenId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId must not be null.");
		}
		if (!StringUtils.hasText(tokenId)) {
			throw new IllegalArgumentException("tokenId must not be blank.");
		}
		return REFRESH_TOKEN_KEY_PREFIX + userId + ":" + tokenId;
	}

}
