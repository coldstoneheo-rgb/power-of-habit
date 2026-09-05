# RELEASE — Play 내부 테스트 배포 절차

applicationId **`com.woodpeckerai.powerofhabit`** (2026-09-05 확정, 변경 불가). Kotlin 패키지·namespace는 `com.example.powerofhabit`을 유지한다(스토어 식별자와 무관).

## 0. 한 번만 하는 준비

### 0-1. 릴리스 키스토어 생성 (사용자가 직접 — 비밀번호 입력 필요)
프로젝트 루트에서:
```
keytool -genkeypair -v -keystore release.jks -alias powerofhabit -keyalg RSA -keysize 2048 -validity 10000
```
- `release.jks`는 gitignore 대상이다. **분실하면 같은 앱으로 업데이트할 수 없다** — Play App Signing을 쓰더라도 업로드 키로 필요하므로 비밀번호와 함께 안전한 곳(비밀 관리자·오프라인 백업)에 보관한다.
- `keystore.properties.example`을 `keystore.properties`로 복사해 값을 채운다(gitignore 대상). CI에서는 환경변수 `POH_STORE_FILE / POH_STORE_PASSWORD / POH_KEY_ALIAS / POH_KEY_PASSWORD`로 대체할 수 있다.

### 0-2. Play Console
1. Play Console → 앱 만들기: 이름 "습관의 힘", 기본 언어 한국어, 앱, 무료.
2. 앱 무결성 → **Play 앱 서명** 사용(기본). 첫 업로드 AAB의 서명 키가 업로드 키로 등록된다.
3. 테스트 → 내부 테스트 → 테스터 목록 생성(이메일 최대 100명), 참여 링크 확보.
4. 스토어 등록정보의 최소 항목(아이콘·그래픽·설명·개인정보처리방침 URL)은 내부 테스트 단계에서도 요구될 수 있다.

### 0-3. 업로드 자동화용 서비스 계정 (선택, 권장)
1. Google Cloud Console → 서비스 계정 생성 → JSON 키 다운로드 → 루트에 `play-service-account.json`(gitignore).
2. Play Console → 사용자 및 권한 → 서비스 계정 이메일 초대 → 앱 권한: "출시 관리(테스트 트랙)".
3. 첫 업로드는 Console에서 수동으로 한 번 해야 API 업로드가 열린다(Play 정책).

### 0-4. Google Drive 백업용 OAuth 클라이언트 (백업 기능을 살릴 때 필수)
applicationId와 서명 키가 바뀌었으므로 Drive API용 Android OAuth 클라이언트를 **같은 Cloud 프로젝트**에 새로 등록해야 한다.
1. `keytool -list -v -keystore release.jks -alias powerofhabit` 로 업로드 키 SHA-1 확인. Play Console → 앱 무결성에서 **Play 앱 서명 키 SHA-1**도 확인.
2. Cloud Console → 사용자 인증 정보 → OAuth 클라이언트 ID(Android) → 패키지 `com.woodpeckerai.powerofhabit` + 두 SHA-1 각각 등록(디버그 키 SHA-1도 개발용으로 추가).
3. 다른 Cloud 프로젝트에 만들면 `appDataFolder`가 분리돼 예전 백업 파일이 보이지 않는다.
4. 디버그 빌드로 테스트하려면 디버그 키 SHA-1도 등록한다:
   `keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android`
5. Cloud Console → API 및 서비스 → **Google Drive API 사용 설정**. OAuth 동의 화면은 "테스트" 상태여도 되며(`drive.appdata`는 비민감 범위), 테스트 사용자에 본인 계정을 넣는다.
> **현재 상태(2026-09-05, 이 PR 이후)**: 설정 → 백업하기/복원하기를 누르면 앱이 Google 로그인을 요청하고(`drive.appdata` 범위), 성공하면 바로 이어서 백업/복원한다. OAuth 클라이언트가 등록돼 있지 않으면 로그인 결과가 **코드 10(DEVELOPER_ERROR)** 로 실패하며 앱이 그 사실을 토스트로 알린다 — 위 1~5를 끝내면 코드 없이 해결된다.

## 1. 빌드 규칙
| 항목 | 규칙 |
|---|---|
| `versionCode` (release만) | `origin/main` 커밋 수(스쿼시 머지 1 PR = +1). `-PversionCode=N`으로 덮어쓰기. git 실패·비정수면 빌드 실패(조용히 1로 떨어지지 않음). 업로드 시 GPP `AUTO`가 스토어 최대값+1로 재조정 |
| `versionName` (release만) | `1.0.<versionCode>` (`baseVersionName`은 `app/build.gradle.kts`). debug는 `1.0-dev` / code 1 고정(구성 캐시 보호) |
| release | `minify + shrinkResources` ON, 규칙은 `app/proguard-rules.pro` |
| 서명 | `keystore.properties`/`POH_*` 네 값이 **모두** 있으면 서명, 전부 없으면 미서명 빌드, 일부만 있으면 즉시 실패. `publish*` 태스크와 `-PrequireSigning`은 서명 필수 |

## 2. 배포 명령
```
# 검증 (하네스 기본 게이트)
./gradlew.bat testDebugUnitTest

# 서명된 AAB 생성 → app/build/outputs/bundle/release/app-release.aab
./gradlew.bat bundleRelease

# 내부 테스트 트랙 업로드 (play-service-account.json 필요)
./gradlew.bat publishReleaseBundle
```
- `bundleRelease`는 비밀번호를 `keystore.properties`/환경변수에서 비대화식으로 읽으므로 하네스가 실행해도 된다(없으면 미서명).
- `publishReleaseBundle`은 **스토어 업로드(외부 효과)** 이므로 사용자가 `!`로 직접 실행한다. 하네스는 호출하지 않는다.
- 업로드 후 Play 처리(수 분~수십 분)가 끝나면 테스터 기기에 자동 업데이트된다.
- R8 매핑은 AAB 안에 포함돼 Play가 자동으로 추출한다(별도 mapping.txt 업로드 없음). 로컬 사본은 `app/build/outputs/mapping/release/mapping.txt`.

## 3. 개발 중 실기기 확인
- 가장 빠른 경로: `adb install -r app/build/outputs/apk/debug/app-debug.apk` (USB 또는 무선 디버깅).
- Google Drive 복사는 옵트인: `local.properties`에 `google.drive.apk.dir=<경로>`를 넣은 머신에서만 동작한다.

## 4. 데이터 이전·백업
applicationId가 바뀌었으므로 `com.example.powerofhabit`로 설치된 기존(옛) 앱과는 **별개 앱**이다. Google Drive 백업/복원은 버튼을 누를 때 Google 로그인을 요청하며, §0-4의 OAuth 클라이언트 등록이 선행돼야 실제로 동작한다(미등록이면 코드 10으로 실패).

### 4-1. JSON 내보내기/가져오기 — 새 앱 ↔ 새 앱 (기기 교체·수동 백업용) ✅ 구현됨
설정 다이얼로그(홈 우상단 톱니) 하단의 **"파일로 내보내기" / "파일에서 가져오기"**(JSON). 시스템 파일 선택기(SAF)로 위치를 고르므로 권한이 필요 없다.
- 내보내기: 습관·기록·뱃지 전부를 `power-of-habit-<yyyyMMdd>.json`(formatVersion 1)으로 쓴다. 코드: `data/transfer/HabitTransfer.kt`(순수 Kotlin, JVM 테스트) + `TransferManager.kt`(SAF I/O).
- 가져오기는 **덮어쓰지 않는 병합**이다.
  - 습관: `(title, createdAt)`이 같으면 기존 습관으로 보고 id를 재사용, 아니면 새 습관으로 추가(id 재매핑).
  - 기록: `(habitId, date)` 기준. 파일 안 중복은 recordId가 큰 것을 택하고, **기존 DB에 같은 날 기록이 있으면 기존 것을 유지**한다. status 문자열은 해석 없이 그대로 옮긴다.
  - 뱃지: badgeId가 없는 것만 추가. 가져온 뒤 뱃지 재판정·Drive 자동 백업은 실행하지 않는다(다음 체크 때 정상 경로로 처리).
  - 트랜잭션이 아니므로 중간 실패 시 일부만 들어갈 수 있으나, 규칙이 멱등이라 같은 파일을 다시 가져오면 나머지만 채워진다.
- 파일의 `formatVersion`이 앱이 아는 값보다 크면 거부한다(앱 업데이트 후 재시도).
- 기기 교체 절차: 옛 기기 새 앱에서 내보내기 → 파일 전달(Drive·메신저 등) → 새 기기 새 앱에서 가져오기.

### 4-2. 옛 앱(`com.example.powerofhabit`) → 새 앱 — **미구현, 후속**
옛 앱에는 §4-1의 JSON 내보내기가 없고, 옛 앱을 재빌드·재배포하지 않는다(서명 키가 다르면 갱신 불가 = 데이터 소실 경로. 결정 기록: `docs/decisions/2026-09-05-value-3state-widget-tap-migration.md` 결정 3 — widget-fixes 브랜치에서 들어오는 문서).
- 옛 앱이 **debug 빌드**면 DB 파일을 뽑을 수는 있다:
  ```
  adb shell am force-stop com.example.powerofhabit
  adb shell run-as com.example.powerofhabit ls databases        # 목록이 보이면 debug 빌드
  adb exec-out run-as com.example.powerofhabit cat databases/power_of_habit.db     > old.db
  adb exec-out run-as com.example.powerofhabit cat databases/power_of_habit.db-wal > old.db-wal   # 있을 때만
  adb exec-out run-as com.example.powerofhabit cat databases/power_of_habit.db-shm > old.db-shm   # 있을 때만
  ```
  (`exec-out`은 바이너리를 그대로 전달한다 — `adb shell ... >`는 Windows에서 줄바꿈 변환으로 DB가 깨질 수 있다. DB 파일명 `power_of_habit.db`는 `di/DatabaseModule.kt` 기준. release 빌드면 `run-as`가 거부되며 루팅 없이는 꺼낼 수 없다.)
- 그러나 이 파일은 SQLite이고 **새 앱은 DB 파일 가져오기를 지원하지 않는다**(JSON만). 따라서 지금은 "뽑아 보관"까지만 가능하다.
- 후속 과제(택1): (a) 새 앱에 **DB 파일 가져오기**(SQLite 헤더·`user_version ≤ 현재`·`integrity_check` 통과 시에만, 기존 DB는 `pre_restore_<ts>.db`로 보존, 경고 다이얼로그) 추가, 또는 (b) PC에서 `old.db`를 §4-1 JSON 형식으로 변환하는 스크립트. 둘 중 하나가 들어가기 전까지 **옛 앱을 지우지 말 것.**

## 5. 체크리스트 (첫 내부 테스트 전)
- [ ] `release.jks` + `keystore.properties` 준비, 백업 완료
- [ ] `./gradlew.bat bundleRelease` 성공, 서명 확인(`apksigner verify` 또는 Play 업로드 통과)
- [ ] 릴리스 빌드 실기기 설치 후 스모크: 습관 추가·체크·상세·위젯 배치·CSV 내보내기·JSON 내보내기/가져오기(§4-1)·Drive 백업/복원(§0-4 OAuth 클라이언트 등록 후 — 로그인 요청 → 백업 → 복원 확인 다이얼로그 → 재시작)
- [ ] Play Console 앱 생성·내부 테스터 등록
- [ ] 첫 AAB 수동 업로드 → 이후 `publishReleaseBundle`
