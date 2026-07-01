package band.platform.domain.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import band.platform.domain.board.dto.PostUpdateResponse;
import band.platform.domain.board.entity.BoardType;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class PostUpdateControllerTest extends PostControllerTestSupport {

	@Test
	@DisplayName("작성자가 게시글 수정을 요청하면 200 응답과 수정된 게시글 정보를 반환한다")
	void updatePost() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 4, 10, 0);
		when(postUpdateService.update(eq(1L), eq(10L), any()))
			.thenReturn(new PostUpdateResponse(
				10L,
				BoardType.FREE,
				"수정된 합주 공지",
				"토요일 오후 3시로 변경합니다.",
				1L,
				updatedAt
			));

		mockMvc.perform(patch("/api/posts/{postId}", 10L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postUpdateRequest("수정된 합주 공지", "토요일 오후 3시로 변경합니다.")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.id").value(10))
			.andExpect(jsonPath("$.data.boardType").value("FREE"))
			.andExpect(jsonPath("$.data.title").value("수정된 합주 공지"))
			.andExpect(jsonPath("$.data.content").value("토요일 오후 3시로 변경합니다."))
			.andExpect(jsonPath("$.data.authorId").value(1))
			.andExpect(jsonPath("$.data.updatedAt").value("2026-01-04T10:00:00"));

		verify(postUpdateService).update(eq(1L), eq(10L), any());
	}

	@Test
	@DisplayName("수정 요청에 게시판 타입이 포함되어도 기존 게시판 타입을 응답한다")
	void updatePostIgnoresBoardType() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 4, 10, 0);
		when(postUpdateService.update(eq(1L), eq(10L), any()))
			.thenReturn(new PostUpdateResponse(
				10L,
				BoardType.FREE,
				"수정된 합주 공지",
				"토요일 오후 3시로 변경합니다.",
				1L,
				updatedAt
			));

		mockMvc.perform(patch("/api/posts/{postId}", 10L)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"boardType": "SECRET",
						"title": "수정된 합주 공지",
						"content": "토요일 오후 3시로 변경합니다."
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.boardType").value("FREE"))
			.andExpect(jsonPath("$.data.title").value("수정된 합주 공지"))
			.andExpect(jsonPath("$.data.content").value("토요일 오후 3시로 변경합니다."));
	}

	@Test
	@DisplayName("작성자가 아닌 회원이 게시글 수정을 요청하면 A02 에러 응답을 반환한다")
	void updatePostByNonAuthor() throws Exception {
		setAuthenticatedPrincipal(2L, "other");
		when(postUpdateService.update(eq(2L), eq(10L), any()))
			.thenThrow(new BusinessException(ErrorCode.AUTH_FORBIDDEN));

		mockMvc.perform(patch("/api/posts/{postId}", 10L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postUpdateRequest("수정된 합주 공지", "토요일 오후 3시로 변경합니다.")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.code").value("A02"))
			.andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
	}

	@Test
	@DisplayName("존재하지 않는 게시글 수정은 E02 에러 응답을 반환한다")
	void updateMissingPost() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		when(postUpdateService.update(eq(1L), eq(999L), any()))
			.thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));

		mockMvc.perform(patch("/api/posts/{postId}", 999L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postUpdateRequest("수정된 합주 공지", "토요일 오후 3시로 변경합니다.")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("E02"))
			.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("수정 제목이 비어 있으면 E01 에러 응답을 반환한다")
	void blankUpdateTitle() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(patch("/api/posts/{postId}", 10L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postUpdateRequest(" ", "토요일 오후 3시로 변경합니다.")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("수정 내용이 비어 있으면 E01 에러 응답을 반환한다")
	void blankUpdateContent() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(patch("/api/posts/{postId}", 10L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postUpdateRequest("수정된 합주 공지", " ")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("수정 제목이 100자를 초과하면 E01 에러 응답을 반환한다")
	void updateTitleLongerThan100() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(patch("/api/posts/{postId}", 10L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postUpdateRequest("a".repeat(101), "토요일 오후 3시로 변경합니다.")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("수정 내용이 65535자를 초과하면 E01 에러 응답을 반환한다")
	void updateContentLongerThan65535() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(patch("/api/posts/{postId}", 10L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postUpdateRequest("수정된 합주 공지", "a".repeat(65536))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}
}
