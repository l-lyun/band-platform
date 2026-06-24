package band.platform.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialOAuthState;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class RedisSocialOAuthStateRepositoryTest {

	private static final String STATE = "oauth-state";
	private static final String NONCE = "oauth-nonce";
	private static final String STATE_KEY = "oauth:state:naver:" + STATE;
	private static final Duration STATE_TTL = Duration.ofMinutes(10);

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private RedisSocialOAuthStateRepository redisSocialOAuthStateRepository;

	@Test
	@DisplayName("제공자별 OAuth state와 nonce를 TTL과 함께 저장한다")
	void save() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		redisSocialOAuthStateRepository.save(SocialProvider.NAVER, new SocialOAuthState(STATE, NONCE), STATE_TTL);

		verify(valueOperations).set(STATE_KEY, NONCE, STATE_TTL);
	}

	@Test
	@DisplayName("같은 제공자에 저장된 state를 소비하면 nonce를 반환한다")
	void consume() {
		when(redisTemplate.execute(
			ArgumentMatchers.<RedisScript<String>>any(),
			eq(List.of(STATE_KEY))
		)).thenReturn(NONCE);

		Optional<SocialOAuthState> oauthState = redisSocialOAuthStateRepository.consume(SocialProvider.NAVER, STATE);

		assertThat(oauthState).contains(new SocialOAuthState(STATE, NONCE));
	}

	@Test
	@DisplayName("같은 제공자에 저장된 state가 없으면 빈 Optional을 반환한다")
	void consumeMissingState() {
		when(redisTemplate.execute(
			ArgumentMatchers.<RedisScript<String>>any(),
			eq(List.of(STATE_KEY))
		)).thenReturn(null);

		assertThat(redisSocialOAuthStateRepository.consume(SocialProvider.NAVER, STATE)).isEmpty();
	}

	@Test
	@DisplayName("state가 비어 있으면 A10 에러를 반환한다")
	void consumeBlankState() {
		assertThatThrownBy(() -> redisSocialOAuthStateRepository.consume(SocialProvider.NAVER, " "))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_OAUTH_STATE_INVALID)
			);
	}

	@Test
	@DisplayName("LOCAL 제공자로 state를 소비하면 A09 에러를 반환한다")
	void consumeLocalProvider() {
		assertThatThrownBy(() -> redisSocialOAuthStateRepository.consume(SocialProvider.LOCAL, STATE))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED)
			);
	}

	@Test
	@DisplayName("state 소비 스크립트는 nonce 조회 후 state 키를 삭제한다")
	void consumeScript() {
		assertThat(RedisSocialOAuthStateRepository.CONSUME_OAUTH_STATE_SCRIPT_TEXT)
			.contains("redis.call('GET', stateKey)")
			.contains("redis.call('DEL', stateKey)");
	}

}
