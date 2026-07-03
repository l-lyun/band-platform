package band.platform.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import band.platform.domain.user.dto.UserInterestResponse;
import band.platform.domain.user.dto.UserInterestUpdateRequest;
import band.platform.domain.user.service.UserInterestService;
import band.platform.global.ApiResult;
import band.platform.global.security.jwt.JwtAuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me/interests")
@RequiredArgsConstructor
public class UserInterestController {

	private final UserInterestService userInterestService;

	@GetMapping
	public ResponseEntity<ApiResult<UserInterestResponse>> getMyInterests(
		@AuthenticationPrincipal JwtAuthenticationPrincipal principal
	) {
		return ApiResult.ok(userInterestService.getMyInterests(principal.userId())).toResponseEntity();
	}

	@PutMapping
	public ResponseEntity<ApiResult<UserInterestResponse>> updateMyInterests(
		@AuthenticationPrincipal JwtAuthenticationPrincipal principal,
		@Valid @RequestBody UserInterestUpdateRequest request
	) {
		return ApiResult.ok(userInterestService.updateMyInterests(principal.userId(), request)).toResponseEntity();
	}
}
