package band.platform.domain.user.social;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

import band.platform.domain.user.repository.SocialPendingSignupRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialPendingSignupService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final int TOKEN_BYTE_LENGTH = 32;

	private final SocialPendingSignupRepository socialPendingSignupRepository;
	private final SocialOAuthProperties socialOAuthProperties;

	public String issue(SocialUserInfo socialUserInfo) {
		String token = randomToken();
		socialPendingSignupRepository.save(
			token,
			SocialPendingSignup.from(socialUserInfo),
			socialOAuthProperties.getStateTtl()
		);
		return token;
	}

	public Optional<SocialUserInfo> consume(String token) {
		return socialPendingSignupRepository.consume(token)
			.map(SocialPendingSignup::toSocialUserInfo);
	}

	private String randomToken() {
		byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
		SECURE_RANDOM.nextBytes(bytes);
		return TOKEN_ENCODER.encodeToString(bytes);
	}
}
