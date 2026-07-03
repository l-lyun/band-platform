package band.platform.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.UserProfileResponse;
import band.platform.domain.user.dto.UserProfileUpdateRequest;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserProfileResponse getMyProfile(Long userId) {
		return UserProfileResponse.from(findActiveUser(userId));
	}

	@Transactional
	public UserProfileResponse updateMyProfile(Long userId, UserProfileUpdateRequest request) {
		User user = findActiveUser(userId);
		user.updateProfile(
			request.name(),
			request.position(),
			request.profileImg(),
			request.gender(),
			request.description(),
			request.opened()
		);

		return UserProfileResponse.from(user);
	}

	private User findActiveUser(Long userId) {
		return userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
	}
}
