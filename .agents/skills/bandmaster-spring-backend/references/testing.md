# Testing Reference

Use this when adding or changing backend behavior.

## Test Selection

- Domain rules: unit tests.
- Application service orchestration: unit or slice tests with fakes.
- Controller validation and error response: MVC/controller tests.
- JPA queries and auth persistence: integration tests.
- Time-dependent auth behavior: fixed `Clock`.

## Test Readability

- Every test should make its intent clear in Korean with `@DisplayName`.
- The display name should explain the behavior being verified, not just repeat the method name.
- Prefer names shaped like "상황을 결과로 변환한다" or "조건이면 결과를 반환한다".
- Keep test method names concise and stable; use the Korean `@DisplayName` as the reader-facing explanation.
- Avoid vague display names like "성공 테스트", "실패 테스트", "테스트한다".

Examples:

```java
@Test
@DisplayName("중복 이메일이면 U02 에러를 반환한다")
void duplicateEmail() {
}

@Test
@DisplayName("인증되지 않은 요청을 A01 공통 에러 응답으로 변환한다")
void unauthenticatedRequest() {
}
```

## Test Design

- Tests should be easy to read before they are clever.
- Prefer simple setup, explicit inputs, and direct assertions.
- Extract reusable test fixtures only when the same setup is repeated enough to hide the behavior under test.
- Do not make helpers so generic that the test reader must jump across files to understand one case.
- Use builders, factory methods, or fixtures when they reduce noise and keep the scenario obvious.
- If a clean and reusable test is hard to write because production code is too coupled, too private, or doing too many things, propose a small production-code refactor instead of forcing a complex test.
- When proposing a refactor for testability, explain what makes the current test hard and what boundary would make it simpler.

## Required Auth Cases

For login-related work, cover these cases as soon as the implementation exists:

- signup success
- duplicate login ID
- duplicate email
- login success
- wrong password
- five consecutive failures locks account
- locked account cannot login
- lock expires after 24 hours
- password reset code invalid
- password reset code expired

## Verification

Run the narrowest useful task.

- Shared backend behavior: `./gradlew test`
- Single test class: `./gradlew test --tests 'fully.qualified.TestClass'`

If dependency downloads fail because of restricted network access, retry with approval when the test is required for confidence.
