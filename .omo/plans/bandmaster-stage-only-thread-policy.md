# bandmaster-stage-only-thread-policy - Work Plan

## TL;DR (For humans)
**What you'll get:** BandMaster 구현 스레드들이 앞으로 커밋까지 가지 않고, 검증 후 `git add`까지만 수행해 IntelliJ staged 파일에서 사용자가 직접 확인할 수 있게 하는 운영 계획입니다.

**Why this approach:** 레포 스킬은 검증/보고까지 요구하지만 커밋을 필수로 하지 않습니다. 따라서 각 작업 프롬프트에 `git commit`, `git push`, PR 생성 금지를 명시하고, `git add <명시 파일>` 후 정지하도록 지시하는 방식이 가장 안전합니다.

**What it will NOT do:** 이 계획은 product code를 수정하지 않습니다. 이 계획 스레드는 staging도 하지 않고, 구현 스레드가 add까지만 하도록 지시합니다.

**Effort:** Quick
**Risk:** Medium - staging 범위를 잘못 잡으면 IntelliJ 검토 대상에 unrelated 파일이 섞일 수 있습니다.
**Decisions to sanity-check:** 모든 작업자가 `git add .` 대신 명시 파일만 stage하고, Notion/PR 업데이트는 사용자 승인 후로 미루는 정책을 적용합니다.

Your next move: 아래 스레드별 명령문을 각 구현 스레드에 그대로 보내세요. Full execution detail follows below.

---

> TL;DR (machine): Stage-only thread policy: verify, explicit git add, status report, stop; no commit/push/PR/Notion writes.

## Scope
### Must have
- Adapt repo skill workflow into a local stage-only handoff policy.
- Provide exact worker commands for current PR #36 review-fix and the next two lanes.
- Ensure staged files are reviewable in IntelliJ by avoiding broad staging.
- Keep PR/Notion/commit actions blocked until user approval.

### Must NOT have (guardrails, anti-slop, scope boundaries)
- Do not commit.
- Do not push.
- Do not create or update PRs.
- Do not write Notion pages from worker threads before user staged-file approval.
- Do not run `git add .`; only stage explicit files belonging to the task.

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: worker-specific tests-after or TDD depending on slice; this planning artifact itself has no code tests.
- Evidence: workers must report test command output summary, staged file list from `git diff --cached --name-status`, and unstaged remainder from `git status --short`.

## Execution strategy
### Parallel execution waves
> Target 5-8 todos per wave. Fewer than 3 (except the final) means you under-split.
- Wave 0: Finish/stage current PR #36 review-fix worktree.
- Wave 1: Start #27 Kakao provider client and #34 password reset as separate workers only after base branch is clean.
- Wave 2: User reviews staged files in IntelliJ, then explicitly authorizes commit/push/PR/Notion updates.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| T0 stage-only policy | none | all workers | none |
| T1 PR #36 review-fix staging | existing PR #36 worktree | PR #36 commit/push after user review | T2/T3 only if separate worktrees |
| T2 #27 Kakao staging | PR #36 merge or explicit stacked branch | #29 social login API | T3 |
| T3 #34 password reset staging | PR #35 merged and latest dev base | account recovery PR | T2 |
| T4 user review gate | T1/T2/T3 staged outputs | commit/push/Notion | none |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [ ] T0. Universal stage-only ending for every worker
  What to do / Must NOT do: Every implementation thread must end with staged changes only. Do not commit, push, create/update PR, or write Notion unless the user explicitly approves after IntelliJ staging review. Do not use `git add .`.
  Parallelization: Wave 0 | Blocked by: none | Blocks: all implementation work
  References: `.agents/skills/bandmaster-spring-backend/SKILL.md`, `docs/notion-pr-workflow.md`, user instruction in this planning turn.
  Acceptance criteria: Worker final report includes `git diff --cached --name-status`, `git status --short`, tests run, and a proposed commit message, but no commit hash.
  QA scenarios: happy `git diff --cached --name-status` lists only task files; failure `git status --short` shows unrelated staged files or broad `.worktrees/` staged, worker must unstage unrelated files before stopping.
  Commit: N.

- [ ] T1. PR #36 Naver review-fix worker stage-only command
  What to do / Must NOT do: Use existing worktree `/Users/macbook/Desktop/Projects/band-platform/.worktrees/pr-36-naver-review-fix`. Finish only the Naver review-fix. Stage only `NaverSocialLoginClient.java` and `NaverSocialLoginClientTest.java` if they are the only task files. Do not commit/push/update PR/Notion.
  Parallelization: Wave 0 | Blocked by: existing PR #36 | Blocks: user review before PR #36 update
  References: PR #36, issue #26, files currently modified in review-fix worktree.
  Acceptance criteria: relevant Naver tests pass; `git diff --cached --name-status` shows only review-fix files.
  QA scenarios: happy `./gradlew test --tests '*NaverSocialLoginClientTest' --rerun-tasks`; failure regression in provider error mapping fails the same command and nothing is staged.
  Commit: N; proposed later `fix(auth): refine naver social client error handling`.

- [ ] T2. #27 Kakao social client worker stage-only command
  What to do / Must NOT do: Create/use a separate clean worktree. Start after PR #36 merge or explicitly branch/stack from `feat/#26-naver-social-client`. Implement only Kakao client scope. Stage only Kakao files after tests. Do not commit/push/PR/Notion.
  Parallelization: Wave 1 | Blocked by: PR #36 merge or stack decision | Blocks: #29
  References: issue #27, #26 common provider support, repo auth/testing references.
  Acceptance criteria: Kakao client tests pass; staged files are under `src/main/java/band/platform/domain/user/social/client/kakao/**` and `src/test/java/band/platform/domain/user/social/client/kakao/**`.
  QA scenarios: happy `./gradlew test --tests '*KakaoSocialLoginClientTest'`; failure provider error/invalid response maps to common AUTH provider errors.
  Commit: N; proposed later `feat(auth): implement kakao social login client`.

- [ ] T3. #34 password reset worker stage-only command
  What to do / Must NOT do: Use a separate clean worktree based on latest `dev` after PR #35 merge. Implement password reset only. Stage only account-recovery/password-reset files after tests. Do not commit/push/PR/Notion.
  Parallelization: Wave 1 | Blocked by: latest dev including PR #35 | Blocks: account recovery PR
  References: issue #34, `auth-flow.md`, `testing.md`, PR #35 ID-find behavior.
  Acceptance criteria: password reset service/controller tests pass; final `./gradlew test` passes if shared auth/token behavior changed.
  QA scenarios: happy reset request -> verify code -> set new password -> old refresh token invalid; failure invalid/expired/reused reset code cannot change password.
  Commit: N; proposed later `feat(auth): implement password reset flow`.

## Ready-to-send thread commands

### Command for PR #36 review-fix thread
```text
레포 스킬 `.agents/skills/bandmaster-spring-backend/SKILL.md`를 먼저 읽고 적용해주세요.

이번 스레드는 PR #36 네이버 소셜 로그인 Client 리뷰 수정만 담당합니다.
워크트리: `/Users/macbook/Desktop/Projects/band-platform/.worktrees/pr-36-naver-review-fix`
브랜치: `feat/#26-naver-social-client`

중요 운영 규칙:
- 절대 `git commit` 하지 마세요.
- 절대 `git push` 하지 마세요.
- PR/Notion 업데이트도 하지 마세요.
- 사용자가 IntelliJ에서 staged files를 확인할 수 있게 `git add`까지만 하고 멈추세요.
- `git add .` 금지. 수정한 작업 파일만 명시해서 add 하세요.

작업 범위:
- `src/main/java/band/platform/domain/user/social/client/naver/NaverSocialLoginClient.java`
- `src/test/java/band/platform/domain/user/social/client/naver/NaverSocialLoginClientTest.java`

진행:
1. `git status --short --branch`로 현재 변경 파일을 확인합니다.
2. PR #36 리뷰 수정에 필요한 최소 변경만 마무리합니다.
3. `./gradlew test --tests '*NaverSocialLoginClientTest' --rerun-tasks`를 실행합니다.
4. 필요하면 `./gradlew test --tests 'band.platform.domain.user.social.*' --rerun-tasks`도 실행합니다.
5. 통과하면 아래처럼 명시 파일만 stage 합니다.
   `git add src/main/java/band/platform/domain/user/social/client/naver/NaverSocialLoginClient.java src/test/java/band/platform/domain/user/social/client/naver/NaverSocialLoginClientTest.java`
6. `git diff --cached --name-status`와 `git status --short` 결과를 보고하고 멈춥니다.

최종 보고에는 테스트 결과, staged 파일 목록, 남은 unstaged 파일 여부, 추천 커밋 메시지만 적어주세요. 커밋은 하지 마세요.
```

### Command for #27 Kakao worker
```text
레포 스킬 `.agents/skills/bandmaster-spring-backend/SKILL.md`를 먼저 읽고, `auth-flow.md`와 `testing.md`도 확인해주세요.

이번 스레드는 Issue #27 `[FEAT] 카카오 소셜 로그인 Client 구현`만 담당합니다.

중요 운영 규칙:
- 절대 `git commit` 하지 마세요.
- 절대 `git push` 하지 마세요.
- PR 생성/수정, Notion 업데이트도 하지 마세요.
- 사용자가 IntelliJ에서 staged files를 확인할 수 있게 `git add`까지만 하고 멈추세요.
- `git add .` 금지. 작업 파일만 명시해서 add 하세요.

브랜치:
- 권장: PR #36 merge 후 최신 `dev`에서 `feat/#27-kakao-social-client`
- PR #36이 아직 merge 전이면 구현 시작 전에 stack 여부를 보고하고 멈추세요.

수정 가능:
- `src/main/java/band/platform/domain/user/social/client/kakao/**`
- `src/test/java/band/platform/domain/user/social/client/kakao/**`

수정 금지:
- `AuthController`
- `UserController`
- `SecurityConfig`
- `PublicEndpoints`
- `UserTokenService`
- #26에서 만든 common HTTP/ErrorCode support 재수정

테스트:
- `./gradlew test --tests '*KakaoSocialLoginClientTest'`
- 필요 시 `./gradlew test --tests 'band.platform.domain.user.social.*'`

마지막:
- 통과 후 Kakao 관련 파일만 `git add <명시 파일들>`로 stage 합니다.
- `git diff --cached --name-status`, `git status --short` 결과를 보고하고 멈춥니다.
- 추천 커밋 메시지만 제안하세요: `feat(auth): implement kakao social login client`
```

### Command for #34 password reset worker
```text
레포 스킬 `.agents/skills/bandmaster-spring-backend/SKILL.md`를 먼저 읽고, `auth-flow.md`, `testing.md`, `pr-learning-docs.md`를 확인해주세요.

이번 스레드는 Issue #34 `[FEAT] PW 찾기 및 비밀번호 재설정 구현`만 담당합니다.

중요 운영 규칙:
- 절대 `git commit` 하지 마세요.
- 절대 `git push` 하지 마세요.
- PR 생성/수정, Notion 업데이트도 하지 마세요.
- 사용자가 IntelliJ에서 staged files를 확인할 수 있게 `git add`까지만 하고 멈추세요.
- `git add .` 금지. 작업 파일만 명시해서 add 하세요.

브랜치:
- PR #35는 merge됐으므로 최신 `dev`에서 `feat/#34-password-reset`로 시작하세요.
- 시작 전 `git fetch origin --prune`, `git status --short --branch`, `git log --oneline -n 5`로 기준을 보고하세요.

수정 가능:
- password reset 관련 controller/service/dto/repository
- reset code/token 저장소
- 메일 발송 interface와 개발용 구현
- password reset 후 refresh token invalidation 연결
- 대응 테스트

수정 금지:
- social provider client
- social login API 연결
- OAuth provider 파일
- unrelated dependency 추가

보안 기준:
- reset code/token은 secure random으로 생성
- 원문 저장 금지, hash 저장
- single-use, short-lived, retry limit
- 검증 전 password 변경 금지
- reset 성공 후 기존 refresh token 무효화
- password/reset token/refresh token 로그 출력 금지

테스트:
- reset 요청 성공
- invalid code
- expired code
- retry limit 초과
- 성공 reset 후 새 비밀번호 로그인 가능
- reset token 재사용 실패
- reset 후 기존 refresh token 무효화
- 공유 auth/token을 건드렸으면 최종 `./gradlew test`

마지막:
- 통과 후 password reset 관련 파일만 `git add <명시 파일들>`로 stage 합니다.
- `git diff --cached --name-status`, `git status --short` 결과를 보고하고 멈춥니다.
- 추천 커밋 메시지만 제안하세요: `feat(auth): implement password reset flow`
```

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [ ] F1. Plan compliance audit
- [ ] F2. Code quality review
- [ ] F3. Real manual QA
- [ ] F4. Scope fidelity

## Commit strategy
- Worker threads do not commit.
- Worker threads only propose commit messages.
- The user reviews staged files in IntelliJ.
- Only after user approval should a separate commit/push/PR-doc step run.

## Success criteria
- Stage-only policy is explicit in every worker command.
- Worker commands mention `git commit`, `git push`, and `git add .` only as explicit prohibitions, never as actions to run.
- Every worker command requires `git diff --cached --name-status` and `git status --short` in the final report.
- Existing PR #36 review-fix worktree is addressed separately from new implementation lanes.
