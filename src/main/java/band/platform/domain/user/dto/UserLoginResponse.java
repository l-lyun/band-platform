package band.platform.domain.user.dto;

import band.platform.domain.user.entity.User;

public record UserLoginResponse(
	Long id,
	String loginId,
	String email,
	String accessToken,
	String tokenType,
	long expiresIn
) {

	public static UserLoginResponse from(User user) {
		return new UserLoginResponse(
			user.getId(),
			user.getLoginId(),
			user.getEmail(),
			null,
			null,
			0
		);
	}

	public UserLoginResponse withToken(TokenResponse token) {
		return new UserLoginResponse(
			id,
			loginId,
			email,
			token.accessToken(),
			token.tokenType(),
			token.expiresIn()
		);
	}

}
