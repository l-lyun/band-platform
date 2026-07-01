package band.platform.domain.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import band.platform.domain.user.dto.FindLoginIdRequest;
import band.platform.domain.user.dto.FindLoginIdResponse;
import band.platform.domain.user.dto.PasswordResetCompleteRequest;
import band.platform.domain.user.dto.PasswordResetRequestCodeRequest;
import band.platform.domain.user.dto.PasswordResetVerifyCodeRequest;
import band.platform.domain.user.dto.SocialLoginRequest;
import band.platform.domain.user.dto.SocialLoginResponse;
import band.platform.domain.user.dto.SocialLoginStartRequest;
import band.platform.domain.user.dto.SocialLoginStartResponse;
import band.platform.domain.user.dto.SocialSignupRequest;
import band.platform.domain.user.dto.UserLoginRequest;
import band.platform.domain.user.dto.UserLoginResponse;
import band.platform.domain.user.dto.UserSignupRequest;
import band.platform.domain.user.dto.UserSignupResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.service.UserLoginService;
import band.platform.domain.user.service.UserPasswordResetService;
import band.platform.domain.user.service.UserSocialLoginResult;
import band.platform.domain.user.service.UserSocialLoginService;
import band.platform.domain.user.service.UserSignupService;
import band.platform.domain.user.service.UserTokenService;
import band.platform.global.ApiResult;
import band.platform.global.security.cookie.PasswordResetTokenCookieFactory;
import band.platform.global.security.cookie.RefreshTokenCookieFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserSignupService userSignupService;
	private final UserLoginService userLoginService;
	private final UserTokenService userTokenService;
	private final UserPasswordResetService userPasswordResetService;
	private final UserSocialLoginService userSocialLoginService;
	private final RefreshTokenCookieFactory refreshTokenCookieFactory;
	private final PasswordResetTokenCookieFactory passwordResetTokenCookieFactory;

	@PostMapping("/sign-up")
	public ResponseEntity<ApiResult<UserSignupResponse>> signup(
		@Valid @RequestBody UserSignupRequest request
	) {
		return ApiResult.created(userSignupService.signup(request)).toResponseEntity();
	}

	@PostMapping("/sign-in")
	public ResponseEntity<ApiResult<UserLoginResponse>> login(
		@Valid @RequestBody UserLoginRequest request
	) {
		UserLoginResponse loginResponse = userLoginService.login(request);
		UserTokenIssueResult tokenIssueResult = userTokenService.issue(loginResponse.id(), loginResponse.loginId());

		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				refreshTokenCookieFactory.create(tokenIssueResult.refreshToken(), tokenIssueResult.refreshTokenMaxAgeSeconds())
					.toString()
			)
			.body(ApiResult.ok(loginResponse.withToken(tokenIssueResult.tokenResponse())));
	}

	@PostMapping("/social/sign-in")
	public ResponseEntity<ApiResult<SocialLoginResponse>> socialSignIn(
		@Valid @RequestBody SocialLoginRequest request
	) {
		UserSocialLoginResult socialLoginResult = userSocialLoginService.signIn(request);
		UserTokenIssueResult tokenIssueResult = socialLoginResult.tokenIssueResult();
		if (tokenIssueResult == null) {
			return ApiResult.ok(socialLoginResult.response()).toResponseEntity();
		}

		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				refreshTokenCookieFactory.create(tokenIssueResult.refreshToken(), tokenIssueResult.refreshTokenMaxAgeSeconds())
					.toString()
			)
			.body(ApiResult.ok(socialLoginResult.response()));
	}

	@PostMapping("/social/sign-up")
	public ResponseEntity<ApiResult<SocialLoginResponse>> socialSignup(
		@Valid @RequestBody SocialSignupRequest request
	) {
		UserSocialLoginResult socialLoginResult = userSocialLoginService.signup(request);
		UserTokenIssueResult tokenIssueResult = socialLoginResult.tokenIssueResult();

		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				refreshTokenCookieFactory.create(tokenIssueResult.refreshToken(), tokenIssueResult.refreshTokenMaxAgeSeconds())
					.toString()
			)
			.body(ApiResult.ok(socialLoginResult.response()));
	}

	@PostMapping("/social/authorization")
	public ResponseEntity<ApiResult<SocialLoginStartResponse>> socialAuthorization(
		@Valid @RequestBody SocialLoginStartRequest request
	) {
		return ApiResult.ok(userSocialLoginService.start(request)).toResponseEntity();
	}

	@PostMapping("/find-login-id")
	public ResponseEntity<ApiResult<FindLoginIdResponse>> findLoginId(
		@Valid @RequestBody FindLoginIdRequest request
	) {
		return ApiResult.ok(userLoginService.findLoginId(request)).toResponseEntity();
	}

	@PostMapping("/password-reset/request")
	public ResponseEntity<ApiResult<Void>> requestPasswordResetCode(
		@Valid @RequestBody PasswordResetRequestCodeRequest request
	) {
		userPasswordResetService.request(request.loginId(), request.email());
		return ApiResult.ok().toResponseEntity();
	}

	@PostMapping("/password-reset/verify")
	public ResponseEntity<ApiResult<Void>> verifyPasswordResetCode(
		@Valid @RequestBody PasswordResetVerifyCodeRequest request
	) {
		String resetToken = userPasswordResetService.verify(request.loginId(), request.email(), request.code());
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, passwordResetTokenCookieFactory.create(resetToken).toString())
			.body(ApiResult.ok());
	}

	@PostMapping("/password-reset/complete")
	public ResponseEntity<ApiResult<Void>> completePasswordReset(
		@CookieValue(name = PasswordResetTokenCookieFactory.COOKIE_NAME, required = false) String resetToken,
		@Valid @RequestBody PasswordResetCompleteRequest request
	) {
		userPasswordResetService.complete(resetToken, request.newPassword());
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, passwordResetTokenCookieFactory.delete().toString())
			.body(ApiResult.ok());
	}

}
