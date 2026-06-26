package band.platform.domain.user.repository;

import java.time.Duration;
import java.util.Optional;

public interface UserPasswordResetRepository {

	void saveCode(Long userId, String codeHash, Duration ttl);

	Optional<PasswordResetCode> findCode(Long userId);

	Optional<PasswordResetCode> incrementCodeAttempts(Long userId);

	void deleteCode(Long userId);

	void saveToken(String tokenHash, Long userId, Duration ttl);

	boolean consumeCodeAndSaveToken(Long userId, String tokenHash, Duration tokenTtl);

	Optional<Long> consumeToken(String tokenHash);

	record PasswordResetCode(String codeHash, int attempts) {
	}
}
