package band.platform.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FindLoginIdRequest(
	@NotBlank
	@Email
	@Size(max = 255)
	String email
) {
}
