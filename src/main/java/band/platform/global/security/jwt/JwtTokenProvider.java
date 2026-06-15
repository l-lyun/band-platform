package band.platform.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private final ObjectMapper objectMapper;
	private final Clock clock;

	@Value("${security.jwt.issuer}")
	private String issuer;

	@Value("${security.jwt.secret}")
	private String secret;

	@Value("${security.jwt.access-token-ttl-seconds}")
	private long accessTokenTtlSeconds;

	@Value("${security.jwt.refresh-token-ttl-seconds}")
	private long refreshTokenTtlSeconds;

	public JwtToken issueAccessToken(Long userId, String loginId) {
		return issueToken(userId, loginId, null, JwtTokenType.ACCESS, accessTokenTtlSeconds);
	}

	public JwtToken issueRefreshToken(Long userId, String loginId, String tokenId) {
		return issueToken(userId, loginId, tokenId, JwtTokenType.REFRESH, refreshTokenTtlSeconds);
	}

	public JwtAccessTokenClaims parseAccessToken(String token) {
		Map<String, Object> claims = parseToken(token, JwtTokenType.ACCESS);
		return new JwtAccessTokenClaims(readUserId(claims), readString(claims, "loginId"));
	}

	public JwtRefreshTokenClaims parseRefreshToken(String token) {
		Map<String, Object> claims = parseToken(token, JwtTokenType.REFRESH);
		return new JwtRefreshTokenClaims(
			readUserId(claims),
			readString(claims, "loginId"),
			readString(claims, "jti")
		);
	}

	public long refreshTokenTtlSeconds() {
		return refreshTokenTtlSeconds;
	}

	private JwtToken issueToken(
		Long userId,
		String loginId,
		String tokenId,
		JwtTokenType tokenType,
		long ttlSeconds
	) {
		Instant now = Instant.now(clock);
		Instant expiresAt = now.plusSeconds(ttlSeconds);

		Map<String, Object> header = new LinkedHashMap<>();
		header.put("alg", "HS256");
		header.put("typ", "JWT");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("iss", issuer);
		payload.put("sub", String.valueOf(userId));
		payload.put("loginId", loginId);
		payload.put("type", tokenType.name());
		payload.put("iat", now.getEpochSecond());
		payload.put("exp", expiresAt.getEpochSecond());
		if (tokenId != null) {
			payload.put("jti", tokenId);
		}

		String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(payload);
		return new JwtToken(unsignedToken + "." + sign(unsignedToken), ttlSeconds);
	}

	private Map<String, Object> parseToken(String token, JwtTokenType expectedTokenType) {
		String[] parts = token == null ? new String[0] : token.split("\\.");
		if (parts.length != 3) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}

		String unsignedToken = parts[0] + "." + parts[1];
		if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}

		Map<String, Object> claims = readJson(parts[1]);
		if (!issuer.equals(claims.get("iss")) || !expectedTokenType.name().equals(claims.get("type"))) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
		if (!expiresAt(claims).isAfter(Instant.now(clock))) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
		}
		return claims;
	}

	private String base64UrlJson(Map<String, Object> value) {
		try {
			return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR, exception);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> readJson(String encodedJson) {
		try {
			return objectMapper.readValue(BASE64_URL_DECODER.decode(encodedJson), Map.class);
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
			return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR, exception);
		}
	}

	private boolean constantTimeEquals(String expected, String actual) {
		return java.security.MessageDigest.isEqual(
			expected.getBytes(StandardCharsets.UTF_8),
			actual.getBytes(StandardCharsets.UTF_8)
		);
	}

	private Instant expiresAt(Map<String, Object> claims) {
		Object exp = claims.get("exp");
		if (!(exp instanceof Number number)) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
		return Instant.ofEpochSecond(number.longValue());
	}

	private Long readUserId(Map<String, Object> claims) {
		try {
			return Long.valueOf(readString(claims, "sub"));
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
	}

	private String readString(Map<String, Object> claims, String name) {
		Object value = claims.get(name);
		if (!(value instanceof String stringValue) || stringValue.isBlank()) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
		return stringValue;
	}

}
