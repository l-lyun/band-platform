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
		ErrorCode errorCode = ErrorCode.COMMON_INTERNAL_SERVER_ERROR;

		return ResponseEntity
			.status(errorCode.status())
			.body(ErrorResponse.from(errorCode));
	}

}
