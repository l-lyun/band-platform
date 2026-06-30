package band.platform.domain.user.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class JavaMailPasswordResetMailSender implements PasswordResetMailSender {

	private final JavaMailSender javaMailSender;
	private final PasswordResetProperties properties;

	public JavaMailPasswordResetMailSender(JavaMailSender javaMailSender, PasswordResetProperties properties) {
		this.javaMailSender = javaMailSender;
		this.properties = properties;
	}

	@Override
	public void sendPasswordResetCode(String email, String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(properties.getMail().getFrom());
		message.setTo(email);
		message.setSubject(properties.getMail().getSubject());
		message.setText("""
			BandMaster 비밀번호 재설정 인증 코드입니다.

			인증 코드: %s
			""".formatted(code));

		javaMailSender.send(message);
	}
}
