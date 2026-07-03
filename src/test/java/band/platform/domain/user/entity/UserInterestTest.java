package band.platform.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserInterestTest {

	@Test
	@DisplayName("관심사를 생성하면 카테고리별 항목을 정규화한다")
	void createNormalizesItems() {
		User user = createUser();

		UserInterest userInterest = UserInterest.create(
			user,
			List.of("  넬  ", "넬", " ", "쏜애플"),
			null,
			List.of(" 올림픽공원 ", "올림픽공원", "\t"),
			List.of(" 2025 서울재즈페스티벌 ")
		);

		assertThat(userInterest.getUser()).isEqualTo(user);
		assertThat(userInterest.getFavoriteArtists()).containsExactly("넬", "쏜애플");
		assertThat(userInterest.getFavoriteEquipments()).isEmpty();
		assertThat(userInterest.getFavoriteVenues()).containsExactly("올림픽공원");
		assertThat(userInterest.getVisitedConcerts()).containsExactly("2025 서울재즈페스티벌");
	}

	@Test
	@DisplayName("관심사를 수정하면 null 항목과 공백 항목을 제거하고 중복은 첫 항목만 남긴다")
	void updateNormalizesItems() {
		UserInterest userInterest = UserInterest.create(createUser(), List.of("넬"), List.of(), List.of(), List.of());

		userInterest.update(
			listWithNull("아이유", null, " 아이유 ", "검정치마"),
			listWithNull(null, " Fender Jazz Bass ", "Fender Jazz Bass", " "),
			null,
			List.of(" 2024 펜타포트 ", "2024 펜타포트")
		);

		assertThat(userInterest.getFavoriteArtists()).containsExactly("아이유", "검정치마");
		assertThat(userInterest.getFavoriteEquipments()).containsExactly("Fender Jazz Bass");
		assertThat(userInterest.getFavoriteVenues()).isEmpty();
		assertThat(userInterest.getVisitedConcerts()).containsExactly("2024 펜타포트");
	}

	private List<String> listWithNull(String first, String second, String third, String fourth) {
		return java.util.Arrays.asList(first, second, third, fourth);
	}

	private User createUser() {
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
