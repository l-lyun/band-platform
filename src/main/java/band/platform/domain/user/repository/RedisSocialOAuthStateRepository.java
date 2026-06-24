package band.platform.domain.user.repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialOAuthState;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisSocialOAuthStateRepository implements SocialOAuthStateRepository {

	private static final String OAUTH_STATE_KEY_PREFIX = "oauth:state:";
	static final String CONSUME_OAUTH_STATE_SCRIPT_TEXT = """
		local stateKey = KEYS[1]
		local nonce = redis.call('GET', stateKey)

		if not nonce then
			return nil
		end

		redis.call('DEL', stateKey)
		return nonce
		""";
	private static final RedisScript<String> CONSUME_OAUTH_STATE_SCRIPT =
		RedisScript.of(CONSUME_OAUTH_STATE_SCRIPT_TEXT, String.class);

	private final StringRedisTemplate redisTemplate;

	@Override
	public void save(SocialProvider provider, SocialOAuthState oauthState, Duration ttl) {
		redisTemplate.opsForValue()
			.set(oauthStateKey(provider, oauthState.state()), oauthState.nonce(), ttl);
	}

	@Override
	public Optional<SocialOAuthState> consume(SocialProvider provider, String state) {
		String nonce = redisTemplate.execute(CONSUME_OAUTH_STATE_SCRIPT, List.of(oauthStateKey(provider, state)));
		if (!StringUtils.hasText(nonce)) {
			return Optional.empty();
		}
		return Optional.of(new SocialOAuthState(state, nonce));
	}

	private static String oauthStateKey(SocialProvider provider, String state) {
		if (provider == null || provider == SocialProvider.LOCAL) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED);
		}
		if (!StringUtils.hasText(state)) {
			throw new BusinessException(ErrorCode.AUTH_OAUTH_STATE_INVALID);
		}
		return OAUTH_STATE_KEY_PREFIX + provider.name().toLowerCase() + ":" + state;
	}

}
