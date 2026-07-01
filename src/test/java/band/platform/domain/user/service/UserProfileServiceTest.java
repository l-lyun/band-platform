package band.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.UserProfileResponse;
import band.platform.domain.user.dto.UserProfileUpdateRequest;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.Position;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@SpringBootTest
@Transactional
class UserProfileServiceTest {

	@Autowired
	private UserProfileService userProfileService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("ACTIVE 회원이면 내 프로필을 조회한다")
	void getMyProfile() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		user.updateProfile("김김김", Position.GUITAR, "img", Gender.MALE, "자기소개", false);

		UserProfileResponse response = userProfileService.getMyProfile(user.getId());

		assertThat(response.id()).isEqualTo(user.getId());
		assertThat(response.name()).isEqualTo("김김김");
		assertThat(response.position()).isEqualTo(Position.GUITAR);
		assertThat(response.profileImg()).isEqualTo("img");
		assertThat(response.gender()).isEqualTo(Gender.MALE);
		assertThat(response.description()).isEqualTo("자기소개");
		assertThat(response.opened()).isFalse();
	}

	@Test
	@DisplayName("ACTIVE 회원이면 내 프로필을 수정하고 저장한다")
	void updateMyProfile() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		UserProfileUpdateRequest request = new UserProfileUpdateRequest(
			"새닉네임",
			Position.BASS,
			"https://image.example.com/profile.png",
			Gender.FEMALE,
			"베이스 연주자입니다.",
			true
		);

		UserProfileResponse response = userProfileService.updateMyProfile(user.getId(), request);
		entityManager.flush();
		entityManager.clear();
		User foundUser = userRepository.findById(user.getId()).orElseThrow();

		assertThat(response.name()).isEqualTo("새닉네임");
		assertThat(response.position()).isEqualTo(Position.BASS);
		assertThat(response.profileImg()).isEqualTo("https://image.example.com/profile.png");
		assertThat(response.gender()).isEqualTo(Gender.FEMALE);
		assertThat(response.description()).isEqualTo("베이스 연주자입니다.");
		assertThat(response.opened()).isTrue();
		assertThat(foundUser.getName()).isEqualTo("새닉네임");
		assertThat(foundUser.getPosition()).isEqualTo(Position.BASS);
		assertThat(foundUser.getProfileImg()).isEqualTo("https://image.example.com/profile.png");
		assertThat(foundUser.getGender()).isEqualTo(Gender.FEMALE);
		assertThat(foundUser.getDescription()).isEqualTo("베이스 연주자입니다.");
		assertThat(foundUser.getOpened()).isTrue();
	}

	@Test
	@DisplayName("회원이 없으면 E02 예외를 던진다")
	void userNotFound() {
		assertThatThrownBy(() -> userProfileService.getMyProfile(999L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
	}

	@Test
	@DisplayName("ACTIVE 상태가 아니면 E02 예외를 던진다")
	void inactiveUserNotFound() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		entityManager.flush();
		entityManager.createNativeQuery("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?")
			.setParameter(1, user.getId())
			.executeUpdate();
		entityManager.clear();

		assertThatThrownBy(() -> userProfileService.getMyProfile(user.getId()))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
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
}
