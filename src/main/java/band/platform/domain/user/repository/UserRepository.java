package band.platform.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByLoginId(String loginId);

	Optional<User> findByLoginIdAndEmail(String loginId, String email);

	Optional<User> findByLoginIdAndEmailAndStatus(String loginId, String email, UserStatus status);

	Optional<User> findByIdAndStatus(Long id, UserStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id and u.status = :status")
	Optional<User> findByIdAndStatusForUpdate(@Param("id") Long id, @Param("status") UserStatus status);

	Optional<User> findByEmail(String email);

	Optional<User> findByEmailAndStatus(String email, UserStatus status);

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);
}
