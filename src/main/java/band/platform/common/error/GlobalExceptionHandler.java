package band.platform.common.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();

		return ResponseEntity
			.status(errorCode.status())
			.body(ErrorResponse.of(errorCode, exception.getMessage()));
	}

	@ExceptionHandler({
		MethodArgumentNotValidException.class,
		ConstraintViolationException.class,
		HttpMessageNotReadableException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidInput(Exception exception) {
		ErrorCode errorCode = ErrorCode.COMMON_INVALID_INPUT;

		return ResponseEntity
			.status(errorCode.status())
			.body(ErrorResponse.from(errorCode));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
		if (exception instanceof org.springframework.web.ErrorResponse errorResponse) {
			int statusCode = errorResponse.getStatusCode().value();
			ErrorCode errorCode = resolveSpringErrorCode(statusCode);

			return ResponseEntity
				.status(statusCode)
				.body(ErrorResponse.of(statusCode, errorCode));
		}

		ErrorCode errorCode = ErrorCode.COMMON_INTERNAL_SERVER_ERROR;

		return ResponseEntity
			.status(errorCode.status())
			.body(ErrorResponse.from(errorCode));
	}

	private ErrorCode resolveSpringErrorCode(int statusCode) {
		return switch (statusCode) {
			case 404 -> ErrorCode.COMMON_NOT_FOUND;
			case 409 -> ErrorCode.COMMON_CONFLICT;
			default -> {
				if (statusCode >= 400 && statusCode < 500) {
					yield ErrorCode.COMMON_INVALID_INPUT;
				}
				yield ErrorCode.COMMON_INTERNAL_SERVER_ERROR;
			}
		};
	}

}
