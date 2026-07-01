package band.platform.domain.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.board.dto.PostDetailResponse;
import band.platform.domain.board.dto.PostPageResponse;
import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;
import band.platform.domain.board.repository.PostRepository;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class PostQueryServiceTest {

	@Autowired
	private PostQueryService postQueryService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("게시글 아이디로 단건 조회하면 상세 응답에 작성자와 작성 시각을 담는다")
	void getPost() {
		User author = saveUser("bandmaster", "bandmaster@example.com");
		Post post = postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));

		entityManager.flush();
		entityManager.clear();

		PostDetailResponse response = postQueryService.getPost(post.getId());

		assertThat(response.id()).isEqualTo(post.getId());
		assertThat(response.boardType()).isEqualTo(BoardType.FREE);
		assertThat(response.title()).isEqualTo("합주 공지");
		assertThat(response.content()).isEqualTo("토요일 오후 2시에 합주합니다.");
		assertThat(response.authorId()).isEqualTo(author.getId());
		assertThat(response.createdAt()).isNotNull();
		assertThat(response.updatedAt()).isNotNull();
	}

	@Test
	@DisplayName("존재하지 않는 게시글을 단건 조회하면 E02 예외를 던진다")
	void getMissingPost() {
		assertThatThrownBy(() -> postQueryService.getPost(999L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
	}

	@Test
	@DisplayName("게시판 타입으로 목록 조회하면 해당 게시판 게시글만 최신순으로 응답한다")
	void getPostsByBoardTypeLatestFirst() {
		User author = saveUser("freeband", "freeband@example.com");
		Post oldFreePost = postRepository.save(Post.create("오래된 자유글", "첫 번째 자유글입니다.", author, BoardType.FREE));
		Post latestFreePost = postRepository.save(Post.create("최신 자유글", "세 번째 자유글입니다.", author, BoardType.FREE));
		Post middleFreePost = postRepository.save(Post.create("중간 자유글", "두 번째 자유글입니다.", author, BoardType.FREE));
		postRepository.save(Post.create("비밀 합주", "멤버에게만 공개합니다.", author, BoardType.SECRET));

		entityManager.flush();
		updateCreatedAt(oldFreePost, LocalDateTime.of(2026, 1, 1, 10, 0));
		updateCreatedAt(middleFreePost, LocalDateTime.of(2026, 1, 2, 10, 0));
		updateCreatedAt(latestFreePost, LocalDateTime.of(2026, 1, 3, 10, 0));
		entityManager.clear();

		PostPageResponse response = postQueryService.getPosts(BoardType.FREE, PageRequest.of(0, 2));

		assertThat(response.posts())
			.extracting("id")
			.containsExactly(latestFreePost.getId(), middleFreePost.getId());
		assertThat(response.posts())
			.extracting("boardType")
			.containsOnly(BoardType.FREE);
		assertThat(response.page()).isEqualTo(0);
		assertThat(response.size()).isEqualTo(2);
		assertThat(response.totalElements()).isEqualTo(3);
		assertThat(response.totalPages()).isEqualTo(2);
		assertThat(response.first()).isTrue();
		assertThat(response.last()).isFalse();
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

	private void updateCreatedAt(Post post, LocalDateTime createdAt) {
		entityManager.createNativeQuery("update posts set created_at = ? where id = ?")
			.setParameter(1, createdAt)
			.setParameter(2, post.getId())
			.executeUpdate();
	}
}
