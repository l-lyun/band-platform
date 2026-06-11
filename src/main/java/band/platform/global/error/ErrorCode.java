package band.platform.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	COMMON_INVALID_INPUT("E01", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
	COMMON_NOT_FOUND("E02", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	COMMON_CONFLICT("E03", HttpStatus.CONFLICT, "이미 존재하는 값입니다."),
	COMMON_INTERNAL_SERVER_ERROR("E04", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

	AUTH_REQUIRED("A01", HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	AUTH_FORBIDDEN("A02", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	AUTH_INVALID_CREDENTIALS("A03", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
	AUTH_ACCOUNT_LOCKED("A04", HttpStatus.LOCKED, "로그인 실패 횟수가 초과되어 계정이 잠겼습니다."),
	AUTH_TOKEN_EXPIRED("A05", HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다."),
	AUTH_TOKEN_INVALID("A06", HttpStatus.UNAUTHORIZED, "인증 토큰이 올바르지 않습니다."),
	AUTH_CODE_INVALID("A07", HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
	AUTH_CODE_EXPIRED("A08", HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다."),

	USER_LOGIN_ID_DUPLICATED("U01", HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
	USER_EMAIL_DUPLICATED("U02", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;

	ErrorCode(String code, HttpStatus status, String message) {
		this.code = code;
		this.status = status;
		this.message = message;
	}

	public String code() {
		return code;
	}

	public HttpStatus status() {
		return status;
	}

	public String message() {
		return message;
	}

}
