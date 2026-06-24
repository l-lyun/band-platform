package band.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import band.platform.domain.user.dto.FindLoginIdRequest;
import band.platform.domain.user.dto.FindLoginIdResponse;
import band.platform.domain.user.entity.Gender;
import band.platform.domain.user.entity.User;
import band.platform.domain.user.repository.UserRepository;
import band.platform.global.error.BusinessException;
import band.platform.global.error.ErrorCode;

@SpringBootTest
@Transactional
class UserFindLoginIdServiceTest {

	private static final String RAW_PASSWORD = "password123!";

	@Autowired
	private UserFindLoginIdService userFindLoginIdService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("가입된 이메일이면 로그인 아이디를 반환한다")
	void findLoginId() {
		saveUser("bandmaster", "bandmaster@example.com");

		FindLoginIdResponse response = userFindLoginIdService.findLoginId(
			new FindLoginIdRequest("bandmaster@example.com")
		);

		assertThat(response.loginId()).isEqualTo("bandmaster");
	}

	@Test
	@DisplayName("가입되지 않은 이메일이면 E02 예외를 던진다")
	void unknownEmail() {
		assertThatThrownBy(() -> userFindLoginIdService.findLoginId(
			new FindLoginIdRequest("unknown@example.com")
		))
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND)
			);
	}

	private User saveUser(String loginId, String email) {
		return userRepository.save(User.createLocalUser(
			"김김김",
			loginId,
			passwordEncoder.encode(RAW_PASSWORD),
			email,
			"자기소개",
			false,
			"01012345678",
			Gender.MALE,
			"img",
			true,
			true
		));
	}

}
