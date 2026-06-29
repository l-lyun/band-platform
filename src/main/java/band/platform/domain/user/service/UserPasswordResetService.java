package band.platform.domain.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserPasswordResetRepository;
import band.platform.domain.user.repository.UserRefreshTokenRepository;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@Service
public class UserPasswordResetService {

	private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
	private static final int PASSWORD_MIN_LENGTH = 8;
	private static final int PASSWORD_MAX_LENGTH = 15;

	private final UserRepository userRepository;
	private final UserPasswordResetRepository passwordResetRepository;
	private final UserRefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordResetSecureValueGenerator secureValueGenerator;
	private final PasswordResetProperties properties;
	private final PasswordResetMailSender mailSender;
	private final UserSessionLockManager userSessionLockManager;

	@Autowired
	public UserPasswordResetService(
		UserRepository userRepository,
		UserPasswordResetRepository passwordResetRepository,
		UserRefreshTokenRepository refreshTokenRepository,
		PasswordEncoder passwordEncoder,
		PasswordResetSecureValueGenerator secureValueGenerator,
		PasswordResetProperties properties,
		ObjectProvider<PasswordResetMailSender> mailSenderProvider,
		UserSessionLockManager userSessionLockManager
	) {
		this(
			userRepository,
			passwordResetRepository,
			refreshTokenRepository,
			passwordEncoder,
			secureValueGenerator,
			properties,
			mailSenderProvider.getIfAvailable(UnavailablePasswordResetMailSender::new),
			userSessionLockManager
		);
	}

	UserPasswordResetService(
		UserRepository userRepository,
		UserPasswordResetRepository passwordResetRepository,
		UserRefreshTokenRepository refreshTokenRepository,
		PasswordEncoder passwordEncoder,
		PasswordResetSecureValueGenerator secureValueGenerator,
		PasswordResetProperties properties,
		PasswordResetMailSender mailSender,
		UserSessionLockManager userSessionLockManager
	) {
		this.userRepository = userRepository;
		this.passwordResetRepository = passwordResetRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.secureValueGenerator = secureValueGenerator;
		this.properties = properties;
		this.mailSender = mailSender;
		this.userSessionLockManager = userSessionLockManager;
	}

	@Transactional
	public void request(String loginId, String email) {
		User user = findResetTarget(loginId, email);
		String code = secureValueGenerator.generateCode();
		String codeHash = passwordEncoder.encode(code);

		passwordResetRepository.saveCode(user.getId(), codeHash, properties.getCodeTtl());
		mailSender.sendPasswordResetCode(user.getEmail(), code);
	}

	@Transactional
	public String verify(String loginId, String email, String code) {
		if (!StringUtils.hasText(code)) {
			throw new BusinessException(ErrorCode.AUTH_CODE_INVALID);
		}

		User user = findResetTarget(loginId, email);
		UserPasswordResetRepository.PasswordResetCode resetCode = passwordResetRepository.findCode(user.getId())
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_CODE_EXPIRED));

		if (resetCode.attempts() >= properties.getMaxAttempts()) {
			passwordResetRepository.deleteCode(user.getId());
			throw new BusinessException(ErrorCode.AUTH_CODE_INVALID);
		}

		if (!passwordEncoder.matches(code, resetCode.codeHash())) {
			UserPasswordResetRepository.PasswordResetCode updatedCode = passwordResetRepository.incrementCodeAttempts(user.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_CODE_EXPIRED));
			if (updatedCode.attempts() >= properties.getMaxAttempts()) {
				passwordResetRepository.deleteCode(user.getId());
			}
			throw new BusinessException(ErrorCode.AUTH_CODE_INVALID);
		}

		String resetToken = secureValueGenerator.generateToken();
		boolean tokenSaved = passwordResetRepository.consumeCodeAndSaveToken(
			user.getId(),
			sha256(resetToken),
			properties.getTokenTtl()
		);
		if (!tokenSaved) {
			throw new BusinessException(ErrorCode.AUTH_CODE_EXPIRED);
		}
		return resetToken;
	}

	@Transactional
	public void complete(String resetToken, String newPassword) {
		if (!StringUtils.hasText(resetToken)) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
		}
		if (!StringUtils.hasText(newPassword) || exceedsBcryptByteLimit(newPassword)
			|| isInvalidPasswordLength(newPassword)) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}

		Long userId = passwordResetRepository.consumeToken(sha256(resetToken))
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));

		userSessionLockManager.withLock(userId, () -> {
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
			user.changePassword(passwordEncoder.encode(newPassword));
			refreshTokenRepository.deleteAll(userId);
		});
	}

	static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
		}
	}

	private User findResetTarget(String loginId, String email) {
		return userRepository.findByLoginIdAndEmailAndStatus(loginId, email, UserStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
	}

	private boolean isInvalidPasswordLength(String password) {
		return password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH;
	}

	private boolean exceedsBcryptByteLimit(String password) {
		return password != null && password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES;
	}

	private static class UnavailablePasswordResetMailSender implements PasswordResetMailSender {

		@Override
		public void sendPasswordResetCode(String email, String code) {
			throw new BusinessException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
		}
	}
}
