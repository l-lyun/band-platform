package band.platform.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialPendingSignup;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class RedisSocialPendingSignupRepositoryTest {

	private static final String TOKEN = "pending-signup-token";
	private static final String PENDING_SIGNUP_KEY = "oauth:pending-signup:" + TOKEN;
	private static final Duration PENDING_SIGNUP_TTL = Duration.ofMinutes(10);
	private static final SocialPendingSignup PENDING_SIGNUP = new SocialPendingSignup(
		SocialProvider.APPLE,
		"apple-subject",
		"bandmaster@example.com",
		null,
		null
	);

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private RedisSocialPendingSignupRepository redisSocialPendingSignupRepository;

	@Test
	@DisplayName("소셜 가입 대기 정보를 TTL과 함께 저장한다")
	void save() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		redisSocialPendingSignupRepository.save(TOKEN, PENDING_SIGNUP, PENDING_SIGNUP_TTL);

		verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
	}

	@Test
	@DisplayName("소셜 가입 대기 토큰은 한 번만 소비된다")
	void consumeOnce() {
		Map<String, String> redisValues = new HashMap<>();
		givenRedisBackedBy(redisValues);

		redisSocialPendingSignupRepository.save(TOKEN, PENDING_SIGNUP, PENDING_SIGNUP_TTL);

		assertThat(redisSocialPendingSignupRepository.consume(TOKEN))
			.contains(PENDING_SIGNUP);
		assertThat(redisSocialPendingSignupRepository.consume(TOKEN))
			.isEmpty();
		assertThat(redisValues).doesNotContainKey(PENDING_SIGNUP_KEY);
	}

	@Test
	@DisplayName("소셜 가입 대기 토큰이 비어 있으면 A18 에러를 반환한다")
	void blankToken() {
		assertThatThrownBy(() -> redisSocialPendingSignupRepository.consume(" "))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_PENDING_SIGNUP_INVALID)
			);
	}

	private void givenRedisBackedBy(Map<String, String> redisValues) {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		doAnswer(invocation -> {
			redisValues.put(invocation.getArgument(0), invocation.getArgument(1));
			return null;
		}).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
		when(valueOperations.getAndDelete(anyString()))
			.thenAnswer(invocation -> redisValues.remove(invocation.getArgument(0)));
	}

}
