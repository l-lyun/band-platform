package band.platform.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import band.platform.domain.user.dto.UserSignupRequest;
import band.platform.domain.user.dto.UserSignupResponse;
import band.platform.domain.user.service.UserSignupService;
import band.platform.global.ApiResult;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserSignupService userSignupService;

	public UserController(UserSignupService userSignupService) {
		this.userSignupService = userSignupService;
	}

	@PostMapping("/sign-up")
	public ResponseEntity<ApiResult<UserSignupResponse>> signup(
		@Valid @RequestBody UserSignupRequest request
	) {
		return ApiResult.created(userSignupService.signup(request)).toResponseEntity();
	}

}
