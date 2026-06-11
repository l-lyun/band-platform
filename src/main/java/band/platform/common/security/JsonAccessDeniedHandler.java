package band.platform.common.security;

import band.platform.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityErrorResponseWriter errorResponseWriter;

	public JsonAccessDeniedHandler(SecurityErrorResponseWriter errorResponseWriter) {
		this.errorResponseWriter = errorResponseWriter;
	}

	@Override
	public void handle(
		HttpServletRequest request,
		HttpServletResponse response,
		AccessDeniedException accessDeniedException
	) throws IOException {
		errorResponseWriter.write(response, ErrorCode.AUTH_FORBIDDEN);
	}

}
