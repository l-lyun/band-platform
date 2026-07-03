package band.platform.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import band.platform.domain.user.entity.UserInterest;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

	Optional<UserInterest> findByUserId(Long userId);
}
