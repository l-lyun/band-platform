package band.platform.domain.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import band.platform.domain.board.dto.PostCreateResponse;
import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.service.PostCreateService;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.jwt.JwtAuthenticationPrincipal;

class PostControllerTest {

	private MockMvc mockMvc;
	private PostCreateService postCreateService;

	@BeforeEach
	void setUp() {
		postCreateService = mock(PostCreateService.class);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new PostController(postCreateService))
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("인증된 사용자가 게시글 생성을 요청하면 201 응답과 생성된 게시글 정보를 반환한다")
	void createPost() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		when(postCreateService.create(eq(1L), any()))
			.thenReturn(new PostCreateResponse(
				10L,
				BoardType.FREE,
				"합주 공지",
				"토요일 오후 2시에 합주합니다.",
				1L
			));

		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(postCreateRequest("FREE", "합주 공지", "토요일 오후 2시에 합주합니다.")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value(201))
			.andExpect(jsonPath("$.message").value("리소스가 생성되었습니다."))
			.andExpect(jsonPath("$.data.id").value(10))
			.andExpect(jsonPath("$.data.boardType").value("FREE"))
			.andExpect(jsonPath("$.data.title").value("합주 공지"))
			.andExpect(jsonPath("$.data.content").value("토요일 오후 2시에 합주합니다."))
			.andExpect(jsonPath("$.data.authorId").value(1));

		verify(postCreateService).create(eq(1L), any());
	}

	@Test
	@DisplayName("제목이 비어 있으면 E01 에러 응답을 반환한다")
	void blankTitle() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(postCreateRequest("FREE", " ", "토요일 오후 2시에 합주합니다.")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("내용이 비어 있으면 E01 에러 응답을 반환한다")
	void blankContent() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(postCreateRequest("FREE", "합주 공지", " ")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("게시판 타입이 없으면 E01 에러 응답을 반환한다")
	void missingBoardType() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"title": "합주 공지",
						"content": "토요일 오후 2시에 합주합니다."
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("게시판 타입이 null이면 E01 에러 응답을 반환한다")
	void nullBoardType() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"boardType": null,
						"title": "합주 공지",
						"content": "토요일 오후 2시에 합주합니다."
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("게시판 타입이 올바르지 않으면 E01 에러 응답을 반환한다")
	void invalidBoardType() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(postCreateRequest("UNKNOWN", "합주 공지", "토요일 오후 2시에 합주합니다.")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("제목이 100자를 초과하면 E01 에러 응답을 반환한다")
	void titleLongerThan100() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(postCreateRequest("FREE", "a".repeat(101), "토요일 오후 2시에 합주합니다.")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("작성자 회원을 찾을 수 없으면 E02 에러 응답을 반환한다")
	void authorNotFound() throws Exception {
		setAuthenticatedPrincipal(999L, "unknown");
		when(postCreateService.create(eq(999L), any()))
			.thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));

		mockMvc.perform(post("/api/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(postCreateRequest("FREE", "합주 공지", "토요일 오후 2시에 합주합니다.")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("E02"))
			.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));
	}

	private void setAuthenticatedPrincipal(Long userId, String loginId) {
		JwtAuthenticationPrincipal principal = new JwtAuthenticationPrincipal(userId, loginId);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private String postCreateRequest(String boardType, String title, String content) {
		return """
			{
				"boardType": "%s",
				"title": "%s",
				"content": "%s"
			}
			""".formatted(boardType, title, content);
	}
}
