package band.platform.global;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record ApiResult<T>(
	int status,
	String message,
	T data
) {

	private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공했습니다.";
	private static final String CREATED_MESSAGE = "리소스가 생성되었습니다.";

	public static <T> ApiResult<T> ok(T data) {
		return of(HttpStatus.OK, DEFAULT_SUCCESS_MESSAGE, data);
	}

	public static ApiResult<Void> ok() {
		return of(HttpStatus.OK, DEFAULT_SUCCESS_MESSAGE, null);
	}

	public static <T> ApiResult<T> created(T data) {
		return of(HttpStatus.CREATED, CREATED_MESSAGE, data);
	}

	public static ApiResult<Void> created() {
		return of(HttpStatus.CREATED, CREATED_MESSAGE, null);
	}

	public static <T> ApiResult<T> of(HttpStatus status, String message, T data) {
		return new ApiResult<>(status.value(), message, data);
	}

	public ResponseEntity<ApiResult<T>> toResponseEntity() {
		return ResponseEntity.status(status).body(this);
	}

}
