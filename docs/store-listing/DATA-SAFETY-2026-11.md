# 데이터 보안 폼 답안 · 앱 콘텐츠 선언 · 권한 실측 — 습관의 힘 (2026-11)

> 상태: **답안 초안**(2026-11-02, 클라우드 세션 — 콘솔 미반영). 블록 B에서 사용자가 콘솔에 입력한다.
> 기준: **Drive 백업 v1 포함**(LC 10-03 결정). 처리방침 [`docs/privacy/privacy-policy.html`](../privacy/privacy-policy.html), 등록정보 [`LISTING-2026-11.md`](LISTING-2026-11.md)와 문장이 어긋나면 셋을 같이 고친다.
> 판정 원칙: Play 정의상 «수집 = 앱이 데이터를 기기 밖으로 전송». 개발자 서버가 없더라도 Drive API로 보내는 것은 기기 밖 전송이므로 **수집 = 예(선택)** 로 보수 답한다.

## 1. 데이터 보안 폼 답안

### 1-1. 개요 문항
| 문항 | 답 | 근거 |
|---|---|---|
| 앱이 필수 사용자 데이터 유형을 수집하거나 공유하나요? | **예** | Drive 백업 시 습관 DB가 기기 밖(사용자 본인 Drive)으로 전송됨 — `backup/GoogleDriveBackupManager.kt:191-237`(`backupDatabase`: DB zip → `files().create/update`) |
| 수집한 모든 사용자 데이터가 전송 중에 암호화되나요? | **예** | Drive API 클라이언트가 `https://www.googleapis.com`로만 통신(`NetHttpTransport`, `GoogleDriveBackupManager.kt:174-181`). 개발자 서버 통신 없음 |
| 사용자가 데이터 삭제를 요청할 수 있는 방법을 제공하나요? | **예** | 사용자가 직접 삭제: ① 앱에서 습관 삭제 ② Google Drive 웹 → 설정 → 앱 관리 → «습관의 힘» → 옵션 → «숨겨진 앱 데이터 삭제» ③ 앱 삭제 시 기기 데이터 삭제. 처리방침 §4·§6에 같은 경로 |
| 앱에서 계정을 만들 수 있나요? | **아니요**(계정 삭제 URL 해당 없음) | 회원가입·앱 계정 없음. Google 로그인은 Drive `drive.appdata` 권한 승인용(`GoogleDriveBackupManager.kt:101-108`)이며 앱 계정을 만들지 않는다 |
| 독립 보안 검토(MASA) | 아니요 | — |

### 1-2. 데이터 유형별
| 데이터 유형(콘솔 분류) | 수집 | 공유 | 일시 처리 | 필수/선택 | 목적 | 근거·메모 |
|---|---|---|---|---|---|---|
| 앱 활동 → **기타 사용자 생성 콘텐츠**(습관·기록·뱃지 DB) | **예** | **아니요** | 아니요 | **선택**(사용자가 Drive 백업을 켤 때만) | **앱 기능**(백업·복원) | 저장 위치 = 사용자 본인 Google 계정 Drive `appDataFolder`(`GoogleDriveBackupManager.kt:59-60, 211-221`). 개발자 접근 불가. 연결된 동안 기록 변경 5초 뒤 자동 백업(`:159-170`) — 처리방침 §4에 명시 |
| 개인 정보 → 이메일 주소 | **아니요** | 아니요 | — | — | — | Google 로그인 결과의 이메일을 설정 화면 표시(`GoogleDriveBackupManager.kt:94` `signedInEmail`)에만 쓰고 앱이 기기 밖으로 보내지 않는다. 백업 zip에도 없다. ⚠ 판정이 갈릴 수 있음 — 콘솔 도움말이 «Google 로그인으로 받은 이메일»을 수집으로 본다고 하면 «예·선택·앱 기능»으로 바꾼다 |
| 위치·연락처·사진·건강·금융·메시지·기기 ID·앱 정보와 성능(크래시 로그)·웹 기록 | 아니요 | 아니요 | — | — | — | 해당 권한·SDK 없음(§4 권한표, `app/build.gradle.kts:170-234` 의존성에 분석·광고·크래시 SDK 0) |

### 1-3. «공유 = 아니요»의 근거
- Play 예외: **사용자가 시작한 동작**으로 사용자가 예상하는 곳에 보내는 전송은 공유가 아니다. Drive 백업은 사용자가 버튼으로 켜고, 목적지가 **사용자 본인 계정**이다.
- 파일 내보내기(JSON·CSV): 시스템 파일 선택기/공유 시트에서 사용자가 대상을 고른다(`data/transfer/TransferManager.kt`, `ui/screens/HabitDetailScreen.kt:762-790`) → 앱의 전송이 아니므로 수집·공유 모두 해당 없음.
- 개발자 서버·제3자 SDK로의 전송: 0.

## 2. 앱 콘텐츠 선언표 (콘솔 «앱 콘텐츠»)

| 선언 | 답 | 근거·메모 |
|---|---|---|
| 개인정보처리방침 | Vercel 실측 URL(`docs/privacy/DEPLOY.md` §4) | 꺾쇠 금지 · 배포 후 200 확인 |
| **광고** | **아니요, 광고 없음** | 광고 SDK 0(`app/build.gradle.kts` 의존성) |
| 광고 ID(AD_ID) | **사용 안 함** | 광고·분석 SDK 0. 병합 매니페스트에 `com.google.android.gms.permission.AD_ID`가 없는지 §4-3 명령으로 확인(있으면 별도 코드 PR로 제거) |
| **앱 액세스** | **모든 기능을 특별한 액세스 없이 이용 가능**(제한 없음) | 로그인 없이 전 기능 사용. Drive 백업은 **아무 Google 계정**으로나 검토 가능하므로 심사용 자격 증명 불필요 |
| 콘텐츠 등급 | IARC 설문 → `LISTING-2026-11.md` §9 | 예상 전체이용가(추정) |
| 타겟층 및 콘텐츠 | 18세 이상 · 아동 관심 유도 아님 | `LISTING-2026-11.md` §10 |
| 데이터 보안 | 이 문서 §1 | |
| **뉴스 앱** | **해당 없음**(뉴스 앱 아님) | |
| **정부 앱** | **해당 없음** | |
| **금융 기능** | **해당 없음**(금융 기능 없음) | 결제·대출·투자 기능 0 |
| **건강** | **해당 없음**(건강 앱 아님) | 건강·의료 데이터·센서 0. 카테고리를 «생산성»으로 두는 이유(`LISTING-2026-11.md` §4) |
| 코로나19 접촉 추적 | 해당 없음(문항이 보이면) | |
| 포그라운드 서비스 권한 | **해당 없음(추정)** | 앱 코드에 포그라운드 서비스 0. 의존성(WorkManager)이 `FOREGROUND_SERVICE`를 더할 수 있으나 유형별 권한(`FOREGROUND_SERVICE_*`)은 없을 것으로 추정 → 콘솔이 선언을 요구하면 §4-3 결과로 답한다 |
| 정확한 알람 | 선언 대상 아님(추정) | `SCHEDULE_EXACT_ALARM`(사용자 허용형)만 사용. 제한 권한 `USE_EXACT_ALARM`은 쓰지 않음(`AndroidManifest.xml:6`) |

⚠ **Android 시스템 자동 백업**: `AndroidManifest.xml:11`(11-02 `INTERNET` 추가로 :10→:11) `android:allowBackup="true"`이고 `res/xml/backup_rules.xml`·`data_extraction_rules.xml`은 매니페스트에 연결돼 있지 않다(둘 다 샘플 그대로). 따라서 사용자가 기기 Google 백업을 켜 두면 OS가 앱 DB를 사용자 계정으로 백업한다. 이 경로는 OS·Google이 처리하며 처리방침 §5에 고지했다. 데이터 보안 폼에서 이 경로를 따로 선언해야 하는지는 **콘솔 도움말 확인 필요** — 앱 자체 Drive 백업을 이미 «수집 = 예»로 선언하므로 결과 답은 같다.

## 3. 선언과 모순되면 안 되는 문구 (등록정보·처리방침 공통)
| 쓰지 않는다 | 이유 | 대신 |
|---|---|---|
| «어떤 데이터도 수집하지 않습니다» | §1 «수집 = 예(선택)»과 모순 | «개발자 서버로 보내지 않습니다» |
| «완전 오프라인 앱» | Drive 백업·로그인에 네트워크 사용 | «기록은 기기 안에 저장» |
| «Drive 전체에 백업» | 범위는 `drive.appdata`(앱 전용 숨김 공간)뿐 | «본인 Google Drive의 앱 전용 숨김 공간» |
| «암호화 저장» | 기기 DB·백업 zip은 암호화하지 않는다(전송만 HTTPS) | «암호화된 연결(HTTPS)로 전송» |

## 4. 병합 매니페스트 권한 실측표

### 4-1. 측정 방법과 한계
- 이 클라우드 VM은 그래들 빌드를 하지 않는다(HO 규약 6). 의존성 AAR 매니페스트를 직접 열어 보려 했으나 `maven.google.com`·`dl.google.com`이 프록시에서 403/연결 거부(2026-11-02 `curl` 실측) → **라이브러리 기여분은 전부 «추정»**이다.
- 실측 가능한 것: 앱 소스 매니페스트 선언 4개(11-02 `INTERNET` 추가 뒤), 의존성 선언 줄, 앱 코드의 네트워크 사용.

### 4-2. 표
| 권한 | 출처 | 근거(파일:줄) | 판정 |
|---|---|---|---|
| `RECEIVE_BOOT_COMPLETED` | 앱 선언 | `app/src/main/AndroidManifest.xml:4` · 사용처 `reminder/BootReceiver.kt` | **실측** |
| `POST_NOTIFICATIONS` | 앱 선언 | `AndroidManifest.xml:5` · 런타임 요청 `ui/screens/AddEditHabitScreen.kt:550-554` | **실측** |
| `SCHEDULE_EXACT_ALARM` | 앱 선언 | `AndroidManifest.xml:6` · `reminder/HabitReminderManager.kt:54-68`(`canScheduleExactAlarms` 확인 후 `setExactAndAllowWhileIdle`) | **실측** |
| `WAKE_LOCK` | `androidx.work:work-runtime`(Glance 전이 의존) | `app/build.gradle.kts:206` `glance.appwidget` · `gradle/libs.versions.toml:21` glance 1.2.0 | 추정 |
| `ACCESS_NETWORK_STATE` | work-runtime(전이) | 같음 | 추정 |
| `FOREGROUND_SERVICE` | work-runtime(전이) | 같음 | 추정 |
| `RECEIVE_BOOT_COMPLETED`(중복 병합) | work-runtime(전이) | 같음 | 추정 |
| `com.woodpeckerai.powerofhabit.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`(signature, 자체 정의) | `androidx.core` 1.9+ | `app/build.gradle.kts:175` `core.ktx` | 추정 |
| **`INTERNET`** | 앱 선언(명시, 11-02) — `AndroidManifest.xml:7`. play-services-auth 20.7.0(`build.gradle.kts:227`) · google-api-client-android 2.2.0(`:231`) · google-api-services-drive(`:228`)가 같은 권한을 더해도 병합 시 중복은 무해 | 앱 코드는 `NetHttpTransport`로 Drive REST 호출(`GoogleDriveBackupManager.kt:174-181`) → **필요** | **선언(명시, 11-02)** — 일반 권한(위험 권한 아님)이라 데이터 보안 답 불변 |
| `GET_ACCOUNTS` | play-services-auth | `build.gradle.kts:227` | 추정 **추가 안 함** — `GoogleSignIn` API는 계정 목록 권한이 필요 없고, `GoogleAccountCredential.setSelectedAccount(account.account)`로 계정을 직접 지정(`GoogleDriveBackupManager.kt:175-177`) |
| `com.google.android.gms.permission.AD_ID` | (광고 식별자 라이브러리) | 의존성에 `play-services-ads*`·`-measurement` 없음 | 추정 **없음** |

✅ **`INTERNET` 해소(11-02)**: 소스 매니페스트에 명시 선언했다(`AndroidManifest.xml:7`, 클라우드 13건째 PR). 이전에는 저장소 이력 전체에서 `INTERNET` 0건이었고 Drive HTTP 경로는 한 번도 실행된 적이 없었다(WORKLOG 09-05·09-06 blockers). 병합 결과 실측은 그대로 §4-3을 블록 A 직후 1회 실행해 «추정» 칸을 바꾼다.

### 4-3. 로컬 확인 명령 (블록 A 직후, Windows Git Bash)
```bash
./gradlew.bat :app:processReleaseMainManifest
```
```bash
grep -rho 'android:name="[a-zA-Z0-9_.]*permission[a-zA-Z0-9_.]*"' $(find app/build/intermediates -path '*release*' -name AndroidManifest.xml | grep -i merged) | sort -u
```
- 권한별로 어느 라이브러리가 더했는지는 `app/build/outputs/logs/manifest-merger-release-report.txt`에서 `uses-permission`을 찾으면 나온다.
- 결과로 §4-2 «추정» 칸을 «실측»으로 바꿔 커밋한다.
