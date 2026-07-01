package band.platform.domain.board.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.board.dto.PostUpdateRequest;
import band.platform.domain.board.dto.PostUpdateResponse;
import band.platform.domain.board.entity.Post;
import band.platform.domain.board.repository.PostRepository;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostUpdateService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;

	@Transactional
	public PostUpdateResponse update(Long authorId, Long postId, PostUpdateRequest request) {
		userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));

		if (!post.isWrittenBy(authorId)) {
			throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
		}

		post.update(request.title(), request.content());

		return PostUpdateResponse.from(postRepository.saveAndFlush(post));
	}
}
