package band.platform.global;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResultTest {

	@Test
	@DisplayName("성공 응답에 HTTP 상태와 데이터를 담는다")
	void ok() {
		ApiResult<String> result = ApiResult.ok("data");

		assertThat(result.status()).isEqualTo(200);
		assertThat(result.message()).isEqualTo("요청이 성공했습니다.");
		assertThat(result.data()).isEqualTo("data");
	}

	@Test
	@DisplayName("생성 응답을 ResponseEntity로 변환한다")
	void createdToResponseEntity() {
		ResponseEntity<ApiResult<Long>> response = ApiResult.created(1L).toResponseEntity();

		assertThat(response.getStatusCode().value()).isEqualTo(201);
		assertThat(response.getBody().status()).isEqualTo(201);
		assertThat(response.getBody().message()).isEqualTo("리소스가 생성되었습니다.");
		assertThat(response.getBody().data()).isEqualTo(1L);
	}

}
