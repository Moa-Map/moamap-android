# MoaMap — Mapbox 역량 쇼케이스 (디자이너 리뷰용)

- 작성일: 2026-07-06
- 상태: 설계 승인됨 (구현 계획 대기)

## 1. 목적

디자이너에게 "Mapbox로 무엇이 가능한가"를 실제 안드로이드 앱으로 보여주기 위한
쇼케이스. 최종 제품 화면이 아니라, 실기기에서 직접 조작하며 룩앤필·인터랙션·표현
범위를 판단할 수 있게 하는 **살아있는 데모**다. 이 데모를 바탕으로 디자이너가
MoaMap 지도 화면의 방향을 결정한다.

## 2. 결정 사항 (브레인스토밍 결과)

- **지도 엔진**: Mapbox가 메인 지도. 기존 Kakao SDK(`v2-all`)는 로그인/공유 등
  다른 용도로만 남겨두고, 이 작업에서는 **손대지 않는다**.
- **전달 형태**: 웹 목업이 아니라 **실제 안드로이드 앱** + 진짜 Mapbox SDK.
- **토큰 상태**: Mapbox 계정 보유. public 토큰 확인 + secret 다운로드 토큰 발급 안내 필요.
- **섹션 구성**: 아래 4개 섹션. **3D는 섹션 3 하나만**, 섹션 1·2·4는 전부 2D
  (카메라 pitch 0, 기울기 없음).

## 3. 화면 구성

`MainActivity` → `Scaffold` + 하단 `NavigationBar`(탭 4개) → 각 탭이 아래 섹션 Composable.
각 탭은 실제 조작 가능(핀치줌·팬·마커 탭). 정적 이미지가 아니다.

| # | 섹션 | 차원 | 보여주는 Mapbox 역량 |
|---|------|------|----------------------|
| 1 | 스타일 스위처 | 2D | Standard / Streets / Satellite / Dark + 커스텀 브랜드 스타일 1종 전환 |
| 2 | 마커 & 클러스터링 | 2D | 커스텀 핀 마커, 줌아웃 시 클러스터 묶기, 탭 시 말풍선(콜아웃) |
| 3 | 3D 시티뷰 | 3D | 3D 건물 돌출(extrusion), 카메라 pitch·회전, fly-to 애니메이션, 하늘 레이어 |
| 4 | 데이터 오버레이 | 2D | 장소 밀집 히트맵 + 경로 라인(폴리라인) |

### 섹션별 상세

- **섹션 1 — 스타일 스위처**: 하단 또는 상단에 스타일 선택 칩/세그먼트. 선택 시
  같은 카메라 위치에서 스타일만 교체. 커스텀 브랜드 스타일 1종 포함(Mapbox Studio
  스타일 URL, 없으면 Standard 파생 임시 스타일). 카메라 평면(2D).
- **섹션 2 — 마커 & 클러스터링**: 서울 지역 더미 장소 ~30~50개. MoaMap 커스텀 핀
  아이콘. 줌아웃 시 클러스터로 묶고 개수 배지 표시, 줌인 시 개별 핀. 마커 탭 시
  간단한 콜아웃(장소명). 카메라 평면(2D).
- **섹션 3 — 3D 시티뷰 (유일한 3D)**: fill-extrusion으로 3D 건물, pitch(예: 60°)와
  bearing 적용, 진입 시 fly-to 애니메이션, 하늘(sky) 레이어. 이 섹션에서만 기울기 사용.
- **섹션 4 — 데이터 오버레이**: 더미 포인트 밀집 데이터로 히트맵 레이어 + 두 지점을
  잇는 경로 폴리라인(정적 좌표 배열, Directions API 호출은 범위 밖). 카메라 평면(2D).

## 4. 기술 스택 / 의존성

- Mapbox Maps SDK for Android v11 + Compose 확장
  - `com.mapbox.extension:maps-compose:11.24.3`
  - `com.mapbox.maps:android:11.24.3`
  - (버전은 구현 시점 최신 11.x로 재확인)
- 공식 `MapboxMap` Composable + `rememberMapViewportState` 사용. 현재 프로젝트가
  Jetpack Compose이므로 정합.
- 기존 스택 유지: compileSdk 37 / minSdk 24, Compose BOM 2024.09, Kotlin 2.2.10, AGP 9.1.1.

## 5. 토큰 처리

- **public 토큰 (`pk.…`)**: 런타임 지도 로딩용. 앱 리소스(`res/values`의 문자열,
  예: `mapbox_access_token`)에 넣어 Mapbox SDK에 전달. 데모용으로 리소스 커밋 허용
  하되, 원칙적으로는 노출 최소화(공개 배포 아님).
- **secret 다운로드 토큰 (`sk.…`, scope `DOWNLOADS:READ`)**: SDK 아티팩트 내려받기용.
  `~/.gradle/gradle.properties`의 `MAPBOX_DOWNLOADS_TOKEN`에 저장. **레포에 커밋 금지.**
- `settings.gradle.kts`의 `dependencyResolutionManagement.repositories`에 Mapbox
  maven 저장소를 basic 인증(username=`mapbox`, password=`MAPBOX_DOWNLOADS_TOKEN`)으로 추가.

## 6. 진행 순서

1. **토큰 발급**: public 토큰 확인 + secret 다운로드 토큰(`DOWNLOADS:READ`) 발급,
   저장 위치 안내(`~/.gradle/gradle.properties`).
2. **gradle 배선**: `settings.gradle.kts`에 Mapbox maven 저장소(인증) 추가,
   `libs.versions.toml` + `app/build.gradle.kts`에 의존성 추가.
3. **권한/토큰 리소스**: 위치 권한(Manifest), public 토큰 리소스 등록.
4. **지도 첫 렌더**: 섹션 1(스타일 스위처)부터 실기기 렌더 확인 = "연결 성공" 판정 지점.
5. **섹션 2·3·4 순차 구현.**

## 7. 범위 밖 (YAGNI)

- Kakao 지도/로그인 통합 변경 없음.
- 실제 백엔드/실데이터 없음 — 모든 장소·경로·히트맵은 더미 데이터.
- Directions API 실시간 경로 계산 없음(섹션 4는 정적 폴리라인).
- 최종 MoaMap 제품 화면(홈/온보딩 등)은 이 스펙 범위 아님.

## 8. 성공 기준

- 실기기에서 앱 실행 → 4개 탭 모두 지도가 정상 렌더.
- 섹션 1: 스타일 5종(커스텀 포함) 전환 동작.
- 섹션 2: 클러스터↔개별 핀 전환, 마커 탭 콜아웃 동작.
- 섹션 3: 3D 건물 + pitch 뷰 + fly-to 동작(유일한 3D 섹션).
- 섹션 4: 히트맵 + 경로 라인 동시 표시.
- 섹션 1·2·4는 기울기 없이 2D 유지.
