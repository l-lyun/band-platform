# Defensive Code Reference

과도한 방어 코드를 줄이고 검증, 예외, 외부 연동 실패 책임을 한곳에 모으기 위한 기준이다.

핵심 선호:

- 외부 입력 검증은 DTO, 비즈니스 규칙은 Service/Domain, 외부 연동 실패는 Adapter/Client.
- Bean Validation 제외, 비즈니스 예외는 Service/Domain 중심.
- 방어 코드가 비즈니스 로직보다 커지면 안 된다. 계층 간 검증을 중복하지 않는다.

## Request DTO 검증

간단한 입력값 검증은 Request DTO에서 Bean Validation으로 처리한다.

- `@NotNull`, `@NotBlank`, `@Size`, `@Email`을 우선 사용한다.
- 숫자 범위, 문자열 길이, 형식 검증은 `min/max/format` 성격의 Bean Validation으로 둔다.
- Controller는 DTO 파라미터에 `@Valid`를 적용한다.
- Service는 이미 검증된 DTO 필드에 대해 null, blank, 길이, 이메일 형식 같은 검사를 반복하지 않는다.

## Validation 예외 응답

Bean Validation 실패는 `GlobalExceptionHandler`가 처리한다.

- `MethodArgumentNotValidException`은 기존 `ErrorResponse` 형식으로 변환한다.
- 공통 `ErrorCode(COMMON_INVALID_INPUT)`를 사용하고, Validation 응답에는 `fieldErrors(field, message)`를 포함한다.
- 단, 로그인 실패처럼 보안에 민감한 흐름에서는 상세 필드 메시지를 노출하지 않는다.
- 내부 예외 메시지, 클래스명, SQL 오류, 외부 provider 응답 원문은 응답에 노출하지 않는다.

## Service/Domain 예외 책임

Service는 비즈니스 실패에만 `BusinessException`을 던진다.

- repository 조회 결과 없음
- 인증된 사용자의 권한/소유권 없음
- Domain 상태 위반
- 중복 데이터
- 정책 위반
- Service가 소유한 경계에서 발생한 외부 시스템 실패

Domain 또는 Service에서 보호해야 하는 불변식 예시는 다음과 같다.

- 탈퇴한 사용자는 정보를 수정할 수 없다.
- 닉네임은 중복될 수 없다.
- 만료된 토큰은 사용할 수 없다.
- 이미 결제된 좌석은 다시 예약할 수 없다.

## 피해야 할 방어 코드

다음 코드는 기본적으로 추가하지 않는다.

- DI로 주입되는 의존성에 대한 null 체크
- 내부에서 직접 생성한 객체에 대한 null 체크
- 이미 Bean Validation을 통과한 DTO 필드 재검증
- 넓은 `catch (RuntimeException)` 후 `BusinessException`으로 일괄 래핑
- `IllegalArgumentException`, `RuntimeException` 남용
- 의미 없는 safety if
- 같은 검증을 Controller, Service, Domain에 반복 배치

## 외부 Boundary 예외 변환

외부 provider, API client, adapter 경계에서는 실패를 프로젝트 예외 체계로 변환한다.

- null body
- 4xx/5xx 응답
- timeout/connect failure
- JSON parsing failure
- 외부 설정 오류

변환 대상은 기존 `BusinessException`, `ErrorCode`, `ErrorResponse` 흐름을 따른다. 새 `ErrorCode`는 실제로 구분이 필요할 때만 최소로 추가한다.

## 구현 원칙

- 기존 `ErrorResponse`, `ErrorCode`, `BusinessException`을 우선 따른다.
- 새 예외 타입이나 새 에러 코드는 필요한 경우에만 추가한다.
- 검증 위치가 겹치면 DTO Bean Validation, Service/Domain 비즈니스 규칙, Adapter/Client 외부 실패 변환 중 한곳으로 책임을 정리한다.
- 읽기 쉬운 정상 흐름을 먼저 유지하고, 방어 코드는 경계와 불변식에 집중한다.

## Self-review Checklist

- 중복 방어 코드가 들어갔는가?
- Service가 DTO Bean Validation 검사를 반복하는가?
- 비즈니스 실패는 `BusinessException`과 기존 `ErrorCode` 흐름을 따르는가?
- Validation 실패가 `fieldErrors(field, message)`를 담아 기존 `ErrorResponse`로 반환되는가?
- 로그인 등 보안 민감 흐름에서 상세 검증 메시지가 노출되지 않는가?
- 외부 provider/API/client/adapter 경계의 실패 변환이 충분한가?
