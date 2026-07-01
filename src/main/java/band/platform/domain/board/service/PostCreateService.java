package band.platform.domain.board.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.board.dto.PostCreateRequest;
import band.platform.domain.board.dto.PostCreateResponse;
import band.platform.domain.board.entity.Post;
import band.platform.domain.board.repository.PostRepository;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostCreateService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;

	@Transactional
	public PostCreateResponse create(Long authorId, PostCreateRequest request) {
		User author = userRepository.findByIdAndStatus(authorId, UserStatus.ACTIVE)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
		Post post = Post.create(request.title(), request.content(), author, request.boardType());

		return PostCreateResponse.from(postRepository.save(post));
	}
}
