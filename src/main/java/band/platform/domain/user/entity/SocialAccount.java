package band.platform.domain.user.entity;

import band.platform.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "social_accounts",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_social_accounts_provider_subject",
			columnNames = {"provider", "provider_subject"}
		),
		@UniqueConstraint(
			name = "uk_social_accounts_user_provider",
			columnNames = {"user_id", "provider"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SocialAccount extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private SocialProvider provider;

	@Column(nullable = false, length = 191)
	private String providerSubject;

	private SocialAccount(User user, SocialProvider provider, String providerSubject) {
		if (provider == SocialProvider.LOCAL) {
			throw new IllegalArgumentException("LOCAL provider cannot be used for social account");
		}
		this.user = user;
		this.provider = provider;
		this.providerSubject = providerSubject;
	}

	public static SocialAccount connect(User user, SocialProvider provider, String providerSubject) {
		return new SocialAccount(user, provider, providerSubject);
	}

}
