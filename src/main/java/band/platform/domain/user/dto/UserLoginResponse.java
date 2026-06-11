package band.platform.domain.user.dto;

import band.platform.domain.user.entity.User;

public record UserLoginResponse(
	Long id,
	String loginId,
	String email
) {

	public static UserLoginResponse from(User user) {
		return new UserLoginResponse(
			user.getId(),
			user.getLoginId(),
			user.getEmail()
		);
	}

}
