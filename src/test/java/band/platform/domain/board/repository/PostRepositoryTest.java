package band.platform.domain.board.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
	@DisplayName("비밀 게시판 타입으로 조회하면 비밀 게시글만 반환한다")
	void findAllByBoardType() {
		User author = saveUser("secretband", "secretband@example.com");
		Post secretPost = postRepository.save(Post.create("비밀 합주", "멤버에게만 공개합니다.", author, BoardType.SECRET));
		postRepository.save(Post.create("자유 합주", "누구나 볼 수 있습니다.", author, BoardType.FREE));
		postRepository.save(Post.create("장비 대여", "앰프 대여합니다.", author, BoardType.EQUIPMENT));

		entityManager.flush();
		entityManager.clear();

		List<Post> posts = postRepository.findAllByBoardType(BoardType.SECRET);

		assertThat(posts)
			.singleElement()
			.satisfies(post -> {
				assertThat(post.getId()).isEqualTo(secretPost.getId());
				assertThat(post.getTitle()).isEqualTo("비밀 합주");
				assertThat(post.getBoardType()).isEqualTo(BoardType.SECRET);
			});
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
