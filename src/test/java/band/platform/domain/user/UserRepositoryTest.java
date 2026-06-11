package band.platform.domain.user;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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
