# Notion PR Documentation Workflow

## 목적

BandMaster 작업은 GitHub Issue, Branch, Pull Request, Notion 작업 문서를 함께 남긴다.

PR마다 Notion 문서를 1개 작성해서 작업에 필요했던 개념, 참고한 공식 문서, 정리가 잘 된 블로그, 테스트 결과, 다음 작업자가 이어받을 내용을 남긴다.

## Notion 위치

- BandMaster 운영 홈: https://app.notion.com/p/37be86f38f4d80dea654cb40c3fb6fac
- PR 작업 문서 운영: https://app.notion.com/p/37be86f38f4d81fbbcbbd5624fcc0fd6
- PR 작업 문서 템플릿: https://app.notion.com/p/37be86f38f4d81dcb5ece99a1f772e45

## PR 작성 전 체크리스트

1. GitHub Issue 번호를 확인한다.
2. 브랜치 이름이 `feat/#이슈번호-작업-요약` 형식인지 확인한다.
3. Notion의 `PR 작업 문서 템플릿`을 복사해 이번 PR 문서를 만든다.
4. PR 문서에 다음 내용을 채운다.
   - GitHub Issue
   - Branch
   - Pull Request
   - 작업 요약
   - 변경 파일
   - 핵심 개념 정리
   - 공식 문서
   - 블로그 / 아티클
   - 구현 메모
   - 테스트 결과
   - AI 인수인계 메모
5. PR 본문에 Notion 문서 링크를 추가한다.

## PR 본문 참고 형식

```md
## #️⃣ 연관된 이슈

resolves: #이슈번호

## 📝 작업 내용

- 

## 테스트

- [ ] `./gradlew test`

## 참고 문서

- Notion: 
```

## 자료 정리 기준

- 공식 문서를 우선 링크한다.
- 블로그는 공식 문서만으로 이해가 부족한 내부 동작이나 작성 원리를 보완할 때 추가한다.
- 코드 구현과 직접 관련 없는 자료는 넣지 않는다.
- 다음 작업자가 같은 결정을 반복하지 않도록 "왜 이 방식을 골랐는지"를 짧게 남긴다.

## 핵심 개념 작성 기준

핵심 개념은 단순 키워드 목록으로 쓰지 않는다. 작업자가 나중에 코드를 봤을 때 내부 동작과 작성 원리를 이해할 수 있을 정도로 적는다.

각 개념은 가능하면 다음 내용을 포함한다.

- 문제 상황
- 이 개념이 필요한 이유
- Spring / Java 내부 동작 흐름
- 이 프로젝트에서 적용한 방식
- 다른 선택지도 있었는지
- 주의할 점

예를 들어 Spring 예외 처리 작업이라면 `@RestControllerAdvice`를 썼다는 사실만 적지 않는다. 기본 `/error` 흐름, `HandlerExceptionResolver`, `@ExceptionHandler`가 어떤 순서로 동작하는지까지 적는다.

## AI 인수인계 메모

사람이 반드시 읽어야 하는 섹션은 아니다.

다음 AI/Codex가 같은 맥락을 이어받을 때 필요한 파일 범위, 남은 결정, 주의할 충돌 지점을 적는다.

## 에러코드 Prefix

| Prefix | 의미 |
| --- | --- |
| `E**` | 전역/공통 에러 |
| `A**` | 인증/Auth 도메인 에러 |
| `U**` | 유저/User 도메인 에러 |
