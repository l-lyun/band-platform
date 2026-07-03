package band.platform.domain.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.board.dto.PostMediaRequest;
import band.platform.domain.board.dto.PostMediaResponse;
import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.entity.Post;
import band.platform.domain.board.entity.PostMediaType;
import band.platform.domain.board.repository.PostMediaRepository;
import band.platform.domain.board.repository.PostRepository;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@SpringBootTest
@Transactional
class PostMediaServiceTest {

	@Autowired
	private PostMediaService postMediaService;

	@Autowired
	private PostMediaRepository postMediaRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("null 미디어 목록이면 빈 응답을 반환한다")
	void attachNullMediaItems() {
		Post post = savePost("nullmedia", "nullmedia@example.com");

		List<PostMediaResponse> responses = postMediaService.attach(post, null);

		assertThat(responses).isEmpty();
		assertThat(postMediaRepository.countByPostIdAndDeletedFalse(post.getId())).isZero();
	}

	@Test
	@DisplayName("게시글에 미디어를 붙이면 정렬 순서대로 응답한다")
	void attachMediaItems() {
		Post post = savePost("attachmedia", "attachmedia@example.com");

		List<PostMediaResponse> responses = postMediaService.attach(post, List.of(
			request(PostMediaType.VIDEO, "https://cdn.example.com/video.mp4", 1),
			request(PostMediaType.IMAGE, "https://cdn.example.com/image.jpg", 0)
		));

		assertThat(responses)
			.extracting(PostMediaResponse::sortOrder)
			.containsExactly(0, 1);
		assertThat(responses)
			.extracting(PostMediaResponse::mediaType)
			.containsExactly(PostMediaType.IMAGE, PostMediaType.VIDEO);
		assertThat(postMediaRepository.countByPostIdAndDeletedFalse(post.getId())).isEqualTo(2);
	}

	@Test
	@DisplayName("미디어가 20개를 초과하면 E01 예외를 던진다")
	void moreThanTwentyMediaItems() {
		Post post = savePost("manymedia", "manymedia@example.com");
		List<PostMediaRequest> requests = IntStream.range(0, 21)
			.mapToObj(order -> request(PostMediaType.IMAGE, "https://cdn.example.com/%d.jpg".formatted(order), order))
			.toList();

		assertInvalidInput(() -> postMediaService.attach(post, requests));
	}

	@Test
	@DisplayName("정렬 순서가 중복되면 E01 예외를 던진다")
	void duplicateSortOrder() {
		Post post = savePost("duplicatemedia", "duplicatemedia@example.com");

		assertInvalidInput(() -> postMediaService.attach(post, List.of(
			request(PostMediaType.IMAGE, "https://cdn.example.com/1.jpg", 0),
			request(PostMediaType.VIDEO, "https://cdn.example.com/2.mp4", 0)
		)));
	}

	@Test
	@DisplayName("정렬 순서가 0부터 연속되지 않으면 E01 예외를 던진다")
	void nonContiguousSortOrder() {
		Post post = savePost("missingmedia", "missingmedia@example.com");

		assertInvalidInput(() -> postMediaService.attach(post, List.of(
			request(PostMediaType.IMAGE, "https://cdn.example.com/1.jpg", 0),
			request(PostMediaType.VIDEO, "https://cdn.example.com/2.mp4", 2)
		)));
	}

	@Test
	@DisplayName("미디어 URL이 비어 있으면 E01 예외를 던진다")
	void blankMediaUrl() {
		Post post = savePost("blankmedia", "blankmedia@example.com");

		assertInvalidInput(() -> postMediaService.attach(post, List.of(
			request(PostMediaType.IMAGE, " ", 0)
		)));
	}

	@Test
	@DisplayName("미디어 타입이 null이면 E01 예외를 던진다")
	void nullMediaType() {
		Post post = savePost("nulltypemedia", "nulltypemedia@example.com");

		assertInvalidInput(() -> postMediaService.attach(post, List.of(
			request(null, "https://cdn.example.com/image.jpg", 0)
		)));
	}

	private PostMediaRequest request(PostMediaType mediaType, String mediaUrl, int sortOrder) {
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

	private Post savePost(String loginId, String email) {
		User author = userRepository.save(User.createLocalUser(
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
		return postRepository.save(Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", author, BoardType.FREE));
	}

	private void assertInvalidInput(ThrowingCallable callable) {
		assertThatThrownBy(callable::call)
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT)
			);
	}

	@FunctionalInterface
	private interface ThrowingCallable {
		void call();
	}
}
