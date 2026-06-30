package band.platform.domain.user.social.client.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

import band.platform.domain.user.social.SocialOAuthProperties;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class AppleIdTokenVerifierTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-29T00:00:00Z"), ZoneOffset.UTC);

	@Test
	@DisplayName("nonce가 일치하는 유효한 id_token이면 Apple 클레임을 반환한다")
	void validTokenWithNonce() throws Exception {
		Fixture fixture = fixture();

		AppleIdTokenClaims claims = fixture.verifier()
			.verifyClaims(fixture.idToken(tokenClaims()), properties(), "oauth-nonce");

		assertThat(claims.subject()).isEqualTo("apple-subject");
		assertThat(claims.email()).isEqualTo("member@example.com");
		assertThat(claims.toSocialUserInfo().providerSubject()).isEqualTo("apple-subject");
	}

	@Test
	@DisplayName("같은 Apple 설정으로 반복 검증해도 JwtDecoder를 한 번만 생성한다")
	void reuseJwtDecoderForSameProvider() throws Exception {
		AtomicInteger factoryCalls = new AtomicInteger();
		Fixture fixture = fixture(factoryCalls);
		SocialOAuthProperties.Provider properties = properties();

		fixture.verifier().verifyClaims(fixture.idToken(tokenClaims()), properties, "oauth-nonce");
		fixture.verifier()
			.verifyClaims(fixture.idToken(tokenClaims().subject("other-apple-subject")), properties, "oauth-nonce");

		assertThat(factoryCalls.get()).isEqualTo(1);
	}

	@Test
	@DisplayName("요청 nonce가 없으면 id_token nonce가 없어도 Apple 클레임을 반환한다")
	void validTokenWithoutNonceWhenExpectedNonceIsNull() throws Exception {
		Fixture fixture = fixture();

		AppleIdTokenClaims claims = fixture.verifier()
			.verifyClaims(fixture.idToken(tokenClaims().nonce(null)), properties(), null);

		assertThat(claims.subject()).isEqualTo("apple-subject");
		assertThat(claims.email()).isEqualTo("member@example.com");
	}

	@Test
	@DisplayName("요청 nonce와 id_token nonce가 다르면 A14 에러로 변환한다")
	void nonceMismatch() throws Exception {
		Fixture fixture = fixture();

		assertBusinessError(
			() -> fixture.verifier().verifyClaims(fixture.idToken(tokenClaims().nonce("other-nonce")), properties(), "oauth-nonce"),
			ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID
		);
	}

	@Test
	@DisplayName("요청 nonce가 있는데 id_token nonce가 없으면 A14 에러로 변환한다")
	void missingNonce() throws Exception {
		Fixture fixture = fixture();

		assertBusinessError(
			() -> fixture.verifier().verifyClaims(fixture.idToken(tokenClaims().nonce(null)), properties(), "oauth-nonce"),
			ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID
		);
	}

	@Test
	@DisplayName("id_token issuer가 Apple이 아니면 A14 에러로 변환한다")
	void invalidIssuer() throws Exception {
		Fixture fixture = fixture();

		assertBusinessError(
			() -> fixture.verifier()
				.verifyClaims(fixture.idToken(tokenClaims().issuer("https://attacker.example.com")), properties(), "oauth-nonce"),
			ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID
		);
	}

	@Test
	@DisplayName("id_token audience에 client_id가 없으면 A14 에러로 변환한다")
	void invalidAudience() throws Exception {
		Fixture fixture = fixture();

		assertBusinessError(
			() -> fixture.verifier()
				.verifyClaims(fixture.idToken(tokenClaims().audience(List.of("other-client-id"))), properties(), "oauth-nonce"),
			ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID
		);
	}

	@Test
	@DisplayName("id_token audience가 없으면 A14 에러로 변환한다")
	void missingAudience() throws Exception {
		Fixture fixture = fixture();

		assertBusinessError(
			() -> fixture.verifier()
				.verifyClaims(fixture.idToken(tokenClaims().audience(null)), properties(), "oauth-nonce"),
			ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID
		);
	}

	@Test
	@DisplayName("id_token subject가 비어 있으면 A14 에러로 변환한다")
	void blankSubject() throws Exception {
		Fixture fixture = fixture();

		assertBusinessError(
			() -> fixture.verifier().verifyClaims(fixture.idToken(tokenClaims().subject(" ")), properties(), "oauth-nonce"),
			ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID
		);
	}

	@Test
	@DisplayName("id_token 서명이 맞지 않으면 A14 에러로 변환한다")
	void invalidSignature() throws Exception {
		Fixture trustedFixture = fixture();
		Fixture attackerFixture = fixture();

		assertBusinessError(
			() -> trustedFixture.verifier()
				.verifyClaims(attackerFixture.idToken(tokenClaims()), properties(), "oauth-nonce"),
			ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID
		);
	}

	@Test
	@DisplayName("id_token이 만료되었으면 A14 에러로 변환한다")
	void expiredToken() throws Exception {
		Fixture fixture = fixture();

		assertBusinessError(
			() -> fixture.verifier()
				.verifyClaims(fixture.idToken(tokenClaims().expiresAt(Instant.parse("2026-06-28T23:59:59Z"))), properties(), "oauth-nonce"),
			ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID
		);
	}

	private static void assertBusinessError(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable::call)
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(errorCode)
			);
	}

	private static Fixture fixture() throws Exception {
		return fixture(new AtomicInteger());
	}

	private static Fixture fixture(AtomicInteger factoryCalls) throws Exception {
		RSAKey jwk = rsaJwk();
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
			.withJwkSource(new ImmutableJWKSet<SecurityContext>(new JWKSet(jwk.toPublicJWK())))
			.jwsAlgorithm(SignatureAlgorithm.RS256)
			.build();
		jwtDecoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
		AppleIdTokenVerifier verifier = new AppleIdTokenVerifier(provider -> {
			factoryCalls.incrementAndGet();
			return jwtDecoder;
		}, FIXED_CLOCK);
		JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(jwk)));
		return new Fixture(verifier, jwtEncoder);
	}

	private static String idToken(JwtEncoder jwtEncoder, TokenClaims claims) {
		JwtClaimsSet.Builder claimsSet = JwtClaimsSet.builder()
			.issuer(claims.issuer())
			.subject(claims.subject())
			.issuedAt(Instant.parse("2026-06-28T23:59:00Z"))
			.expiresAt(claims.expiresAt());

		if (claims.audience() != null) {
			claimsSet.audience(claims.audience());
		}
		if (claims.nonce() != null) {
			claimsSet.claim("nonce", claims.nonce());
		}
		if (claims.email() != null) {
			claimsSet.claim("email", claims.email());
		}

		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
			.keyId("apple-test-key")
			.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claimsSet.build())).getTokenValue();
	}

	private static SocialOAuthProperties.Provider properties() {
		SocialOAuthProperties.Provider properties = new SocialOAuthProperties.Provider();
		properties.setClientId("apple-client-id");
		properties.setJwkSetUri("https://appleid.apple.com/auth/keys");
		return properties;
	}

	private static TokenClaims tokenClaims() {
		return new TokenClaims(
			"https://appleid.apple.com",
			List.of("apple-client-id"),
			"apple-subject",
			"member@example.com",
			"oauth-nonce",
			Instant.parse("2026-06-29T00:05:00Z")
		);
	}

	private static RSAKey rsaJwk() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		return new RSAKey.Builder((RSAPublicKey)keyPair.getPublic())
			.privateKey((RSAPrivateKey)keyPair.getPrivate())
			.keyID("apple-test-key")
			.build();
	}

	private record Fixture(AppleIdTokenVerifier verifier, JwtEncoder jwtEncoder) {

		String idToken(TokenClaims claims) {
			return AppleIdTokenVerifierTest.idToken(jwtEncoder, claims);
		}
	}

	private record TokenClaims(
		String issuer,
		List<String> audience,
		String subject,
		String email,
		String nonce,
		Instant expiresAt
	) {

		TokenClaims issuer(String issuer) {
			return new TokenClaims(issuer, audience, subject, email, nonce, expiresAt);
		}

		TokenClaims audience(List<String> audience) {
			return new TokenClaims(issuer, audience, subject, email, nonce, expiresAt);
		}

		TokenClaims subject(String subject) {
			return new TokenClaims(issuer, audience, subject, email, nonce, expiresAt);
		}

		TokenClaims nonce(String nonce) {
			return new TokenClaims(issuer, audience, subject, email, nonce, expiresAt);
		}

		TokenClaims expiresAt(Instant expiresAt) {
			return new TokenClaims(issuer, audience, subject, email, nonce, expiresAt);
		}
	}

	@FunctionalInterface
	private interface ThrowingCallable {

		void call() throws Exception;
	}
}
