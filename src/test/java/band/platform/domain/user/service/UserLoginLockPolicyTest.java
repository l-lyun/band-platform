package band.platform.domain.user.service;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import band.platform.domain.user.service.UserLoginLockPolicy.LoginFailureResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLoginLockPolicyTest {

	private static final String LOGIN_ID = "bandmaster";
	private static final String FAILURE_KEY = "login:failure:" + LOGIN_ID;
	private static final String LOCK_KEY = "login:lock:" + LOGIN_ID;
	private static final List<String> LOCK_KEYS = List.of(FAILURE_KEY, LOCK_KEY);

	@Mock
	private StringRedisTemplate redisTemplate;

	@InjectMocks
	private UserLoginLockPolicy userLoginLockPolicy;

	@Test
	@DisplayName("로그인 실패를 기록할 때 실패 키와 잠금 키를 함께 전달한다")
	void recordFirstFailure() {
		givenScriptResult(1L);

		LoginFailureResult result = userLoginLockPolicy.recordFailure(LOGIN_ID);

		assertThat(result.failureCount()).isEqualTo(1);
		assertThat(result.locked()).isFalse();
		verifyRecordFailureScript();
	}

	@Test
	@DisplayName("로그인 실패가 5회가 되면 잠금 상태를 반환한다")
	void recordFifthFailure() {
		givenScriptResult(5L);

		LoginFailureResult result = userLoginLockPolicy.recordFailure(LOGIN_ID);

		assertThat(result.failureCount()).isEqualTo(UserLoginLockPolicy.MAX_LOGIN_FAILURE_COUNT);
		assertThat(result.locked()).isTrue();
		verifyRecordFailureScript();
	}

	@Test
	@DisplayName("로그인 실패 스크립트는 실패 키 삭제 후 잠금 키를 24시간 동안 생성한다")
	void recordFailureScript() {
		assertThat(UserLoginLockPolicy.RECORD_FAILURE_SCRIPT_TEXT)
			.contains("redis.call('DEL', failureKey)")
			.contains("redis.call('SET', lockKey, lockValue, 'EX', lockTtlSeconds)");
		assertThat(UserLoginLockPolicy.LOGIN_FAILURE_TTL).isEqualTo(Duration.ofHours(24));
		assertThat(UserLoginLockPolicy.LOGIN_LOCK_TTL).isEqualTo(Duration.ofHours(24));
	}

	@Test
	@DisplayName("잠금 키가 있으면 잠긴 상태로 판단한다")
	void isLocked() {
		when(redisTemplate.hasKey(LOCK_KEY)).thenReturn(true);

		assertThat(userLoginLockPolicy.isLocked(LOGIN_ID)).isTrue();
	}

	@Test
	@DisplayName("로그인 성공 후 실패 키와 잠금 키를 함께 삭제한다")
	void clear() {
		userLoginLockPolicy.clear(LOGIN_ID);

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
