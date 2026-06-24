package band.platform.domain.user.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class SocialOAuthPropertiesTest {

	@Test
	@DisplayName("제공자별 OAuth 설정을 조회한다")
	void provider() {
		SocialOAuthProperties properties = new SocialOAuthProperties();
		SocialOAuthProperties.Provider kakao = validProvider();
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
	@DisplayName("초기화 시 바인딩된 제공자 필수 설정값이 비어 있으면 설정을 거부한다")
	void validateBoundProvidersOnInitialization() {
		SocialOAuthProperties properties = new SocialOAuthProperties();
		SocialOAuthProperties.Provider kakao = validProvider();
		kakao.setClientId(" ");
		EnumMap<SocialProvider, SocialOAuthProperties.Provider> providers = new EnumMap<>(SocialProvider.class);
		providers.put(SocialProvider.KAKAO, kakao);
		ReflectionTestUtils.setField(properties, "providers", providers);

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.refresh();

			assertThatThrownBy(() ->
				context.getAutowireCapableBeanFactory().initializeBean(properties, "socialOAuthProperties")
			).hasRootCauseInstanceOf(IllegalArgumentException.class)
				.satisfies(exception -> assertThat(rootCause(exception)).hasMessageContaining("kakao.client-id"));
		}
	}

	@Test
	@DisplayName("제공자 필수 설정값이 비어 있으면 setter 바인딩 설정을 거부한다")
	void blankProviderRequiredValue() {
		SocialOAuthProperties properties = new SocialOAuthProperties();
		SocialOAuthProperties.Provider kakao = validProvider();
		kakao.setClientId(" ");
		EnumMap<SocialProvider, SocialOAuthProperties.Provider> providers = new EnumMap<>(SocialProvider.class);
		providers.put(SocialProvider.KAKAO, kakao);

		assertThatThrownBy(() -> properties.setProviders(providers))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("kakao.client-id");
	}

	@Test
	@DisplayName("조회 시 제공자 필수 설정값이 비어 있으면 설정을 거부한다")
	void providerValidatesRequiredValueAtLookupTime() {
		SocialOAuthProperties properties = new SocialOAuthProperties();
		SocialOAuthProperties.Provider naver = validProvider();
		naver.setTokenUri(" ");
		EnumMap<SocialProvider, SocialOAuthProperties.Provider> providers = new EnumMap<>(SocialProvider.class);
		providers.put(SocialProvider.NAVER, naver);
		ReflectionTestUtils.setField(properties, "providers", providers);

		assertThatThrownBy(() -> properties.provider(SocialProvider.NAVER))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("naver.token-uri");
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

	private SocialOAuthProperties.Provider validProvider() {
		SocialOAuthProperties.Provider provider = new SocialOAuthProperties.Provider();
		provider.setClientId("kakao-client-id");
		provider.setClientSecret("kakao-client-secret");
		provider.setRedirectUri("http://localhost:3000/callback/kakao");
		provider.setAuthorizationUri("https://kauth.kakao.com/oauth/authorize");
		provider.setTokenUri("https://kauth.kakao.com/oauth/token");
		provider.setUserInfoUri("https://kapi.kakao.com/v2/user/me");
		provider.setScopes(List.of("profile_nickname"));
		return provider;
	}

	private Throwable rootCause(Throwable throwable) {
		Throwable rootCause = throwable;
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		return rootCause;
	}

}
