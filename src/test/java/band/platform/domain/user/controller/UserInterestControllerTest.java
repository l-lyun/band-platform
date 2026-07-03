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

import band.platform.domain.user.dto.UserInterestResponse;
import band.platform.domain.user.service.UserInterestService;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;
import band.platform.global.error.GlobalExceptionHandler;
import band.platform.global.security.jwt.JwtAuthenticationPrincipal;

class UserInterestControllerTest {

	private MockMvc mockMvc;
	private UserInterestService userInterestService;

	@BeforeEach
	void setUp() {
		userInterestService = mock(UserInterestService.class);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new UserInterestController(userInterestService))
			.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("인증된 사용자가 내 관심사를 조회하면 관심사 목록을 반환한다")
	void getMyInterests() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		when(userInterestService.getMyInterests(1L))
			.thenReturn(interestResponse());

		mockMvc.perform(get("/api/users/me/interests"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.favoriteArtists[0]").value("넬"))
			.andExpect(jsonPath("$.data.favoriteEquipments[0]").value("Fender Jazz Bass"))
			.andExpect(jsonPath("$.data.favoriteVenues[0]").value("올림픽공원"))
			.andExpect(jsonPath("$.data.visitedConcerts[0]").value("서울재즈페스티벌"));

		verify(userInterestService).getMyInterests(1L);
	}

	@Test
	@DisplayName("인증된 사용자가 내 관심사를 수정하면 수정된 관심사 목록을 반환한다")
	void updateMyInterests() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");
		when(userInterestService.updateMyInterests(eq(1L), any()))
			.thenReturn(interestResponse());

		mockMvc.perform(put("/api/users/me/interests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(interestUpdateRequest()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
			.andExpect(jsonPath("$.data.favoriteArtists[0]").value("넬"))
			.andExpect(jsonPath("$.data.favoriteEquipments[0]").value("Fender Jazz Bass"))
			.andExpect(jsonPath("$.data.favoriteVenues[0]").value("올림픽공원"))
			.andExpect(jsonPath("$.data.visitedConcerts[0]").value("서울재즈페스티벌"));

		verify(userInterestService).updateMyInterests(eq(1L), any());
	}

	@Test
	@DisplayName("관심사 카테고리 목록이 50개를 초과하면 E01 에러 응답을 반환한다")
	void categoryTooManyItems() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(put("/api/users/me/interests")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"favoriteArtists": %s,
						"favoriteEquipments": [],
						"favoriteVenues": [],
						"visitedConcerts": []
					}
					""".formatted(jsonArray(51))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("관심사 항목이 100자를 초과하면 E01 에러 응답을 반환한다")
	void itemTooLong() throws Exception {
		setAuthenticatedPrincipal(1L, "bandmaster");

		mockMvc.perform(put("/api/users/me/interests")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"favoriteArtists": ["%s"],
						"favoriteEquipments": [],
						"favoriteVenues": [],
						"visitedConcerts": []
					}
					""".formatted("a".repeat(101))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("E01"))
			.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
	}

	@Test
	@DisplayName("관심사를 조회할 ACTIVE 회원이 없으면 E02 에러 응답을 반환한다")
	void userNotFound() throws Exception {
		setAuthenticatedPrincipal(999L, "unknown");
		when(userInterestService.getMyInterests(999L))
			.thenThrow(new BusinessException(ErrorCode.COMMON_NOT_FOUND));

		mockMvc.perform(get("/api/users/me/interests"))
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

	private UserInterestResponse interestResponse() {
		return new UserInterestResponse(
			List.of("넬"),
			List.of("Fender Jazz Bass"),
			List.of("올림픽공원"),
			List.of("서울재즈페스티벌")
		);
	}

	private String interestUpdateRequest() {
		return """
			{
				"favoriteArtists": [" 넬 ", "쏜애플"],
				"favoriteEquipments": ["Fender Jazz Bass"],
				"favoriteVenues": ["올림픽공원"],
				"visitedConcerts": ["서울재즈페스티벌"]
			}
			""";
	}

	private String jsonArray(int size) {
		return "[" + String.join(",", java.util.Collections.nCopies(size, "\"item\"")) + "]";
	}
}
