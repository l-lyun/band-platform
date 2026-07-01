package band.platform.domain.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import band.platform.domain.board.dto.PostDetailResponse;
import band.platform.domain.board.dto.PostListItemResponse;
import band.platform.domain.board.dto.PostPageResponse;
import band.platform.domain.board.entity.BoardType;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

class PostQueryControllerTest extends PostControllerTestSupport {

	@Test
	@DisplayName("게시글 단건 조회를 요청하면 200 응답과 상세 정보를 반환한다")
	void getPost() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
		LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 10, 0);
		when(postQueryService.getPost(10L))
			.thenReturn(new PostDetailResponse(
				10L,
				BoardType.FREE,
				"합주 공지",
				"토요일 오후 2시에 합주합니다.",
				1L,
				createdAt,
				updatedAt
			));

		mockMvc.perform(get("/api/posts/{postId}", 10L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.id").value(10))
			.andExpect(jsonPath("$.data.boardType").value("FREE"))
			.andExpect(jsonPath("$.data.title").value("합주 공지"))
			.andExpect(jsonPath("$.data.content").value("토요일 오후 2시에 합주합니다."))
			.andExpect(jsonPath("$.data.authorId").value(1))
			.andExpect(jsonPath("$.data.createdAt").value("2026-01-01T10:00:00"))
			.andExpect(jsonPath("$.data.updatedAt").value("2026-01-02T10:00:00"));

		verify(postQueryService).getPost(10L);
	}

	@Test
	@DisplayName("존재하지 않는 게시글 단건 조회는 E02 에러 응답을 반환한다")
	void getMissingPost() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		when(postQueryService.getPost(999L))
			.thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));

		mockMvc.perform(get("/api/posts/{postId}", 999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("E02"))
			.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("게시글 ID가 숫자가 아니면 단건 조회에서 E01 에러 응답을 반환한다")
	void getPostByNonNumericPostId() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(get("/api/posts/{postId}", "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));

		verifyNoInteractions(postCreateService, postQueryService, postUpdateService, postDeleteService);
	}

	@Test
	@DisplayName("게시판 타입 목록 조회를 요청하면 200 응답과 페이징된 게시글 요약을 최신순으로 반환한다")
	void getPostsByBoardType() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		LocalDateTime latestCreatedAt = LocalDateTime.of(2026, 1, 3, 10, 0);
		LocalDateTime middleCreatedAt = LocalDateTime.of(2026, 1, 2, 10, 0);
		when(postQueryService.getPosts(eq(BoardType.FREE), any()))
			.thenReturn(new PostPageResponse(
				List.of(
					new PostListItemResponse(12L, BoardType.FREE, "최신 자유글", 1L, latestCreatedAt, latestCreatedAt),
					new PostListItemResponse(11L, BoardType.FREE, "중간 자유글", 1L, middleCreatedAt, middleCreatedAt)
				),
				0,
				20,
				3,
				1,
				true,
				true
			));

		mockMvc.perform(get("/api/posts")
				.param("boardType", "FREE")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.page").value(0))
			.andExpect(jsonPath("$.data.size").value(20))
			.andExpect(jsonPath("$.data.totalElements").value(3))
			.andExpect(jsonPath("$.data.totalPages").value(1))
			.andExpect(jsonPath("$.data.first").value(true))
			.andExpect(jsonPath("$.data.last").value(true))
			.andExpect(jsonPath("$.data.posts[0].id").value(12))
			.andExpect(jsonPath("$.data.posts[0].boardType").value("FREE"))
			.andExpect(jsonPath("$.data.posts[0].title").value("최신 자유글"))
			.andExpect(jsonPath("$.data.posts[0].authorId").value(1))
			.andExpect(jsonPath("$.data.posts[1].id").value(11))
			.andExpect(jsonPath("$.data.posts[1].boardType").value("FREE"));

		verify(postQueryService).getPosts(eq(BoardType.FREE), any());
	}

	@Test
	@DisplayName("게시판 타입이 올바르지 않은 목록 조회는 E01 에러 응답을 반환한다")
	void getPostsByInvalidBoardType() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(get("/api/posts")
				.param("boardType", "UNKNOWN")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));

		verifyNoInteractions(postCreateService, postQueryService, postUpdateService, postDeleteService);
	}

	@Test
	@DisplayName("목록 조회 page가 음수이면 E01 에러 응답을 반환한다")
	void getPostsByNegativePage() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(get("/api/posts")
				.param("boardType", "FREE")
				.param("page", "-1")
				.param("size", "20"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));

		verifyNoInteractions(postQueryService);
	}

	@Test
	@DisplayName("목록 조회 page가 숫자가 아니면 E01 에러 응답을 반환한다")
	void getPostsByNonNumericPage() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(get("/api/posts")
				.param("boardType", "FREE")
				.param("page", "abc")
				.param("size", "20"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));

		verifyNoInteractions(postQueryService);
	}

	@Test
	@DisplayName("목록 조회 size가 0이면 E01 에러 응답을 반환한다")
	void getPostsByZeroSize() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(get("/api/posts")
				.param("boardType", "FREE")
				.param("page", "0")
				.param("size", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));

		verifyNoInteractions(postQueryService);
	}

	@Test
	@DisplayName("목록 조회 size가 100을 초과하면 E01 에러 응답을 반환한다")
	void getPostsBySizeGreaterThan100() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(get("/api/posts")
				.param("boardType", "FREE")
				.param("page", "0")
				.param("size", "101"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));

		verifyNoInteractions(postCreateService, postQueryService, postUpdateService, postDeleteService);
	}

	@Test
	@DisplayName("목록 조회 size가 숫자가 아니면 E01 에러 응답을 반환한다")
	void getPostsByNonNumericSize() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(get("/api/posts")
				.param("boardType", "FREE")
				.param("page", "0")
				.param("size", "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));

		verifyNoInteractions(postQueryService);
	}
}
