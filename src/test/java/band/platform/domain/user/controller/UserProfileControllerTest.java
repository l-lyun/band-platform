package band.platform.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import band.platform.domain.user.dto.UserProfileResponse;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.Position;
import band.platform.domain.user.service.UserProfileService;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.jwt.JwtAuthenticationPrincipal;

class UserProfileControllerTest {

	private MockMvc mockMvc;
	private UserProfileService userProfileService;

	@BeforeEach
	void setUp() {
		userProfileService = mock(UserProfileService.class);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserProfileController(userProfileService))
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("인증된 사용자가 내 프로필을 조회하면 프로필 정보를 반환한다")
	void getMyProfile() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		when(userProfileService.getMyProfile(1L))
			.thenReturn(profileResponse());

		mockMvc.perform(get("/api/users/me/profile"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.name").value("김김김"))
			.andExpect(jsonPath("$.data.position").value("GUITAR"))
			.andExpect(jsonPath("$.data.profileImg").value("img"))
			.andExpect(jsonPath("$.data.gender").value("MALE"))
			.andExpect(jsonPath("$.data.description").value("자기소개"))
			.andExpect(jsonPath("$.data.opened").value(false));

		verify(userProfileService).getMyProfile(1L);
	}

	@Test
	@DisplayName("인증된 사용자가 내 프로필을 수정하면 수정된 프로필 정보를 반환한다")
	void updateMyProfile() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		when(userProfileService.updateMyProfile(eq(1L), any()))
			.thenReturn(new UserProfileResponse(
				1L,
				"새닉네임",
				Position.DRUM,
				"https://image.example.com/profile.png",
				Gender.FEMALE,
				"드럼 연주자입니다.",
				true
			));

		mockMvc.perform(put("/api/users/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileUpdateRequest(
					"새닉네임",
					"DRUM",
					"https://image.example.com/profile.png",
					"FEMALE",
					"드럼 연주자입니다.",
					true
				)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.name").value("새닉네임"))
			.andExpect(jsonPath("$.data.position").value("DRUM"))
			.andExpect(jsonPath("$.data.profileImg").value("https://image.example.com/profile.png"))
			.andExpect(jsonPath("$.data.gender").value("FEMALE"))
			.andExpect(jsonPath("$.data.description").value("드럼 연주자입니다."))
			.andExpect(jsonPath("$.data.opened").value(true));

		verify(userProfileService).updateMyProfile(eq(1L), any());
	}

	@Test
	@DisplayName("프로필 이름이 비어 있으면 E01 에러 응답을 반환한다")
	void blankName() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(put("/api/users/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileUpdateRequest(" ", "GUITAR", "img", "MALE", "자기소개", false)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("프로필 포지션이 null이면 E01 에러 응답을 반환한다")
	void nullPosition() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(put("/api/users/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"name": "김김김",
						"position": null,
						"profileImg": "img",
						"gender": "MALE",
						"description": "자기소개",
						"opened": false
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("프로필 이미지가 500자를 초과하면 E01 에러 응답을 반환한다")
	void profileImgTooLong() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(put("/api/users/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileUpdateRequest("김김김", "GUITAR", "a".repeat(501), "MALE", "자기소개", false)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("프로필 소개가 500자를 초과하면 E01 에러 응답을 반환한다")
	void descriptionTooLong() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(put("/api/users/me/profile")
				.contentType(MediaType.APPLICATION_JSON)
				.content(profileUpdateRequest("김김김", "GUITAR", "img", "MALE", "a".repeat(501), false)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("프로필을 조회할 ACTIVE 회원이 없으면 E02 에러 응답을 반환한다")
	void userNotFound() throws Exception {
		setAuthenticatedPrincipal(999L, "unknown");
		when(userProfileService.getMyProfile(999L))
			.thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));

		mockMvc.perform(get("/api/users/me/profile"))
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

	private UserProfileResponse profileResponse() {
		return new UserProfileResponse(
			1L,
			"김김김",
			Position.GUITAR,
			"img",
			Gender.MALE,
			"자기소개",
			false
		);
	}

	private String profileUpdateRequest(
		String name,
		String position,
		String profileImg,
		String gender,
		String description,
		boolean opened
	) {
		return """
			{
				"name": "%s",
				"position": "%s",
				"profileImg": "%s",
				"gender": "%s",
				"description": "%s",
				"opened": %s
			}
			""".formatted(name, position, profileImg, gender, description, opened);
	}
}
