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

경량 Git Flow 기반으로 운영합니다.

| 브랜치 | 역할 | 직접 커밋 | 분기 출처 | 병합 대상 |
| --- | --- | --- | --- | --- |
| `main` | 배포 가능한 안정 상태 | X | - | - |
| `dev` | 개발 통합 브랜치 | X | `main` | `release/*` |
| `release/*` | 배포 준비 브랜치 | X | `dev` | `main`, `dev` |
| `feat/*` | 기능 개발 | O | `dev` | `dev` |
| `fix/*` | 기능 수정, 버그 수정 | O | `dev` | `dev` |
| `refactor/*` | 리팩터링 | O | `dev` | `dev` |
| `chore/*` | 설정, 빌드, 문서 등 기타 작업 | O | `dev` | `dev` |

### 기본 규칙

- 모든 작업은 Issue를 먼저 생성한 뒤 진행합니다.
- `main`, `dev`, `release/*`에는 직접 push하지 않습니다.
- 작업 브랜치는 `dev`에서 분기합니다.
- 작업 브랜치는 `dev`를 대상으로 PR을 생성합니다.
- 배포 준비 시 `dev`에서 `release/*` 브랜치를 생성합니다.
- 배포 검증 후 `release/*`를 `main`에 병합합니다.
- `main` 병합 후 버전 태그를 생성합니다.
- release 과정에서 발생한 수정 사항은 `dev`에도 반영합니다.

## 작업 흐름

```bash
git checkout dev
git pull origin dev
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
base: dev
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
