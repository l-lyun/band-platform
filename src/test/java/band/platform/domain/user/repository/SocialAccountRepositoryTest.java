package band.platform.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.SocialAccount;
import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.entity.User;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class SocialAccountRepositoryTest {

	@Autowired
	private SocialAccountRepository socialAccountRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("제공자와 제공자 고유 식별자로 소셜 계정을 조회한다")
	void findByProviderAndProviderSubject() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		saveSocialAccount(user, SocialProvider.NAVER, "naver-subject");

		assertThat(socialAccountRepository.findByProviderAndProviderSubject(SocialProvider.NAVER, "naver-subject"))
			.hasValueSatisfying(socialAccount -> assertThat(socialAccount.getUser().getId()).isEqualTo(user.getId()));
		assertThat(socialAccountRepository.findByProviderAndProviderSubject(SocialProvider.KAKAO, "naver-subject")).isEmpty();
		assertThat(socialAccountRepository.findByProviderAndProviderSubject(SocialProvider.NAVER, "unknown")).isEmpty();
	}

	@Test
	@DisplayName("제공자와 제공자 고유 식별자의 존재 여부를 반환한다")
	void existsByProviderAndProviderSubject() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		saveSocialAccount(user, SocialProvider.KAKAO, "kakao-subject");

		assertThat(socialAccountRepository.existsByProviderAndProviderSubject(SocialProvider.KAKAO, "kakao-subject")).isTrue();
		assertThat(socialAccountRepository.existsByProviderAndProviderSubject(SocialProvider.NAVER, "kakao-subject")).isFalse();
		assertThat(socialAccountRepository.existsByProviderAndProviderSubject(SocialProvider.KAKAO, "unknown")).isFalse();
	}

	@Test
	@DisplayName("같은 제공자와 같은 고유 식별자는 중복 저장할 수 없다")
	void duplicatedProviderSubject() {
		User firstUser = saveUser("bandmaster", "bandmaster@example.com");
		User secondUser = saveUser("other", "other@example.com");
		saveSocialAccount(firstUser, SocialProvider.APPLE, "apple-subject");

		assertThatThrownBy(() -> {
			socialAccountRepository.saveAndFlush(
				SocialAccount.connect(secondUser, SocialProvider.APPLE, "apple-subject")
			);
		}).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("제공자가 다르면 같은 고유 식별자도 별도 계정으로 저장된다")
	void sameSubjectWithDifferentProvider() {
		User firstUser = saveUser("bandmaster", "bandmaster@example.com");
		User secondUser = saveUser("other", "other@example.com");

		saveSocialAccount(firstUser, SocialProvider.NAVER, "same-subject");
		saveSocialAccount(secondUser, SocialProvider.KAKAO, "same-subject");
		entityManager.flush();
		entityManager.clear();

		assertThat(socialAccountRepository.findByProviderAndProviderSubject(SocialProvider.NAVER, "same-subject")).isPresent();
		assertThat(socialAccountRepository.findByProviderAndProviderSubject(SocialProvider.KAKAO, "same-subject")).isPresent();
	}

	private SocialAccount saveSocialAccount(User user, SocialProvider provider, String providerSubject) {
		return socialAccountRepository.save(SocialAccount.connect(user, provider, providerSubject));
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
