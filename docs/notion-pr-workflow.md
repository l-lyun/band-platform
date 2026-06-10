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
   - 다음 작업자에게 남길 내용
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
- 블로그는 공식 문서만으로 이해가 부족한 개념을 보완할 때 추가한다.
- 코드 구현과 직접 관련 없는 자료는 넣지 않는다.
- 다음 작업자가 같은 결정을 반복하지 않도록 "왜 이 방식을 골랐는지"를 짧게 남긴다.

## 에러코드 Prefix

| Prefix | 의미 |
| --- | --- |
| `E**` | 전역/공통 에러 |
| `A**` | 인증/Auth 도메인 에러 |
| `U**` | 유저/User 도메인 에러 |
