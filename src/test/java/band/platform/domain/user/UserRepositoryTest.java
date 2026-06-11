package band.platform.domain.user;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void findByLoginId() {
		User user = saveUser("bandmaster", "bandmaster@example.com");

		Assertions.assertThat(userRepository.findByLoginId(user.getLoginId())).isPresent();
		Assertions.assertThat(userRepository.findByLoginId("unknown")).isEmpty();
	}

	@Test
	void existsByLoginId() {
		saveUser("bandmaster", "bandmaster@example.com");

		Assertions.assertThat(userRepository.existsByLoginId("bandmaster")).isTrue();
		Assertions.assertThat(userRepository.existsByLoginId("unknown")).isFalse();
	}

	@Test
	void existsByEmail() {
		saveUser("bandmaster", "bandmaster@example.com");

		Assertions.assertThat(userRepository.existsByEmail("bandmaster@example.com")).isTrue();
		Assertions.assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
	}

	@Test
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

		Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
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
