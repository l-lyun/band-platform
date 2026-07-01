package band.platform.domain.board.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;

class PostTest {

	@Test
	@DisplayName("게시글을 생성하면 제목, 내용, 작성자, 게시판 타입을 저장하고 식별자는 비어 있다")
	void create() {
		User author = createLocalUserFixture();

		Post post = Post.create("합주 멤버 모집", "이번 주말 합주할 기타 멤버를 찾습니다.", author, BoardType.RECRUIT);

		assertThat(post.getId()).isNull();
		assertThat(post.getTitle()).isEqualTo("합주 멤버 모집");
		assertThat(post.getContent()).isEqualTo("이번 주말 합주할 기타 멤버를 찾습니다.");
		assertThat(post.getAuthor()).isEqualTo(author);
		assertThat(post.getBoardType()).isEqualTo(BoardType.RECRUIT);
	}

	@Test
	@DisplayName("제목이 비어 있으면 게시글을 생성하지 않는다")
	void rejectBlankTitle() {
		User author = createLocalUserFixture();

		assertThatThrownBy(() -> Post.create(" ", "내용", author, BoardType.RECRUIT))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("내용이 비어 있으면 게시글을 생성하지 않는다")
	void rejectBlankContent() {
		User author = createLocalUserFixture();

		assertThatThrownBy(() -> Post.create("제목", " ", author, BoardType.RECRUIT))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("작성자가 없으면 게시글을 생성하지 않는다")
	void rejectNullAuthor() {
		assertThatThrownBy(() -> Post.create("제목", "내용", null, BoardType.RECRUIT))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("게시판 타입이 없으면 게시글을 생성하지 않는다")
	void rejectNullBoardType() {
		User author = createLocalUserFixture();

		assertThatThrownBy(() -> Post.create("제목", "내용", author, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private User createLocalUserFixture() {
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

}
