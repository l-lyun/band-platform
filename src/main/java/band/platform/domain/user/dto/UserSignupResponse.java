package band.platform.domain.user.dto;

import band.platform.domain.user.entity.User;

public record UserSignupResponse(
	Long id,
	String loginId,
	String email
) {

	public static UserSignupResponse from(User user) {
		return new UserSignupResponse(
			user.getId(),
			user.getLoginId(),
			user.getEmail()
		);
	}

}
