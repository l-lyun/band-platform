package band.platform.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.UserLoginRequest;
import band.platform.domain.user.dto.UserLoginResponse;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@Service
public class UserLoginService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserLoginService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public UserLoginResponse login(UserLoginRequest request) {
		User user = userRepository.findByLoginId(request.loginId())
			.orElseThrow(this::invalidCredentials);

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw invalidCredentials();
		}

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw invalidCredentials();
		}

		return UserLoginResponse.from(user);
	}

	private BusinessException invalidCredentials() {
		return new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
	}

}
