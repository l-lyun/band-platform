package band.platform.domain.user.entity;

import band.platform.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {

	@Column(nullable = false)
	private String name;

	@Column(unique = true)
	private String loginId;

	@Column
	private String password;

	@Column(nullable = false, unique = true)
	private String email;

	@Column
	private String description;

	@Column(nullable = false)
	private Boolean opened;

	@Column(nullable = false)
	private String phoneNumber;

	@Column
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

	@Column(nullable = false, columnDefinition = "varchar(20) default 'ACTIVE'")
	@Enumerated(EnumType.STRING)
	private UserStatus status = UserStatus.ACTIVE;

	private User(
		String name,
		String loginId,
		String password,
		String email,
		String description,
		Boolean opened,
		String phoneNumber,
		Gender gender,
		String profileImg,
		SocialProvider socialProvider,
		Boolean privacyPolicyAgreed,
		Boolean marketingPolicyAgreed
	) {
		this.name = name;
		this.loginId = loginId;
		this.password = password;
		this.email = email;
		this.description = description;
		this.opened = opened;
		this.phoneNumber = phoneNumber;
		this.gender = gender;
		this.profileImg = profileImg;
		this.socialProvider = socialProvider;
		this.privacyPolicyAgreed = privacyPolicyAgreed;
		this.marketingPolicyAgreed = marketingPolicyAgreed;
		this.status = UserStatus.ACTIVE;
	}

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

	public static User createSocialUser(
		String name,
		String email,
		String phoneNumber,
		SocialProvider socialProvider,
		Boolean privacyPolicyAgreed,
		Boolean marketingPolicyAgreed
	) {
		if (socialProvider == SocialProvider.LOCAL) {
			throw new IllegalArgumentException("LOCAL provider cannot be used for social user");
		}
		return new User(
			name,
			null,
			null,
			email,
			null,
			false,
			phoneNumber,
			null,
			null,
			socialProvider,
			privacyPolicyAgreed,
			marketingPolicyAgreed
		);
	}

	public void changePassword(String password) {
		this.password = password;
	}

}
