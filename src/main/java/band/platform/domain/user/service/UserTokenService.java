package band.platform.domain.user.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;

import band.platform.domain.user.dto.TokenResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.repository.UserRefreshTokenRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.security.cookie.RefreshTokenCookieFactory;
import band.platform.global.security.jwt.JwtRefreshTokenClaims;
import band.platform.global.security.jwt.JwtToken;
import band.platform.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserTokenService {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRefreshTokenRepository userRefreshTokenRepository;
	private final RefreshTokenCookieFactory refreshTokenCookieFactory;
	private final UserSessionLockManager userSessionLockManager;

	public UserTokenIssueResult issue(Long userId, String loginId) {
		return userSessionLockManager.withLock(userId, () -> {
			String refreshTokenId = newRefreshTokenId();
			JwtToken accessToken = jwtTokenProvider.issueAccessToken(userId, loginId);
			JwtToken refreshToken = jwtTokenProvider.issueRefreshToken(userId, loginId, refreshTokenId);
			userRefreshTokenRepository.save(userId, refreshTokenId, refreshTokenTtl());

			return new UserTokenIssueResult(
				TokenResponse.bearer(accessToken.value(), accessToken.expiresIn()),
				refreshToken.value(),
				jwtTokenProvider.refreshTokenTtlSeconds()
			);
		});
	}

	public UserTokenIssueResult reissue(HttpServletRequest servletRequest) {

		String refreshToken = extractRefreshToken(servletRequest);

		JwtRefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
		return userSessionLockManager.withLock(claims.userId(), () -> {
			String newRefreshTokenId = newRefreshTokenId();
			boolean rotated = userRefreshTokenRepository.rotate(
				claims.userId(),
				claims.tokenId(),
				newRefreshTokenId,
				refreshTokenTtl()
			);
			if (!rotated) {
				throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
			}

			JwtToken accessToken = jwtTokenProvider.issueAccessToken(claims.userId(), claims.loginId());
			JwtToken newRefreshToken = jwtTokenProvider.issueRefreshToken(claims.userId(), claims.loginId(), newRefreshTokenId);

			return new UserTokenIssueResult(
				TokenResponse.bearer(accessToken.value(), accessToken.expiresIn()),
				newRefreshToken.value(),
				jwtTokenProvider.refreshTokenTtlSeconds()
			);
		});
	}

	public void logout(HttpServletRequest servletRequest) {
		try {
			refreshTokenCookieFactory.extract(servletRequest.getCookies())
				.ifPresent(this::deleteRefreshTokenIfValid);
		} catch (BusinessException exception) {
			if (!isIgnorableLogoutError(exception.getErrorCode())) {
				throw exception;
			}
		}
	}

	private Duration refreshTokenTtl() {
		return Duration.ofSeconds(jwtTokenProvider.refreshTokenTtlSeconds());
	}

	private String newRefreshTokenId() {
		return UUID.randomUUID().toString();
	}

	private String extractRefreshToken(HttpServletRequest request) {
		return refreshTokenCookieFactory.extract(request.getCookies())
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
	}

	private void deleteRefreshTokenIfValid(String refreshToken) {
		JwtRefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
		userSessionLockManager.withLock(claims.userId(), () ->
			userRefreshTokenRepository.delete(claims.userId(), claims.tokenId())
		);
	}

	private boolean isIgnorableLogoutError(ErrorCode errorCode) {
		return errorCode == ErrorCode.AUTH_TOKEN_INVALID
			|| errorCode == ErrorCode.AUTH_TOKEN_EXPIRED;
	}

}
