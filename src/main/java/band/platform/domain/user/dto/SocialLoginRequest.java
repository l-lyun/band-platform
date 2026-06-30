package band.platform.domain.user.dto;

import band.platform.domain.user.entity.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginRequest(
	@NotNull
	SocialProvider provider,

	@NotBlank
	String code,

	@NotBlank
	String state,

	@NotBlank
	String redirectUri
) {

}
