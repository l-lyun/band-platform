package band.platform.domain.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.board.dto.PostCreateRequest;
import band.platform.domain.board.dto.PostCreateResponse;
import band.platform.domain.board.dto.PostMediaRequest;
import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;
import band.platform.domain.board.entity.PostMedia;
import band.platform.domain.board.entity.PostMediaType;
import band.platform.domain.board.repository.PostMediaRepository;
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
class PostCreateServiceTest {

	@Autowired
	private PostCreateService postCreateService;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private PostMediaRepository postMediaRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("존재하는 회원이면 게시판 타입, 제목, 내용, 작성자를 가진 게시글을 저장한다")
	void createPost() {
		User author = saveUser("bandmaster", "bandmaster@example.com");
		PostCreateRequest request = new PostCreateRequest(
			BoardType.FREE,
			"합주 공지",
			"토요일 오후 2시에 합주합니다."
		);

		PostCreateResponse response = postCreateService.create(author.getId(), request);

		entityManager.flush();
		entityManager.clear();

		Post post = postRepository.findById(response.id()).orElseThrow();
		assertThat(response.id()).isNotNull();
		assertThat(response.boardType()).isEqualTo(BoardType.FREE);
		assertThat(response.title()).isEqualTo("합주 공지");
		assertThat(response.content()).isEqualTo("토요일 오후 2시에 합주합니다.");
		assertThat(response.authorId()).isEqualTo(author.getId());
		assertThat(response.mediaItems()).isEmpty();
		assertThat(post.getBoardType()).isEqualTo(BoardType.FREE);
		assertThat(post.getTitle()).isEqualTo("합주 공지");
		assertThat(post.getContent()).isEqualTo("토요일 오후 2시에 합주합니다.");
		assertThat(post.getAuthor().getId()).isEqualTo(author.getId());
	}

	@Test
	@DisplayName("미디어 목록이 있으면 게시글 저장 후 정렬 순서대로 미디어 응답을 반환한다")
	void createPostWithMediaItems() {
		User author = saveUser("mediaauthor", "mediaauthor@example.com");
		PostCreateRequest request = new PostCreateRequest(
			BoardType.FREE,
			"합주 공지",
			"토요일 오후 2시에 합주합니다.",
			List.of(
				mediaRequest(PostMediaType.VIDEO, "https://cdn.example.com/video.mp4", 1),
				mediaRequest(PostMediaType.IMAGE, "https://cdn.example.com/image.jpg", 0)
			)
		);

		PostCreateResponse response = postCreateService.create(author.getId(), request);

		entityManager.flush();
		entityManager.clear();

		List<PostMedia> mediaItems =
			postMediaRepository.findAllByPostIdAndDeletedFalseOrderBySortOrderAsc(response.id());
		assertThat(response.mediaItems())
			.extracting(media -> media.sortOrder())
			.containsExactly(0, 1);
		assertThat(response.mediaItems())
			.extracting(media -> media.mediaType())
			.containsExactly(PostMediaType.IMAGE, PostMediaType.VIDEO);
		assertThat(mediaItems)
			.extracting(PostMedia::getSortOrder)
			.containsExactly(0, 1);
	}

	@Test
	@DisplayName("미디어 정렬 순서가 중복되면 E01 예외를 던진다")
	void duplicateMediaSortOrder() {
		User author = saveUser("duplicateauthor", "duplicateauthor@example.com");
		PostCreateRequest request = new PostCreateRequest(
			BoardType.FREE,
			"합주 공지",
			"토요일 오후 2시에 합주합니다.",
			List.of(
				mediaRequest(PostMediaType.IMAGE, "https://cdn.example.com/image.jpg", 0),
				mediaRequest(PostMediaType.VIDEO, "https://cdn.example.com/video.mp4", 0)
			)
		);

		assertThatThrownBy(() -> postCreateService.create(author.getId(), request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT)
			);
	}

	@Test
	@DisplayName("작성자 회원이 없으면 E02 예외를 던진다")
	void authorNotFound() {
		PostCreateRequest request = new PostCreateRequest(
			BoardType.FREE,
			"합주 공지",
			"토요일 오후 2시에 합주합니다."
		);

		assertThatThrownBy(() -> postCreateService.create(999L, request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
	}

	@Test
	@DisplayName("탈퇴한 회원이면 E02 예외를 던지고 게시글을 저장하지 않는다")
	void withdrawnAuthor() {
		User author = saveUser("withdrawn", "withdrawn@example.com");
		ReflectionTestUtils.setField(author, "status", UserStatus.WITHDRAWN);
		PostCreateRequest request = new PostCreateRequest(
			BoardType.FREE,
			"합주 공지",
			"토요일 오후 2시에 합주합니다."
		);

		assertThatThrownBy(() -> postCreateService.create(author.getId(), request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
		assertThat(postRepository.findAll()).isEmpty();
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

	private PostMediaRequest mediaRequest(PostMediaType mediaType, String mediaUrl, int sortOrder) {
		return new PostMediaRequest(
			mediaType,
			mediaUrl,
			"https://cdn.example.com/thumb.jpg",
			"media",
			"image/jpeg",
			1024L,
			sortOrder
		);
	}
}
