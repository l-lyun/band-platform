package band.platform.domain.user.social.client.apple;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import band.platform.domain.user.social.SocialOAuthProperties;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AppleIdTokenVerifier {

	private static final String APPLE_ISSUER = "https://appleid.apple.com";

	private final Function<SocialOAuthProperties.Provider, JwtDecoder> jwtDecoderFactory;
	private final Clock clock;
	private final ConcurrentMap<DecoderCacheKey, JwtDecoder> jwtDecoders = new ConcurrentHashMap<>();

	public AppleIdTokenVerifier() {
		this(AppleIdTokenVerifier::createJwtDecoder, Clock.systemUTC());
	}

	AppleIdTokenVerifier(Function<SocialOAuthProperties.Provider, JwtDecoder> jwtDecoderFactory) {
		this(jwtDecoderFactory, Clock.systemUTC());
	}

	public AppleIdTokenClaims verifyClaims(
		String idToken,
		SocialOAuthProperties.Provider properties,
		String expectedNonce
	) {
		Jwt jwt = decode(idToken, properties);
		requireIssuer(jwt);
		requireAudience(jwt, properties.getClientId());
		requireSubject(jwt);
		requireTemporalClaims(jwt);
		requireNonce(jwt, expectedNonce);
		return new AppleIdTokenClaims(jwt.getSubject(), jwt.getClaimAsString("email"));
	}

	private static JwtDecoder createJwtDecoder(SocialOAuthProperties.Provider properties) {
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
		jwtDecoder.setJwtValidator(jwtValidator(properties.getClientId()));
		return jwtDecoder;
	}

	private Jwt decode(String idToken, SocialOAuthProperties.Provider properties) {
		try {
			return cachedJwtDecoder(properties).decode(idToken);
		} catch (JwtException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		}
	}

	private JwtDecoder cachedJwtDecoder(SocialOAuthProperties.Provider properties) {
		DecoderCacheKey cacheKey = new DecoderCacheKey(properties.getJwkSetUri(), properties.getClientId());
		return jwtDecoders.computeIfAbsent(cacheKey, key -> jwtDecoderFactory.apply(properties));
	}

	private static OAuth2TokenValidator<Jwt> jwtValidator(String clientId) {
		return new DelegatingOAuth2TokenValidator<>(
			JwtValidators.createDefaultWithIssuer(APPLE_ISSUER),
			jwt -> hasAudience(jwt, clientId)
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))
		);
	}

	private void requireIssuer(Jwt jwt) {
		if (jwt.getIssuer() == null || !APPLE_ISSUER.equals(jwt.getIssuer().toString())) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		}
	}

	private void requireAudience(Jwt jwt, String clientId) {
		if (!hasAudience(jwt, clientId)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		}
	}

	private static boolean hasAudience(Jwt jwt, String clientId) {
		List<String> audience = jwt.getAudience();
		return audience != null && audience.contains(clientId);
	}

	private void requireSubject(Jwt jwt) {
		if (!StringUtils.hasText(jwt.getSubject())) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		}
	}

	private void requireTemporalClaims(Jwt jwt) {
		Instant now = Instant.now(clock);
		Instant expiresAt = jwt.getExpiresAt();
		if (expiresAt == null || !now.isBefore(expiresAt)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		}

		Instant notBefore = jwt.getNotBefore();
		if (notBefore != null && now.isBefore(notBefore)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		}
	}

	private void requireNonce(Jwt jwt, String nonce) {
		if (nonce == null) {
			return;
		}
		String tokenNonce = jwt.getClaimAsString("nonce");
		if (!nonce.equals(tokenNonce)) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		}
	}

	private record DecoderCacheKey(String jwkSetUri, String clientId) {
	}
}
