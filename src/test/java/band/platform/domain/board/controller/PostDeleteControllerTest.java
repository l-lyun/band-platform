package band.platform.domain.board.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import band.platform.global.error.ErrorCode;

class PostDeleteControllerTest extends PostControllerTestSupport {

	@Test
	@DisplayName("작성자가 게시글 삭제를 요청하면 200 응답과 공통 성공 응답을 반환한다")
	void deletePost() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(delete("/api/posts/{postId}", 10L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		verify(postDeleteService).delete(1L, 10L);
	}

	@Test
	@DisplayName("작성자가 아닌 회원이 게시글 삭제를 요청하면 A02 에러 응답을 반환한다")
	void deletePostByNonAuthor() throws Exception {
		setAuthenticatedPrincipal(2L, "other");
		whenDeleteFails(2L, 10L, ErrorCode.AUTH_FORBIDDEN);

		mockMvc.perform(delete("/api/posts/{postId}", 10L))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.code").value("A02"))
			.andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
	}

	@Test
	@DisplayName("존재하지 않는 게시글 삭제는 E02 에러 응답을 반환한다")
	void deleteMissingPost() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		whenDeleteFails(1L, 999L, ErrorCode.COMMON_NOT_FOUND);

		mockMvc.perform(delete("/api/posts/{postId}", 999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("E02"))
			.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("탈퇴한 작성자가 게시글 삭제를 요청하면 E02 에러 응답을 반환한다")
	void deletePostByWithdrawnAuthor() throws Exception {
		setAuthenticatedPrincipal(1L, "withdrawn");
		whenDeleteFails(1L, 10L, ErrorCode.COMMON_NOT_FOUND);

		mockMvc.perform(delete("/api/posts/{postId}", 10L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("E02"))
			.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));
	}
}
