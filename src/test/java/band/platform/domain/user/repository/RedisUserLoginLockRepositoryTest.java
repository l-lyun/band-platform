package band.platform.domain.user.repository;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisUserLoginLockRepositoryTest {

	private static final String LOGIN_ID = "bandmaster";
	private static final String FAILURE_KEY = "login:failure:" + LOGIN_ID;
	private static final String LOCK_KEY = "login:lock:" + LOGIN_ID;
	private static final List<String> LOCK_KEYS = List.of(FAILURE_KEY, LOCK_KEY);

	@Mock
	private StringRedisTemplate redisTemplate;

	@InjectMocks
	private RedisUserLoginLockRepository redisUserLoginLockRepository;

	@Test
	@DisplayName("로그인 실패 횟수를 증가시킬 때 실패 키와 잠금 키를 함께 전달한다")
	void increaseFailureCountAndLockIfThresholdReached() {
		givenScriptResult(1L);

		int failureCount = redisUserLoginLockRepository.increaseFailureCountAndLockIfThresholdReached(
			LOGIN_ID,
			5,
			Duration.ofHours(24),
			Duration.ofHours(24)
		);

		assertThat(failureCount).isEqualTo(1);
		verifyRecordFailureScript();
	}

	@Test
	@DisplayName("로그인 실패 스크립트는 실패 키 삭제 후 잠금 키를 24시간 동안 생성한다")
	void recordFailureScript() {
		assertThat(RedisUserLoginLockRepository.RECORD_FAILURE_SCRIPT_TEXT)
			.contains("redis.call('DEL', failureKey)")
			.contains("redis.call('SET', lockKey, lockValue, 'EX', lockTtlSeconds)");
	}

	@Test
	@DisplayName("잠금 키가 있으면 잠긴 상태로 판단한다")
	void existsLock() {
		when(redisTemplate.hasKey(LOCK_KEY)).thenReturn(true);

		assertThat(redisUserLoginLockRepository.existsLock(LOGIN_ID)).isTrue();
	}

	@Test
	@DisplayName("실패 횟수와 잠금 키를 함께 삭제한다")
	void deleteFailureCountAndLock() {
		redisUserLoginLockRepository.deleteFailureCountAndLock(LOGIN_ID);

		verify(redisTemplate).delete(FAILURE_KEY);
		verify(redisTemplate).delete(LOCK_KEY);
	}

	private void givenScriptResult(Long failureCount) {
		when(redisTemplate.execute(
			ArgumentMatchers.<RedisScript<Long>>any(),
			eq(LOCK_KEYS),
			eq("5"),
			eq("86400"),
			eq("86400"),
			eq("LOCKED")
		)).thenReturn(failureCount);
	}

	private void verifyRecordFailureScript() {
		verify(redisTemplate).execute(
			ArgumentMatchers.<RedisScript<Long>>any(),
			eq(LOCK_KEYS),
			eq("5"),
			eq("86400"),
			eq("86400"),
			eq("LOCKED")
		);
	}

}
