package band.platform.domain.user.service;

public interface PasswordResetMailSender {

	void sendPasswordResetCode(String email, String code);
}
