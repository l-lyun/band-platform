package band.platform.domain.user.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import band.platform.domain.user.repository.SocialOAuthStateRepository;
import band.platform.domain.user.entity.SocialProvider;

class SocialOAuthStateServiceTest {

	@Mock
	private SocialOAuthStateRepository socialOAuthStateRepository;

	private SocialOAuthStateService socialOAuthStateService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		SocialOAuthProperties properties = new SocialOAuthProperties();
		properties.setStateTtl(Duration.ofMinutes(10));
		socialOAuthStateService = new SocialOAuthStateService(socialOAuthStateRepository, properties);
	}

	@Test
	@DisplayName("제공자 문맥과 함께 state와 nonce를 생성해 TTL과 함께 저장한다")
	void issue() {
		ArgumentCaptor<SocialOAuthState> captor = ArgumentCaptor.forClass(SocialOAuthState.class);

		SocialOAuthState oauthState = socialOAuthStateService.issue(SocialProvider.NAVER);

		verify(socialOAuthStateRepository).save(eq(SocialProvider.NAVER), captor.capture(), eq(Duration.ofMinutes(10)));
		assertThat(oauthState.state()).isNotBlank();
		assertThat(oauthState.nonce()).isNotBlank();
		assertThat(captor.getValue()).isEqualTo(oauthState);
	}

	@Test
	@DisplayName("state 소비는 제공자 문맥과 함께 저장소에 위임한다")
	void consume() {
		SocialOAuthState oauthState = new SocialOAuthState("state", "nonce");
		when(socialOAuthStateRepository.consume(any(), any())).thenReturn(Optional.of(oauthState));

		assertThat(socialOAuthStateService.consume(SocialProvider.NAVER, "state")).contains(oauthState);
		verify(socialOAuthStateRepository).consume(SocialProvider.NAVER, "state");
	}

}
