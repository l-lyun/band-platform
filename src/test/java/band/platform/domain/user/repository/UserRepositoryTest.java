package band.platform.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.Position;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;

@SpringBootTest
@Transactional
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("로그인 아이디로 회원을 조회하고 없으면 빈 Optional을 반환한다")
	void findByLoginId() {
		User user = saveUser("bandmaster", "bandmaster@example.com");

		assertThat(userRepository.findByLoginId(user.getLoginId()))
			.hasValueSatisfying(foundUser -> assertThat(foundUser.getLoginId()).isEqualTo("bandmaster"));
		assertThat(userRepository.findByLoginId("unknown")).isEmpty();
	}

	@Test
	@DisplayName("로그인 아이디 존재 여부를 반환한다")
	void existsByLoginId() {
		saveUser("bandmaster", "bandmaster@example.com");

		assertThat(userRepository.existsByLoginId("bandmaster")).isTrue();
		assertThat(userRepository.existsByLoginId("unknown")).isFalse();
	}

	@Test
	@DisplayName("이메일 존재 여부를 반환한다")
	void existsByEmail() {
		saveUser("bandmaster", "bandmaster@example.com");

		assertThat(userRepository.existsByEmail("bandmaster@example.com")).isTrue();
		assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
	}

	@Test
	@DisplayName("프로필 포지션을 저장하고 다시 조회한다")
	void saveProfilePosition() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		user.updateProfile(
			user.getName(),
			Position.KEYBOARD,
			user.getProfileImg(),
			user.getGender(),
			user.getDescription(),
			user.getOpened()
		);
		userRepository.flush();
		entityManager.clear();

		User foundUser = userRepository.findByLoginId("bandmaster").orElseThrow();

		assertThat(foundUser.getPosition()).isEqualTo(Position.KEYBOARD);
	}

	@Test
	@DisplayName("ACTIVE 회원을 쓰기 잠금으로 조회한다")
	void findByIdAndStatusForUpdate() {
		User user = saveUser("bandmaster", "bandmaster@example.com");

		assertThat(userRepository.findByIdAndStatusForUpdate(user.getId(), UserStatus.ACTIVE))
			.hasValueSatisfying(foundUser -> assertThat(foundUser.getId()).isEqualTo(user.getId()));
	}

	@Test
	@DisplayName("ACTIVE 상태가 아니면 쓰기 잠금 조회에서 빈 Optional을 반환한다")
	void findByIdAndStatusForUpdateInactiveUser() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		entityManager.flush();
		entityManager.createNativeQuery("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?")
			.setParameter(1, user.getId())
			.executeUpdate();
		entityManager.clear();

		assertThat(userRepository.findByIdAndStatusForUpdate(user.getId(), UserStatus.ACTIVE)).isEmpty();
	}

	@Test
	@DisplayName("status 컬럼이 없는 기존 회원은 ACTIVE 상태로 조회된다")
	void existingUserRowsUseDefaultStatus() {
		entityManager.createNativeQuery("""
			INSERT INTO users (
				name,
				login_id,
				password,
				email,
				description,
				opened,
				phone_number,
				gender,
				profile_img,
				social_provider,
				privacy_policy_agreed,
				marketing_policy_agreed,
				created_at,
				updated_at
			) VALUES (
				'기존회원',
				'legacy',
				'encoded-password',
				'legacy@example.com',
				'자기소개',
				false,
				'01012345678',
				'MALE',
				'img',
				'LOCAL',
				true,
				true,
				CURRENT_TIMESTAMP,
				CURRENT_TIMESTAMP
			)
			""").executeUpdate();
		entityManager.flush();
		entityManager.clear();

		User user = userRepository.findByLoginId("legacy").orElseThrow();

		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
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
