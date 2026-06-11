package band.platform.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
	@NotBlank
	@Size(min = 4, max = 12)
	String loginId,

	@NotBlank
	@Size(min = 8, max = 15)
	String password
) {

}
