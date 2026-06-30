package band.platform.domain.user.social.client.apple;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

import band.platform.domain.user.social.SocialOAuthProperties;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AppleClientSecretGenerator {

	private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
	private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);
	private static final String PKCS8_BEGIN = "-----BEGIN PRIVATE KEY-----";
	private static final String PKCS8_END = "-----END PRIVATE KEY-----";

	private final Clock clock;
	private final Function<ECKey, JwtEncoder> jwtEncoderFactory;

	public AppleClientSecretGenerator() {
		this(Clock.systemUTC());
	}

	public AppleClientSecretGenerator(Clock clock) {
		this(clock, AppleClientSecretGenerator::jwtEncoder);
	}

	public String generate(SocialOAuthProperties.Provider properties) {
		requireValidProperties(properties);
		try {
			ECKey key = parsePkcs8PrivateKey(properties.getPrivateKey(), properties.getKeyId());
			JwtEncoder encoder = jwtEncoderFactory.apply(key);
			Instant issuedAt = Instant.now(clock);
			Instant expiresAt = issuedAt.plus(CLIENT_SECRET_TTL);

			JwsHeader headers = JwsHeader.with(SignatureAlgorithm.ES256)
				.keyId(properties.getKeyId())
				.build();
			JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.getTeamId())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.audience(List.of(APPLE_AUDIENCE))
				.subject(properties.getClientId())
				.build();

			return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
		} catch (NoSuchAlgorithmException
			| InvalidKeySpecException
			| IllegalArgumentException
			| ArithmeticException
			| ClassCastException
			| JwtEncodingException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
	}

	private void requireValidProperties(SocialOAuthProperties.Provider properties) {
		if (properties == null
			|| !StringUtils.hasText(properties.getClientId())
			|| !StringUtils.hasText(properties.getTeamId())
			|| !StringUtils.hasText(properties.getKeyId())
			|| !StringUtils.hasText(properties.getPrivateKey())) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
	}

	private ECKey parsePkcs8PrivateKey(String privateKeyPem, String keyId)
		throws NoSuchAlgorithmException, InvalidKeySpecException {
		String normalizedPem = privateKeyPem.replace("\\n", "\n").trim();
		if (!normalizedPem.startsWith(PKCS8_BEGIN) || !normalizedPem.endsWith(PKCS8_END)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}

		String encodedKey = normalizedPem
			.replace(PKCS8_BEGIN, "")
			.replace(PKCS8_END, "")
			.replaceAll("\\s", "");
		byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
		ECPrivateKey privateKey = (ECPrivateKey)KeyFactory.getInstance("EC")
			.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
		if (privateKey.getParams().getCurve().getField().getFieldSize() != 256) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}

		ECPoint publicPoint = multiply(privateKey.getParams().getGenerator(), privateKey.getS(), privateKey.getParams());
		return new ECKey.Builder(
			Curve.P_256,
			ECKey.encodeCoordinate(256, publicPoint.getAffineX()),
			ECKey.encodeCoordinate(256, publicPoint.getAffineY())
		)
			.privateKey(privateKey)
			.keyID(keyId)
			.build();
	}

	private static JwtEncoder jwtEncoder(ECKey key) {
		return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
	}

	private ECPoint multiply(ECPoint point, BigInteger scalar, ECParameterSpec params) {
		ECPoint result = null;
		ECPoint addend = point;
		for (int bit = 0; bit < scalar.bitLength(); bit++) {
			if (scalar.testBit(bit)) {
				result = add(result, addend, params);
			}
			addend = add(addend, addend, params);
		}
		if (result == null) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
		return result;
	}

	private ECPoint add(ECPoint first, ECPoint second, ECParameterSpec params) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}

		BigInteger prime = ((ECFieldFp)params.getCurve().getField()).getP();
		BigInteger x1 = first.getAffineX();
		BigInteger y1 = first.getAffineY();
		BigInteger x2 = second.getAffineX();
		BigInteger y2 = second.getAffineY();

		if (x1.equals(x2) && y1.add(y2).mod(prime).equals(BigInteger.ZERO)) {
			return null;
		}

		BigInteger lambda;
		if (x1.equals(x2) && y1.equals(y2)) {
			BigInteger numerator = x1.pow(2).multiply(BigInteger.valueOf(3))
				.add(params.getCurve().getA());
			BigInteger denominator = y1.multiply(BigInteger.TWO).modInverse(prime);
			lambda = numerator.multiply(denominator).mod(prime);
		} else {
			BigInteger numerator = y2.subtract(y1);
			BigInteger denominator = x2.subtract(x1).mod(prime).modInverse(prime);
			lambda = numerator.multiply(denominator).mod(prime);
		}

		BigInteger x3 = lambda.pow(2).subtract(x1).subtract(x2).mod(prime);
		BigInteger y3 = lambda.multiply(x1.subtract(x3)).subtract(y1).mod(prime);
		return new ECPoint(x3, y3);
	}
}
