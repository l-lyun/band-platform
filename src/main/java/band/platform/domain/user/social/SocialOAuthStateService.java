package band.platform.domain.user.social;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import band.platform.domain.user.repository.SocialOAuthStateRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialOAuthStateService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final int TOKEN_BYTE_LENGTH = 32;

	private final SocialOAuthStateRepository socialOAuthStateRepository;
	private final SocialOAuthProperties socialOAuthProperties;

	public SocialOAuthState issue() {
		SocialOAuthState oauthState = new SocialOAuthState(randomToken(), randomToken());
		socialOAuthStateRepository.save(oauthState, socialOAuthProperties.getStateTtl());
		return oauthState;
	}

	public Optional<SocialOAuthState> consume(String state) {
		return socialOAuthStateRepository.consume(state);
	}

	private String randomToken() {
		byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
		SECURE_RANDOM.nextBytes(bytes);
		return TOKEN_ENCODER.encodeToString(bytes);
	}

}
