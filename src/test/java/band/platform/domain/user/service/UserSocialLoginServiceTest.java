package band.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import band.platform.domain.user.dto.SocialLoginRequest;
import band.platform.domain.user.dto.SocialLoginResponse;
import band.platform.domain.user.dto.SocialLoginStartRequest;
import band.platform.domain.user.dto.SocialLoginStartResponse;
import band.platform.domain.user.dto.TokenResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.SocialAccount;
import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.SocialAccountRepository;
import band.platform.domain.user.social.SocialAuthorizationCode;
import band.platform.domain.user.social.SocialLoginClient;
import band.platform.domain.user.social.SocialLoginClientResolver;
import band.platform.domain.user.social.SocialOAuthProperties;
import band.platform.domain.user.social.SocialOAuthState;
import band.platform.domain.user.social.SocialOAuthStateService;
import band.platform.domain.user.social.SocialUserInfo;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class UserSocialLoginServiceTest {

	private static final Long USER_ID = 1L;
	private static final String LOGIN_ID = "bandmaster";
	private static final String CODE = "authorization-code";
	private static final String STATE = "oauth-state";
	private static final String NONCE = "oauth-nonce";
	private static final String REDIRECT_URI = "http://localhost:3000/oauth/naver";
	private static final String PROVIDER_SUBJECT = "provider-subject";
	private static final String EMAIL = "bandmaster@example.com";
	private static final String NAME = "김밴드";
	private static final String PROFILE_IMAGE_URL = "https://example.com/profile.png";

	@Mock
	private SocialLoginClientResolver socialLoginClientResolver;

	@Mock
	private SocialLoginClient socialLoginClient;

	@Mock
	private SocialOAuthStateService socialOAuthStateService;

	private SocialOAuthProperties socialOAuthProperties;

	@Mock
	private SocialAccountRepository socialAccountRepository;

	@Mock
	private UserTokenService userTokenService;

	private UserSocialLoginService userSocialLoginService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		socialOAuthProperties = socialOAuthProperties();
		userSocialLoginService = new UserSocialLoginService(
			socialLoginClientResolver,
			socialOAuthStateService,
			socialOAuthProperties,
			socialAccountRepository,
			userTokenService
		);
	}

	@Test
	@DisplayName("소셜 로그인 시작 시 state를 발급하고 제공자 authorization URL을 반환한다")
	void startIssuesStateAndBuildsAuthorizationUrl() {
		when(socialOAuthStateService.issue(SocialProvider.NAVER))
			.thenReturn(new SocialOAuthState(STATE, NONCE));

		SocialLoginStartResponse response = userSocialLoginService.start(new SocialLoginStartRequest(SocialProvider.NAVER));

		assertThat(response.provider()).isEqualTo(SocialProvider.NAVER);
		assertThat(response.state()).isEqualTo(STATE);
		assertThat(response.authorizationUrl()).startsWith("https://nid.naver.com/oauth2.0/authorize?");
		Map<String, String> queryParams = queryParams(response.authorizationUrl());
		assertThat(queryParams).containsEntry("response_type", "code");
		assertThat(queryParams).containsEntry("client_id", "naver-client-id");
		assertThat(queryParams).containsEntry("redirect_uri", "http://localhost:3000/callback/naver");
		assertThat(queryParams).containsEntry("state", STATE);
		assertThat(queryParams).containsEntry("scope", "profile email");
		assertThat(queryParams).doesNotContainKey("nonce");
		verify(socialOAuthStateService).issue(SocialProvider.NAVER);
	}

	@Test
	@DisplayName("OIDC 제공자 소셜 로그인 시작 URL에는 nonce를 포함한다")
	void startIncludesNonceForOpenIdProvider() {
		when(socialOAuthStateService.issue(SocialProvider.APPLE))
			.thenReturn(new SocialOAuthState(STATE, NONCE));

		SocialLoginStartResponse response = userSocialLoginService.start(new SocialLoginStartRequest(SocialProvider.APPLE));

		Map<String, String> queryParams = queryParams(response.authorizationUrl());
		assertThat(response.provider()).isEqualTo(SocialProvider.APPLE);
		assertThat(queryParams).containsEntry("client_id", "apple-client-id");
		assertThat(queryParams).containsEntry("scope", "openid email name");
		assertThat(queryParams).containsEntry("nonce", NONCE);
	}

	@Test
	@DisplayName("연결된 활성 소셜 계정이면 state를 소비하고 nonce로 제공자 조회 후 토큰 응답을 반환한다")
	void signInLinkedActiveAccount() {
		User user = activeUser();
		UserTokenIssueResult tokenIssueResult = tokenIssueResult();
		when(socialOAuthStateService.consume(SocialProvider.NAVER, STATE))
			.thenReturn(Optional.of(new SocialOAuthState(STATE, NONCE)));
		when(socialLoginClientResolver.resolve(SocialProvider.NAVER)).thenReturn(socialLoginClient);
		when(socialLoginClient.fetchUserInfo(any()))
			.thenReturn(socialUserInfo(SocialProvider.NAVER));
		when(socialAccountRepository.findByProviderAndProviderSubject(SocialProvider.NAVER, PROVIDER_SUBJECT))
			.thenReturn(Optional.of(SocialAccount.connect(user, SocialProvider.NAVER, PROVIDER_SUBJECT)));
		when(userTokenService.issue(USER_ID, LOGIN_ID)).thenReturn(tokenIssueResult);
		ArgumentCaptor<SocialAuthorizationCode> authorizationCode = ArgumentCaptor.forClass(SocialAuthorizationCode.class);

		var result = userSocialLoginService.signIn(request(SocialProvider.NAVER));

		verify(socialLoginClient).fetchUserInfo(authorizationCode.capture());
		assertThat(authorizationCode.getValue().nonce()).isEqualTo(NONCE);
		assertThat(result.tokenIssueResult()).isSameAs(tokenIssueResult);
		SocialLoginResponse response = result.response();
		assertThat(response.signupRequired()).isFalse();
		assertThat(response.provider()).isEqualTo(SocialProvider.NAVER);
		assertThat(response.email()).isEqualTo(EMAIL);
		assertThat(response.name()).isEqualTo(NAME);
		assertThat(response.profileImageUrl()).isEqualTo(PROFILE_IMAGE_URL);
		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(1800L);
		assertResponseDoesNotExposeProviderSubject(response);
	}

	@Test
	@DisplayName("연결되지 않은 소셜 계정이면 가입 필요 응답을 반환하고 토큰을 발급하지 않는다")
	void signInUnlinkedAccount() {
		when(socialOAuthStateService.consume(SocialProvider.KAKAO, STATE))
			.thenReturn(Optional.of(new SocialOAuthState(STATE, NONCE)));
		when(socialLoginClientResolver.resolve(SocialProvider.KAKAO)).thenReturn(socialLoginClient);
		when(socialLoginClient.fetchUserInfo(any()))
			.thenReturn(socialUserInfo(SocialProvider.KAKAO));
		when(socialAccountRepository.findByProviderAndProviderSubject(SocialProvider.KAKAO, PROVIDER_SUBJECT))
			.thenReturn(Optional.empty());

		var result = userSocialLoginService.signIn(request(SocialProvider.KAKAO));

		verify(userTokenService, never()).issue(any(), any());
		assertThat(result.tokenIssueResult()).isNull();
		SocialLoginResponse response = result.response();
		assertThat(response.signupRequired()).isTrue();
		assertThat(response.provider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(response.email()).isEqualTo(EMAIL);
		assertThat(response.name()).isEqualTo(NAME);
		assertThat(response.profileImageUrl()).isEqualTo(PROFILE_IMAGE_URL);
		assertThat(response.accessToken()).isNull();
		assertThat(response.tokenType()).isNull();
		assertThat(response.expiresIn()).isNull();
	}

	@Test
	@DisplayName("소비됐거나 없는 OAuth state이면 A10 예외를 던지고 제공자 호출 전에 중단한다")
	void signInConsumedState() {
		when(socialOAuthStateService.consume(SocialProvider.NAVER, STATE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userSocialLoginService.signIn(request(SocialProvider.NAVER)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_OAUTH_STATE_INVALID)
			);

		verify(socialLoginClientResolver, never()).resolve(any());
		verify(socialLoginClient, never()).fetchUserInfo(any());
		verify(socialAccountRepository, never()).findByProviderAndProviderSubject(any(), any());
		verify(userTokenService, never()).issue(any(), any());
	}

	@Test
	@DisplayName("LOCAL 제공자이면 A09 예외를 던지고 제공자 호출 전에 중단한다")
	void signInLocalProvider() {
		when(socialOAuthStateService.consume(SocialProvider.LOCAL, STATE))
			.thenThrow(new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED));

		assertThatThrownBy(() -> userSocialLoginService.signIn(request(SocialProvider.LOCAL)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED)
			);

		verify(socialLoginClientResolver, never()).resolve(any());
		verify(socialLoginClient, never()).fetchUserInfo(any());
		verify(userTokenService, never()).issue(any(), any());
	}

	@Test
	@DisplayName("제공자 클라이언트 예외는 다른 에러로 바꾸지 않고 그대로 전달한다")
	void signInProviderClientFailure() {
		when(socialOAuthStateService.consume(SocialProvider.NAVER, STATE))
			.thenReturn(Optional.of(new SocialOAuthState(STATE, NONCE)));
		when(socialLoginClientResolver.resolve(SocialProvider.NAVER)).thenReturn(socialLoginClient);
		when(socialLoginClient.fetchUserInfo(any()))
			.thenThrow(new BusinessException(ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE));

		assertThatThrownBy(() -> userSocialLoginService.signIn(request(SocialProvider.NAVER)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE)
			);

		verify(socialAccountRepository, never()).findByProviderAndProviderSubject(any(), any());
		verify(userTokenService, never()).issue(any(), any());
	}

	@Test
	@DisplayName("탈퇴한 연결 회원이면 A03 예외를 던지고 토큰을 발급하지 않는다")
	void signInWithdrawnLinkedUser() {
		User user = withdrawnUser();
		when(socialOAuthStateService.consume(SocialProvider.NAVER, STATE))
			.thenReturn(Optional.of(new SocialOAuthState(STATE, NONCE)));
		when(socialLoginClientResolver.resolve(SocialProvider.NAVER)).thenReturn(socialLoginClient);
		when(socialLoginClient.fetchUserInfo(any()))
			.thenReturn(socialUserInfo(SocialProvider.NAVER));
		when(socialAccountRepository.findByProviderAndProviderSubject(SocialProvider.NAVER, PROVIDER_SUBJECT))
			.thenReturn(Optional.of(SocialAccount.connect(user, SocialProvider.NAVER, PROVIDER_SUBJECT)));

		assertThatThrownBy(() -> userSocialLoginService.signIn(request(SocialProvider.NAVER)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS)
			);

		verify(userTokenService, never()).issue(any(), any());
	}

	private SocialLoginRequest request(SocialProvider provider) {
		return new SocialLoginRequest(provider, CODE, STATE, REDIRECT_URI);
	}

	private SocialOAuthProperties socialOAuthProperties() {
		SocialOAuthProperties properties = new SocialOAuthProperties();
		EnumMap<SocialProvider, SocialOAuthProperties.Provider> providers = new EnumMap<>(SocialProvider.class);
		providers.put(SocialProvider.NAVER, provider(
			"naver-client-id",
			"http://localhost:3000/callback/naver",
			"https://nid.naver.com/oauth2.0/authorize",
			"https://nid.naver.com/oauth2.0/token",
			"https://openapi.naver.com/v1/nid/me",
			List.of("profile", "email"),
			false
		));
		providers.put(SocialProvider.APPLE, appleProvider());
		properties.setProviders(providers);
		return properties;
	}

	private SocialOAuthProperties.Provider provider(
		String clientId,
		String redirectUri,
		String authorizationUri,
		String tokenUri,
		String userInfoUri,
		List<String> scopes,
		boolean openId
	) {
		SocialOAuthProperties.Provider provider = new SocialOAuthProperties.Provider();
		provider.setClientId(clientId);
		provider.setRedirectUri(redirectUri);
		provider.setAuthorizationUri(authorizationUri);
		provider.setTokenUri(tokenUri);
		provider.setUserInfoUri(userInfoUri);
		provider.setScopes(scopes);
		provider.setOpenId(openId);
		return provider;
	}

	private SocialOAuthProperties.Provider appleProvider() {
		SocialOAuthProperties.Provider provider = provider(
			"apple-client-id",
			"http://localhost:3000/callback/apple",
			"https://appleid.apple.com/auth/authorize",
			"https://appleid.apple.com/auth/token",
			null,
			List.of("openid", "email", "name"),
			true
		);
		provider.setJwkSetUri("https://appleid.apple.com/auth/keys");
		provider.setTeamId("team-id");
		provider.setKeyId("key-id");
		provider.setPrivateKey("private-key");
		return provider;
	}

	private Map<String, String> queryParams(String authorizationUrl) {
		String query = authorizationUrl.substring(authorizationUrl.indexOf('?') + 1);
		return Arrays.stream(query.split("&"))
			.map(parameter -> parameter.split("=", 2))
			.collect(Collectors.toMap(
				parameter -> decode(parameter[0]),
				parameter -> decode(parameter[1])
			));
	}

	private String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private SocialUserInfo socialUserInfo(SocialProvider provider) {
		return new SocialUserInfo(provider, PROVIDER_SUBJECT, EMAIL, NAME, PROFILE_IMAGE_URL);
	}

	private UserTokenIssueResult tokenIssueResult() {
		return new UserTokenIssueResult(
			TokenResponse.bearer("access-token", 1800L),
			"refresh-token",
			1_209_600L
		);
	}

	private User activeUser() {
		User user = User.createLocalUser(
			NAME,
			LOGIN_ID,
			"encoded-password",
			EMAIL,
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			PROFILE_IMAGE_URL,
			true,
			true
		);
		ReflectionTestUtils.setField(user, "id", USER_ID);
		return user;
	}

	private User withdrawnUser() {
		User user = activeUser();
		ReflectionTestUtils.setField(user, "status", UserStatus.WITHDRAWN);
		return user;
	}

	private void assertResponseDoesNotExposeProviderSubject(SocialLoginResponse response) {
		assertThat(Arrays.stream(response.getClass().getRecordComponents())
			.map(RecordComponent::getName)
		).doesNotContain("providerSubject");
	}
}
