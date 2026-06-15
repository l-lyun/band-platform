package band.platform.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import band.platform.domain.user.entity.SocialAccount;
import band.platform.domain.user.entity.SocialProvider;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

	Optional<SocialAccount> findByProviderAndProviderSubject(SocialProvider provider, String providerSubject);

	boolean existsByProviderAndProviderSubject(SocialProvider provider, String providerSubject);
}
