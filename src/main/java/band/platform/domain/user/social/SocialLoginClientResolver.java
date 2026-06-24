package band.platform.domain.user.social;

import org.springframework.stereotype.Component;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SocialLoginClientResolver {

	private final SocialLoginClientRegistry registry;

	public SocialLoginClient resolve(SocialProvider provider) {
		if (provider == null || provider == SocialProvider.LOCAL) {
			throw new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED);
		}
		return registry.findByProvider(provider)
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED));
	}

}
