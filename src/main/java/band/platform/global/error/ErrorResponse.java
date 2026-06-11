package band.platform.global.error;

public record ErrorResponse(
	int status,
	String code,
	String message
) {

	public static ErrorResponse from(ErrorCode errorCode) {
		return new ErrorResponse(
			errorCode.status().value(),
			errorCode.code(),
			errorCode.message()
		);
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(
			errorCode.status().value(),
			errorCode.code(),
			message
		);
	}

	public static ErrorResponse of(int status, ErrorCode errorCode) {
		return new ErrorResponse(
			status,
			errorCode.code(),
			errorCode.message()
		);
	}

}
