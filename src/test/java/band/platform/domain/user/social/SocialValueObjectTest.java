package band.platform.domain.user.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import band.platform.domain.user.entity.SocialProvider;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class SocialValueObjectTest {

	@Test
	@DisplayName("인가 코드 요청 값은 제공자와 code, state, redirectUri, nonce를 가진다")
	void authorizationCode() {
		SocialAuthorizationCode authorizationCode = new SocialAuthorizationCode(
			SocialProvider.NAVER,
			"authorization-code",
			"state",
			"http://localhost:3000/callback/naver",
			"nonce"
		);

		assertThat(authorizationCode.provider()).isEqualTo(SocialProvider.NAVER);
		assertThat(authorizationCode.code()).isEqualTo("authorization-code");
		assertThat(authorizationCode.state()).isEqualTo("state");
		assertThat(authorizationCode.redirectUri()).isEqualTo("http://localhost:3000/callback/naver");
		assertThat(authorizationCode.nonce()).isEqualTo("nonce");
	}

	@Test
	@DisplayName("소셜 사용자 정보는 제공자와 제공자 고유 식별자가 필수다")
	void userInfo() {
		SocialUserInfo userInfo = new SocialUserInfo(
			SocialProvider.APPLE,
			"apple-subject",
			null,
			null,
			null
		);

		assertThat(userInfo.provider()).isEqualTo(SocialProvider.APPLE);
		assertThat(userInfo.providerSubject()).isEqualTo("apple-subject");
	}

	@Test
	@DisplayName("OAuth state는 state와 nonce를 함께 가진다")
	void oauthState() {
		SocialOAuthState oauthState = new SocialOAuthState("state", "nonce");

		assertThat(oauthState.state()).isEqualTo("state");
		assertThat(oauthState.nonce()).isEqualTo("nonce");
	}

	@Test
	@DisplayName("LOCAL 제공자는 소셜 인가 코드에 사용할 수 없다")
	void localAuthorizationCodeProvider() {
		assertThatThrownBy(() -> new SocialAuthorizationCode(SocialProvider.LOCAL, "code", "state", "redirect-uri"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_PROVIDER_UNSUPPORTED)
			);
	}

	@Test
	@DisplayName("인가 코드가 비어 있으면 A07 에러를 반환한다")
	void blankAuthorizationCode() {
		assertThatThrownBy(() -> new SocialAuthorizationCode(SocialProvider.NAVER, " ", "state", "redirect-uri"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_CODE_INVALID)
			);
	}

	@Test
	@DisplayName("state가 비어 있으면 A10 에러를 반환한다")
	void blankAuthorizationState() {
		assertThatThrownBy(() -> new SocialAuthorizationCode(SocialProvider.NAVER, "code", " ", "redirect-uri"))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_OAUTH_STATE_INVALID)
			);
	}

	@Test
	@DisplayName("redirectUri가 비어 있으면 A11 에러를 반환한다")
	void blankRedirectUri() {
		assertThatThrownBy(() -> new SocialAuthorizationCode(SocialProvider.NAVER, "code", "state", " "))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_REDIRECT_URI_INVALID)
			);
	}

	@Test
	@DisplayName("제공자 고유 식별자가 비어 있으면 A12 에러를 반환한다")
	void blankProviderSubject() {
		assertThatThrownBy(() -> new SocialUserInfo(SocialProvider.KAKAO, " ", null, null, null))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_SOCIAL_USER_INFO_INVALID)
			);
	}

	@Test
	@DisplayName("nonce가 필요 없는 제공자는 인가 코드 nonce를 비워둘 수 있다")
	void authorizationCodeWithoutNonce() {
		SocialAuthorizationCode authorizationCode = new SocialAuthorizationCode(
			SocialProvider.KAKAO,
			"authorization-code",
			"state",
			"http://localhost:3000/callback/kakao"
		);

		assertThat(authorizationCode.nonce()).isNull();
	}

	@Test
	@DisplayName("nonce가 비어 있으면 A10 에러를 반환한다")
	void blankAuthorizationCodeNonce() {
		assertThatThrownBy(() -> new SocialAuthorizationCode(SocialProvider.NAVER, "code", "state", "redirect-uri", " "))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_OAUTH_STATE_INVALID)
			);
	}

	@Test
	@DisplayName("OAuth state nonce가 비어 있으면 A10 에러를 반환한다")
	void blankNonce() {
		assertThatThrownBy(() -> new SocialOAuthState("state", " "))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_OAUTH_STATE_INVALID)
			);
	}

}
