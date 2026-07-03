package band.platform.domain.user.dto;

import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
	@NotBlank
	@Size(max = 50)
	String name,

	@NotNull
	Position position,

	@Size(max = 500)
	String profileImg,

	@NotNull
	Gender gender,

	@Size(max = 500)
	String description,

	@NotNull
	Boolean opened
) {
}
