package band.platform.domain.user.repository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialPendingSignup;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisSocialPendingSignupRepository implements SocialPendingSignupRepository {

	private static final String PENDING_SIGNUP_KEY_PREFIX = "oauth:pending-signup:";
	private static final String VALUE_DELIMITER = "\\|";
	private static final String STORED_DELIMITER = "|";
	private static final String NULL_VALUE = "-";
	private static final Base64.Encoder VALUE_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder VALUE_DECODER = Base64.getUrlDecoder();

	private final StringRedisTemplate redisTemplate;

	@Override
	public void save(String token, SocialPendingSignup pendingSignup, Duration ttl) {
		redisTemplate.opsForValue()
			.set(pendingSignupKey(token), serialize(pendingSignup), ttl);
	}

	@Override
	public Optional<SocialPendingSignup> consume(String token) {
		String value = redisTemplate.opsForValue().getAndDelete(pendingSignupKey(token));
		if (!StringUtils.hasText(value)) {
			return Optional.empty();
		}
		return Optional.of(deserialize(value));
	}

	private String serialize(SocialPendingSignup pendingSignup) {
		return String.join(
			STORED_DELIMITER,
			pendingSignup.provider().name(),
			encode(pendingSignup.providerSubject()),
			encode(pendingSignup.email()),
			encode(pendingSignup.name()),
			encode(pendingSignup.profileImageUrl())
		);
	}

	private SocialPendingSignup deserialize(String value) {
		String[] fields = value.split(VALUE_DELIMITER, -1);
		if (fields.length != 5) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PENDING_SIGNUP_INVALID);
		}
		try {
			return new SocialPendingSignup(
				SocialProvider.valueOf(fields[0]),
				decode(fields[1]),
				decode(fields[2]),
				decode(fields[3]),
				decode(fields[4])
			);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PENDING_SIGNUP_INVALID);
		}
	}

	private static String encode(String value) {
		if (value == null) {
			return NULL_VALUE;
		}
		return VALUE_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private static String decode(String value) {
		if (NULL_VALUE.equals(value)) {
			return null;
		}
		return new String(VALUE_DECODER.decode(value), StandardCharsets.UTF_8);
	}

	private static String pendingSignupKey(String token) {
		if (!StringUtils.hasText(token)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PENDING_SIGNUP_INVALID);
		}
		return PENDING_SIGNUP_KEY_PREFIX + token;
	}
}
