package band.platform.domain.user.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class SocialOAuthPropertiesTest {

	@Test
	@DisplayName("제공자별 OAuth 설정을 조회한다")
	void provider() {
		SocialOAuthProperties properties = new SocialOAuthProperties();
		SocialOAuthProperties.Provider kakao = new SocialOAuthProperties.Provider();
		kakao.setClientId("kakao-client-id");
		kakao.setClientSecret("kakao-client-secret");
		kakao.setScopes(List.of("profile_nickname"));
		EnumMap<SocialProvider, SocialOAuthProperties.Provider> providers = new EnumMap<>(SocialProvider.class);
		providers.put(SocialProvider.KAKAO, kakao);
		properties.setProviders(providers);
		properties.setStateTtl(Duration.ofMinutes(5));

		SocialOAuthProperties.Provider provider = properties.provider(SocialProvider.KAKAO);

		assertThat(properties.getStateTtl()).isEqualTo(Duration.ofMinutes(5));
		assertThat(provider.getClientId()).isEqualTo("kakao-client-id");
		assertThat(provider.hasClientSecret()).isTrue();
		assertThat(provider.getScopes()).containsExactly("profile_nickname");
	}

	@Test
	@DisplayName("설정되지 않은 제공자이면 A13 에러를 반환한다")
	void missingProvider() {
		SocialOAuthProperties properties = new SocialOAuthProperties();

		assertThatThrownBy(() -> properties.provider(SocialProvider.NAVER))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID)
			);
	}

	@Test
	@DisplayName("LOCAL 제공자는 OAuth 설정으로 조회할 수 없다")
	void localProvider() {
		SocialOAuthProperties properties = new SocialOAuthProperties();

		assertThatThrownBy(() -> properties.provider(SocialProvider.LOCAL))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED)
			);
	}

}
