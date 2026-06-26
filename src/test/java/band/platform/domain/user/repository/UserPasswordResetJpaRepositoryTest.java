package band.platform.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;

@SpringJUnitConfig(UserPasswordResetJpaRepositoryTest.JpaTestConfig.class)
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:password_reset;MODE=MySQL;DB_CLOSE_DELAY=-1",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class UserPasswordResetJpaRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("로그인 아이디와 이메일이 모두 일치하는 회원을 비밀번호 재설정 대상으로 조회한다")
	void findByLoginIdAndEmail() {
		User user = saveUser("bandmaster", "bandmaster@example.com");

		assertThat(userRepository.findByLoginIdAndEmail("bandmaster", "bandmaster@example.com"))
			.hasValueSatisfying(foundUser -> assertThat(foundUser.getId()).isEqualTo(user.getId()));
		assertThat(userRepository.findByLoginIdAndEmail("bandmaster", "other@example.com")).isEmpty();
		assertThat(userRepository.findByLoginIdAndEmail("other", "bandmaster@example.com")).isEmpty();
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

	@TestConfiguration(proxyBeanMethods = false)
	@EnableJpaAuditing
	@EnableJpaRepositories(basePackageClasses = UserRepository.class)
	@EntityScan(basePackageClasses = User.class)
	@ImportAutoConfiguration({
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		TransactionAutoConfiguration.class
	})
	static class JpaTestConfig {
	}

}
