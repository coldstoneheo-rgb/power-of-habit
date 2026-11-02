# 블록 D — Google Drive 백업 OAuth 체크리스트

> 원문: [`docs/RELEASE.md`](RELEASE.md) §0-4. 이 문서는 그 절을 **블록 D(사용자 손, ≤25분)** 실행 순서로 옮긴 것이다. 어긋나면 RELEASE.md를 고치고 이 문서를 맞춘다.
> 선행: 블록 C(첫 AAB 수동 업로드) — **Play 앱 서명 키 SHA-1은 첫 업로드 뒤에야 콘솔에 보인다.**
> 결정: Drive 백업 **v1 포함**(LC 10-03, 회의 F4 기각). 이 블록 없이는 Drive 백업/복원이 코드 10(DEVELOPER_ERROR)으로 실패한다(RELEASE.md §0-4 «현재 상태»).
> 증거 폴더: `D:\사진\power-of-habit\release\` (`D-1-oauth-clients.png` · `D-2-consent-production.png` · `D-3-drive-backup-ok.png`).

## 0. 앱이 요구하는 것 (코드 기준 — 바꾸지 않는다)
| 항목 | 값 | 근거 |
|---|---|---|
| 패키지 | `com.woodpeckerai.powerofhabit` | `app/build.gradle.kts:80` |
| 범위(scope) | `https://www.googleapis.com/auth/drive.appdata` 1개 + 기본 로그인(이메일) | `backup/GoogleDriveBackupManager.kt:59-60, 101-108` |
| API | Google Drive API v3 | `app/build.gradle.kts:228` |
| 클라이언트 유형 | **Android** OAuth 클라이언트(웹 클라이언트 ID·`google-services.json` 불필요 — 코드가 `requestIdToken`을 쓰지 않는다) | `GoogleDriveBackupManager.kt:101-108` |

## 1. SHA-1 두 개(+디버그) 모으기
- [ ] **업로드 키 SHA-1** (블록 A의 `release.jks`):
  ```
  keytool -list -v -keystore release.jks -alias powerofhabit
  ```
  → `SHA1:` 줄만 메모(비밀번호 화면은 캡처하지 않는다).
- [ ] **Play 앱 서명 키 SHA-1**: Play 콘솔 → 습관의 힘 → 테스트 및 출시 → 설정 → **앱 무결성** → «앱 서명» 탭 → «앱 서명 키 인증서»의 SHA-1.
  - Play에서 설치한 앱(내부·비공개·프로덕션 전부)은 이 키로 서명된다 → **이게 빠지면 스토어 설치본만 코드 10**으로 실패한다.
- [ ] (선택, 개발용) 디버그 키 SHA-1:
  ```
  keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```

## 2. Cloud 프로젝트 고르기 — **같은 프로젝트**
- [ ] 예전 앱(`com.example.powerofhabit`)용 OAuth/Drive를 만들었던 **같은 Google Cloud 프로젝트**를 연다(RELEASE.md §0-4-3).
  - 다른 프로젝트에 만들면 `appDataFolder`가 프로젝트별로 분리돼 서로의 백업이 보이지 않는다.
  - 예전 프로젝트가 없으면(옛 앱엔 Drive 로그인 코드가 없었다 — 결정 기록 09-05 «확인된 사실») 새 프로젝트 1개를 만들고 **이후로는 그것만** 쓴다. 프로젝트 이름 권장: `power-of-habit`.
- [ ] API 및 서비스 → 라이브러리 → **Google Drive API → 사용 설정**.

## 3. Android OAuth 클라이언트 등록 (SHA-1마다 1개)
Google Auth Platform(구 «OAuth 동의 화면/사용자 인증 정보») → **클라이언트** → 클라이언트 만들기 → 유형 **Android**:
| # | 이름(권장) | 패키지 이름 | SHA-1 |
|---|---|---|---|
| 1 | `poh-android-upload` | `com.woodpeckerai.powerofhabit` | 업로드 키 SHA-1 |
| 2 | `poh-android-play-signing` | `com.woodpeckerai.powerofhabit` | **Play 앱 서명 키 SHA-1** |
| 3 | `poh-android-debug`(선택) | `com.woodpeckerai.powerofhabit` | 디버그 키 SHA-1 |
- [ ] 3개(최소 1·2)가 목록에 보이는 화면 캡처 → `D-1-oauth-clients.png`.
- 클라이언트 ID·시크릿을 앱에 넣을 필요는 없다(Android 클라이언트는 패키지+SHA-1로 매칭).

## 4. 동의 화면(브랜딩) 입력 — 붙여 넣을 문안
| 칸 | 값 |
|---|---|
| 앱 이름 | `습관의 힘` |
| 사용자 지원 이메일 | Play 콘솔 개발자 연락처와 같은 이메일 |
| 앱 로고 | **올리지 않는다**(로고를 올리면 브랜드 확인 심사가 붙을 수 있다 — 추정, 콘솔 안내 우선) |
| 앱 홈페이지 | 비움(또는 처리방침 URL) |
| 개인정보처리방침 링크 | `docs/privacy/DEPLOY.md` §4에서 200을 확인한 실측 URL |
| 서비스 약관 링크 | 비움 |
| 승인된 도메인 | 처리방침 링크를 넣으면 콘솔이 요구하는 도메인(예: `vercel.app` 하위 도메인)을 넣는다. 도메인 소유 확인을 요구하면 처리방침 링크를 비우고 진행(비민감 범위만 쓰는 앱은 링크 없이도 게시 가능 — 추정) |
| 개발자 연락처 | 위와 같은 이메일 |
| 데이터 액세스(범위) | `…/auth/drive.appdata` 추가 — 콘솔 분류가 **비민감**인지 확인(RELEASE.md §0-4-5 «비민감 범위») |

범위 사용 설명이 필요하면(사용자에게 보이는 문안, 그대로 붙여 넣기):
> 습관의 힘은 사용자가 설정에서 «백업하기»를 누른 경우에만, 사용자의 습관 기록 파일을 사용자 본인 Google Drive의 이 앱 전용 숨김 폴더(appDataFolder)에 저장하고, «복원하기»를 누르면 그 파일을 다시 읽어 옵니다. 앱은 사용자의 다른 Drive 파일을 읽거나 바꾸지 않으며, 백업 파일은 개발자를 포함한 누구와도 공유되지 않습니다.

## 5. 게시 상태 «프로덕션» 전환
- [ ] Google Auth Platform → **대상(Audience)** → 게시 상태 «테스트» → **«앱 게시(프로덕션으로 푸시)»**.
  - 왜: «테스트» 상태는 등록한 테스트 사용자(최대 100명)만 로그인할 수 있고 승인이 7일마다 만료된다 → 비공개 테스터·일반 사용자가 Drive 백업을 못 쓴다.
  - 비민감 범위(`drive.appdata`)만 쓰고 로고가 없으면 확인(verification) 심사 없이 바로 «프로덕션»이 된다(추정 — 콘솔이 «확인 필요»를 띄우면 [BLOCKED]로 LC에 보고).
- [ ] «게시 상태: 프로덕션» 화면 캡처 → `D-2-consent-production.png`.

## 6. 동작 확인 (내부 테스트 설치본 = Play 앱 서명 키로 서명된 빌드)
- [ ] 앱 → 설정(홈 우상단 톱니) → **백업하기** → Google 계정 선택 → 권한 동의 → 토스트 «백업이 완료되었습니다.»(`ui/main/MainScreenViewModel.kt:225`).
- [ ] 같은 화면에 «연결된 계정: …» 표시.
- [ ] **복원하기** → «백업에서 복원할까요?» 다이얼로그 → 복원 → 앱 재시작 → 기록 그대로.
- [ ] 캡처 → `D-3-drive-backup-ok.png`. 실패 시 토스트 문구(코드 번호 포함)를 그대로 적는다:
  | 증상 | 원인 후보 |
  |---|---|
  | «Google 로그인 설정 오류(코드 10): …»(`GoogleDriveBackupManager.kt:149-150`) | 패키지/SHA-1 불일치 — 대부분 **Play 앱 서명 키 SHA-1 누락** |
  | «Google 로그인에 실패했습니다. 잠시 후…»(12500) · «Google 로그인이 취소되었습니다»(12501) | 동의 화면 미완성·게시 상태 «테스트»인데 테스트 사용자 아님 / 사용자가 취소 |
  | 로그인은 되는데 «백업에 실패했습니다.» | Drive API 미사용 설정, 또는 **병합 매니페스트에 `INTERNET` 없음**(`docs/store-listing/DATA-SAFETY-2026-11.md` §4-2 ⚠) |
- 결과는 [`DEVICE-QA-2026-11.md`](DEVICE-QA-2026-11.md) 13행에도 적는다.

## 7. 알아 둘 위험 (이 블록에서 고치지 않는다)
- 앱은 `com.google.android.gms.auth.api.signin.GoogleSignIn`(play-services-auth 20.7.0, `app/build.gradle.kts:227`)을 쓴다. Google은 이 API를 Credential Manager·`AuthorizationClient`로 이전하라고 안내하고 있다 — 지금은 동작하지만 출시 후 백로그 후보.
