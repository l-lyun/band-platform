package band.platform.domain.user.social;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.repository.SocialOAuthStateRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialOAuthStateService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final int TOKEN_BYTE_LENGTH = 32;

	private final SocialOAuthStateRepository socialOAuthStateRepository;
	private final SocialOAuthProperties socialOAuthProperties;

	public SocialOAuthState issue(SocialProvider provider) {
		requireSocialProvider(provider);
		SocialOAuthState oauthState = new SocialOAuthState(randomToken(), randomToken());
		socialOAuthStateRepository.save(provider, oauthState, socialOAuthProperties.getStateTtl());
		return oauthState;
	}

	public Optional<SocialOAuthState> consume(SocialProvider provider, String state) {
		requireSocialProvider(provider);
		return socialOAuthStateRepository.consume(provider, state);
	}

	private String randomToken() {
		byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
		SECURE_RANDOM.nextBytes(bytes);
		return TOKEN_ENCODER.encodeToString(bytes);
	}

	private void requireSocialProvider(SocialProvider provider) {
		if (provider == null || provider == SocialProvider.LOCAL) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED);
		}
	}

}
