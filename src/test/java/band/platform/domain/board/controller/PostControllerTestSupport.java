package band.platform.domain.board.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import band.platform.domain.board.service.PostCreateService;
import band.platform.domain.board.service.PostDeleteService;
import band.platform.domain.board.service.PostQueryService;
import band.platform.domain.board.service.PostUpdateService;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.jwt.JwtAuthenticationPrincipal;

abstract class PostControllerTestSupport {

	protected MockMvc mockMvc;
	protected PostCreateService postCreateService;
	protected PostQueryService postQueryService;
	protected PostUpdateService postUpdateService;
	protected PostDeleteService postDeleteService;

	@BeforeEach
	void setUp() {
		postCreateService = mock(PostCreateService.class);
		postQueryService = mock(PostQueryService.class);
		postUpdateService = mock(PostUpdateService.class);
		postDeleteService = mock(PostDeleteService.class);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new PostController(postCreateService, postQueryService, postUpdateService, postDeleteService))
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	protected void setAuthenticatedPrincipal(Long userId, String loginId) {
		JwtAuthenticationPrincipal principal = new JwtAuthenticationPrincipal(userId, loginId);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	protected String postCreateRequest(String boardType, String title, String content) {
		return """
			{
				"boardType": "%s",
				"title": "%s",
				"content": "%s"
			}
			""".formatted(boardType, title, content);
	}

	protected String postUpdateRequest(String title, String content) {
		return """
			{
				"title": "%s",
				"content": "%s"
			}
			""".formatted(title, content);
	}

	protected void whenDeleteFails(Long authorId, Long postId, ErrorCode errorCode) {
		doThrow(new BusinessException(errorCode))
			.when(postDeleteService)
			.delete(authorId, postId);
	}
}
