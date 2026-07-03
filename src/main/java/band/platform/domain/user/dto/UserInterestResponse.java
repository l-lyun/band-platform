package band.platform.domain.user.dto;

import java.util.List;

import band.platform.domain.user.entity.UserInterest;

public record UserInterestResponse(
	List<String> favoriteArtists,
	List<String> favoriteEquipments,
	List<String> favoriteVenues,
	List<String> visitedConcerts
) {

	public static UserInterestResponse from(UserInterest userInterest) {
		return new UserInterestResponse(
			userInterest.getFavoriteArtists(),
			userInterest.getFavoriteEquipments(),
			userInterest.getFavoriteVenues(),
			userInterest.getVisitedConcerts()
		);
	}

	public static UserInterestResponse empty() {
		return new UserInterestResponse(List.of(), List.of(), List.of(), List.of());
	}
}
