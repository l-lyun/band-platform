package band.platform.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@ExtendWith(MockitoExtension.class)
class RedisUserPasswordResetRepositoryTest {

	private static final Long USER_ID = 1L;
	private static final String CODE_KEY = "auth:password-reset:code:" + USER_ID;
	private static final String TOKEN_HASH = "token-hash";
	private static final String TOKEN_KEY = "auth:password-reset:token:" + TOKEN_HASH;
	private static final String CODE_HASH = "$2a$10$encoded-reset-code";
	private static final Duration CODE_TTL = Duration.ofMinutes(5);
	private static final Duration TOKEN_TTL = Duration.ofMinutes(10);

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private BoundHashOperations<String, Object, Object> codeOperations;

	@InjectMocks
	private RedisUserPasswordResetRepository repository;

	@Test
	@DisplayName("비밀번호 재설정 코드를 해시와 시도 횟수로 TTL과 함께 저장한다")
	void saveCode() {
		when(redisTemplate.boundHashOps(CODE_KEY)).thenReturn(codeOperations);

		repository.saveCode(USER_ID, CODE_HASH, CODE_TTL);

		verify(codeOperations).putAll(Map.of(
			"codeHash", CODE_HASH,
			"attempts", "0"
		));
		verify(redisTemplate).expire(CODE_KEY, CODE_TTL);
	}

	@Test
	@DisplayName("저장된 비밀번호 재설정 코드 해시와 시도 횟수를 조회한다")
	void findCode() {
		when(redisTemplate.boundHashOps(CODE_KEY)).thenReturn(codeOperations);
		when(codeOperations.entries()).thenReturn(Map.of(
			"codeHash", CODE_HASH,
			"attempts", "2"
		));

		Optional<UserPasswordResetRepository.PasswordResetCode> code = repository.findCode(USER_ID);

		assertThat(code).contains(new UserPasswordResetRepository.PasswordResetCode(CODE_HASH, 2));
	}

	@Test
	@DisplayName("비밀번호 재설정 코드 실패 횟수를 기존 TTL 안에서 증가시킨다")
	void incrementCodeAttempts() {
		when(redisTemplate.hasKey(CODE_KEY)).thenReturn(true);
		when(redisTemplate.boundHashOps(CODE_KEY)).thenReturn(codeOperations);
		when(codeOperations.increment("attempts", 1)).thenReturn(2L);
		when(codeOperations.get("codeHash")).thenReturn(CODE_HASH);
		when(redisTemplate.getExpire(CODE_KEY, TimeUnit.SECONDS)).thenReturn(120L);

		Optional<UserPasswordResetRepository.PasswordResetCode> code = repository.incrementCodeAttempts(USER_ID);

		assertThat(code).contains(new UserPasswordResetRepository.PasswordResetCode(CODE_HASH, 2));
		verify(codeOperations).increment("attempts", 1);
	}

	@Test
	@DisplayName("비밀번호 재설정 코드가 없으면 실패 횟수를 증가시키지 않는다")
	void incrementMissingCodeAttempts() {
		when(redisTemplate.hasKey(CODE_KEY)).thenReturn(false);

		assertThat(repository.incrementCodeAttempts(USER_ID)).isEmpty();
	}

	@Test
	@DisplayName("만료 직후 실패 횟수만 증가된 비밀번호 재설정 코드 키는 삭제한다")
	void incrementCodeAttemptsDeletesExpiredRaceKey() {
		when(redisTemplate.hasKey(CODE_KEY)).thenReturn(true);
		when(redisTemplate.boundHashOps(CODE_KEY)).thenReturn(codeOperations);
		when(codeOperations.increment("attempts", 1)).thenReturn(1L);
		when(codeOperations.get("codeHash")).thenReturn(null);

		assertThat(repository.incrementCodeAttempts(USER_ID)).isEmpty();

		verify(redisTemplate).delete(CODE_KEY);
	}

	@Test
	@DisplayName("비밀번호 재설정 코드를 삭제한다")
	void deleteCode() {
		repository.deleteCode(USER_ID);

		verify(redisTemplate).delete(CODE_KEY);
	}

	@Test
	@DisplayName("비밀번호 재설정 토큰 해시를 사용자 식별자와 TTL로 저장한다")
	void saveToken() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		repository.saveToken(TOKEN_HASH, USER_ID, TOKEN_TTL);

		verify(valueOperations).set(TOKEN_KEY, String.valueOf(USER_ID), TOKEN_TTL);
	}

	@Test
	@DisplayName("비밀번호 재설정 토큰은 처음 소비할 때 사용자 식별자를 반환한다")
	void consumeToken() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.getAndDelete(TOKEN_KEY)).thenReturn(String.valueOf(USER_ID));

		Optional<Long> userId = repository.consumeToken(TOKEN_HASH);

		assertThat(userId).contains(USER_ID);
	}

	@Test
	@DisplayName("비밀번호 재설정 토큰은 두 번째 소비할 때 빈 Optional을 반환한다")
	void consumeTokenTwice() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.getAndDelete(TOKEN_KEY)).thenReturn(String.valueOf(USER_ID), (String)null);

		assertThat(repository.consumeToken(TOKEN_HASH)).contains(USER_ID);
		assertThat(repository.consumeToken(TOKEN_HASH)).isEmpty();
	}

	@Test
	@DisplayName("손상된 비밀번호 재설정 코드 값은 공통 서버 예외로 변환한다")
	void findCorruptCode() {
		when(redisTemplate.boundHashOps(CODE_KEY)).thenReturn(codeOperations);
		when(codeOperations.entries()).thenReturn(Map.of(
			"codeHash", CODE_HASH,
			"attempts", "corrupt-value"
		));

		assertThatThrownBy(() -> repository.findCode(USER_ID))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INTERNAL_SERVER_ERROR)
			);
	}
}
