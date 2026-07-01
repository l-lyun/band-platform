package band.platform.domain.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class PostRepositoryTest {

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("게시글을 저장한 뒤 아이디로 제목, 내용, 작성자, 게시판 타입을 조회한다")
	void saveAndFindById() {
		User author = saveUser("bandmaster", "bandmaster@example.com");
		Post post = postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));

		entityManager.flush();
		entityManager.clear();

		Post foundPost = postRepository.findById(post.getId()).orElseThrow();

		assertThat(foundPost.getTitle()).isEqualTo("합주 공지");
		assertThat(foundPost.getContent()).isEqualTo("토요일 오후 2시에 합주합니다.");
		assertThat(foundPost.getAuthor().getId()).isEqualTo(author.getId());
		assertThat(foundPost.getBoardType()).isEqualTo(BoardType.FREE);
	}

	@Test
	@DisplayName("게시판 타입으로 조회하면 해당 타입 게시글을 최신순으로 페이징한다")
	void findAllByBoardTypeOrderByCreatedAtDesc() {
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

		Page<Post> posts = postRepository.findAllByBoardTypeOrderByCreatedAtDesc(
			BoardType.FREE,
			PageRequest.of(0, 2)
		);

		assertThat(posts)
			.hasSize(2)
			.extracting(Post::getId)
			.containsExactly(latestFreePost.getId(), middleFreePost.getId());
		assertThat(posts.getTotalElements()).isEqualTo(3);
		assertThat(posts.getContent())
			.extracting(Post::getBoardType)
			.containsOnly(BoardType.FREE);
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
