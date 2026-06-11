package band.platform.global.security;

import band.platform.global.error.ErrorCode;
import band.platform.global.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

	private final ObjectMapper objectMapper;

	public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getWriter(), ErrorResponse.from(errorCode));
	}

}
