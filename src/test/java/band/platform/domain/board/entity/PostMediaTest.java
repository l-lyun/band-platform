package band.platform.domain.board.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class PostMediaTest {

	@Test
	@DisplayName("이미지 타입과 정렬 순서를 가진 게시글 미디어를 생성한다")
	void createImageMedia() {
		Post post = post();

		PostMedia media = PostMedia.create(
			post,
			PostMediaType.IMAGE,
			"https://cdn.example.com/image.jpg",
			"https://cdn.example.com/thumb.jpg",
			"image.jpg",
			"image/jpeg",
			1024L,
			0
		);

		assertThat(media.getPost()).isEqualTo(post);
		assertThat(media.getMediaType()).isEqualTo(PostMediaType.IMAGE);
		assertThat(media.getMediaUrl()).isEqualTo("https://cdn.example.com/image.jpg");
		assertThat(media.getThumbnailUrl()).isEqualTo("https://cdn.example.com/thumb.jpg");
		assertThat(media.getOriginalFileName()).isEqualTo("image.jpg");
		assertThat(media.getContentType()).isEqualTo("image/jpeg");
		assertThat(media.getFileSizeBytes()).isEqualTo(1024L);
		assertThat(media.getSortOrder()).isZero();
		assertThat(media.getDeleted()).isFalse();
	}

	@Test
	@DisplayName("동영상 타입과 정렬 순서를 가진 게시글 미디어를 생성한다")
	void createVideoMedia() {
		PostMedia media = PostMedia.create(
			post(),
			PostMediaType.VIDEO,
			"https://cdn.example.com/video.mp4",
			null,
			"video.mp4",
			"video/mp4",
			2048L,
			1
		);

		assertThat(media.getMediaType()).isEqualTo(PostMediaType.VIDEO);
		assertThat(media.getSortOrder()).isEqualTo(1);
	}

	@Test
	@DisplayName("삭제 표시를 하면 deleted가 true가 된다")
	void markDeleted() {
		PostMedia media = imageMedia(0);

		media.markDeleted();

		assertThat(media.getDeleted()).isTrue();
	}

	@Test
	@DisplayName("미디어 URL이 비어 있으면 E01 예외를 던진다")
	void blankMediaUrl() {
		assertInvalidInput(() -> PostMedia.create(
			post(),
			PostMediaType.IMAGE,
			" ",
			null,
			"image.jpg",
			"image/jpeg",
			1024L,
			0
		));
	}

	@Test
	@DisplayName("미디어 타입이 null이면 E01 예외를 던진다")
	void nullMediaType() {
		assertInvalidInput(() -> PostMedia.create(
			post(),
			null,
			"https://cdn.example.com/image.jpg",
			null,
			"image.jpg",
			"image/jpeg",
			1024L,
			0
		));
	}

	@Test
	@DisplayName("정렬 순서가 음수이면 E01 예외를 던진다")
	void negativeSortOrder() {
		assertInvalidInput(() -> PostMedia.create(
			post(),
			PostMediaType.IMAGE,
			"https://cdn.example.com/image.jpg",
			null,
			"image.jpg",
			"image/jpeg",
			1024L,
			-1
		));
	}

	private PostMedia imageMedia(int sortOrder) {
		return PostMedia.create(
			post(),
			PostMediaType.IMAGE,
			"https://cdn.example.com/image.jpg",
			null,
			"image.jpg",
			"image/jpeg",
			1024L,
			sortOrder
		);
	}

	private Post post() {
		return Post.create("합주 공지", "토요일 오후 2시에 합주합니다.", user(), BoardType.FREE);
	}

	private User user() {
		return User.createLocalUser(
			"김김김",
			"bandmaster",
			"encoded-password",
			"bandmaster@example.com",
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			"img",
			true,
			true
		);
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
