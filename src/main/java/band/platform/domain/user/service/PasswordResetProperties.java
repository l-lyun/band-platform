package band.platform.domain.user.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
	private Mail mail = new Mail();

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
		if (mail == null || !StringUtils.hasText(mail.getFrom()) || !StringUtils.hasText(mail.getSubject())) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
	}

	@Getter
	@Setter
	public static class Mail {

		private String from = "no-reply@band-platform.local";
		private String subject = "[BandMaster] 비밀번호 재설정 인증 코드";
	}
}
