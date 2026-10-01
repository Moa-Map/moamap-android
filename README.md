# MoaMap Android

MoaMap Android 애플리케이션 레포지토리입니다.

## 프로젝트 구조

```text
app/
└── src/main/java/com/example/moamap/
    ├── core/
    │   ├── common/
    │   ├── designsystem/
    │   ├── navigation/
    │   └── network/
    └── feature/
        ├── onboarding/
        ├── explore/
        ├── collection/
        ├── notification/
        └── mypage/
```

## Git 브랜치 전략

GitHub Flow 기반으로 운영합니다. 장기 브랜치는 `main` 하나만 둡니다.

| 브랜치 | 역할 | 직접 커밋 | 분기 출처 | 병합 대상 |
| --- | --- | --- | --- | --- |
| `main` | 항상 빌드·실행 가능한 기준 브랜치 | X | - | - |
| `feat/*` | 기능 개발 | O | `main` | `main` |
| `fix/*` | 기능 수정, 버그 수정 | O | `main` | `main` |
| `refactor/*` | 리팩터링 | O | `main` | `main` |
| `chore/*` | 설정, 빌드, 문서 등 기타 작업 | O | `main` | `main` |

### 기본 규칙

- 모든 작업은 Issue를 먼저 생성한 뒤 진행합니다.
- `main`에는 직접 push하지 않고, PR로만 병합합니다.
- 작업 브랜치는 최신 `main`에서 분기하고, `main`을 대상으로 PR을 생성합니다.
- 작업 브랜치는 짧게 유지하고, 병합 후 바로 삭제합니다.
- `main`은 언제든 빌드·실행 가능한 상태를 유지합니다.

### 배포

- 스토어 배포 시 `main`의 배포 커밋에 버전 태그(`v1.2.0`)를 생성합니다.
- 배포 후 긴급 수정도 `main`에서 `fix/*` 브랜치를 분기해 PR로 반영한 뒤, 새 버전 태그를 생성합니다.

## 작업 흐름

```bash
git checkout main
git pull origin main
git checkout -b chore/#9/readme-ci-setting
```

작업 완료 후:

```bash
git add .
git commit -m "[CHORE] README 문서 및 CI 설정 #9"
git push origin chore/#9/readme-ci-setting
```

GitHub에서 PR을 생성합니다.

```text
base: main
compare: 작업 브랜치
```

## 브랜치 네이밍

```text
type/#이슈번호/작업명
```

예시:

```text
feat/#12/kakao-login
fix/#31/token-expiration
refactor/#18/place-dto
chore/#9/readme-ci-setting
```

## Commit 컨벤션

```text
[TYPE] 작업 내용 #Issue번호
```

예시:

```text
[FEAT] 카카오 로그인 구현 #12
[FIX] JWT 만료 검증 수정 #31
[REFACTOR] Place DTO 분리 #18
[CHORE] GitHub Actions 설정 #9
[DOCS] README 문서 작성 #9
```

### TYPE

| Type | 설명 |
| --- | --- |
| `INIT` | 프로젝트 초기 설정 |
| `FEAT` | 기능 추가 |
| `FIX` | 기능 수정, 버그 수정 |
| `REFACTOR` | 리팩터링 |
| `CHORE` | 설정, 빌드, 의존성, 기타 작업 |
| `DOCS` | 문서 작업 |
| `TEST` | 테스트 코드 |
| `RELEASE` | 배포 준비 |

## Pull Request 컨벤션

### PR 제목

```text
[TYPE] 작업 내용을 합니다 (#Issue번호)
```

예시:

```text
[FEAT] 카카오 로그인 기능을 구현합니다 (#12)
[FIX] JWT 만료 검증 오류를 수정합니다 (#31)
[CHORE] README 문서 및 CI 설정을 추가합니다 (#9)
```

### PR 본문

PR 템플릿에 맞춰 작성합니다.

- 작업 내용
- 연결된 이슈
- 리뷰 요청 사항
- 테스트 결과
- 체크리스트

### 병합 규칙

- GitHub Actions 통과
- 리뷰 승인
- Squash and Merge
- 병합 후 작업 브랜치 삭제
