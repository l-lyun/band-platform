package band.platform.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.UserSignupRequest;
import band.platform.domain.user.dto.UserSignupResponse;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@Service
public class UserSignupService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserSignupService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserSignupResponse signup(UserSignupRequest request) {
		validateDuplicateLoginId(request.loginId());
		validateDuplicateEmail(request.email());
		validatePrivacyPolicyAgreement(request.privacyPolicyAgreed());

		User user = User.createLocalUser(
			request.name(),
			request.loginId(),
			passwordEncoder.encode(request.password()),
			request.email(),
			request.description(),
			request.opened(),
			request.phoneNumber(),
			request.gender(),
			request.profileImg(),
			request.privacyPolicyAgreed(),
			request.marketingPolicyAgreed()
		);

		return UserSignupResponse.from(userRepository.save(user));
	}

	private void validateDuplicateLoginId(String loginId) {
		if (userRepository.existsByLoginId(loginId)) {
			throw new BusinessException(ErrorCode.USER_LOGIN_ID_DUPLICATED);
		}
	}

	private void validateDuplicateEmail(String email) {
		if (userRepository.existsByEmail(email)) {
			throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATED);
		}
	}

	private void validatePrivacyPolicyAgreement(Boolean privacyPolicyAgreed) {
		if (!Boolean.TRUE.equals(privacyPolicyAgreed)) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
	}

}
