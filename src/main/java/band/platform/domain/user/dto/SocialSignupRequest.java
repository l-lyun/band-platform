package band.platform.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SocialSignupRequest(
	@NotBlank
	String pendingSignupToken,

	@Size(max = 50)
	String name,

	@NotBlank
	@Size(max = 30)
	String phoneNumber,

	@NotNull
	Boolean privacyPolicyAgreed,

	@NotNull
	Boolean marketingPolicyAgreed,

	boolean linkExistingAccount
) {

}
