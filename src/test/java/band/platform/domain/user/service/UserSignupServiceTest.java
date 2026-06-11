package band.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.UserSignupRequest;
import band.platform.domain.user.dto.UserSignupResponse;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.SocialProvider;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@SpringBootTest
@Transactional
class UserSignupServiceTest {

	private static final String RAW_PASSWORD = "password123!";

	@Autowired
	private UserSignupService userSignupService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("회원가입하면 비밀번호를 암호화해서 로컬 회원을 저장한다")
	void signup() {
		UserSignupResponse response = userSignupService.signup(signupRequest("bandmaster", "bandmaster@example.com"));

		User user = userRepository.findByLoginId(response.loginId()).orElseThrow();

		assertThat(response.id()).isNotNull();
		assertThat(response.loginId()).isEqualTo("bandmaster");
		assertThat(response.email()).isEqualTo("bandmaster@example.com");
		assertThat(user.getPassword()).isNotEqualTo(RAW_PASSWORD);
		assertThat(passwordEncoder.matches(RAW_PASSWORD, user.getPassword())).isTrue();
		assertThat(user.getSocialProvider()).isEqualTo(SocialProvider.LOCAL);
	}

	@Test
	@DisplayName("이미 사용 중인 로그인 아이디이면 U01 예외를 던진다")
	void duplicateLoginId() {
		userSignupService.signup(signupRequest("bandmaster", "bandmaster@example.com"));

		assertThatThrownBy(() -> userSignupService.signup(signupRequest("bandmaster", "other@example.com")))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_LOGIN_ID_DUPLICATED)
			);
	}

	@Test
	@DisplayName("이미 사용 중인 이메일이면 U02 예외를 던진다")
	void duplicateEmail() {
		userSignupService.signup(signupRequest("bandmaster", "bandmaster@example.com"));

		assertThatThrownBy(() -> userSignupService.signup(signupRequest("other", "bandmaster@example.com")))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_EMAIL_DUPLICATED)
			);
	}

	@Test
	@DisplayName("개인정보 필수 동의가 아니면 E01 예외를 던진다")
	void privacyPolicyNotAgreed() {
		UserSignupRequest request = new UserSignupRequest(
			"김김김",
			"bandmaster",
			RAW_PASSWORD,
			"bandmaster@example.com",
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			"img",
			false,
			true
		);

		assertThatThrownBy(() -> userSignupService.signup(request))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT)
			);
	}

	private UserSignupRequest signupRequest(String loginId, String email) {
		return new UserSignupRequest(
			"김김김",
			loginId,
			RAW_PASSWORD,
			email,
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			"img",
			true,
			true
		);
	}

}
