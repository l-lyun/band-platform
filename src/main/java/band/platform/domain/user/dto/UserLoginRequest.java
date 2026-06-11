package band.platform.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
	@NotBlank
	@Size(min = 4, max = 30)
	String loginId,

	@NotBlank
	@Size(min = 8, max = 72)
	String password
) {
}
