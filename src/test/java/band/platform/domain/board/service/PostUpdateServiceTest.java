package band.platform.domain.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.board.dto.PostUpdateRequest;
import band.platform.domain.board.dto.PostUpdateResponse;
import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;
import band.platform.domain.board.repository.PostRepository;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class PostUpdateServiceTest {

	@Autowired
	private PostUpdateService postUpdateService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("작성자가 게시글을 수정하면 제목과 내용만 변경하고 게시판 타입은 유지한다")
	void updatePost() {
		User author = saveUser("bandmaster", "bandmaster@example.com");
		Post post = postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));
		PostUpdateRequest request = new PostUpdateRequest(
			"수정된 합주 공지",
			"토요일 오후 3시로 변경합니다."
		);

		PostUpdateResponse response = postUpdateService.update(author.getId(), post.getId(), request);

		entityManager.flush();
		entityManager.clear();

		Post updatedPost = postRepository.findById(post.getId()).orElseThrow();
		assertThat(response.id()).isEqualTo(post.getId());
		assertThat(response.boardType()).isEqualTo(BoardType.FREE);
		assertThat(response.title()).isEqualTo("수정된 합주 공지");
		assertThat(response.content()).isEqualTo("토요일 오후 3시로 변경합니다.");
		assertThat(response.authorId()).isEqualTo(author.getId());
		assertThat(response.updatedAt()).isNotNull();
		assertThat(updatedPost.getBoardType()).isEqualTo(BoardType.FREE);
		assertThat(updatedPost.getTitle()).isEqualTo("수정된 합주 공지");
		assertThat(updatedPost.getContent()).isEqualTo("토요일 오후 3시로 변경합니다.");
		assertThat(updatedPost.getAuthor().getId()).isEqualTo(author.getId());
	}

	@Test
	@DisplayName("작성자가 아닌 회원이면 A02 예외를 던지고 게시글을 변경하지 않는다")
	void updatePostByNonAuthor() {
		User author = saveUser("bandmaster", "bandmaster@example.com");
		User otherUser = saveUser("other", "other@example.com");
		Post post = postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));
		PostUpdateRequest request = new PostUpdateRequest(
			"수정된 합주 공지",
			"토요일 오후 3시로 변경합니다."
		);

		assertThatThrownBy(() -> postUpdateService.update(otherUser.getId(), post.getId(), request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN)
			);

		entityManager.flush();
		entityManager.clear();

		Post unchangedPost = postRepository.findById(post.getId()).orElseThrow();
		assertThat(unchangedPost.getTitle()).isEqualTo("합주 공지");
		assertThat(unchangedPost.getContent()).isEqualTo("토요일 오후 2시에 합주합니다.");
		assertThat(unchangedPost.getBoardType()).isEqualTo(BoardType.FREE);
	}

	@Test
	@DisplayName("존재하지 않는 게시글이면 E02 예외를 던진다")
	void updateMissingPost() {
		User author = saveUser("bandmaster", "bandmaster@example.com");
		PostUpdateRequest request = new PostUpdateRequest(
			"수정된 합주 공지",
			"토요일 오후 3시로 변경합니다."
		);

		assertThatThrownBy(() -> postUpdateService.update(author.getId(), 999L, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
	}

	@Test
	@DisplayName("탈퇴한 작성자이면 E02 예외를 던지고 게시글을 변경하지 않는다")
	void withdrawnAuthor() {
		User author = saveUser("withdrawn", "withdrawn@example.com");
		Post post = postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));
		ReflectionTestUtils.setField(author, "status", UserStatus.WITHDRAWN);
		PostUpdateRequest request = new PostUpdateRequest(
			"수정된 합주 공지",
			"토요일 오후 3시로 변경합니다."
		);

		assertThatThrownBy(() -> postUpdateService.update(author.getId(), post.getId(), request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);

		entityManager.flush();
		entityManager.clear();

		Post unchangedPost = postRepository.findById(post.getId()).orElseThrow();
		assertThat(unchangedPost.getTitle()).isEqualTo("합주 공지");
		assertThat(unchangedPost.getContent()).isEqualTo("토요일 오후 2시에 합주합니다.");
		assertThat(unchangedPost.getBoardType()).isEqualTo(BoardType.FREE);
	}

	private User saveUser(String loginId, String email) {
		return userRepository.save(User.createLocalUser(
			"김김김",
			loginId,
			"encoded-password",
			email,
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			"img",
			true,
			true
		));
	}
}
