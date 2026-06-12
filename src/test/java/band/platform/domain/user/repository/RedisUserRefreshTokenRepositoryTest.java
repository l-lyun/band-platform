package band.platform.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RedisUserRefreshTokenRepositoryTest {

	private static final Long USER_ID = 1L;
	private static final String OLD_TOKEN_ID = "old-token-id";
	private static final String NEW_TOKEN_ID = "new-token-id";
	private static final String OLD_KEY = "auth:refresh:" + USER_ID + ":" + OLD_TOKEN_ID;
	private static final String NEW_KEY = "auth:refresh:" + USER_ID + ":" + NEW_TOKEN_ID;
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private RedisUserRefreshTokenRepository redisUserRefreshTokenRepository;

	@Test
	@DisplayName("리프레시 토큰 식별자를 TTL과 함께 저장한다")
	void save() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		redisUserRefreshTokenRepository.save(USER_ID, OLD_TOKEN_ID, REFRESH_TOKEN_TTL);

		verify(valueOperations).set(OLD_KEY, "ACTIVE", REFRESH_TOKEN_TTL);
	}

	@Test
	@DisplayName("기존 리프레시 토큰이 있으면 새 토큰으로 회전한다")
	void rotate() {
		when(redisTemplate.execute(
			ArgumentMatchers.<RedisScript<Long>>any(),
			eq(List.of(OLD_KEY, NEW_KEY)),
			eq("ACTIVE"),
			eq("1209600")
		)).thenReturn(1L);

		boolean rotated = redisUserRefreshTokenRepository.rotate(
			USER_ID,
			OLD_TOKEN_ID,
			NEW_TOKEN_ID,
			REFRESH_TOKEN_TTL
		);

		assertThat(rotated).isTrue();
	}

	@Test
	@DisplayName("기존 리프레시 토큰이 없으면 회전에 실패한다")
	void rotateMissingOldToken() {
		when(redisTemplate.execute(
			ArgumentMatchers.<RedisScript<Long>>any(),
			eq(List.of(OLD_KEY, NEW_KEY)),
			eq("ACTIVE"),
			eq("1209600")
		)).thenReturn(0L);

		boolean rotated = redisUserRefreshTokenRepository.rotate(
			USER_ID,
			OLD_TOKEN_ID,
			NEW_TOKEN_ID,
			REFRESH_TOKEN_TTL
		);

		assertThat(rotated).isFalse();
	}

	@Test
	@DisplayName("로그아웃 시 리프레시 토큰 키를 삭제한다")
	void delete() {
		redisUserRefreshTokenRepository.delete(USER_ID, OLD_TOKEN_ID);

		verify(redisTemplate).delete(OLD_KEY);
	}

}
