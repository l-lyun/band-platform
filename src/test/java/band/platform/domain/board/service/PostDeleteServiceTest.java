package band.platform.domain.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

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
class PostDeleteServiceTest {

	@Autowired
	private PostDeleteService postDeleteService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("작성자가 게시글을 삭제하면 저장소에서 물리 삭제된다")
	void deletePost() {
		User author = saveUser("bandmaster", "bandmaster@example.com");
		Post post = postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));

		postDeleteService.delete(author.getId(), post.getId());

		entityManager.flush();
		entityManager.clear();

		assertThat(postRepository.findById(post.getId())).isEmpty();
	}

	@Test
	@DisplayName("작성자가 아닌 회원이면 A02 예외를 던지고 게시글을 보존한다")
	void deletePostByNonAuthor() {
		User author = saveUser("bandmaster", "bandmaster@example.com");
		User otherUser = saveUser("other", "other@example.com");
		Post post = postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));

		assertThatThrownBy(() -> postDeleteService.delete(otherUser.getId(), post.getId()))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN)
			);

		entityManager.flush();
		entityManager.clear();

		assertThat(postRepository.findById(post.getId())).isPresent();
	}

	@Test
	@DisplayName("존재하지 않는 게시글이면 E02 예외를 던진다")
	void deleteMissingPost() {
		User author = saveUser("bandmaster", "bandmaster@example.com");

		assertThatThrownBy(() -> postDeleteService.delete(author.getId(), 999L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
	}

	@Test
	@DisplayName("탈퇴한 작성자이면 E02 예외를 던지고 게시글을 보존한다")
	void deletePostByWithdrawnAuthor() {
		User author = saveUser("withdrawn", "withdrawn@example.com");
		Post post = postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));
		ReflectionTestUtils.setField(author, "status", UserStatus.WITHDRAWN);

		assertThatThrownBy(() -> postDeleteService.delete(author.getId(), post.getId()))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);

		entityManager.flush();
		entityManager.clear();

		assertThat(postRepository.findById(post.getId())).isPresent();
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
