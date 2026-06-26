package band.platform.domain.user.service;

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.FindLoginIdRequest;
import band.platform.domain.user.dto.FindLoginIdResponse;
import band.platform.domain.user.dto.UserLoginRequest;
import band.platform.domain.user.dto.UserLoginResponse;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.ApiResult;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserLoginService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	private static final String DUMMY_PASSWORD_HASH = "$2a$10$4Y9jNnLYxZVP2oBTnEafn.ZNtqzxdUgh2jgAHcz3lAK95RD3cJDBu";
	private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

	@Transactional(readOnly = true)
	public UserLoginResponse login(UserLoginRequest request) {
		if (exceedsBcryptByteLimit(request.password())) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}

		User user = userRepository.findByLoginId(request.loginId())
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

		isActiveUser(user);
		isMatchedPassword(user, request);

		return UserLoginResponse.from(user);
	}

	@Transactional(readOnly = true)
	public FindLoginIdResponse findLoginId(FindLoginIdRequest request) {
		return userRepository.findByEmailAndStatus(request.email(), UserStatus.ACTIVE)
			.map(user -> new FindLoginIdResponse(user.getLoginId()))
			.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
	}

	private void isActiveUser(User user) {
		if (user.getStatus() != UserStatus.ACTIVE) {
			verifyPassword(user.getPassword(), DUMMY_PASSWORD_HASH);
			throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}
	}

	private void isMatchedPassword(User foundUser, UserLoginRequest request) {
		if (!verifyPassword(foundUser.getPassword(), request.password())) {
			throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
		}
	}

	private boolean verifyPassword(String rawPassword, String encodedPassword) {
		return passwordEncoder.matches(rawPassword, encodedPassword);
	}

	private boolean exceedsBcryptByteLimit(String password) {
		return password != null && password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES;
	}

}
