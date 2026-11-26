# 비공개 테스트 → 2027-01-01 공개: 사실 · 일정 · 준비물 · 완성도 (2026-11-25)

사용자 순서 = [FIRST-UPLOAD-2026-11.md](FIRST-UPLOAD-2026-11.md) · 모집 글 = [TESTER-POST-2026-11.md](TESTER-POST-2026-11.md). 콘솔 쓰기 · 키 · 테스터 연락은 사용자 손이다.

## 1. Play · Google 요건 사실표
출처 번호 = `support.google.com/googleplay/android-developer/answer/<번호>?hl=en`(조회 2026-11-25 17:49 KST, curl 원문). 같은 요건의 문장 전체는 공개 레포 reading-diary `docs/store-listing/CLOSED-TEST-2026-11.md` §1.

| 물음 | 원문 | 출처 |
|---|---|---|
| 우리 계정에 적용되나 | «personal accounts created after November 13, 2023» → **적용된다고 보고 진행**(같은 계정의 다른 앱이 이 절차를 거침 — LC 확인. 계정 만든 날은 미대조) | 14151465 |
| 접근 요건 | «At least 12 testers must be opted in to your closed test when you apply for production access, and they must have been opted in continuously for the preceding 14 days.» | 14151465 |
| 나갔다 들어온 사람 | «If a tester opts out and opts back in later, the 14 days must be consecutive» | 14151465 |
| 비공개 시작 조건 | «You can start a closed test after completing your app setup.» | 14151465 |
| 내부 테스트 | «Internal testing is optional, but recommended as a starting point.» → 건너뛸 수 있다 | 14151465 |
| 접근 신청 검토 | «Review usually takes seven days or less, but can occasionally take longer.» | 14151465 |
| 첫 비공개 검토 | 비공개만의 말은 없음. «Processing can take a few hours or up to seven days (or longer in exceptional cases)» | 9859654 |
| 옵트인 링크 | «The opt-in link displays only when an app status is 'Published'.» | 9845334 |
| 새 빌드가 14일을 다시 세나 | **말이 없음**(세는 대상은 옵트인) | 14151465 |
| 검토 중 추가 변경 | «your app may be pushed to the back of the app review queue» | 9859654 |
| 처리방침 | «You must link to a privacy policy on your store listing and within your app» | 9859455 |
| 데이터 보안 폼 | 비공개 트랙도 필수, 내부만 쓰면 면제 | 10787469 |
| Drive 범위 | `drive.appdata` = «Non-sensitive scopes … recommended for most use cases» | developers.google.com/workspace/drive/api/guides/api-specific-auth |
| 앱 확인(OAuth) | «If your app utilizes only non-sensitive scopes, it is not mandatory for your app to complete the app verification process.» 이름 · 로고 표시는 «brand-verification» | support.google.com/cloud/answer/13463073 |
| 동의 화면 «테스트» | «limited to up to 100 test users» · «Authorizations by a test user will expire seven days» → 프로덕션으로 게시 | support.google.com/cloud/answer/15549945 |
| Drive 비용 | «Standard Tier: Enabled by default at no cost» · «Usage on this tier never produces charges on a Google Cloud bill» | developers.google.com/workspace/drive/api/guides/limits |
| 서명 지문 | «you must register the Google-held app signing key fingerprint with your API providers, not just your local upload key» · 새 앱은 «automatically enrolled in quantum-ready, hybrid signing» → **지문 3개가 기본** | 9842756 |
| 정확한 알람 | 안드로이드 14+ 새 설치에서 `SCHEDULE_EXACT_ALARM` «denied by default» | developer.android.com/about/versions/14/changes/schedule-exact-alarms |
| 기기 자동 백업 | «Every app can allocate up to 25 MB» · «does not count toward the user's personal Google Drive quota» | developer.android.com/identity/data/autobackup |

못 찾은 것: 연말 검토 지연 · 초안 상태에서 앱 서명 지문이 보이는지. 관리형 게시는 첫 공개에 쓸 수 없다(9859654 «You can't use it when publishing an app for the first time») — 비공개로 먼저 공개한 뒤의 프로덕션에서 되는지만 콘솔에서 확인.

## 2. 역산 일정표(검토마다 7일 · 모집 0일 가정)

**01-01은 보장선이 아니다.** 아래 역산 표는 검토가 각각 7일 안에 끝나고 12명이 검토 통과 당일에 모두 옵트인할 때만 맞는다. 모집에 하루 걸리면 01-02, 사흘 걸리면 01-04가 된다. 보내는 날이 하루 빠르면 공개도 하루 빠르다. 문서는 검토가 7일보다 더 걸릴 수 있다고 한다(9859654 «or longer in exceptional cases»). 01-01에 닿으려면 세 번의 검토 가운데 어디선가 모집에 쓸 날만큼 빨리 끝나야 한다.

닿는 날(11-27에 보낼 때 · 모집 3일 가정):

| | 첫 검토 통과 | 12명 | 접근 신청 | 프로덕션 제출 | 공개 |
|---|---|---|---|---|---|
| 검토 7일씩(더 길 수도 있다) | 12-04 | 12-07 | 12-21 | 12-28 | **01-04** |
| 검토 3일씩(가정 — 문서에 없는 값) | 11-30 | 12-03 | 12-17 | 12-20 | **12-23** |

아래는 01-01에서 거꾸로 센 «늦어도» 표다(모집 0일).

| 늦어도 | 단계 | 누가 |
|---|---|---|
| 11-26 | 키 · 콘솔 앱 · Cloud 프로젝트 / 처리방침 게시 승인 | 사용자 |
| 11-26 | 수리 PR(#42 머지됨) · 앱 안 주소 PR · 그림 | 워커 |
| **11-27** | 등록정보 · 앱 콘텐츠 · 서명 빌드 · Drive 등록 · **«검토로 보내기»** | 사용자(FIRST-UPLOAD 7 ~ 12 = 95분) |
| 12-04 | 검토 통과(최대 7일) → 본인 휴대폰 확인 → 모집 글 → **같은 날** 12명 옵트인(15 ~ 18명 모집) — 모집에 쓸 날이 0일 | 사용자 |
| 12-18 | 프로덕션 접근 신청(답안 틀 = RELEASE-PLAN §5) | 사용자 · 초안 워커 |
| 12-25 | 접근 승인 → 프로덕션 제출 | 사용자 |
| **01-01** | 공개 | |

- 보낼 수 있는 가장 이른 날은 11-26 밤이다(위 11-26 두 줄이 모두 끝날 때).
- 하루 늦게 보낼 때마다 이 표의 날짜도 하루 밀린다. 검토가 7일보다 빨리 끝난 만큼만 모집에 쓸 날이 생긴다(보장 없음).
- 가장 먼저 밀리는 단계 = 앱 설정 · 첫 제출(처리방침 주소가 맨 앞). 그때 버릴 것 = 실기기 15행 완주(테스트 기간으로) · 스크린샷 7장 → 4장 · 동의 화면 로고. 버리지 않을 것 = 처리방침 · 데이터 보안 · Drive 등록.
- 01-01을 놓치면 접근 승인 뒤 바로 프로덕션 검토를 보내고 통과하는 날 공개한다.

## 3. 등록 준비물 전수표
| 준비물 | 상태 | 어디 · 누가 |
|---|---|---|
| 앱 이름 | 확정 — 스토어 제목 `습관의 힘 - 하루 한 번 기록`(부제는 LC 가정), 런처 라벨 `습관의 힘` | §6 |
| 짧은 설명 · 자세한 설명 | 준비됨(처리방침 줄 1곳은 게시 뒤 확인) | `LISTING-2026-11.md` §2 · §3 |
| 카테고리 · 연락처 | 준비됨 — 생산성 · report@woodpecker-ai.com | `LISTING-2026-11.md` §4 · §5 |
| 아이콘 512 · 1024×500 · 스크린샷 4장 이상 | 아이콘 · 이름 박힌 1024×500(`feature-1024x500.png`, 24비트) · 스크린샷 4장(1080×1920 24비트, 홈 = «습관의 힘»)은 만듦 | 사용자 PC 그림 폴더(레포에 커밋하지 않는다 — LISTING §6 · §7의 커밋 경로 대신) |
| 처리방침 본문 | 준비됨(시행일은 게시하는 날) | `docs/privacy/privacy-policy.html` |
| 처리방침 공개 주소 | **사용자 승인 대기** → 워커 게시 | `docs/privacy/DEPLOY.md` |
| 앱 안 처리방침 링크 | 링크는 #42(머지됨) · 주소 1줄은 게시 뒤 PR | 워커 |
| 데이터 보안 답안(Drive 포함) | 준비됨 | `DATA-SAFETY-2026-11.md` §1 |
| 앱 콘텐츠(광고 · 앱 액세스 · 뉴스 · 정부 · 금융 · 건강 · 광고 ID) | 준비됨 | `DATA-SAFETY-2026-11.md` §2 |
| 콘텐츠 등급 · 타겟 연령 | 준비됨 — 18세 이상만 | `LISTING-2026-11.md` §9 · §10 |
| 서명 AAB · Drive 클라이언트 · 구글 그룹 | 사용자 손 | FIRST-UPLOAD 5 · 9 · 11 |
| 모집 글 | 준비됨 | `TESTER-POST-2026-11.md` |
| 접근 신청서 | 틀 준비됨(숫자는 콘솔 실측) | `docs/RELEASE-PLAN-2026-11.md` §5 |

## 4. 완성도 — 처음 쓰는 사람의 한 바퀴
이 PC에는 에뮬레이터 시스템 이미지 · AVD가 0개다. 그래서 실제 액티비티 · Hilt · Room을 JVM에서 돌리는 화면 시험(Robolectric)으로 «설치 → 첫 습관 → 기록 → 다시 열기 → 수정 → 삭제»를 돈다(`FirstRunJourneyTest` · 결과는 #42 본문). **위젯 · 실제 알림 · Drive 로그인은 이 시험이 못 본다** — FIRST-UPLOAD 13단계에서 처음 본다.

| 갈래 | 항목 | 상태 |
|---|---|---|
| 출시 전 반드시 | 앱이 메모리에 남은 채 날이 바뀌면 «오늘» 칸이 어제로 남아 어제에 기록된다(홈이 날짜를 한 번만 읽음) | #42 머지 |
| 출시 전 반드시 | 수정 화면을 열었다 나온 뒤 «만들기»에 옛 값이 남을 수 있다(화면들이 ViewModel 하나를 나눠 씀) | #42 머지 |
| 출시 전 반드시 | 수정 화면 휴지통이 묻지 않고 습관과 기록을 지운다 · 삭제 뒤 «저장되었습니다»가 뜬다 | #42 머지 |
| 출시 전 반드시 | 저장을 빠르게 두 번 누르면 습관이 두 개 생길 수 있다 | #42 머지 |
| 출시 전 반드시 | 첫 화면 «No habits. Tap + to add one!» · 알림 채널 · 등록 화면 오류 문구가 영어 | #42 머지 |
| 출시 전 반드시 | 앱 안 처리방침 링크 없음(9859455) | #42 머지(주소는 게시 뒤) |
| 출시 전 반드시 | 안드로이드 14+ 새 설치에서 절전 중 알림이 밀린다 · 복원이나 강제 종료 뒤 알림이 사라진다 | #42 머지 |
| 출시 전 반드시 | 설정 창이 작은 화면 · 큰 글꼴에서 잘리고 스크롤이 안 된다 · 등록 화면 아래 칸이 키보드에 가린다 | #42 머지 |
| 출시 전 반드시 | 테마 스위치가 실제 화면과 다르다(라이트인데 켜짐) | #42 머지 |
| 출시 전 반드시 | Drive 백업 · 복원을 한 번도 실제로 돌린 적이 없다(R8 빌드 포함) | FIRST-UPLOAD 11 · 13(사용자) |
| 출시 전 반드시 | 뒤로를 빠르게 두 번 누르면 첫 화면까지 닫혀 앱이 죽을 수 있다(백스택이 빔 · critic) | #42 머지 |
| 출시 전 반드시 | 상세에서 기록을 고치는 도중 화면을 닫으면 지우기만 되고 끝날 수 있다(critic) | #42 머지 |
| 테스트 중 | 상세 화면은 켜 둔 채 날이 바뀌면 통계 날짜가 늦게 바뀜 · 테마를 «시스템»으로 되돌릴 수 없음(critic) | 남음 |
| 테스트 중 | 새 폰에서 복원보다 «백업하기»를 먼저 누르면 옛 백업을 덮는다(묻지 않음) | 남음 — 앞쪽 |
| 테스트 중 | 복원 실패는 원인이 네트워크여도 «올바른 백업 파일이 없습니다»로 뜬다 | 남음 |
| 테스트 중 | 쓰는 중인 DB를 파일째 압축한다(-wal 포함 · 쓰기와 겹치면 어긋날 수 있음) | 남음 |
| 테스트 중 | 등록 화면에서 회전하면 입력이 사라진다 · 뒤로 갈 때 묻지 않는다 | 남음 |
| 테스트 중 | 이미 한 날에도 · 쉬는 주기에도 알림이 온다 | 남음 |
| 테스트 중 | 홈에서 «안 적은 날»과 «미완료»가 같은 ×로 보인다 | 남음(디자인 결정) |
| 테스트 중 | 방금 만든 습관인데 만들기 전 날짜에도 ×가 보인다(한 바퀴 캡처에서 봄) | 남음 |
| 테스트 중 | 기록 저장 실패가 화면에 보이지 않는다(로그만) | 남음 |
| 테스트 중 | 이하 목표에도 «↑» · «옛 앱 DB 파일 가져오기»가 일반 사용자에게 보인다 | 남음 |
| 테스트 중 | 라이트 테마 오렌지 글자 대비(2.1:1 관찰) | 남음(DEVICE-QA 15행) |
| 테스트 중 | 강제 종료 뒤 알람 재예약이 PendingIntent 잔존으로 건너뛸 수 있다(추정 · 미확인 — 게이트 관찰) | 남음 |
| 테스트 중 | 습관을 지우는 도중 뒤로 가면 지워진 습관의 알림이 한 번 울릴 수 있다 | 남음 |
| 테스트 중 | 처리방침 주소 검사가 문자열 일치이고 서명 APK(assembleRelease)는 막지 않는다 · 실제로 걸리는지 안 봄 | 남음 |
| 테스트 중 | Drive 백업 · 복원 한 바퀴와 며칠 치 기록은 시험에 없다 · 홈 행을 빠르게 두 번 눌러 같은 상세가 두 번 열리는 경우 | 남음 |
| 테스트 중 | 뒤로 두 번 · 저장 두 번 방어는 시험이 없다(뒤로 두 번에 앱이 죽는 것은 코드 판독 · 미재현) | 남음 |
| 테스트 중 | 점수 그래프 아래 날짜 글자가 서로 붙고 마지막 하나가 두 줄로 깨진다 · 등록/수정 화면 «색» 라벨 위가 잘린다(360dp 스크린샷에서 봄) | 남음 |
| 테스트 중 | 홈 · 상세 불러오기 실패와 파일 가져오기/내보내기 실패에 예외 원문(영어일 수 있음)이 그대로 보인다 | 남음 |
| 1.0 뒤 | GoogleSignIn → Credential Manager 이전 · Compose 낡은 API(Divider 등) | 남음 |

**DEVICE-QA 15행(닫힌 것 0):** 01 · 02 · 03 · 04 · 05 · 08 · 09 · 15 = 테스트 첫날 본인 휴대폰 / 06 · 07 위젯 · 10 · 11 · 12 알림 = 일부만 FIRST-UPLOAD 13의 3(위젯 1개 · 알림 1번)에서 보고 나머지는 테스트 기간 / 13 Drive = 모집 글 전 필수 / 14 마이그레이션 = 2번째 빌드 때.

## 5. 출시 노트(첫 비공개 빌드)
```
<ko-KR>
첫 비공개 테스트 버전입니다.
- 습관을 등록하고 하루 한 번 눌러 기록할 수 있어요.
- 기록은 점수 · 연속 기록 · 달력과 홈 화면 위젯으로 볼 수 있어요.
</ko-KR>
```

## 6. 사용자가 고를 것
1. ~~앱 이름~~ — **«습관의 힘» 그대로**(사용자 2026-11-25 «habit은 원래대로 '습관의 힘'으로 가자»). 스토어 제목은 `습관의 힘 - 하루 한 번 기록`(17/30, LC 가정 — «부제 없이»라는 답이 오면 되돌린다), 런처 라벨은 «습관의 힘» 그대로.
   - 참고(바꾸지 않음): Play 검색(2026-11-25 · ko/KR 첫 화면)에 «습관의 힘:일일 플래너» · «아주 작은 습관의 힘 - 1분 미세 루틴 트래커»가 있고 널리 알려진 번역서 제목과도 같다. 스토어 문안에는 책 · 저자를 언급하지 않는다(상표 조사는 하지 않았다).
   - 앱 이름이 바뀌지 않으므로 `strings.xml` · `MainScreen.kt` · 처리방침 · `TESTER-POST` · `DRIVE-OAUTH-CHECKLIST`의 «습관의 힘»은 그대로다. 나중에 이름을 바꾸면 git grep «습관의 힘»으로 다시 찾는다(`docs/RELEASE.md` · `RELEASE-PLAN` · `DEVICE-QA` 포함).
2. ~~allowBackup~~ — **그대로(켜짐)** 로 정함(LC 가정 2026-11-25, 사용자 통보). 새 폰을 구글 계정으로 복원하면 기록이 따라온다.

## 7. 로그인 세부정보(앱 콘텐츠 · 2026-11-26 저장본 — 사용자가 LC 다듬은 문안으로 직접 저장)
- 선언: «예 — 앱의 일부 기능에 로그인 필요»(선택 기능인 Drive 백업 때문). 검토용 Google 계정 아이디 · 비밀번호는 **사용자가 콘솔 칸에 직접** 넣는다 — 이 문서 · 채팅 · 커밋에 적지 않는다.
- 이름(40/60):
```
Google account for optional Drive backup
```
- 기타 정보(486/500):
```
Sign-in is optional. The app is fully usable without an account. Google sign-in is used only for the optional Google Drive backup and restore. To test it: tap the gear icon (Settings, the rightmost icon) at the top right of the home screen, find the backup buttons, tap "백업하기" (Back up), sign in with the Google account provided above and allow access. "복원하기" (Restore) restores that backup and restarts the app. No other screen requires sign-in. The account has no 2-step verification.
```
- 계정: 검토 전용 Google 계정(2단계 인증 없음) · «전체 액세스 권한» 체크 — 값은 콘솔에만 있다.
- 순서 잠금: 타겟층 설문은 이 섹션을 저장한 뒤에 열린다(화면 «로그인 세부정보 섹션 완료»).
