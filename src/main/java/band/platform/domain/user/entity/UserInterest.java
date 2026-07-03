package band.platform.domain.user.entity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import band.platform.global.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "user_interests",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_interests_user_id", columnNames = "user_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInterest extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@ElementCollection
	@CollectionTable(name = "user_interest_favorite_artists", joinColumns = @JoinColumn(name = "user_interest_id"))
	@OrderColumn(name = "display_order")
	@Column(name = "artist_name", nullable = false, length = 100)
	private List<String> favoriteArtists = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "user_interest_favorite_equipments", joinColumns = @JoinColumn(name = "user_interest_id"))
	@OrderColumn(name = "display_order")
	@Column(name = "equipment_name", nullable = false, length = 100)
	private List<String> favoriteEquipments = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "user_interest_favorite_venues", joinColumns = @JoinColumn(name = "user_interest_id"))
	@OrderColumn(name = "display_order")
	@Column(name = "venue_name", nullable = false, length = 100)
	private List<String> favoriteVenues = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "user_interest_visited_concerts", joinColumns = @JoinColumn(name = "user_interest_id"))
	@OrderColumn(name = "display_order")
	@Column(name = "concert_name", nullable = false, length = 100)
	private List<String> visitedConcerts = new ArrayList<>();

	private UserInterest(
		User user,
		List<String> favoriteArtists,
		List<String> favoriteEquipments,
		List<String> favoriteVenues,
		List<String> visitedConcerts
	) {
		this.user = user;
		update(favoriteArtists, favoriteEquipments, favoriteVenues, visitedConcerts);
	}

	public static UserInterest create(
		User user,
		List<String> favoriteArtists,
		List<String> favoriteEquipments,
		List<String> favoriteVenues,
		List<String> visitedConcerts
	) {
		return new UserInterest(user, favoriteArtists, favoriteEquipments, favoriteVenues, visitedConcerts);
	}

	public void update(
		List<String> favoriteArtists,
		List<String> favoriteEquipments,
		List<String> favoriteVenues,
		List<String> visitedConcerts
	) {
		replace(this.favoriteArtists, normalize(favoriteArtists));
		replace(this.favoriteEquipments, normalize(favoriteEquipments));
		replace(this.favoriteVenues, normalize(favoriteVenues));
		replace(this.visitedConcerts, normalize(visitedConcerts));
	}

	public User getUser() {
		return user;
	}

	public List<String> getFavoriteArtists() {
		return List.copyOf(favoriteArtists);
	}

	public List<String> getFavoriteEquipments() {
		return List.copyOf(favoriteEquipments);
	}

	public List<String> getFavoriteVenues() {
		return List.copyOf(favoriteVenues);
	}

	public List<String> getVisitedConcerts() {
		return List.copyOf(visitedConcerts);
	}

	private static List<String> normalize(List<String> items) {
		if (items == null) {
			return new ArrayList<>();
		}

		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String item : items) {
			if (item == null) {
				continue;
			}

			String trimmedItem = item.trim();
			if (!trimmedItem.isBlank()) {
				normalized.add(trimmedItem);
			}
		}
		return new ArrayList<>(normalized);
	}

	private static void replace(List<String> target, List<String> source) {
		target.clear();
		target.addAll(source);
	}
}
