package band.platform.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetCompleteRequest(
	@NotBlank
	@Size(min = 8, max = 15)
	String newPassword
) {
}
