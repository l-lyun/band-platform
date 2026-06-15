package band.platform.global.security.handler;

import band.platform.global.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JsonAuthenticationEntryPointTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(
		new SecurityErrorResponseWriter(objectMapper)
	);

	@Test
	@DisplayName("인증되지 않은 요청을 A01 공통 에러 응답으로 변환한다")
	void commenceUnauthenticatedRequest() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(
			new MockHttpServletRequest(),
			response,
			new BadCredentialsException("unauthenticated")
		);

		ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(body.status()).isEqualTo(401);
		assertThat(body.code()).isEqualTo("A01");
		assertThat(body.message()).isEqualTo("로그인이 필요합니다.");
	}

}
