package band.platform.domain.user;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTest {

	User user;

	@BeforeEach
	void setUp() {
		user = User.createLocalUser(
			"김김김",
			"aa",
			"11",
			"aaa",
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			"img",
			true,
			true
			);
	}


	@Test
	void userCreateTest() {
		Assertions.assertThat(user).isNotNull();
		Assertions.assertThat(user.getId()).isNull();
	}
}