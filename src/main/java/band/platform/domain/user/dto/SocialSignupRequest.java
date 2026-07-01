package band.platform.domain.user.dto;

import band.platform.domain.user.entity.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialSignupRequest(
	@NotNull
	SocialProvider provider,

	@NotBlank
	String code,

	@NotBlank
	String state,

	@NotBlank
	String redirectUri,

	@NotBlank
	String phoneNumber,

	@NotNull
	Boolean privacyPolicyAgreed,

	@NotNull
	Boolean marketingPolicyAgreed,

	boolean linkExistingAccount
) {

}
