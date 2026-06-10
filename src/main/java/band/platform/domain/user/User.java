package band.platform.domain.user;

import band.platform.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {

	@Column(nullable = false)
	private String name;

	@Column(unique = true)
	private String loginId;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false, unique = true)
	private String email;

	@Column
	private String description;

	@Column(nullable = false)
	private Boolean opened;

	@Column(nullable = false)
	private String phoneNumber;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private Gender gender;

	@Column
	private String profileImg;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private SocialProvider socialProvider;

	@Column(nullable = false)
	private Boolean privacyPolicyAgreed;

	@Column(nullable = false)
	private Boolean marketingPolicyAgreed;

	public static User createLocalUser(
		String name,
		String loginId,
		String password,
		String email,
		String description,
		Boolean opened,
		String phoneNumber,
		Gender gender,
		String profileImg,
		Boolean privacyPolicyAgreed,
		Boolean marketingPolicyAgreed
	) {
		return new User(
			name,
			loginId,
			password,
			email,
			description,
			opened,
			phoneNumber,
			gender,
			profileImg,
			SocialProvider.LOCAL,
			privacyPolicyAgreed,
			marketingPolicyAgreed
		);
	}

}
