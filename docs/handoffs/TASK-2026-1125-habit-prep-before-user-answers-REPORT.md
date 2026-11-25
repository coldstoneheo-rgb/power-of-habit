# TASK-2026-1125 — 사용자 답 2개가 오기 전 준비 REPORT

> origin:lc(`life-coordinator-7e`) · 시작 2026-11-25 21:17 · 기준 main `aaecc13` · 워커 `power-of-habit-dd`(worktree `prep-before-answers`) · 예산 40분

## 체크리스트(완료 조건)
| # | 항목 | 상태 | 결과 |
|---|---|---|---|
| 1 | 단위 시험 | ✅ | `testDebugUnitTest` **126/0**(건너뜀 0) · 21:23:06 · `--rerun --no-build-cache`(첫 실행은 `FROM-CACHE`라 버림) |
| 2 | 광고 ID | ✅ | 릴리스 병합 매니페스트 `AD_ID` **0건** → 사용자가 고를 답 **«아니요»**(DATA-SAFETY §4-2 · FIRST-UPLOAD 8단계 반영) |
| 3 | 로그인 정보 저장 | ✅ | **둔다** — 아래 §3. 처리방침 4항 · 5항 문장을 늘림, 데이터 보안 답은 그대로 |
| 4 | 게시 준비(푸시 0) | ✅ | 처리방침은 시행일만 비어 있음 · 게시 명령 3줄 연습 완료 · 앱 주소 PR **#45 draft** · 아래 §4 |
| 5 | PR | ✅ | 문서 PR **#46** 머지 `6e1fbc0`(critic 86 → 93 PASS · LC PASS-관찰) |

## 1. 단위 시험
`./gradlew.bat :app:testDebugUnitTest --rerun --no-build-cache --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1536m` → exit 0 · `BUILD SUCCESSFUL in 1m 49s` · 시험 결과 XML 합계 126 / 실패 0 / 건너뜀 0. 그 시점 작업본은 문서만 바뀐 `aaecc13`이었다. 앞 창의 126/0과 같다. 시작 전 여유 메모리 7.6GB(21:17).

## 2. 광고 ID
- 명령: `:app:processReleaseMainManifest :app:processReleaseManifestForPackage` exit 0(21:20:59)
- 확인: `grep -c com.google.android.gms.permission.AD_ID` = **0**(`merged_manifest` · `merged_manifests` · `packaged_manifests`의 release 3개 모두, 21:21) · `GET_ACCOUNTS` 0 · `FOREGROUND_SERVICE_` 0
- 권한 8개 = 앱 선언 4 + `WAKE_LOCK` · `ACCESS_NETWORK_STATE` · `FOREGROUND_SERVICE` · `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
- `bundleRelease`(R8)는 이번에 돌리지 않았다(R8은 매니페스트 권한을 더하지 않는다 — 추론이지 실행 결과가 아니다).

## 3. 로그인 라이브러리의 저장 — «둔다»
방법: Gradle 캐시의 AAR을 풀어 바이트코드를 `javap -c`로 읽었다(실기기 저장 파일을 본 것은 아니다). 이 프로젝트의 해석 버전 = `play-services-auth 20.7.0`(`app/build.gradle.kts`) · `play-services-base 18.0.1` · `play-services-basement 18.2.0`(매니페스트 병합 보고서).

| 확인 | 근거(클래스 · 메서드) |
|---|---|
| 저장 위치 | `play-services-base` `…signin.internal.Storage` 생성자 → `getSharedPreferences("com.google.android.gms.signin")` |
| 저장 시점 | `play-services-auth` `SignInHubActivity.onActivityResult` → `zbn.zbe` → `Storage.saveDefaultGoogleSignInAccount` |
| 저장 내용 | `GoogleSignInAccount.zad()`가 만드는 JSON 키: `id` · `email` · `displayName` · `givenName` · `familyName` · `photoUrl` · `expirationTime` · `obfuscatedIdentifier` · `grantedScopes`(· `tokenId`는 요청할 때만). `serverAuthCode`는 저장 직전 `remove` |
| 지우는 때 | `GoogleSignInClient.signOut` → `zbm.zbg` → `zbh`(→ `zbn.zbd` → `Storage.clear()`)가 서버 호출보다 먼저 → 앱의 «연결 해제»가 지운다 |
| OS 백업 | `allowBackup="true"` · 백업 규칙 없음 + Android 문서 «By default, Auto Backup includes almost all app files»(developer.android.com/identity/data/autobackup, 2026-11-25 curl 200). `SharedPreferences`는 제외 목록(cache · code_cache · no_backup)에 없다 → 이메일 등이 들어갈 수 있다 |

같은 코드가 `base 18.5.0`에도 있다(18.0.1과 대조). 앱 사용자 코드는 이 파일을 직접 읽지 않는다.

**바뀌는 것 1줄:** 처리방침 4항(로그인 정보를 라이브러리가 기기 안에 보관 · 지우는 방법 · 기기 백업 사본은 바로 안 지워짐), 5항(기기 백업에 로그인 정보 포함 가능), 6항(기기 백업 사본은 앱·개발자가 지울 수 없음)에 문장을 더했다. 데이터 보안 답은 바뀌지 않는다(§1-2 이메일 행 · ⚠ 문단에 근거 추가).

**남은 판단(답을 바꾸지 않음):** 도움말 10787469(2026-11-25 curl 200)에는 «기기 밖으로 보내지 않는 기기 내 처리는 수집이 아님»과 «Libraries and SDKs: … user data transmitted off device … irrespective of whether data is transmitted to you or a third-party server»가 있고, **OS 자동 백업**을 직접 다룬 문장은 못 찾았다(Drive 예외 문장은 앱이 접근하지 않는 경우라 근거로 쓰지 않는다). 콘솔에서 이메일을 묻는 문항이 나오면 DATA-SAFETY §1-2의 ⚠대로 «예 · 선택»으로 바꾼다.

## 4. 게시 준비(`gh-pages` 푸시 · Pages 켜기는 하지 않음)
| 준비 | 상태 |
|---|---|
| 처리방침 본문 | 시행일 자리 `[게시일 입력]` 1곳만 비어 있음. 앱 이름 «습관의 힘» 확정(바꾸지 않음) |
| 게시 명령 | `docs/privacy/DEPLOY.md` §2 — 3줄. 로컬 빈 저장소로 푸시까지 연습(파일 1개 · 시행일 치환 · `[ ]` 0) |
| 앱 주소 PR | **#45 draft** — `PrivacyPolicy.URL` 1줄(`19fecb9`) · `FirstRunJourneyTest` 5/0(21:27) · 200 확인 전 머지 금지 |

**«게시해도 됨»이 오면 남는 것:** 워커 명령 — fetch 1 · 커밋 만들기 1 · `gh-pages` 푸시 1 · 200 확인 1 · #45에 시행일 커밋 + 푸시 2 · ready 1 · 머지 1 = **8개**(+ 가지 삭제 2). **사용자 손 1곳** = GitHub Settings › Pages › `gh-pages` / root › Save(약 1분). 순서: 이 문서 PR(처리방침 문장 보강 포함)이 **먼저 머지**되어야 한다 — 게시는 main의 처리방침을 쓴다. 이름은 그대로라 이름 PR(#47)은 서명 빌드 전이면 어디든 되고, 실제 순서는 #47 → #48(1항 문구) → 게시 → #45였다.

## 앞 창 REPORT와의 관계
앞 REPORT «미확인» 1번은 이 문서 §3으로 닫는다(원문은 고치지 않았다). 2번(로그인 요청 범위 축소)은 그대로다 — 이메일만으로 줄이면 저장되는 이름 · 사진도 줄어들지만, 첫 실기기 Drive 확인 뒤에 바꾼다.

## 사용자 손(요약)
처리방침 게시 확인 · FIRST-UPLOAD 1 ~ 5(지금 할 수 있음).

## 이름 결정(2026-11-25 21:51 무렵 사용자, LC 전달)
앱 이름 «습관의 힘» 그대로 → 앱 코드 · 처리방침 · 모집 글의 이름은 바뀌지 않는다. 스토어 제목 `습관의 힘 - 하루 한 번 기록`(17/30)은 LC 가정. LISTING · CLOSED-TEST §6 · FIRST-UPLOAD 3 · 11단계 · AGENT_STATUS에 반영. 문안 점검: 없는 기능을 약속하는 문장 0(대조: 최근 4일 `MainScreen.kt:224` · 탭 완료/미완료 `HabitDao.kt:61-71` · 길게 누르면 건너뛰기 `MainScreen.kt:879` · 주기 5종 `AddEditHabitScreen.kt:509` · 색 36 `Color.kt:39` · 뱃지 3·7·21·66 `BadgeRules.kt:31-37` · 점수 EMA `HabitStatsCalculator.kt:70`) · 책 · 저자와 관계 있게 읽히는 문장 0(LISTING의 «책 10쪽»은 습관 예시). 고친 문장 1곳: «습관과 기록은 기기 안에만 저장되며» → «기기 안에 저장되며»(Drive 백업 · 기기 백업과 모순되지 않게, DATA-SAFETY §3과 같은 표현). README 첫 줄은 «습관의 힘(The Power of Habit)»이라 영문 책 제목과 같다 — 이번 범위(스토어 문안)가 아니라 바꾸지 않고 LC에 알렸다.

그림: 피처 그래픽 1024×500(24비트, 아이콘과 같은 링 · 체크 + «습관의 힘» Noto Sans KR 700, Edge 헤드리스 렌더)과 스크린샷 4장(`StoreShotsTest` 21:22, 24비트 변환 후 기존 18:57 것 4장과 픽셀 차이 0) — 사용자 PC 그림 폴더(커밋하지 않음).

## 게시 결과(2026-11-25)
- 사용자 «1항 고쳐서 게시해도 됨»(워커 창 23:34 직접 입력) → #48 `a4ce621`(1항 1줄) → `gh-pages` `58d7272`(파일 1개 · force 0 · main 원본과 차이 시행일 1줄) 23:36.
- Pages는 자동으로 켜짐. 주소 HTTP 200(23:36:39) · 게시 파일과 바이트 동일(sha256 e3f71133…). 시행일 2026년 11월 25일 · 1항 새 문구 · 문의 메일 · 운영 주체를 curl로 확인.
- #45 `8b902f2`(= 사용자 9단계 커밋) · versionCode 89 · 서명 가드 풀림(`build.gradle.kts:154` 빈 주소 패턴 0건) · 시험 126/0(23:38).
- 다음 게시(본문이 바뀌면): `DEPLOY.md` §5의 `-p origin/gh-pages`.

## 9단계 SHA 규칙
사용자 서명 빌드의 커밋은 #45 머지 뒤 origin/main의 최신 SHA다. 그 뒤 문서 PR이 머지되면 versionCode(main 커밋 수)가 바뀌므로 최신 SHA를 다시 알린다 — 서명 빌드 전에 머지를 끝내는 것이 낫다.

## LC 관찰(이번 게이트 · 막지 않음)
1. 로그인 라이브러리 저장소를 OS 백업에서 빼는 백업 규칙은 2번째 빌드 후보(첫 빌드에는 넣지 않는다).
2. 콘솔 8단계에서 이메일 문항이 나오면 사용자 캡처 → «예 · 선택 · 앱 기능».

## effort
세션 수준을 올리지 않았다. critic 서브에이전트 2회(읽기 전용 · effort 따로 지정하지 않음) = `1b05b36` **조건부 PASS 86** → 지적 6건을 고친 `aae359e` **PASS 93**(필수 수정 0). 그 뒤 권고 3건을 반영한 문서 차분은 LC 게이트에서 재확인. 결과 요약은 PR #46 댓글. 확인 못 한 것: Play 서비스가 «Drive 권한 거부» 상태에서 계정을 돌려주는지(실기기 필요).

## gradle 기록(로컬 · CI 없음)
| 시각 | 명령 | 결과 |
|---|---|---|
| 21:20 | `testDebugUnitTest`(첫 실행) | exit 0이나 `FROM-CACHE` — 근거로 쓰지 않음 |
| 21:20 | `processReleaseMainManifest` · `processReleaseManifestForPackage` | exit 0 |
| 21:23 | `testDebugUnitTest --rerun --no-build-cache` | 126/0 |
| 21:27 | `FirstRunJourneyTest --rerun`(#45 worktree) | 5/0 |
