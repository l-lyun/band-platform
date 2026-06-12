package band.platform.domain.user.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;

import band.platform.domain.user.dto.TokenResponse;
import band.platform.domain.user.dto.UserTokenIssueResult;
import band.platform.domain.user.repository.UserRefreshTokenRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.security.JwtRefreshTokenClaims;
import band.platform.global.security.JwtToken;
import band.platform.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserTokenService {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRefreshTokenRepository userRefreshTokenRepository;

	public UserTokenIssueResult issue(Long userId, String loginId) {
		String refreshTokenId = newRefreshTokenId();
		JwtToken accessToken = jwtTokenProvider.issueAccessToken(userId, loginId);
		JwtToken refreshToken = jwtTokenProvider.issueRefreshToken(userId, loginId, refreshTokenId);
		userRefreshTokenRepository.save(userId, refreshTokenId, refreshTokenTtl());

		return new UserTokenIssueResult(
			TokenResponse.bearer(accessToken.value(), accessToken.expiresIn()),
			refreshToken.value(),
			jwtTokenProvider.refreshTokenTtlSeconds()
		);
	}

	public UserTokenIssueResult reissue(String refreshToken) {
		JwtRefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
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
	}

	public void logout(String refreshToken) {
		JwtRefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
		userRefreshTokenRepository.delete(claims.userId(), claims.tokenId());
	}

	private Duration refreshTokenTtl() {
		return Duration.ofSeconds(jwtTokenProvider.refreshTokenTtlSeconds());
	}

	private String newRefreshTokenId() {
		return UUID.randomUUID().toString();
	}

}
