package band.platform.domain.user.social.client.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
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

class KakaoSocialLoginClientTest {

	private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
	private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
	private static final String USER_INFO_URI_WITH_SECURE_RESOURCE = USER_INFO_URI + "?secure_resource=true";

	@Test
	@DisplayName("카카오 인가 코드로 토큰과 프로필을 조회해 소셜 사용자 정보로 변환한다")
	void fetchUserInfo() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI_WITH_SECURE_RESOURCE))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-access-token"))
			.andRespond(withSuccess("""
				{
				  "id": 123456789,
				  "kakao_account": {
				    "email": "member@example.com",
				    "is_email_valid": true,
				    "is_email_verified": true,
				    "name": "카카오회원",
				    "profile": {
				      "nickname": "ignored",
				      "profile_image_url": "https://image.example.com/profile.png"
				    }
				  }
				}
				""", MediaType.APPLICATION_JSON));

		SocialUserInfo userInfo = fixture.client().fetchUserInfo(authorizationCode(SocialProvider.KAKAO));

		assertThat(userInfo.provider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(userInfo.providerSubject()).isEqualTo("123456789");
		assertThat(userInfo.email()).isEqualTo("member@example.com");
		assertThat(userInfo.name()).isEqualTo("카카오회원");
		assertThat(userInfo.profileImageUrl()).isEqualTo("https://image.example.com/profile.png");
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 계정 이름이 없으면 프로필 닉네임을 이름으로 변환한다")
	void fallbackNickname() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI_WITH_SECURE_RESOURCE))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-access-token"))
			.andRespond(withSuccess("""
				{
				  "id": 123456789,
				  "kakao_account": {
				    "email": "member@example.com",
				    "profile": {
				      "nickname": "카카오닉네임",
				      "profile_image_url": "https://image.example.com/profile.png"
				    }
				  }
				}
				""", MediaType.APPLICATION_JSON));

		SocialUserInfo userInfo = fixture.client().fetchUserInfo(authorizationCode(SocialProvider.KAKAO));

		assertThat(userInfo.name()).isEqualTo("카카오닉네임");
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 선택 프로필 값이 없어도 사용자 정보를 변환한다")
	void optionalProfileFields() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI_WITH_SECURE_RESOURCE))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				{
				  "id": 123456789,
				  "kakao_account": {}
				}
				""", MediaType.APPLICATION_JSON));

		SocialUserInfo userInfo = fixture.client().fetchUserInfo(authorizationCode(SocialProvider.KAKAO));

		assertThat(userInfo.email()).isNull();
		assertThat(userInfo.name()).isNull();
		assertThat(userInfo.profileImageUrl()).isNull();
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 이메일이 유효하지 않으면 이메일을 null로 변환한다")
	void invalidEmail() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI_WITH_SECURE_RESOURCE))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-access-token"))
			.andRespond(withSuccess("""
				{
				  "id": 123456789,
				  "kakao_account": {
				    "email": "member@example.com",
				    "is_email_valid": false,
				    "is_email_verified": true
				  }
				}
				""", MediaType.APPLICATION_JSON));

		SocialUserInfo userInfo = fixture.client().fetchUserInfo(authorizationCode(SocialProvider.KAKAO));

		assertThat(userInfo.email()).isNull();
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 이메일이 인증되지 않았으면 이메일을 null로 변환한다")
	void unverifiedEmail() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI_WITH_SECURE_RESOURCE))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-access-token"))
			.andRespond(withSuccess("""
				{
				  "id": 123456789,
				  "kakao_account": {
				    "email": "member@example.com",
				    "is_email_valid": true,
				    "is_email_verified": false
				  }
				}
				""", MediaType.APPLICATION_JSON));

		SocialUserInfo userInfo = fixture.client().fetchUserInfo(authorizationCode(SocialProvider.KAKAO));

		assertThat(userInfo.email()).isNull();
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 토큰 API가 400을 반환하면 A07 에러로 변환한다")
	void invalidAuthorizationCode() {
		Fixture fixture = fixture();
		expectTokenError(fixture.server(), HttpStatus.BAD_REQUEST);

		assertBusinessError(fixture.client(), ErrorCode.AUTH_CODE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 토큰 API가 5xx를 반환하면 A15 에러로 변환한다")
	void tokenProviderUnavailable() {
		Fixture fixture = fixture();
		expectTokenServerError(fixture.server());

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 프로필 API가 5xx를 반환하면 A15 에러로 변환한다")
	void profileProviderUnavailable() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI_WITH_SECURE_RESOURCE))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withServerError());

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 프로필 API가 400을 반환하면 A14 에러로 변환한다")
	void profileBadRequest() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI_WITH_SECURE_RESOURCE))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-access-token"))
			.andRespond(withStatus(HttpStatus.BAD_REQUEST).body("""
				{
				  "code": -2,
				  "msg": "invalid request"
				}
				""").contentType(MediaType.APPLICATION_JSON));

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 provider 통신 예외가 발생하면 A15 에러로 변환한다")
	void providerTimeout() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI_WITH_SECURE_RESOURCE))
			.andExpect(method(HttpMethod.GET))
			.andRespond(request -> {
				throw new ResourceAccessException("Read timed out");
			});

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_UNAVAILABLE);
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 토큰 응답 JSON이 깨져 있으면 A14 에러로 변환한다")
	void malformedTokenJson() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), "{");

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 토큰 응답에 access_token이 비어 있으면 A14 에러로 변환한다")
	void blankAccessToken() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server(), """
			{
			  "access_token": " ",
			  "refresh_token": "fake-refresh-token",
			  "token_type": "bearer",
			  "expires_in": 3600
			}
			""");

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_PROVIDER_RESPONSE_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 프로필 응답에 id가 없으면 A12 에러로 변환한다")
	void missingProviderSubject() {
		Fixture fixture = fixture();
		expectTokenSuccess(fixture.server());
		fixture.server().expect(once(), requestTo(USER_INFO_URI_WITH_SECURE_RESOURCE))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
				{
				  "kakao_account": {
				    "email": "member@example.com"
				  }
				}
				""", MediaType.APPLICATION_JSON));

		assertBusinessError(fixture.client(), ErrorCode.AUTH_SOCIAL_USER_INFO_INVALID);
		fixture.server().verify();
	}

	@Test
	@DisplayName("카카오 설정 조회에 실패하면 A13 에러로 변환한다")
	void invalidConfiguration() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		KakaoSocialLoginClient client = new KakaoSocialLoginClient(builder.build(), new SocialOAuthProperties());

		assertBusinessError(client, ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		server.verify();
	}

	@Test
	@DisplayName("카카오 프로필 URI 형식이 잘못되면 토큰 성공 후 A13 에러로 변환한다")
	void malformedUserInfoUri() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		SocialOAuthProperties properties = properties();
		properties.provider(SocialProvider.KAKAO).setUserInfoUri("http://[invalid");
		KakaoSocialLoginClient client = new KakaoSocialLoginClient(builder.build(), properties);
		expectTokenSuccess(server);

		assertBusinessError(client, ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID);
		server.verify();
	}

	@Test
	@DisplayName("카카오 client가 아닌 provider 요청이면 A13 에러로 변환한다")
	void providerMismatch() {
		Fixture fixture = fixture();

		assertThatThrownBy(() -> fixture.client().fetchUserInfo(authorizationCode(SocialProvider.NAVER)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_CONFIGURATION_INVALID)
			);
		fixture.server().verify();
	}

	private static void expectTokenSuccess(MockRestServiceServer server) {
		expectTokenSuccess(server, """
			{
			  "access_token": "fake-access-token",
			  "refresh_token": "fake-refresh-token",
			  "token_type": "bearer",
			  "expires_in": 3600
			}
			""");
	}

	private static void expectTokenSuccess(MockRestServiceServer server, String responseBody) {
		server.expect(once(), requestTo(TOKEN_URI))
			.andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("client_secret="))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(content().formData(tokenRequestForm()))
			.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
	}

	private static void expectTokenError(MockRestServiceServer server, HttpStatus status) {
		server.expect(once(), requestTo(TOKEN_URI))
			.andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("client_secret="))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(content().formData(tokenRequestForm()))
			.andRespond(withStatus(status).body("""
				{
				  "error": "invalid_grant",
				  "error_description": "authorization code not found"
				}
				""").contentType(MediaType.APPLICATION_JSON));
	}

	private static void expectTokenServerError(MockRestServiceServer server) {
		server.expect(once(), requestTo(TOKEN_URI))
			.andExpect(request -> assertThat(request.getURI().toString()).doesNotContain("client_secret="))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(content().formData(tokenRequestForm()))
			.andRespond(withServerError());
	}

	private static MultiValueMap<String, String> tokenRequestForm() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", "kakao-client-id");
		form.add("client_secret", "kakao-client-secret");
		form.add("redirect_uri", "https://band.example.com/oauth/kakao/callback");
		form.add("code", "authorization-code");
		form.add("state", "oauth-state");
		return form;
	}

	private static void assertBusinessError(KakaoSocialLoginClient client, ErrorCode errorCode) {
		assertThatThrownBy(() -> client.fetchUserInfo(authorizationCode(SocialProvider.KAKAO)))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(errorCode)
			);
	}

	private static Fixture fixture() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		return new Fixture(new KakaoSocialLoginClient(builder.build(), properties()), server);
	}

	private static SocialOAuthProperties properties() {
		SocialOAuthProperties properties = new SocialOAuthProperties();
		SocialOAuthProperties.Provider provider = new SocialOAuthProperties.Provider();
		provider.setClientId("kakao-client-id");
		provider.setClientSecret("kakao-client-secret");
		provider.setRedirectUri("https://band.example.com/oauth/kakao/callback");
		provider.setAuthorizationUri("https://kauth.kakao.com/oauth/authorize");
		provider.setTokenUri(TOKEN_URI);
		provider.setUserInfoUri(USER_INFO_URI);
		properties.setProviders(Map.of(SocialProvider.KAKAO, provider));
		return properties;
	}

	private static SocialAuthorizationCode authorizationCode(SocialProvider provider) {
		return new SocialAuthorizationCode(
			provider,
			"authorization-code",
			"oauth-state",
			"https://band.example.com/oauth/kakao/callback"
		);
	}

	private record Fixture(KakaoSocialLoginClient client, MockRestServiceServer server) {
	}

}
