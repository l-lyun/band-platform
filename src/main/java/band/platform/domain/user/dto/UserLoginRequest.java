package band.platform.domain.user.dto;

import java.nio.charset.StandardCharsets;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
	@NotBlank
	@Size(min = 4, max = 30)
	String loginId,

	@NotBlank
	@Size(min = 8, max = 72)
	String password
) {

	private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

	@AssertTrue
	public boolean isPasswordByteLengthValid() {
		return password == null || !exceedsBcryptByteLimit();
	}

	public boolean exceedsBcryptByteLimit() {
		return password != null && password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES;
	}

}
