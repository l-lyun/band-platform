# Auth Flow Reference

Use this when implementing login, signup, lockout, token auth, ID find, password reset, or social login.

## Priority

Implement in this order:

1. Email signup
2. ID/password login
3. 5-failure 24-hour lock
4. Token auth
5. ID find
6. Password reset
7. Social login extension

## Email Signup

Required fields from the product plan:

- login ID
- password
- email
- phone
- privacy agreement

Rules:

- Hash passwords through Spring Security `PasswordEncoder`.
- Enforce unique login ID and email at application and database level.
- Return `U01` for duplicate login ID.
- Return `U02` for duplicate email.
- Never persist plaintext password.

## Login

Flow:

1. Find member by login ID.
2. Check account state.
3. Verify password.
4. On failure, increase consecutive failure count.
5. On five consecutive failures, set `lockedUntil = now + 24h`.
6. On lock, return `A04`.
7. On success, clear failure count and lock state.
8. Issue tokens after all checks pass.

Use a `Clock` bean for time-dependent logic.

## Token Auth

Prefer a split model:

- access token: short-lived API authentication
- refresh token: stored server-side or invalidatable

Rules:

- Return `A05` for expired token.
- Return `A06` for invalid token.
- Invalidate refresh tokens on logout.
- Invalidate refresh tokens after password reset.
- Do not log token values.

## ID Find

The product flow says email input can reveal the ID without separate verification. Flag this as an account-enumeration risk before implementing. If the user still wants it, implement exactly but keep the behavior isolated so it can be changed later.

## Password Reset

Rules:

- Generate reset code/token with secure randomness.
- Store only a hash of the code/token.
- Make it single-use.
- Make it short-lived.
- Limit retry attempts.
- Do not change password until code/token is verified.

## Social Login

Do not mix provider-specific code into the email login service.

Use:

- provider enum: `KAKAO`, `NAVER`, `APPLE`
- social account entity/table with provider + provider subject
- adapter interface for provider user info
- infrastructure implementation per provider

If provider account has no linked member, return a clear signup-needed state instead of creating a partial member silently.
