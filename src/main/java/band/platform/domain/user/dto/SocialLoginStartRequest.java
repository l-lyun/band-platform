package band.platform.domain.user.dto;

import band.platform.domain.user.entity.SocialProvider;
import jakarta.validation.constraints.NotNull;

public record SocialLoginStartRequest(
	@NotNull
	SocialProvider provider
) {

}
