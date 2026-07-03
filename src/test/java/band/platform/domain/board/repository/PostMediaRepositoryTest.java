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
import band.platform.domain.board.entity.PostMedia;
import band.platform.domain.board.entity.PostMediaType;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class PostMediaRepositoryTest {

	@Autowired
	private PostMediaRepository postMediaRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("삭제되지 않은 게시글 미디어만 정렬 순서 오름차순으로 조회한다")
	void findAllByPostIdAndDeletedFalseOrderBySortOrderAsc() {
		Post post = savePost();
		PostMedia orderTwo = postMediaRepository.save(imageMedia(post, 2));
		PostMedia orderZero = postMediaRepository.save(imageMedia(post, 0));
		PostMedia orderOne = postMediaRepository.save(imageMedia(post, 1));
		PostMedia deleted = postMediaRepository.save(imageMedia(post, 3));
		deleted.markDeleted();

		entityManager.flush();
		entityManager.clear();

		List<PostMedia> mediaItems = postMediaRepository.findAllByPostIdAndDeletedFalseOrderBySortOrderAsc(post.getId());

		assertThat(mediaItems)
			.extracting(PostMedia::getId)
			.containsExactly(orderZero.getId(), orderOne.getId(), orderTwo.getId());
	}

	@Test
	@DisplayName("삭제되지 않은 게시글 미디어 개수만 센다")
	void countByPostIdAndDeletedFalse() {
		Post post = savePost();
		postMediaRepository.save(imageMedia(post, 0));
		postMediaRepository.save(imageMedia(post, 1));
		PostMedia deleted = postMediaRepository.save(imageMedia(post, 2));
		deleted.markDeleted();

		entityManager.flush();
		entityManager.clear();

		long count = postMediaRepository.countByPostIdAndDeletedFalse(post.getId());

		assertThat(count).isEqualTo(2);
	}

	private PostMedia imageMedia(Post post, int sortOrder) {
		return PostMedia.create(
			post,
			PostMediaType.IMAGE,
			"https://cdn.example.com/image-%d.jpg".formatted(sortOrder),
			null,
			"image-%d.jpg".formatted(sortOrder),
			"image/jpeg",
			1024L,
			sortOrder
		);
	}

	private Post savePost() {
		User author = userRepository.save(User.createLocalUser(
			"김김김",
			"mediauser",
			"encoded-password",
			"mediauser@example.com",
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			"img",
			true,
			true
		));
		return postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));
	}
}
