package band.platform.domain.board.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoardTypeTest {

	@Test
	@DisplayName("게시판 타입은 자유, 모집, 장비, 비밀, 홍보, 오디오를 제공한다")
	void values() {
		assertThat(BoardType.values()).containsExactly(
			BoardType.FREE,
			BoardType.RECRUIT,
			BoardType.EQUIPMENT,
			BoardType.SECRET,
			BoardType.PROMOTION,
			BoardType.AUDIO
		);
	}

	@Test
	@DisplayName("비밀 게시판 타입만 비밀 게시판으로 식별된다")
	void isSecret() {
		assertThat(BoardType.SECRET.isSecret()).isTrue();
		assertThat(BoardType.values())
			.filteredOn(type -> type != BoardType.SECRET)
			.allSatisfy(type -> assertThat(type.isSecret()).isFalse());
	}

}
