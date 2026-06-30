package band.platform.domain.user.social.client.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialAuthorizationCode;
import band.platform.domain.user.social.SocialOAuthProperties;
import band.platform.domain.user.social.SocialUserInfo;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class AppleSocialLoginClientTest {

	private static final String TOKEN_URI = "https://appleid.apple.com/auth/token";
	private static final String JWK_SET_URI = "https://appleid.apple.com/auth/keys";
	private static final String ID_TOKEN = "encoded.apple.id-token";

	@Test
	@DisplayName("Apple 인가 코드로 토큰을 교환하고 검증된 id_token을 소셜 사용자 정보로 변환한다")
	void fetchUserInfo() {
		StubAppleIdTokenVerifier verifier = new StubAppleIdTokenVerifier();
		Fixture fixture = fixture(verifier);
		expectTokenSuccess(fixture.server(), tokenResponse(ID_TOKEN));

		SocialUserInfo userInfo = fixture.client().fetchUserInfo(authorizationCode("oauth-nonce"));

		assertThat(userInfo.provider()).isEqualTo(SocialProvider.APPLE);
		assertThat(userInfo.providerSubject()).isEqualTo("apple-subject");
		assertThat(userInfo.email()).isEqualTo("member@example.com");
		assertThat(userInfo.name()).isNull();
		assertThat(userInfo.profileImageUrl()).isNull();
		assertThat(verifier.idToken()).isEqualTo(ID_TOKEN);
		assertThat(verifier.properties()).isSameAs(fixture.appleProperties());
		assertThat(verifier.expectedNonce()).isEqualTo("oauth-nonce");
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 요청은 state 없이 필수 form 필드만 body로 전송한다")
	void requestTokenWithRequiredFormFields() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), tokenResponse(ID_TOKEN));

		fixture.client().fetchUserInfo(authorizationCode("oauth-nonce"));

		fixture.server().verify();
	}

	@Test
	@DisplayName("요청 nonce가 없으면 null nonce를 id_token 검증기에 전달한다")
	void nullNonceCompatibility() {
		StubAppleIdTokenVerifier verifier = new StubAppleIdTokenVerifier();
		Fixture fixture = fixture(verifier);
		expectTokenSuccess(fixture.server(), tokenResponse(ID_TOKEN));

		fixture.client().fetchUserInfo(authorizationCode(null));

		assertThat(verifier.expectedNonce()).isNull();
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 API가 400을 반환하면 A07 에러로 변환한다")
	void invalidAuthorizationCode() {
		Fixture fixture = fixture();
		expectTokenError(fixture.server(), HttpStatus.BAD_REQUEST);

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_CODE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 API가 5xx를 반환하면 A15 에러로 변환한다")
	void tokenProviderUnavailable() {
		Fixture fixture = fixture();
		expectTokenServerError(fixture.server());

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 API 통신 예외가 발생하면 A15 에러로 변환한다")
	void tokenNetworkFailure() {
		Fixture fixture = fixture();
		fixture.server().expect(once(), requestTo(TOKEN_URI))
			.andExpect(method(HttpMethod.POST))
			.andRespond(request -> {
				throw new ResourceAccessException("Read timed out");
			});

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 응답 JSON이 깨져 있으면 A14 에러로 변환한다")
	void malformedTokenJson() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), "{");

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 응답 body가 없으면 A14 에러로 변환한다")
	void nullTokenBody() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), "");

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 응답에 id_token이 없으면 A14 에러로 변환한다")
	void missingIdToken() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), "{}");

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 응답 id_token이 null이면 A14 에러로 변환한다")
	void nullIdToken() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), """
			{
			  "id_token": null
			}
			""");

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 응답 id_token이 비어 있으면 A14 에러로 변환한다")
	void blankIdToken() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), """
			{
			  "id_token": " "
			}
			""");

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("id_token 검증기가 provider 응답 오류를 반환하면 A14 에러로 전파한다")
	void verifierRejection() {
		StubAppleIdTokenVerifier verifier = new StubAppleIdTokenVerifier();
		verifier.throwError(ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		Fixture fixture = fixture(verifier);
		expectTokenSuccess(fixture.server(), tokenResponse(ID_TOKEN));

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 설정 조회에 실패하면 A13 에러로 변환한다")
	void invalidConfiguration() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		AppleSocialLoginClient client = new AppleSocialLoginClient(
			builder.build(),
			new SocialOAuthProperties(),
			new StubAppleClientSecretGenerator(),
			new StubAppleIdTokenVerifier()
		);

		assertBusinessError(client, authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		server.verify();
	}

	@Test
	@DisplayName("Apple client가 아닌 provider 요청이면 A13 에러로 변환한다")
	void providerMismatch() {
		Fixture fixture = fixture();

		assertBusinessError(
			fixture.client(),
			authorizationCode(SocialProvider.KAKAO, "oauth-nonce"),
			ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID
		);
		fixture.server().verify();
	}

	@Test
	@DisplayName("client_secret 생성기가 설정 오류를 반환하면 A13 에러를 전파한다")
	void clientSecretGenerationFailure() {
		StubAppleClientSecretGenerator generator = new StubAppleClientSecretGenerator();
		generator.throwError(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		Fixture fixture = fixture(generator, new StubAppleIdTokenVerifier());

		assertBusinessError(fixture.client(), authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("Apple 토큰 URI 형식이 잘못되면 A13 에러로 변환한다")
	void malformedTokenUri() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialOAuthProperties properties = properties();
		properties.provider(SocialProvider.APPLE).setTokenUri("http://[invalid");
		AppleSocialLoginClient client = new AppleSocialLoginClient(
			builder.build(),
			properties,
			new StubAppleClientSecretGenerator(),
			new StubAppleIdTokenVerifier()
		);

		assertBusinessError(client, authorizationCode("oauth-nonce"), ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		server.verify();
	}

	private static Fixture fixture() {
		return fixture(new StubAppleIdTokenVerifier());
	}

	private static Fixture fixture(StubAppleIdTokenVerifier verifier) {
		return fixture(new StubAppleClientSecretGenerator(), verifier);
	}

	private static Fixture fixture(StubAppleClientSecretGenerator generator, StubAppleIdTokenVerifier verifier) {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialOAuthProperties properties = properties();
		AppleSocialLoginClient client = new AppleSocialLoginClient(
			builder.build(),
			properties,
			generator,
			verifier
		);
		return new Fixture(client, server, properties.provider(SocialProvider.APPLE));
	}

	private static void expectTokenSuccess(MockRestServiceServer server, String responseBody) {
		server.expect(once(), requestTo(TOKEN_URI))
			.andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("state="))
			.andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("client_secret="))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(content().formData(tokenRequestForm()))
			.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
	}

	private static void expectTokenError(MockRestServiceServer server, HttpStatus status) {
		server.expect(once(), requestTo(TOKEN_URI))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(content().formData(tokenRequestForm()))
			.andRespond(withStatus(status).body("""
				{
				  "error": "invalid_grant",
				  "error_description": "invalid authorization code"
				}
				""").contentType(MediaType.APPLICATION_JSON));
	}

	private static void expectTokenServerError(MockRestServiceServer server) {
		server.expect(once(), requestTo(TOKEN_URI))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(content().formData(tokenRequestForm()))
			.andRespond(withServerError());
	}

	private static MultiValueMap<String, String> tokenRequestForm() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", "apple-client-id");
		form.add("client_secret", "generated-client-secret");
		form.add("redirect_uri", "https://band.example.com/oauth/apple/callback");
		form.add("code", "authorization-code");
		return form;
	}

	private static String tokenResponse(String idToken) {
		return """
			{
			  "id_token": "%s"
			}
			""".formatted(idToken);
	}

	private static SocialOAuthProperties properties() {
		SocialOAuthProperties properties = new SocialOAuthProperties();
		SocialOAuthProperties.Provider provider = new SocialOAuthProperties.Provider();
		provider.setClientId("apple-client-id");
		provider.setRedirectUri("https://band.example.com/oauth/apple/callback");
		provider.setAuthorizationUri("https://appleid.apple.com/auth/authorize");
		provider.setTokenUri(TOKEN_URI);
		provider.setJwkSetUri(JWK_SET_URI);
		provider.setTeamId("apple-team-id");
		provider.setKeyId("apple-key-id");
		provider.setPrivateKey("-----BEGIN PRIVATE KEY-----\\nfake\\n-----END PRIVATE KEY-----");
		properties.setProviders(Map.of(SocialProvider.APPLE, provider));
		return properties;
	}

	private static SocialAuthorizationCode authorizationCode(String nonce) {
		return authorizationCode(SocialProvider.APPLE, nonce);
	}

	private static SocialAuthorizationCode authorizationCode(SocialProvider provider, String nonce) {
		return new SocialAuthorizationCode(
			provider,
			"authorization-code",
			"oauth-state",
			"https://band.example.com/oauth/apple/callback",
			nonce
		);
	}

	private static void assertBusinessError(
		AppleSocialLoginClient client,
		SocialAuthorizationCode authorizationCode,
		ErrorCode errorCode
	) {
		assertThatThrownBy(() -> client.fetchUserInfo(authorizationCode))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(errorCode)
			);
	}

	private record Fixture(
		AppleSocialLoginClient client,
		MockRestServiceServer server,
		SocialOAuthProperties.Provider appleProperties
	) {
	}

	private static class StubAppleClientSecretGenerator extends AppleClientSecretGenerator {

		private ErrorCode errorCode;

		StubAppleClientSecretGenerator() {
			super(Clock.systemUTC());
		}

		@Override
		public String generate(SocialOAuthProperties.Provider properties) {
			if (errorCode != null) {
				throw new BusinessException(errorCode);
			}
			return "generated-client-secret";
		}

		void throwError(ErrorCode errorCode) {
			this.errorCode = errorCode;
		}
	}

	private static class StubAppleIdTokenVerifier extends AppleIdTokenVerifier {

		private String idToken;
		private SocialOAuthProperties.Provider properties;
		private String expectedNonce;
		private ErrorCode errorCode;

		@Override
		public AppleIdTokenClaims verifyClaims(
			String idToken,
			SocialOAuthProperties.Provider properties,
			String expectedNonce
		) {
			this.idToken = idToken;
			this.properties = properties;
			this.expectedNonce = expectedNonce;
			if (errorCode != null) {
				throw new BusinessException(errorCode);
			}
			return new AppleIdTokenClaims("apple-subject", "member@example.com");
		}

		void throwError(ErrorCode errorCode) {
			this.errorCode = errorCode;
		}

		String idToken() {
			return idToken;
		}

		SocialOAuthProperties.Provider properties() {
			return properties;
		}

		String expectedNonce() {
			return expectedNonce;
		}
	}
}
