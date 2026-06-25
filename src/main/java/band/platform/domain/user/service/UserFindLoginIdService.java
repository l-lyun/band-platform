package band.platform.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.FindLoginIdRequest;
import band.platform.domain.user.dto.FindLoginIdResponse;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserFindLoginIdService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public FindLoginIdResponse findLoginId(FindLoginIdRequest request) {
		return userRepository.findByEmailAndStatus(request.email(), UserStatus.ACTIVE)
			.map(user -> new FindLoginIdResponse(user.getLoginId()))
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
	}

}
