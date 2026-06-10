# Testing Reference

Use this when adding or changing backend behavior.

## Test Selection

- Domain rules: unit tests.
- Application service orchestration: unit or slice tests with fakes.
- Controller validation and error response: MVC/controller tests.
- JPA queries and auth persistence: integration tests.
- Time-dependent auth behavior: fixed `Clock`.

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
