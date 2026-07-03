package band.platform.domain.user.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

public record UserInterestUpdateRequest(
	@Size(max = 50)
	List<@Size(max = 100) String> favoriteArtists,

	@Size(max = 50)
	List<@Size(max = 100) String> favoriteEquipments,

	@Size(max = 50)
	List<@Size(max = 100) String> favoriteVenues,

	@Size(max = 50)
	List<@Size(max = 100) String> visitedConcerts
) {
}
