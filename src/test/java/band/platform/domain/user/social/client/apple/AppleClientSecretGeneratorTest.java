package band.platform.domain.user.social.client.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtEncodingException;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;

import band.platform.domain.user.social.SocialOAuthProperties;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class AppleClientSecretGeneratorTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-29T00:00:00Z"), ZoneOffset.UTC);
	private static final long APPLE_MAX_EXPIRATION_SECONDS = 15_777_000L;

	@Test
	@DisplayName("PKCS#8 PEM 실제 줄바꿈으로 Apple client_secret을 생성한다")
	void generateWithRealNewlinePem() throws Exception {
		TestKey testKey = testKey();
		AppleClientSecretGenerator generator = new AppleClientSecretGenerator(FIXED_CLOCK);

		SignedJWT jwt = parse(generator.generate(properties(testKey.pkcs8Pem())));

		assertThat(jwt.verify(new ECDSAVerifier(testKey.publicKey()))).isTrue();
		assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
		assertThat(jwt.getHeader().getKeyID()).isEqualTo("apple-key-id");
	}

	@Test
	@DisplayName("PKCS#8 PEM 이스케이프 줄바꿈으로 Apple client_secret을 생성한다")
	void generateWithEscapedNewlinePem() throws Exception {
		TestKey testKey = testKey();
		AppleClientSecretGenerator generator = new AppleClientSecretGenerator(FIXED_CLOCK);

		SignedJWT jwt = parse(generator.generate(properties(testKey.escapedPkcs8Pem())));

		assertThat(jwt.verify(new ECDSAVerifier(testKey.publicKey()))).isTrue();
		assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("apple-team-id");
	}

	@Test
	@DisplayName("생성한 JWT는 Apple client_secret 헤더와 클레임을 포함한다")
	void generateHeaderAndClaims() throws Exception {
		TestKey testKey = testKey();
		AppleClientSecretGenerator generator = new AppleClientSecretGenerator(FIXED_CLOCK);

		SignedJWT jwt = parse(generator.generate(properties(testKey.pkcs8Pem())));

		assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
		assertThat(jwt.getHeader().getKeyID()).isEqualTo("apple-key-id");
		assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("apple-team-id");
		assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("apple-client-id");
		assertThat(jwt.getJWTClaimsSet().getAudience()).isEqualTo(List.of("https://appleid.apple.com"));
		assertThat(jwt.getJWTClaimsSet().getIssueTime().toInstant()).isEqualTo(Instant.now(FIXED_CLOCK));
	}

	@Test
	@DisplayName("Apple client_secret 만료 시간은 발급 시각에서 5분 뒤이고 Apple 최대 만료 제한보다 짧다")
	void generateExpiresInFiveMinutesBelowAppleMaximum() throws Exception {
		TestKey testKey = testKey();
		AppleClientSecretGenerator generator = new AppleClientSecretGenerator(FIXED_CLOCK);

		SignedJWT jwt = parse(generator.generate(properties(testKey.pkcs8Pem())));

		Instant issuedAt = jwt.getJWTClaimsSet().getIssueTime().toInstant();
		Instant expiresAt = jwt.getJWTClaimsSet().getExpirationTime().toInstant();
		Duration expirationDuration = Duration.between(issuedAt, expiresAt);
		assertThat(expirationDuration).isEqualTo(Duration.ofMinutes(5));
		assertThat(expirationDuration.toSeconds()).isLessThan(APPLE_MAX_EXPIRATION_SECONDS);
	}

	@Test
	@DisplayName("PKCS#8 PEM 형식이 아니면 A13 에러로 변환한다")
	void malformedPem() {
		AppleClientSecretGenerator generator = new AppleClientSecretGenerator(FIXED_CLOCK);

		assertA13(() -> generator.generate(properties("-----BEGIN EC PRIVATE KEY-----\\nmalformed\\n-----END EC PRIVATE KEY-----")));
	}

	@Test
	@DisplayName("Apple client_secret 필수 설정이 비어 있으면 A13 에러로 변환한다")
	void missingRequiredConfiguration() throws Exception {
		TestKey testKey = testKey();
		SocialOAuthProperties.Provider properties = properties(testKey.pkcs8Pem());
		properties.setTeamId(" ");
		AppleClientSecretGenerator generator = new AppleClientSecretGenerator(FIXED_CLOCK);

		assertA13(() -> generator.generate(properties));
	}

	@Test
	@DisplayName("Apple client_secret 서명 실패는 A13 에러로 변환한다")
	void signingFailure() throws Exception {
		TestKey testKey = testKey();
		AppleClientSecretGenerator generator = new AppleClientSecretGenerator(
			FIXED_CLOCK,
			key -> parameters -> {
				throw new JwtEncodingException("signing failed");
			}
		);

		assertA13(() -> generator.generate(properties(testKey.pkcs8Pem())));
	}

	private static SignedJWT parse(String clientSecret) throws Exception {
		return SignedJWT.parse(clientSecret);
	}

	private static SocialOAuthProperties.Provider properties(String privateKey) {
		SocialOAuthProperties.Provider provider = new SocialOAuthProperties.Provider();
		provider.setClientId("apple-client-id");
		provider.setTeamId("apple-team-id");
		provider.setKeyId("apple-key-id");
		provider.setPrivateKey(privateKey);
		return provider;
	}

	private static void assertA13(ThrowingCallable callable) {
		assertThatThrownBy(callable::call)
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID)
			);
	}

	private static TestKey testKey() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
		keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		String encodedPrivateKey = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
			.encodeToString(keyPair.getPrivate().getEncoded());
		String pkcs8Pem = """
			-----BEGIN PRIVATE KEY-----
			%s
			-----END PRIVATE KEY-----
			""".formatted(encodedPrivateKey);
		return new TestKey((ECPublicKey)keyPair.getPublic(), pkcs8Pem);
	}

	private record TestKey(ECPublicKey publicKey, String pkcs8Pem) {

		String escapedPkcs8Pem() {
			return pkcs8Pem.replace("\n", "\\n");
		}
	}

	@FunctionalInterface
	private interface ThrowingCallable {
		void call() throws Exception;
	}
}
