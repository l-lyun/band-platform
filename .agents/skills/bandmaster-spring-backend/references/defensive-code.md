# Defensive Code Reference

과도한 방어 코드를 줄이고 검증, 예외, 외부 연동 실패 책임을 한곳에 모으기 위한 기준이다.

핵심 선호:

- 외부 입력 검증은 DTO, 비즈니스 규칙은 Service/Domain, 외부 연동 실패는 Adapter/Client.
- DTO Bean Validation을 제외한 비즈니스/인증/인가/도메인 정책 예외는 Service/Domain 중심으로 판단하고 `BusinessException`/`ErrorCode`로 표현한다.
- 방어 코드가 비즈니스 로직보다 커지면 안 된다. 계층 간 검증을 중복하지 않는다.

## 구현 전 책임 분리 워크플로우

방어 코드가 많아질 수 있는 작업은 바로 코드를 쓰지 말고 먼저 책임을 계층별로 나눈다.

1. 구현 전 Controller, Service, Domain, Infrastructure 책임을 표로 정리한다. 각 계층이 검증해야 할 값과 중복 방어 코드가 생길 수 있는 지점을 함께 적고, 아직 코드는 작성하지 않는다.
2. 사용자가 승인하면 그 책임 분리에 맞춰 구현한다. 앞 계층에서 보장된 null/blank/format 검사를 Repository나 Adapter 내부에서 반복하지 않는다.
3. 구현 후 중복 방어 코드 후보를 다시 찾고, 각 후보가 해당 계층에서 반드시 필요한 보호인지 앞 계층에서 이미 보장된 값인지 판정한다.

| 계층 | 검증/보호 책임 | 중복 방어 코드 위험 지점 |
| --- | --- | --- |
| Controller | HTTP 입력 바인딩, `@Valid` 적용, path/query/header/cookie 존재 여부처럼 HTTP 경계에서만 알 수 있는 값 확인, 응답 status/header/cookie 조립 | DTO Bean Validation과 같은 null/blank/format 검사를 Service에 다시 넘기기 전 반복하거나, Service가 소유한 인증/인가/도메인 정책을 `try/catch`로 중복 처리하는 경우 |
| Service | 유스케이스 흐름, 조회 결과 없음, 인증/인가, 소유권, 중복 데이터, 외부 시스템 호출 실패 정책, Domain 메서드 호출 순서 판단 | DTO에서 보장한 단순 입력 검사를 반복하거나, Domain 불변식을 Service와 Domain 양쪽에 같은 조건문으로 배치하는 경우 |
| Domain | 엔티티 상태 불변식, 상태 전이 가능 여부, 도메인 규칙 위반 방지 | Service에서 이미 판단한 use-case 권한 검사를 Domain 내부에 다시 넣거나, 단순 HTTP 입력 검증을 Domain 생성자/메서드에서 반복하는 경우 |
| Infrastructure | Repository, Adapter, Client, provider 같은 외부 경계의 실패 변환, null body/4xx/5xx/timeout/parsing/config 오류 보호 | Service/DTO가 보장한 필드의 null/blank 검사를 Repository 쿼리 메서드 앞에서 반복하거나, 외부 provider 실패를 Controller/Service와 Adapter에서 동시에 래핑하는 경우 |

구현 후 감사 규칙:

- `null`, `blank`, `isEmpty`, `isPresent`, `try/catch`, `Optional`, `BusinessException`, `ErrorCode` 주변을 훑어 중복 후보를 찾는다.
- 후보마다 "이 계층이 직접 보호해야 하는 경계/불변식인가?"와 "앞 계층에서 이미 보장했는가?"를 한 번씩 판정한다.
- 앞 계층 보장이 명확하면 제거하거나 더 앞 계층으로 책임을 이동한다.
- 외부 입력, 도메인 불변식, 외부 provider 응답처럼 현재 계층에서만 알 수 있는 위험은 유지한다.

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

## Controller와 Service 책임 경계

Controller는 HTTP 입력을 바인딩하고 HTTP 응답을 조립하는 얇은 경계로 둔다.

- Request DTO, path/query/header/cookie 같은 HTTP 입력을 메서드 인자로 받는다.
- 성공 응답의 status, header, cookie, body를 조립한다.
- 쿠키 생성, 삭제처럼 응답 표현에 필요한 HTTP 작업은 Controller에서 수행할 수 있다.
- use-case 흐름을 소유한 Service가 판단해야 하는 인증/인가/도메인 실패 정책은 Controller에 중복하지 않는다.
- Validation 실패와 `BusinessException` 실패 응답은 Controller별 `try/catch`가 아니라 기존 `GlobalExceptionHandler`/`ErrorResponse` 흐름에 맡긴다.

토큰/쿠키 인증 흐름에서는 특히 책임을 나눈다.

- Controller는 재발급 성공 시 새 refresh token cookie를 내려주거나, 로그아웃 응답에서 cookie를 만료할 수 있다.
- refresh token cookie가 없는 경우, 값이 비어 있는 경우, 토큰이 만료/위조/회전 완료된 경우의 정책은 `UserTokenService` 또는 해당 인증 Service에 둔다.
- Controller 테스트는 Service가 반환한 결과를 HTTP 응답으로 바꾸는지, Service 예외를 공통 에러 응답으로 전달하는지를 검증한다.
- Service 테스트는 쿠키 추출, 토큰 파싱, 저장소 회전, 실패 시 `BusinessException(ErrorCode.AUTH_TOKEN_INVALID)` 같은 인증 정책을 검증한다.
- 같은 refresh token 유효성 검사를 Controller와 Service에 동시에 두지 않는다.

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

변환은 Controller가 아니라 Adapter/Client에서 수행하고, 결과는 기존 `BusinessException`, `ErrorCode`, `ErrorResponse` 흐름을 따른다. 새 `ErrorCode`는 실제로 구분이 필요할 때만 최소로 추가한다.

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
