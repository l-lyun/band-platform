package band.platform.common.error;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	@DisplayName("비즈니스 예외를 공통 에러 응답으로 변환한다")
	void handleBusinessException() {
		ResponseEntity<ErrorResponse> response = handler.handleBusinessException(
			new BusinessException(ErrorCode.USER_LOGIN_ID_DUPLICATED)
		);

		assertEquals(409, response.getStatusCode().value());
		assertEquals(409, response.getBody().status());
		assertEquals("U01", response.getBody().code());
		assertEquals("이미 사용 중인 아이디입니다.", response.getBody().message());
	}

	@Test
	@DisplayName("요청값 검증 실패를 E01 에러 응답으로 변환한다")
	void handleValidationException() {
		ResponseEntity<ErrorResponse> response = handler.handleInvalidInput(
			new ConstraintViolationException(Set.of())
		);

		assertEquals(400, response.getStatusCode().value());
		assertEquals(400, response.getBody().status());
		assertEquals("E01", response.getBody().code());
		assertEquals("요청 값이 올바르지 않습니다.", response.getBody().message());
	}

}
