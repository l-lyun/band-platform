package band.platform.domain.board.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.board.dto.PostDetailResponse;
import band.platform.domain.board.dto.PostPageResponse;
import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;
import band.platform.domain.board.repository.PostRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

	private final PostRepository postRepository;

	public PostDetailResponse getPost(Long postId) {
		Post post = postRepository.findById(postId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));

		return PostDetailResponse.from(post);
	}

	public PostPageResponse getPosts(BoardType boardType, Pageable pageable) {
		return PostPageResponse.from(postRepository.findAllByBoardTypeOrderByCreatedAtDesc(boardType, pageable));
	}
}
