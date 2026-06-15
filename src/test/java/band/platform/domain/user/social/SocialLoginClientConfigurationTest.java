package band.platform.domain.user.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class SocialLoginClientConfigurationTest {

	private final SocialLoginClientConfiguration configuration = new SocialLoginClientConfiguration();

	@Test
	@DisplayName("소셜 로그인 전략 목록을 provider 기준 Registry로 만든다")
	void socialLoginClientRegistry() {
		SocialLoginClient kakaoClient = new StubSocialLoginClient(SocialProvider.KAKAO);
		SocialLoginClient naverClient = new StubSocialLoginClient(SocialProvider.NAVER);

		SocialLoginClientRegistry registry = configuration.socialLoginClientRegistry(List.of(kakaoClient, naverClient));

		assertThat(registry.findByProvider(SocialProvider.KAKAO)).contains(kakaoClient);
		assertThat(registry.findByProvider(SocialProvider.NAVER)).contains(naverClient);
	}

	@Test
	@DisplayName("같은 제공자 전략이 중복 등록되면 A13 에러를 반환한다")
	void duplicatedProvider() {
		assertThatThrownBy(() -> configuration.socialLoginClientRegistry(List.of(
			new StubSocialLoginClient(SocialProvider.APPLE),
			new StubSocialLoginClient(SocialProvider.APPLE)
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID)
		);
	}

	@Test
	@DisplayName("LOCAL 전략이 등록되면 A13 에러를 반환한다")
	void localProvider() {
		assertThatThrownBy(() -> configuration.socialLoginClientRegistry(List.of(
			new StubSocialLoginClient(SocialProvider.LOCAL)
		))).isInstanceOfSatisfying(BusinessException.class, exception ->
			assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID)
		);
	}

	private record StubSocialLoginClient(SocialProvider provider) implements SocialLoginClient {

		@Override
		public SocialUserInfo fetchUserInfo(SocialAuthorizationCode authorizationCode) {
			return new SocialUserInfo(provider, "provider-subject", null, null, null);
		}
	}

}
