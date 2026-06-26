package band.platform.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import band.platform.domain.user.entity.User;
import band.platform.domain.user.entity.UserStatus;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByLoginId(String loginId);

	Optional<User> findByLoginIdAndEmail(String loginId, String email);

	Optional<User> findByEmail(String email);

	Optional<User> findByEmailAndStatus(String email, UserStatus status);

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);
}
