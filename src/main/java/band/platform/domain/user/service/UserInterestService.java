package band.platform.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.UserInterestResponse;
import band.platform.domain.user.dto.UserInterestUpdateRequest;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserInterest;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserInterestRepository;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserInterestService {

	private final UserRepository userRepository;
	private final UserInterestRepository userInterestRepository;

	@Transactional(readOnly = true)
	public UserInterestResponse getMyInterests(Long userId) {
		findActiveUser(userId);
		return userInterestRepository.findByUserId(userId)
			.map(UserInterestResponse::from)
			.orElseGet(UserInterestResponse::empty);
	}

	@Transactional
	public UserInterestResponse updateMyInterests(Long userId, UserInterestUpdateRequest request) {
		User user = findActiveUser(userId);
		UserInterest userInterest = userInterestRepository.findByUserId(userId)
			.orElseGet(() -> UserInterest.create(user, null, null, null, null));

		userInterest.update(
			request.favoriteArtists(),
			request.favoriteEquipments(),
			request.favoriteVenues(),
			request.visitedConcerts()
		);
		userInterestRepository.save(userInterest);
		return UserInterestResponse.from(userInterest);
	}

	private User findActiveUser(Long userId) {
		return userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
	}
}
