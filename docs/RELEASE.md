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

## 1. 빌드 규칙
| 항목 | 규칙 |
|---|---|
| `versionCode` | main 커밋 수(`git rev-list --count HEAD`) — 자동 단조 증가. `-PversionCode=N`으로 덮어쓰기 가능 |
| `versionName` | `1.0.<versionCode>` (`baseVersionName`은 `app/build.gradle.kts`에서 관리) |
| release | `minify + shrinkResources` ON, 규칙은 `app/proguard-rules.pro` |
| 서명 | `keystore.properties`/환경변수가 있으면 release에 자동 적용, 없으면 미서명 빌드 |

## 2. 배포 명령
```
# 검증 (하네스 기본 게이트)
./gradlew.bat testDebugUnitTest

# 서명된 AAB 생성 → app/build/outputs/bundle/release/app-release.aab
./gradlew.bat bundleRelease

# 내부 테스트 트랙 업로드 (play-service-account.json 필요)
./gradlew.bat publishReleaseBundle
```
- 하네스 루프에서 `bundleRelease`·`publishReleaseBundle`은 키스토어 비밀번호가 필요한 단계이므로 **사용자가 `!`로 직접 실행**한다(CLAUDE.md §1 예외).
- 업로드 후 Play 처리(수 분~수십 분)가 끝나면 테스터 기기에 자동 업데이트된다.
- R8 매핑 파일 `app/build/outputs/mapping/release/mapping.txt`는 GPP가 함께 업로드한다(크래시 난독화 해제용).

## 3. 개발 중 실기기 확인
- 가장 빠른 경로: `adb install -r app/build/outputs/apk/debug/app-debug.apk` (USB 또는 무선 디버깅).
- Google Drive 복사는 옵트인: `local.properties`에 `google.drive.apk.dir=<경로>`를 넣은 머신에서만 동작한다.

## 4. 기존 설치 데이터 이전
applicationId가 바뀌었으므로 `com.example.powerofhabit`로 설치된 기존 앱과는 **별개 앱**이다. 기존 데이터는 앱 내 백업(Google Drive) → 새 앱에서 복원으로 옮긴다. 이전이 끝나면 예전 앱은 삭제한다.

## 5. 체크리스트 (첫 내부 테스트 전)
- [ ] `release.jks` + `keystore.properties` 준비, 백업 완료
- [ ] `./gradlew.bat bundleRelease` 성공, 서명 확인(`apksigner verify` 또는 Play 업로드 통과)
- [ ] 릴리스 빌드 실기기 설치 후 스모크: 습관 추가·체크·상세·백업/복원·위젯 배치
- [ ] Play Console 앱 생성·내부 테스터 등록
- [ ] 첫 AAB 수동 업로드 → 이후 `publishReleaseBundle`
