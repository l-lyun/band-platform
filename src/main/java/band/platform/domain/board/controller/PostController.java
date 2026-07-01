package band.platform.domain.board.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import band.platform.domain.board.dto.PostCreateRequest;
import band.platform.domain.board.dto.PostCreateResponse;
import band.platform.domain.board.service.PostCreateService;
import band.platform.global.ApiResult;
import band.platform.global.security.jwt.JwtAuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostCreateService postCreateService;

	@PostMapping
	public ResponseEntity<ApiResult<PostCreateResponse>> create(
		@AuthenticationPrincipal JwtAuthenticationPrincipal principal,
		@Valid @RequestBody PostCreateRequest request
	) {
		return ApiResult.created(postCreateService.create(principal.userId(), request)).toResponseEntity();
	}
}
