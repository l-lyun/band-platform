package band.platform.domain.user.dto;

import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.Position;
import band.platform.domain.user.entity.User;

public record UserProfileResponse(
	Long id,
	String name,
	Position position,
	String profileImg,
	Gender gender,
	String description,
	Boolean opened
) {

	public static UserProfileResponse from(User user) {
		return new UserProfileResponse(
			user.getId(),
			user.getName(),
			user.getPosition(),
			user.getProfileImg(),
			user.getGender(),
			user.getDescription(),
			user.getOpened()
		);
	}
}
