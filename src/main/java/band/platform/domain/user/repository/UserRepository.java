package band.platform.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import band.platform.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByLoginId(String loginId);

	Optional<User> findByEmail(String email);

	boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);
}
