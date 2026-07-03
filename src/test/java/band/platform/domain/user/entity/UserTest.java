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

	@Test
	@DisplayName("소셜 회원을 생성하면 로그인 아이디와 사용 가능한 비밀번호 없이 제공자 정보가 설정된다")
	void createSocialUser() {
		User user = User.createSocialUser(
			"김김김",
			"social@example.com",
			"01012345678",
			SocialProvider.KAKAO,
			true,
			false
		);

		assertThat(user.getName()).isEqualTo("김김김");
		assertThat(user.getEmail()).isEqualTo("social@example.com");
		assertThat(user.getPhoneNumber()).isEqualTo("01012345678");
		assertThat(user.getSocialProvider()).isEqualTo(SocialProvider.KAKAO);
		assertThat(user.getPrivacyPolicyAgreed()).isTrue();
		assertThat(user.getMarketingPolicyAgreed()).isFalse();
		assertThat(user.getLoginId()).isNull();
		assertThat(user.getPassword()).isNull();
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("프로필 정보를 수정하면 닉네임, 포지션, 이미지, 성별, 소개, 공개 여부가 바뀐다")
	void updateProfile() {
		User user = createLocalUserFixture();

		user.updateProfile(
			"새닉네임",
			Position.DRUM,
			"https://image.example.com/profile.png",
			Gender.FEMALE,
			"드럼 연주자입니다.",
			true
		);

		assertThat(user.getName()).isEqualTo("새닉네임");
		assertThat(user.getPosition()).isEqualTo(Position.DRUM);
		assertThat(user.getProfileImg()).isEqualTo("https://image.example.com/profile.png");
		assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
		assertThat(user.getDescription()).isEqualTo("드럼 연주자입니다.");
		assertThat(user.getOpened()).isTrue();
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
