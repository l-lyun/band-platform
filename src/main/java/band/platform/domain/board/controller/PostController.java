package band.platform.domain.board.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import band.platform.domain.board.dto.PostCreateRequest;
import band.platform.domain.board.dto.PostCreateResponse;
import band.platform.domain.board.dto.PostDetailResponse;
import band.platform.domain.board.dto.PostPageResponse;
import band.platform.domain.board.dto.PostUpdateRequest;
import band.platform.domain.board.dto.PostUpdateResponse;
import band.platform.domain.board.entity.BoardType;
import band.platform.domain.board.service.PostCreateService;
import band.platform.domain.board.service.PostDeleteService;
import band.platform.domain.board.service.PostQueryService;
import band.platform.domain.board.service.PostUpdateService;
import band.platform.global.ApiResult;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.security.jwt.JwtAuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private static final int MAX_PAGE_SIZE = 100;

	private final PostCreateService postCreateService;
	private final PostQueryService postQueryService;
	private final PostUpdateService postUpdateService;
	private final PostDeleteService postDeleteService;

	@GetMapping("/{postId}")
	public ResponseEntity<ApiResult<PostDetailResponse>> getPost(@PathVariable String postId) {
		return ApiResult.ok(postQueryService.getPost(parsePostId(postId))).toResponseEntity();
	}

	@GetMapping
	public ResponseEntity<ApiResult<PostPageResponse>> getPosts(
		@RequestParam String boardType,
		@RequestParam(defaultValue = "0") String page,
		@RequestParam(defaultValue = "20") String size
	) {
		int parsedPage = parseQueryNumber(page);
		int parsedSize = parseQueryNumber(size);
		validatePageRequest(parsedPage, parsedSize);
		BoardType parsedBoardType = parseBoardType(boardType);
		return ApiResult.ok(postQueryService.getPosts(parsedBoardType, PageRequest.of(parsedPage, parsedSize))).toResponseEntity();
	}

	@PostMapping
	public ResponseEntity<ApiResult<PostCreateResponse>> create(
		@AuthenticationPrincipal JwtAuthenticationPrincipal principal,
		@Valid @RequestBody PostCreateRequest request
	) {
		return ApiResult.created(postCreateService.create(principal.userId(), request)).toResponseEntity();
	}

	@PatchMapping("/{postId}")
	public ResponseEntity<ApiResult<PostUpdateResponse>> update(
		@AuthenticationPrincipal JwtAuthenticationPrincipal principal,
		@PathVariable String postId,
		@Valid @RequestBody PostUpdateRequest request
	) {
		return ApiResult.ok(postUpdateService.update(principal.userId(), parsePostId(postId), request)).toResponseEntity();
	}

	@DeleteMapping("/{postId}")
	public ResponseEntity<ApiResult<Void>> delete(
		@AuthenticationPrincipal JwtAuthenticationPrincipal principal,
		@PathVariable String postId
	) {
		postDeleteService.delete(principal.userId(), parsePostId(postId));
		return ApiResult.ok().toResponseEntity();
	}

	private BoardType parseBoardType(String boardType) {
		try {
			return BoardType.valueOf(boardType);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
	}

	private int parseQueryNumber(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
	}

	private Long parsePostId(String postId) {
		try {
			return Long.parseLong(postId);
		} catch (NumberFormatException exception) {
			throw new BusinessException(ErrorCode.COMMON_INVALID_INPUT);
		}
	}
}
