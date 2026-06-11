package band.platform.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

	@Test
	@DisplayName("로컬 회원을 생성하면 기본 상태가 ACTIVE로 설정된다")
	void createLocalUser() {
		User user = createLocalUserFixture();

		assertThat(user).isNotNull();
		assertThat(user.getId()).isNull();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
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
