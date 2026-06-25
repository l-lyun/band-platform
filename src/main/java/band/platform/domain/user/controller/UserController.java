package band.platform.domain.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import band.platform.domain.user.dto.FindLoginIdRequest;
import band.platform.domain.user.dto.FindLoginIdResponse;
import band.platform.domain.user.dto.UserLoginRequest;
import band.platform.domain.user.dto.UserLoginResponse;
import band.platform.domain.user.dto.UserSignupRequest;
import band.platform.domain.user.dto.UserSignupResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.service.UserFindLoginIdService;
import band.platform.domain.user.service.UserLoginService;
import band.platform.domain.user.service.UserSignupService;
import band.platform.domain.user.service.UserTokenService;
import band.platform.global.ApiResult;
import band.platform.global.security.cookie.RefreshTokenCookieFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserSignupService userSignupService;
	private final UserLoginService userLoginService;
	private final UserFindLoginIdService userFindLoginIdService;
	private final UserTokenService userTokenService;
	private final RefreshTokenCookieFactory refreshTokenCookieFactory;

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

	@PostMapping("/find-login-id")
	public ResponseEntity<ApiResult<FindLoginIdResponse>> findLoginId(
		@Valid @RequestBody FindLoginIdRequest request
	) {
		return ApiResult.ok(userFindLoginIdService.findLoginId(request)).toResponseEntity();
	}

}
