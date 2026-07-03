package band.platform.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserInterest;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class UserInterestRepositoryTest {

	@Autowired
	private UserInterestRepository userInterestRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("회원 관심사를 저장하고 순서를 유지해 다시 조회한다")
	void saveAndFindByUser() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		UserInterest userInterest = userInterestRepository.save(UserInterest.create(
			user,
			List.of("넬", "쏜애플"),
			List.of("Fender Jazz Bass", "Marshall Amp"),
			List.of("올림픽공원", "홍대 클럽 FF"),
			List.of("서울재즈페스티벌", "펜타포트")
		));
		entityManager.flush();
		entityManager.clear();

		UserInterest foundInterest = userInterestRepository.findByUserId(user.getId()).orElseThrow();

		assertThat(foundInterest.getId()).isEqualTo(userInterest.getId());
		assertThat(foundInterest.getUser().getId()).isEqualTo(user.getId());
		assertThat(foundInterest.getFavoriteArtists()).containsExactly("넬", "쏜애플");
		assertThat(foundInterest.getFavoriteEquipments()).containsExactly("Fender Jazz Bass", "Marshall Amp");
		assertThat(foundInterest.getFavoriteVenues()).containsExactly("올림픽공원", "홍대 클럽 FF");
		assertThat(foundInterest.getVisitedConcerts()).containsExactly("서울재즈페스티벌", "펜타포트");
	}

	@Test
	@DisplayName("한 회원은 관심사 aggregate를 하나만 가질 수 있다")
	void duplicatedUserInterest() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		userInterestRepository.saveAndFlush(UserInterest.create(
			user,
			List.of("넬"),
			List.of(),
			List.of(),
			List.of()
		));
		entityManager.clear();
		User foundUser = userRepository.findById(user.getId()).orElseThrow();

		assertThatThrownBy(() -> userInterestRepository.saveAndFlush(UserInterest.create(
			foundUser,
			List.of("쏜애플"),
			List.of(),
			List.of(),
			List.of()
		))).isInstanceOf(DataIntegrityViolationException.class);
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
