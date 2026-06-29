package band.platform.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetVerifyCodeRequest(
	@NotBlank
	@Size(min = 4, max = 12)
	String loginId,

	@NotBlank
	@Email
	@Size(max = 255)
	String email,

	@NotBlank
	String code
) {
}
