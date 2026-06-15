package band.platform.global.security.handler;

import band.platform.global.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JsonAccessDeniedHandlerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler(
		new SecurityErrorResponseWriter(objectMapper)
	);

	@Test
	@DisplayName("권한 없음 예외를 A02 공통 에러 응답으로 변환한다")
	void handleAccessDenied() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.handle(
			new MockHttpServletRequest(),
			response,
			new AccessDeniedException("forbidden")
		);

		ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(body.status()).isEqualTo(403);
		assertThat(body.code()).isEqualTo("A02");
		assertThat(body.message()).isEqualTo("접근 권한이 없습니다.");
	}

}
