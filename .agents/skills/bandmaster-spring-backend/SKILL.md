---
name: bandmaster-spring-backend
description: Implement, review, or plan BandMaster Java/Spring Boot backend work. Use for auth/login, member/profile APIs, common error handling, validation, JPA persistence, security, tests, GitHub issue drafting, and learning-oriented PR documentation in this repository.
compatibility: Codex repo skill for a Java 21, Gradle, Spring Boot backend.
---

# BandMaster Spring Backend

This skill turns BandMaster backend requests into small, verified Spring Boot changes or issue drafts.

## First Moves

1. Inspect the current repo before deciding:
   - `build.gradle`
   - `src/main/resources/application.yml`
   - existing `src/main/java` package layout
   - `git status --short`
2. Identify the task type:
   - API/error foundation
   - auth/login
   - member/profile domain
   - persistence/migration
   - test/verification
   - GitHub issue drafting
   - PR/Notion learning documentation
3. Load only the matching reference file below.
4. Make the smallest change that completes the requested slice.
5. Verify with the narrowest useful Gradle task. Use `./gradlew test` when the change touches shared behavior.

## OMO Planning Gate

- Use OMO as a planning gate, not a replacement for this short router or its focused references.
- Broad, cross-cutting, ambiguous, multi-thread, or high-risk backend work should go through `.omo/plans/<slug>.md` before implementation.
- If a plan already exists, read it before editing and follow its scoped task, evidence, and cleanup requirements.
- For narrow single-slice work, keep using the reference router below and the smallest useful verification.

## Reference Router

- For common responses, error codes, validation, and exception handling, read [references/api-errors.md](references/api-errors.md).
- For email login, account lock, ID/PW recovery, token auth, or social login extension points, read [references/auth-flow.md](references/auth-flow.md).
- For package layout, service boundaries, JPA, migrations, and dependency choices, read [references/spring-backend.md](references/spring-backend.md).
- For tests and local verification, read [references/testing.md](references/testing.md).
- For GitHub issue drafts, read [references/github-issues.md](references/github-issues.md).
- For PR/Notion learning documentation, read [references/pr-learning-docs.md](references/pr-learning-docs.md).

## Default Workflow

When implementing code:

1. State the slice in one sentence.
2. Read the relevant reference.
3. Locate existing patterns with `rg`/file reads.
4. Edit only files needed for the slice.
5. Add or update focused tests when behavior changes.
6. Run the relevant verification.
7. Report changed files and verification result.

When drafting issues:

1. Use the existing issue template shape.
2. Keep titles broad and easy to understand.
3. Keep descriptions product-facing.
4. Keep checklists implementation-sized, not overly detailed.
5. Do not add a separate completion-condition section unless the user asks.

When writing PR/Notion docs:

1. Treat the document as the user's learning material, not a changelog.
2. Prefer concise bullet points, numbered flows, and small code snippets.
3. Explain core external tools and framework extension points used by the PR.
4. Connect each concept to the actual implementation.
5. Keep AI handoff notes at the bottom.

## Guardrails

- Do not turn this project into a framework showcase. Add dependencies only when they remove real work or match Spring defaults.
- Prefer established Spring Boot patterns over custom infrastructure.
- Keep auth security conservative; flag product requirements that expose accounts or secrets.
- Never log passwords, reset codes, refresh tokens, OAuth tokens, or provider secrets.
- If a requested behavior conflicts with the references, follow the user request but call out the tradeoff briefly.
