package band.platform.domain.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import band.platform.domain.user.dto.TokenResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.service.UserTokenService;
import band.platform.global.ApiResult;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.security.RefreshTokenCookieFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserTokenService userTokenService;
	private final RefreshTokenCookieFactory refreshTokenCookieFactory;

	@PostMapping("/reissue")
	public ResponseEntity<ApiResult<TokenResponse>> reissue(HttpServletRequest request) {
		String refreshToken = extractRefreshToken(request);
		UserTokenIssueResult tokenIssueResult = userTokenService.reissue(refreshToken);

		return ResponseEntity.ok()
			.header(
				HttpHeaders.SET_COOKIE,
				refreshTokenCookieFactory.create(tokenIssueResult.refreshToken(), tokenIssueResult.refreshTokenMaxAgeSeconds())
					.toString()
			)
			.body(ApiResult.ok(tokenIssueResult.tokenResponse()));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResult<Void>> logout(HttpServletRequest request) {
		refreshTokenCookieFactory.extract(request.getCookies())
			.ifPresent(userTokenService::logout);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.delete().toString())
			.body(ApiResult.ok());
	}

	private String extractRefreshToken(HttpServletRequest request) {
		return refreshTokenCookieFactory.extract(request.getCookies())
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
	}

}
