# API Errors Reference

Use this when implementing common response, error code, validation, and exception handling.

## Response Shape

Every API error should use this core shape:

```json
{
  "status": 400,
  "code": "E01",
  "message": "요청 값이 올바르지 않습니다."
}
```

Fields:

- `status`: HTTP status code number.
- `code`: BandMaster project error code.
- `message`: Korean user-facing message.

Do not expose stack traces, SQL errors, class names, provider tokens, or internal exception messages.

## Initial Error Codes

Prefix rule:

- `E**`: global/common errors
- `A**`: auth errors
- `U**`: user/member errors

| Code | HTTP | Message |
| --- | ---: | --- |
| `E01` | 400 | 요청 값이 올바르지 않습니다. |
| `E02` | 404 | 요청한 리소스를 찾을 수 없습니다. |
| `E03` | 409 | 이미 존재하는 값입니다. |
| `E04` | 500 | 서버 오류가 발생했습니다. |
| `A01` | 401 | 로그인이 필요합니다. |
| `A02` | 403 | 접근 권한이 없습니다. |
| `A03` | 401 | 아이디 또는 비밀번호가 올바르지 않습니다. |
| `A04` | 423 | 로그인 실패 횟수가 초과되어 계정이 잠겼습니다. |
| `A05` | 401 | 인증 토큰이 만료되었습니다. |
| `A06` | 401 | 인증 토큰이 올바르지 않습니다. |
| `A07` | 400 | 인증 코드가 올바르지 않습니다. |
| `A08` | 400 | 인증 코드가 만료되었습니다. |
| `U01` | 409 | 이미 사용 중인 아이디입니다. |
| `U02` | 409 | 이미 사용 중인 이메일입니다. |

## Implementation Pattern

1. Create an `ErrorCode` enum with `code`, `HttpStatus`, and default message.
2. Create a `BusinessException` that carries `ErrorCode`.
3. Add a global exception handler.
4. Convert validation failures to `E01`.
5. Convert authentication failures to `A01`, `A03`, `A05`, or `A06` as appropriate.
6. Convert authorization failures to `A02`.

Validation field details are optional. Add them only when the frontend needs field-level rendering.
