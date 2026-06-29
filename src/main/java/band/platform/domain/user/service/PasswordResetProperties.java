package band.platform.domain.user.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "security.password-reset")
@Getter
@Setter
public class PasswordResetProperties {

	private Duration codeTtl = Duration.ofMinutes(5);
	private Duration tokenTtl = Duration.ofMinutes(10);
	private int maxAttempts = 5;

	@PostConstruct
	void validate() {
		if (codeTtl == null || codeTtl.isZero() || codeTtl.isNegative()) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
		if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
		if (maxAttempts <= 0) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
	}
}
