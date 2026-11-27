# 출시 계획 — 무료 프로덕션 출시 (2026-11 재역산판)

> **2026-11-25 갱신**: 목표가 «새해 공개(늦어도 2027-01-01)»로 바뀌었다(사용자 11-25). 사용자 순서의 정본 = [`store-listing/FIRST-UPLOAD-2026-11.md`](store-listing/FIRST-UPLOAD-2026-11.md), 사실 · 일정 · 준비물의 정본 = [`store-listing/CLOSED-TEST-2026-11.md`](store-listing/CLOSED-TEST-2026-11.md). 아래 블록 표의 소요 · 증거 이름은 참고로 남긴다.

> 작성 2026-11-02(클라우드 세션, 코드 0). 정본 결정: LC `PLAN-2026-1003-portfolio-implementation.md` §5 · `MEETING-2026-1003-power-of-habit.md`(이 문서에는 HO 인라인으로 받은 결정만 옮겼다).
> 이 문서는 **계획·체크리스트**다. 콘솔·키스토어·Cloud Console·GitHub Pages 조작은 전부 사용자 손이다.
> 절차의 정본은 [`docs/RELEASE.md`](RELEASE.md)이며, 첫 올리기는 FIRST-UPLOAD가, 그 밖은 RELEASE.md가 우선한다.

## 0. 이미 결정된 것 (다시 묻지 않는다)

| 항목 | 결정 | 근거 |
|---|---|---|
| 출시 형태 | **무료 프로덕션 출시**, 수익 0(광고·인앱결제 없음) | 사용자 답 10-03 |
| 시한 | ~~2026-12-31 게시~~ → **새해 공개, 늦어도 2027-01-01** | 사용자 답 10-03 → 11-25 |
| Drive 백업 | **v1 포함** — 회의 F4(Drive v1 제외)는 LC가 기각. 블록 D(OAuth) 필수 | LC 10-03 |
| 처리방침 호스팅 | ~~Vercel~~ → GitHub Pages `gh-pages` 한 파일(사용자 승인 뒤) | F5 → LC 가정 11-25 |
| 식별자 | applicationId `com.woodpeckerai.powerofhabit`(변경 불가) · Kotlin 패키지 `com.example.powerofhabit` 유지 | `app/build.gradle.kts:80` · RELEASE.md 머리말 |
| 재검토 조항(F6) | 게시 +90일에 설치 ≥300 **그리고** 평점 ≥4.0 이면 회의. 선택지에 «추가 없음» 상시 포함 | F6 기본값 |

## 1. DoD 9항 체크리스트

«완료 = ⑨까지». ①~⑤는 `docs/RELEASE.md` §5 «첫 내부 테스트 전» 5항을 그대로 옮겼다.

| # | 항목 | 완료 증거 | 현재 상태(11-25) | 담당 블록 |
|---|---|---|---|---|
| ① | `release.jks` 준비·백업 | `keytool -list -v` SHA-1 캡처, 백업 위치 메모 | ⬜ 미착수 — WORKLOG 09-06 «키스토어·Play Console·OAuth 클라이언트(사용자 몫) 대기» | A |
| ② | `./gradlew.bat bundleRelease` **서명** 성공 | exit 0 로그 + Play 업로드 통과(또는 `jarsigner -verify`) | 🟡 부분 — **미서명** R8 AAB는 09-06 성공(WORKLOG 09-06 progress). 서명본은 ① 뒤 | A→C |
| ③ | 릴리스 빌드 실기기 스모크 | [`DEVICE-QA-2026-11.md`](DEVICE-QA-2026-11.md) 15행 전부 결과·캡처 | ⬜ 틀만 있음(이 PR) | C 뒤(사용자 기기) |
| ④ | Play Console 앱 생성 · 내부 테스터 등록 | 콘솔 «앱 만들기» 완료 캡처 · 앱 콘텐츠 선언 완료 캡처 | ⬜ 미착수 | B |
| ⑤ | 첫 AAB 수동 업로드(내부 테스트) → 이후 `publishReleaseBundle` | 내부 테스트 «게시됨» 캡처 | ⬜ 미착수 | C |
| ⑥ | 비공개 테스트 12명 × 14일 | 콘솔 «프로덕션 액세스» 카드 3요건 취소선 캡처 | ⬜ **적용된다고 보고 진행**(같은 계정의 다른 앱이 거침 — LC 확인). 콘솔 과제 표시로 마지막 확인 | E |
| ⑦ | 프로덕션 액세스 신청(3부 답안) | 콘솔 «신청 접수» 캡처 | ⬜ 답안 틀은 §5(이 문서) | F |
| ⑧ | 프로덕션 심사 → 게시 | 콘솔 «게시됨» 캡처 | ⬜ | G |
| ⑨ | 스토어 URL 200 · Android 개발자 인증 «등록됨» | `curl -s -o /dev/null -w "%{http_code}" "https://play.google.com/store/apps/details?id=com.woodpeckerai.powerofhabit"` → `200`(비로그인) · 콘솔 앱 상태 «등록됨» 캡처 | ⬜ 게시 전 = 404가 정상 | G 뒤 |

## 2. 사용자 손 7블록 (각 ≤30분 · 증거 = 캡처 또는 exit 0)

증거 캡처 폴더: «사용자 PC 그림 폴더» (파일명은 아래 열 그대로).

| 블록 | 내용 | 선행 | 소요 | 증거 파일명 | 참고 문서 |
|---|---|---|---|---|---|
| **A** | 릴리스 키스토어 생성 → 사본 두 곳 → 서명 빌드(비밀번호를 파일에 적지 않음 — FIRST-UPLOAD 9단계) | 없음 | 20분 | `A-1-keytool-sha1.png`(SHA-1 줄, 비밀번호 화면 제외) · `A-2-bundleRelease-exit0.png` | RELEASE.md §0-1, §2 |
| **B** | 처리방침 배포 → Play 콘솔 **앱 만들기**(«습관의 힘»·한국어·앱·무료) → 앱 콘텐츠 선언 → 등록정보 붙여넣기 | 처리방침 URL 200(`docs/privacy/DEPLOY.md`) · 그래픽 렌더(로컬 세션, LISTING §6·§7) | 30분 | `B-1-privacy-url-200.png` · `B-2-app-created.png` · `B-3-app-content-done.png` · `B-4-listing-saved.png` | RELEASE.md §0-2 · `store-listing/LISTING-2026-11.md` · `store-listing/DATA-SAFETY-2026-11.md` |
| **C** | 서명 AAB 첫 **수동** 업로드(내부 테스트) → 본인 계정 설치 → 실기기 QA 13행 제외 전부 | A · B | 30분(+Play 처리 대기) | `C-1-internal-published.png` · `C-2-installed-version.png` | RELEASE.md §0-2-3, §5 · `DEVICE-QA-2026-11.md` |
| **D** | Drive OAuth Android 클라이언트(업로드 키 SHA-1 + **Play 앱 서명 키 SHA-1 3개** — 새 앱은 hybrid signing이 기본) · 동의 화면 «프로덕션» · Drive API 사용 | C(앱 서명 키 SHA-1은 첫 업로드 뒤에야 보인다) | 25분 | `D-1-oauth-clients.png` · `D-2-consent-production.png` · `D-3-drive-backup-ok.png` | `DRIVE-OAUTH-CHECKLIST.md` |
| **E** | 비공개 테스트 트랙(알파) 출시 → 테스터 12명 이상 옵트인(T0) | C · (D 권장 — 테스터 기간에 Drive도 검증) · ⑥ 적용 확정 | 30분 | `E-1-closed-track-released.png` · `E-2-optin-12.png` | `store-listing/FIRST-UPLOAD-2026-11.md` 5 · 10단계(Google 그룹) |
| **F** | 프로덕션 액세스 신청서 제출(3부) | E + 14일(콘솔 카드 3요건 완료) | 25분 | `F-1-application-submitted.png` | §5 답안 틀 |
| **G** | 프로덕션 승급(비공개 버전 그대로) → 심사 → 게시 → ⑨ 확인 | F 승인 메일 | 15분(+심사) | `G-1-production-submitted.png` · `G-2-published.png` · `G-3-store-200.txt` | — |

**allowBackup = 그대로 켜 둔다(LC 가정 2026-11-25, 사용자 통보)**. 당시 상태: 현재 `AndroidManifest.xml:11` `allowBackup="true"`, `backup_rules.xml`·`data_extraction_rules.xml` 미연결(샘플 그대로). 선택지: 그대로 둠 / 규칙 파일 연결 / `false`. 답 1줄이면 별도 코드 PR(`DATA-SAFETY-2026-11.md` §2 주석).

| 항목 | 내용 | 시점 | 증거 |
|---|---|---|---|
| **allowBackup 결정** | 그대로(켜짐) — LC 가정, 뒤집히면 코드 PR | 11-25 | LC REPLY 17:53 |

⑥이 **적용 안 됨**으로 확정되면 E·F를 지우고 C 직후 G로 간다(아래 §3 일정은 최악 경로 기준).

## 3. 재역산 일정(2026-11-25)

### 3-1. 근거 수치
공식 문서 값만 쓴다([`CLOSED-TEST-2026-11.md`](store-listing/CLOSED-TEST-2026-11.md) §1). 다른 앱의 실측 수치는 공개 레포에 두지 않는다(2026-11-25 지움). 연말 심사 지연은 수치를 못 찾았다.

### 3-2. 일정표(2026-11-25 다시 세움 — 11-02판 날짜는 모두 지났거나 폐기)

목표 = **2027-01-01 공개**. 표와 근거의 정본은 [`store-listing/CLOSED-TEST-2026-11.md`](store-listing/CLOSED-TEST-2026-11.md) §2.

| 늦어도 | 단계 |
|---|---|
| 11-26 | 키(A) · 콘솔 앱 · Cloud 프로젝트 · 처리방침 승인 / 워커 수리 PR(#42 머지됨) |
| **11-27** | 등록정보 · 앱 콘텐츠(B) · 서명 AAB · Drive 등록(D) · 비공개 «검토로 보내기»(E) |
| 12-04 | 12명 옵트인 끝 |
| 12-18 | 프로덕션 접근 신청(F) |
| 12-25 | 접근 승인 → 프로덕션 제출(G) |
| 01-01 | 공개 |

### 3-3. 슬립
- 11-02판 대비 23일 동안 착수 0. 블록 C(내부 테스트)는 건너뛰고 첫 AAB를 비공개 트랙에 바로 올린다(14151465 «Internal testing is optional»).
- 하루 늦게 보낼 때마다 날짜가 하루 밀린다. 검토마다 7일로 잡으면 모집에 쓸 날이 0일이다.
- 01-01은 보장선이 아니다 — 조건은 CLOSED-TEST §2 첫 문단.

### 3-4. 12명 도달일별 일정(2026-11-27 · 비공개 게시 11-26 12:09 뒤)

모집 글 = 11-27 저녁(사용자). 옵트인 수 장부 = [`store-listing/OPTIN-LEDGER-2026.md`](store-listing/OPTIN-LEDGER-2026.md)(11-27 = 0명, 의도된 값 — 문장서랍과 하루 차이).

근거(Play 도움말 원문 · 14151465는 2026-11-27 콘솔 «질문 미리보기»가 여는 페이지에서 다시 읽음):
- 14151465 «At least 12 testers must be opted in to your closed test when you apply for production access, and they must have been opted in continuously for the preceding 14 days.» → **신청 가능일 = 12번째 사람이 옵트인한 날 + 14일**.
- 14151465 «If a tester opts out and opts back in later, the 14 days must be consecutive» → 중간에 나간 사람은 다시 들어온 날부터 센다.
- 14151465 «Review usually takes seven days or less, but can occasionally take longer.» → 접근 검토 7일.
- 9859654 «Processing can take a few hours or up to seven days (or longer in exceptional cases)» → 프로덕션 검토 7일.
- 14151465 «Reasons for required continued testing include having fewer than 12 opted-in testers or insufficient tester engagement during the testing period.» → 연장 위험(숫자 없음).

| 12명 도달일 | 접근 신청 가능일(+14) | 접근 승인(+7, 최악) | 프로덕션 공개(+7, 최악) | 01-01까지 여유 |
|---|---|---|---|---|
| 11-28 | 12-12 | 12-19 | 12-26 | **+6일** |
| 12-01 | 12-15 | 12-22 | 12-29 | **+3일** |
| 12-03 | 12-17 | 12-24 | 12-31 | **+1일** |
| 12-05 | 12-19 | 12-26 | 01-02 | **−1일**(01-01 못 닿음) |

- 계산: 공개 = 도달일 + 14 + 7 + 7 = 도달일 + 28. 여유 = 01-01 − 공개일. 검토가 7일보다 빠르면 그만큼 앞당겨지고, 7일보다 길면(«occasionally»·«exceptional») 밀린다. 연말 검토 지연 수치는 문서에 없다.
- **01-01에 닿는 마지막 도달일 = 12-04**(12-04 + 28 = 01-01, 여유 0).
- 하루 여유 권장: 시간대 · 옵트인 시각 경계를 문서가 정하지 않으므로 신청은 «+14일» 다음 날로 잡는다(위 표는 여유를 그 하루만큼 줄여 읽는다).
- 접근 승인 당일에 프로덕션 제출(비공개 89 그대로 승급 · 새 빌드가 필요하면 §5-C Q1과 함께 서명 스크립트 `scripts/` 사용).
- 신청 전 확인: 콘솔 대시보드 «현재 참여를 선택한 테스터 N명» ≥ 12 + 12명 이상이 14일 연속(장부 날짜로 확인).

## 4. 리스크

| 리스크 | 영향 | 대응 |
|---|---|---|
| ~~병합 매니페스트에 `INTERNET`이 없을 가능성~~ → **해소(11-02)**: 소스 매니페스트에 명시 선언(`AndroidManifest.xml:7`) | 병합 결과에 반드시 포함 | 블록 A 직후 `DATA-SAFETY-2026-11.md` §4-3으로 나머지 «추정» 권한 실측 |
| ⑥ 적용 여부 미확정 | 일정 22일 차이 | B 직후 앱 대시보드에서 «프로덕션 액세스» 과제 표시 여부 확인 → 이 문서 ⑥ 상태 갱신 |
| 테스터 12명 모집 | T0 지연 = 게시 지연 1:1 | Google 그룹 1개 + 맞테스트 모집 글. 사용자 손 |
| 연말 심사 지연 | 새해 공개 미달 | 첫 제출을 하루라도 앞당긴다(수치 근거 없음) |
| `android:allowBackup="true"` + 규칙 파일 미연결 | Android 자동 백업이 DB를 사용자 Google 계정으로 복사(OS 기능) | `DATA-SAFETY-2026-11.md` §2 주석 · 결정 필요 시 별도 PR |

## 5. 프로덕션 액세스 신청서 3부 답안 틀 (블록 F용)

> 문항은 같은 개발자 계정의 다른 앱 선례 기준. 폼이 바뀌었을 수 있으니 제출 날 문항을 대조한다.

### 5-0. 콘솔 «질문 미리보기» 대조(2026-11-27 12:3x)
대시보드 «프로덕션 액세스 신청» 카드의 «질문 미리보기»는 별도 창이 아니라 도움말 14151465 «Apply for production access» 절로 연결된다. 그 절의 문항(원문)과 아래 틀을 맞춘 결과:

| 부 | 도움말 문항(원문) | 틀 | 판정 |
|---|---|---|---|
| 1 | «Select an option indicating how easy it was to recruit testers for your app.» | 5-A Q2 | 있음 |
| 1 | «Whether testers used all available app features» | 5-A Q3 일부 | **따로 답 필요** → 5-A Q3a |
| 1 | «Whether tester usage matched expected production user behavior, including details on any observed differences» | 없음 | **새 문항** → 5-A Q3b |
| 1 | «Summarize the feedback received from testers and describe how feedback was collected.» | 5-A Q4 | 있음(«수집 방법» 문장 보강 → Q4 앞줄) |
| 1 | (도움말에 없음) 모집 방법 서술 | 5-A Q1 | 틀에만 있음 — 폼에 칸이 없으면 Q3b · Q4에 녹인다 |
| 2 | «Specify the target audience for your app or game. Be as specific as possible.» | 5-B Q1 | 있음 |
| 2 | «For apps: Describe how your app provides value to users.» | 5-B Q2 | 있음 |
| 2 | «Select an estimated install range for your app or game during its first year.» | 5-B Q3 | 있음 |
| 3 | «Describe any changes made to your app or game based on what you learned from your closed test.» | 5-C Q1 | 있음(빈칸 — 기간 끝에) |
| 3 | «Describe how you determined that your app or game was ready for production.» | 5-C Q2 | 있음 |

실제 입력 칸의 글자 수 제한 · 라디오 보기 글자는 «프로덕션 신청»을 눌러야 보인다(12명 · 14일 전에는 버튼이 막힘) — 신청 날 이 표를 다시 대조한다.

**틀에 없던 문항 답 초안**(≤300자 · 숫자는 콘솔 실측만):
- **5-A Q3a 모든 기능 사용 여부**: «아니요, 모든 테스터가 모든 기능을 쓰지는 않았습니다. 테스터에게는 14일 동안 참여를 유지하고 습관을 하나 만들어 보도록 부탁했습니다. 위젯 · 알림 · 파일 내보내기 · Google Drive 백업처럼 일부만 쓰는 기능은 개발자와 지인이 실기기에서 매일 사용하며 확인했습니다. 앱에는 분석 도구가 없어 기능별 사용량은 측정하지 않았습니다.»
- **5-A Q3b 프로덕션 예상 사용과 일치 여부 · 차이**: «대체로 일치했습니다. 출시 후 예상 사용은 하루 한 번 앱이나 위젯에서 습관을 기록하는 짧은 사용입니다. 차이는 테스터 다수가 습관 1 ~ 2개만 등록해 기록 기간이 짧았다는 점이고, 여러 습관을 몇 주 이어서 쓰는 사용은 개발자 · 지인 기기에서 확인했습니다. 테스트 참여 경로는 [모집 경로]였고 [T0]에 [N]명이 참여했습니다.»(차이 문장은 테스트가 끝난 뒤 실제 관찰로 고친다 — 관찰이 없으면 «차이는 측정하지 않았습니다»로)
- **5-A Q4 앞줄(수집 방법)**: «의견은 Play 테스트 의견(콘솔 «테스트 의견»), 스토어 테스트 화면에 적은 메일(report@…), 모집한 커뮤니티 대화방으로 받았습니다.» + 기존 A/B 택1.
> 원칙: 실재하는 기능만 · 최상급/보장성 표현 0 · 숫자는 콘솔 실측만(`[콘솔 실측]` 칸은 추정으로 채우지 않는다) · 각 300자 이내(한글 1자=1).

### 5-A. 1부 «비공개 테스트 정보»
- **Q1 모집 방법(≤300)**: «유료 테스트 제공업체는 이용하지 않았습니다. [모집 경로 — 예: 앱 테스터 품앗이 커뮤니티]로 모집했고, Play Console에는 Google 그룹을 테스터로 등록해 그룹 가입만으로 참여 자격이 생기게 했습니다. 발송 전에 별도 계정으로 가입→참여→설치 경로를 직접 확인했습니다. [T0 날짜]에 [N]명이 참여를 선택했습니다.»
- **Q2 모집 난이도(라디오)**: 실측에 맞춰 선택.
- **Q3 참여도(≤300)**: «참여 선택 테스터는 [T0]에 [N]명에 도달한 뒤 14일 이상 12명 이상으로 유지됐고, 설치 사용자(순, 일별)는 [콘솔 실측]이었습니다. 앱은 분석 도구 없이 기기 안에 기록을 저장하는 구조라 기능별 사용은 측정하지 않았습니다. 전 기능은 개발자가 실기기로 검증했습니다(습관 추가·체크·통계·홈 화면 위젯·알림·파일 내보내기·Google Drive 백업).»
- **Q4 피드백 요약·수집 방법(≤300)**: 피드백 유무에 따라 A(«직접 전달된 의견은 없었습니다. 대신 … 개발자 실기기 관찰을 반영했습니다: [실제 머지 PR 목록]»)/B 중 택1.

### 5-B. 2부 «앱 정보»
- **Q1 주요 대상(≤300)**: «매일 반복하고 싶은 습관(독서·운동·물 마시기 등)을 기록하고, 이어지는 기록과 통계로 동기를 얻고 싶은 성인 사용자입니다. 체크형 습관과 수치형 습관(쪽수·km 등)을 함께 관리하려는 분, 홈 화면 위젯으로 앱을 열지 않고 기록하려는 분을 포함합니다. 앱은 한국어로만 제공됩니다.»
- **Q2 제공 가치(≤300)**: «습관을 등록하고 하루 한 번 탭으로 기록하면, 습관 점수(지수 이동 평균)·연속 기록·요일별 빈도·월간 달력·목표 달성률로 흐름을 보여 줍니다. 회원가입이 없고 기록은 기기 안에 저장되며, 원하면 파일로 내보내거나 본인 Google Drive의 앱 전용 공간에 백업할 수 있습니다. 광고와 결제가 없는 무료 앱입니다.»
- **Q3 첫해 설치 예상(라디오)**: «0~1만» 권장(근거: 마케팅 예산 0, F6 재검토 기준 설치 300).

### 5-C. 3부 «프로덕션 준비»
- **Q1 테스트로 알게 된 내용과 변경(≤300)**: 비공개 기간에 실제로 머지된 PR만 인과로 적는다(«무엇을 알게 돼서 → 무엇을 바꿨나»). 빈칸으로 두고 E 기간 종료 시 채운다.
- **Q2 프로덕션 준비 판단(≤300)**: «단위 테스트 [N]건 0 실패, R8 축소 서명 번들 빌드, 실기기 체크표 15항 통과를 확인했습니다. 비공개 트랙 [N]개 버전이 Google 검토를 통과했고, Android vitals 지난 28일 사용자 인식 비정상 종료·ANR 보고는 [콘솔 실측]건입니다(발생률이 «데이터 없음»이면 그렇게 적는다). 앱 콘텐츠 선언·개인정보처리방침 게시를 마쳤습니다.»
  - 쓰지 않는 말: «크래시율 0%»(표본 부족은 0이 아니다) · «안정성이 검증됐다».
