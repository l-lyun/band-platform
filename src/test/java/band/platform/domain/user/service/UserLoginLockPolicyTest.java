package band.platform.domain.user.service;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import band.platform.domain.user.repository.UserLoginLockRepository;
import band.platform.domain.user.service.UserLoginLockPolicy.LoginFailureResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLoginLockPolicyTest {

	private static final String LOGIN_ID = "bandmaster";

	@Mock
	private UserLoginLockRepository userLoginLockRepository;

	@InjectMocks
	private UserLoginLockPolicy userLoginLockPolicy;

	@Test
	@DisplayName("로그인 실패 횟수가 5회 미만이면 잠금 상태가 아니라고 반환한다")
	void recordFirstFailure() {
		givenFailureCount(1);

		LoginFailureResult result = userLoginLockPolicy.recordFailure(LOGIN_ID);

		assertThat(result.failureCount()).isEqualTo(1);
		assertThat(result.locked()).isFalse();
		verifyIncreaseFailureCount();
	}

	@Test
	@DisplayName("로그인 실패가 5회가 되면 잠금 상태를 반환한다")
	void recordFifthFailure() {
		givenFailureCount(5);

		LoginFailureResult result = userLoginLockPolicy.recordFailure(LOGIN_ID);

		assertThat(result.failureCount()).isEqualTo(UserLoginLockPolicy.MAX_LOGIN_FAILURE_COUNT);
		assertThat(result.locked()).isTrue();
		verifyIncreaseFailureCount();
	}

	@Test
	@DisplayName("잠금 키가 있으면 잠긴 상태로 판단한다")
	void isLocked() {
		when(userLoginLockRepository.existsLock(LOGIN_ID)).thenReturn(true);

		assertThat(userLoginLockPolicy.isLocked(LOGIN_ID)).isTrue();
	}

	@Test
	@DisplayName("로그인 성공 후 실패 키와 잠금 키를 함께 삭제한다")
	void clear() {
		userLoginLockPolicy.clear(LOGIN_ID);

		verify(userLoginLockRepository).deleteFailureCountAndLock(LOGIN_ID);
	}

	private void givenFailureCount(int failureCount) {
		when(userLoginLockRepository.increaseFailureCountAndLockIfThresholdReached(
			eq(LOGIN_ID),
			eq(UserLoginLockPolicy.MAX_LOGIN_FAILURE_COUNT),
			eq(Duration.ofHours(24)),
			eq(Duration.ofHours(24))
		)).thenReturn(failureCount);
	}

	private void verifyIncreaseFailureCount() {
		verify(userLoginLockRepository).increaseFailureCountAndLockIfThresholdReached(
			LOGIN_ID,
			UserLoginLockPolicy.MAX_LOGIN_FAILURE_COUNT,
			Duration.ofHours(24),
			Duration.ofHours(24)
		);
	}

}
