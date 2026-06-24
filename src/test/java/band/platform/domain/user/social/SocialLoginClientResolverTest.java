package band.platform.domain.user.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class SocialLoginClientResolverTest {

	@Test
	@DisplayName("제공자에 맞는 소셜 로그인 전략을 반환한다")
	void resolve() {
		SocialLoginClient kakaoClient = new StubSocialLoginClient(SocialProvider.KAKAO);
		SocialLoginClient naverClient = new StubSocialLoginClient(SocialProvider.NAVER);
		SocialLoginClientResolver resolver = new SocialLoginClientResolver(new SocialLoginClientRegistry(Map.of(
			SocialProvider.KAKAO, kakaoClient,
			SocialProvider.NAVER, naverClient
		)));

		assertThat(resolver.resolve(SocialProvider.KAKAO)).isSameAs(kakaoClient);
		assertThat(resolver.resolve(SocialProvider.NAVER)).isSameAs(naverClient);
	}

	@Test
	@DisplayName("지원하지 않는 제공자이면 예외를 던진다")
	void unsupportedProvider() {
		SocialLoginClientResolver resolver = new SocialLoginClientResolver(new SocialLoginClientRegistry(Map.of(
			SocialProvider.KAKAO, new StubSocialLoginClient(SocialProvider.KAKAO)
		)));

		assertThatThrownBy(() -> resolver.resolve(SocialProvider.NAVER))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED)
			);
	}

	@Test
	@DisplayName("LOCAL 제공자는 소셜 로그인 전략으로 조회할 수 없다")
	void localProvider() {
		SocialLoginClientResolver resolver = new SocialLoginClientResolver(new SocialLoginClientRegistry(Map.of()));

		assertThatThrownBy(() -> resolver.resolve(SocialProvider.LOCAL))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED)
			);
	}

	private record StubSocialLoginClient(SocialProvider provider) implements SocialLoginClient {

		@Override
		public SocialUserInfo fetchUserInfo(SocialAuthorizationCode authorizationCode) {
			return new SocialUserInfo(provider, "provider-subject", null, null, null);
		}
	}

}
