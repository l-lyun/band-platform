package band.platform.domain.user.social.client.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.social.SocialAuthorizationCode;
import band.platform.domain.user.social.SocialOAuthProperties;
import band.platform.domain.user.social.SocialUserInfo;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

import org.springframework.test.web.client.MockRestServiceServer;

class NaverSocialLoginClientTest {

	private static final String TOKEN_URI = "https://nid.naver.com/oauth2.0/token";
	private static final String USER_INFO_URI = "https://openapi.naver.com/v1/nid/me";

	@Test
	@DisplayName("네이버 인가 코드로 토큰과 프로필을 조회해 소셜 사용자 정보로 변환한다")
	void fetchUserInfo() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-access-token"))
			.andRespond(withSuccess("""
				{
				  "resultcode": "00",
				  "message": "success",
				  "unknown": "ignored",
				  "response": {
				    "id": "naver-subject",
				    "email": "member@example.com",
				    "name": "네이버회원",
				    "profile_image": "https://image.example.com/profile.png",
				    "nickname": "ignored"
				  }
				}
				""", MediaType.APPLICATION_JSON));

		SocialUserInfo userInfo = fixture.client().fetchUserInfo(authorizationCode());

		assertThat(userInfo.provider()).isEqualTo(SocialProvider.NAVER);
		assertThat(userInfo.providerSubject()).isEqualTo("naver-subject");
		assertThat(userInfo.email()).isEqualTo("member@example.com");
		assertThat(userInfo.name()).isEqualTo("네이버회원");
		assertThat(userInfo.profileImageUrl()).isEqualTo("https://image.example.com/profile.png");
		fixture.server().verify();
	}

	@Test
	@DisplayName("네이버 토큰 API가 400을 반환하면 A07 에러로 변환한다")
	void invalidAuthorizationCode() {
		Fixture fixture = fixture();
		expectTokenError(fixture.server(), HttpStatus.BAD_REQUEST);

		assertBusinessError(fixture.client(), ErrorCode.AUTH_CODE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("네이버 토큰 API가 5xx를 반환하면 A15 에러로 변환한다")
	void tokenProviderUnavailable() {
		Fixture fixture = fixture();
		expectTokenServerError(fixture.server());

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		fixture.server().verify();
	}

	@Test
	@DisplayName("네이버 프로필 API가 5xx를 반환하면 A15 에러로 변환한다")
	void profileProviderUnavailable() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withServerError());

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		fixture.server().verify();
	}

	@Test
	@DisplayName("네이버 provider 통신 예외가 발생하면 A15 에러로 변환한다")
	void providerTimeout() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI))
			.andExpect(method(HttpMethod.GET))
			.andRespond(request -> {
				throw new ResourceAccessException("Read timed out");
			});

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		fixture.server().verify();
	}

	@Test
	@DisplayName("네이버 토큰 응답 JSON이 깨져 있으면 A14 에러로 변환한다")
	void malformedTokenJson() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), "{");

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("네이버 토큰 응답에 access_token이 비어 있으면 A14 에러로 변환한다")
	void emptyAccessToken() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), """
			{
			  "access_token": " ",
			  "refresh_token": "fake-refresh-token",
			  "token_type": "bearer",
			  "expires_in": "3600"
			}
			""");

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("네이버 프로필 응답에 response.id가 없으면 A12 에러로 변환한다")
	void missingProviderSubject() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				{
				  "resultcode": "00",
				  "message": "success",
				  "response": {
				    "email": "member@example.com"
				  }
				}
				""", MediaType.APPLICATION_JSON));

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_USER_INFO_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("네이버 설정 조회에 실패하면 A13 에러로 변환한다")
	void invalidConfiguration() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		NaverSocialLoginClient client = new NaverSocialLoginClient(builder.build(), new SocialOAuthProperties());

		assertBusinessError(client, ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		server.verify();
	}

	private static void expectTokenSuccess(MockRestServiceServer server) {
		expectTokenSuccess(server, """
			{
			  "access_token": "fake-access-token",
			  "refresh_token": "fake-refresh-token",
			  "token_type": "bearer",
			  "expires_in": "3600"
			}
			""");
	}

	private static void expectTokenSuccess(MockRestServiceServer server, String responseBody) {
		server.expect(once(), requestTo(startsWith(TOKEN_URI)))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("grant_type", "authorization_code"))
			.andExpect(queryParam("client_id", "naver-client-id"))
			.andExpect(queryParam("client_secret", "naver-client-secret"))
			.andExpect(queryParam("redirect_uri", "https://band.example.com/oauth/naver/callback"))
			.andExpect(queryParam("code", "authorization-code"))
			.andExpect(queryParam("state", "oauth-state"))
			.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
	}

	private static void expectTokenError(MockRestServiceServer server, HttpStatus status) {
		server.expect(once(), requestTo(startsWith(TOKEN_URI)))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withStatus(status).body("""
				{
				  "error": "invalid_request",
				  "error_description": "invalid authorization code"
				}
				""").contentType(MediaType.APPLICATION_JSON));
	}

	private static void expectTokenServerError(MockRestServiceServer server) {
		server.expect(once(), requestTo(startsWith(TOKEN_URI)))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withServerError());
	}

	private static void assertBusinessError(NaverSocialLoginClient client, ErrorCode errorCode) {
		assertThatThrownBy(() -> client.fetchUserInfo(authorizationCode()))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(errorCode)
			);
	}

	private static Fixture fixture() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		return new Fixture(new NaverSocialLoginClient(builder.build(), properties()), server);
	}

	private static SocialOAuthProperties properties() {
		SocialOAuthProperties properties = new SocialOAuthProperties();
		SocialOAuthProperties.Provider provider = new SocialOAuthProperties.Provider();
		provider.setClientId("naver-client-id");
		provider.setClientSecret("naver-client-secret");
		provider.setRedirectUri("https://band.example.com/oauth/naver/callback");
		provider.setAuthorizationUri("https://nid.naver.com/oauth2.0/authorize");
		provider.setTokenUri(TOKEN_URI);
		provider.setUserInfoUri(USER_INFO_URI);
		properties.setProviders(Map.of(SocialProvider.NAVER, provider));
		return properties;
	}

	private static SocialAuthorizationCode authorizationCode() {
		return new SocialAuthorizationCode(
			SocialProvider.NAVER,
			"authorization-code",
			"oauth-state",
			"https://band.example.com/oauth/naver/callback"
		);
	}

	private record Fixture(NaverSocialLoginClient client, MockRestServiceServer server) {
	}

}
