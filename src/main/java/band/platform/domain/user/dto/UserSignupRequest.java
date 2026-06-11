package band.platform.domain.user.dto;

import band.platform.domain.user.entity.Gender;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
	@NotBlank
	@Size(max = 50)
	String name,

	@NotBlank
	@Size(min = 4, max = 12)
	String loginId,

	@NotBlank
	@Size(min = 8, max = 15)
	String password,

	@NotBlank
	@Email
	@Size(max = 255)
	String email,

	@Size(max = 500)
	String description,

	@NotNull
	Boolean opened,

	@NotBlank
	@Size(max = 30)
	String phoneNumber,

	@NotNull
	Gender gender,

	@Size(max = 500)
	String profileImg,

	@NotNull
	@AssertTrue
	Boolean privacyPolicyAgreed,

	@NotNull
	Boolean marketingPolicyAgreed
) {
}
