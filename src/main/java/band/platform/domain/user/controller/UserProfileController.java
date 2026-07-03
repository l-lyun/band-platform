package band.platform.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import band.platform.domain.user.dto.UserProfileResponse;
import band.platform.domain.user.dto.UserProfileUpdateRequest;
import band.platform.domain.user.service.UserProfileService;
import band.platform.global.ApiResult;
import band.platform.global.security.jwt.JwtAuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me/profile")
@RequiredArgsConstructor
public class UserProfileController {

	private final UserProfileService userProfileService;

	@GetMapping
	public ResponseEntity<ApiResult<UserProfileResponse>> getMyProfile(
		@AuthenticationPrincipal JwtAuthenticationPrincipal principal
	) {
		return ApiResult.ok(userProfileService.getMyProfile(principal.userId())).toResponseEntity();
	}

	@PutMapping
	public ResponseEntity<ApiResult<UserProfileResponse>> updateMyProfile(
		@AuthenticationPrincipal JwtAuthenticationPrincipal principal,
		@Valid @RequestBody UserProfileUpdateRequest request
	) {
		return ApiResult.ok(userProfileService.updateMyProfile(principal.userId(), request)).toResponseEntity();
	}
}
