# Branch Strategy

## 기본 규칙

이슈 단위로 브랜치를 분기한다.

```text
feat/#이슈번호-작업-요약
```

예시:

```text
feat/#2-backend-foundation
feat/#3-error-handling
feat/#5-member-domain
feat/#6-email-signup
```

## 작업 방식

- 하나의 브랜치는 하나의 GitHub 이슈를 기준으로 작업한다.
- 작업 요약은 영어 kebab-case로 짧게 적는다.
- 같은 이슈를 여러 스레드에서 동시에 구현하지 않는다.
- 병렬 작업이 필요하면 이슈를 나누고 각 브랜치의 파일 소유 범위를 먼저 정한다.

## 충돌 방지

- 공통 기반 작업은 `common/**`, `build.gradle`, 설정 파일을 소유한다.
- 회원 도메인 작업은 `member/**`만 소유한다.
- 인증 작업은 `auth/**`만 소유한다.
- 서로 다른 스레드가 같은 파일을 수정해야 하면 먼저 한쪽 작업을 머지한 뒤 이어서 진행한다.

## 현재 예시

이슈 #2 백엔드 공통 기반 세팅:

```text
feat/#2-backend-foundation
```
