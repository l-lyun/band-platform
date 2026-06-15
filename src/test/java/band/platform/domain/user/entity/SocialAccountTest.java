package band.platform.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SocialAccountTest {

	@Test
	@DisplayName("소셜 계정은 회원과 제공자 고유 식별자를 함께 가진다")
	void connect() {
		User user = createLocalUserFixture();

		SocialAccount socialAccount = SocialAccount.connect(user, SocialProvider.KAKAO, "1234567890");

		assertThat(socialAccount.getUser()).isEqualTo(user);
		assertThat(socialAccount.getProvider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(socialAccount.getProviderSubject()).isEqualTo("1234567890");
	}

	@Test
	@DisplayName("LOCAL 제공자는 소셜 계정으로 연결할 수 없다")
	void localProvider() {
		User user = createLocalUserFixture();

		assertThatThrownBy(() -> SocialAccount.connect(user, SocialProvider.LOCAL, "local-subject"))
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
