package band.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.UserInterestResponse;
import band.platform.domain.user.dto.UserInterestUpdateRequest;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserInterest;
import band.platform.domain.user.repository.UserInterestRepository;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class UserInterestServiceTest {

	@Autowired
	private UserInterestService userInterestService;

	@Autowired
	private UserInterestRepository userInterestRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("ACTIVE 회원이 저장한 관심사가 없으면 빈 목록을 반환한다")
	void getMyInterestsReturnsEmpty() {
		User user = saveUser("bandmaster", "bandmaster@example.com");

		UserInterestResponse response = userInterestService.getMyInterests(user.getId());

		assertThat(response.favoriteArtists()).isEmpty();
		assertThat(response.favoriteEquipments()).isEmpty();
		assertThat(response.favoriteVenues()).isEmpty();
		assertThat(response.visitedConcerts()).isEmpty();
	}

	@Test
	@DisplayName("ACTIVE 회원이 관심사를 저장하면 정규화된 목록을 반환하고 영속화한다")
	void updateMyInterestsCreatesInterest() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		UserInterestUpdateRequest request = new UserInterestUpdateRequest(
			Arrays.asList(" 넬 ", null, "넬", "쏜애플"),
			List.of(" Fender Jazz Bass ", "Fender Jazz Bass"),
			null,
			List.of(" 2025 서울재즈페스티벌 ")
		);

		UserInterestResponse response = userInterestService.updateMyInterests(user.getId(), request);
		entityManager.flush();
		entityManager.clear();
		UserInterest foundInterest = userInterestRepository.findByUserId(user.getId()).orElseThrow();

		assertThat(response.favoriteArtists()).containsExactly("넬", "쏜애플");
		assertThat(response.favoriteEquipments()).containsExactly("Fender Jazz Bass");
		assertThat(response.favoriteVenues()).isEmpty();
		assertThat(response.visitedConcerts()).containsExactly("2025 서울재즈페스티벌");
		assertThat(foundInterest.getFavoriteArtists()).containsExactly("넬", "쏜애플");
		assertThat(foundInterest.getFavoriteEquipments()).containsExactly("Fender Jazz Bass");
		assertThat(foundInterest.getFavoriteVenues()).isEmpty();
		assertThat(foundInterest.getVisitedConcerts()).containsExactly("2025 서울재즈페스티벌");
	}

	@Test
	@DisplayName("기존 관심사가 있으면 같은 aggregate를 수정한다")
	void updateMyInterestsUpdatesExistingInterest() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		UserInterest userInterest = userInterestRepository.save(UserInterest.create(
			user,
			List.of("넬"),
			List.of("기타"),
			List.of("올림픽공원"),
			List.of("펜타포트")
		));
		UserInterestUpdateRequest request = new UserInterestUpdateRequest(
			List.of("아이유"),
			List.of(),
			List.of("블루스퀘어"),
			List.of("서울재즈페스티벌", "서울재즈페스티벌")
		);

		UserInterestResponse response = userInterestService.updateMyInterests(user.getId(), request);
		entityManager.flush();
		entityManager.clear();
		UserInterest foundInterest = userInterestRepository.findByUserId(user.getId()).orElseThrow();

		assertThat(response.favoriteArtists()).containsExactly("아이유");
		assertThat(response.favoriteEquipments()).isEmpty();
		assertThat(response.favoriteVenues()).containsExactly("블루스퀘어");
		assertThat(response.visitedConcerts()).containsExactly("서울재즈페스티벌");
		assertThat(foundInterest.getId()).isEqualTo(userInterest.getId());
		assertThat(foundInterest.getFavoriteArtists()).containsExactly("아이유");
	}

	@Test
	@DisplayName("회원이 없으면 E02 예외를 던진다")
	void userNotFound() {
		assertThatThrownBy(() -> userInterestService.getMyInterests(999L))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
	}

	@Test
	@DisplayName("WITHDRAWN 회원이면 E02 예외를 던진다")
	void withdrawnUserNotFound() {
		User user = saveUser("bandmaster", "bandmaster@example.com");
		entityManager.flush();
		entityManager.createNativeQuery("UPDATE users SET status = 'WITHDRAWN' WHERE id = ?")
			.setParameter(1, user.getId())
			.executeUpdate();
		entityManager.clear();

		assertThatThrownBy(() -> userInterestService.getMyInterests(user.getId()))
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
