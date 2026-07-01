package band.platform.domain.user.dto;

import band.platform.domain.user.entity.SocialProvider;

public record SocialLoginResponse(
	boolean signupRequired,
	SocialProvider provider,
	String email,
	String name,
	String profileImageUrl,
	String pendingSignupToken,
	String accessToken,
	String tokenType,
	Long expiresIn
) {

	public static SocialLoginResponse linked(
		SocialProvider provider,
		String email,
		String name,
		String profileImageUrl,
		TokenResponse token
	) {
		return new SocialLoginResponse(
			false,
			provider,
			email,
			name,
			profileImageUrl,
			null,
			token.accessToken(),
			token.tokenType(),
			token.expiresIn()
		);
	}

	public static SocialLoginResponse signupRequired(
		SocialProvider provider,
		String email,
		String name,
		String profileImageUrl,
		String pendingSignupToken
	) {
		return new SocialLoginResponse(
			true,
			provider,
			email,
			name,
			profileImageUrl,
			pendingSignupToken,
			null,
			null,
			null
		);
	}

}
