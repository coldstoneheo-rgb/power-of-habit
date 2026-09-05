# WORKLOG — power-of-habit

> 오케스트레이터(Life Coordinator) `/project-scan`용 요약+신호 로그. **최신이 맨 위(append-only).**
> 작업 세션을 끝낼 때마다 맨 위에 아래 형식의 2-Layer 블록을 추가한다(블록 사이 `---` 구분).
> 규약 전문: https://github.com/coldstoneheo-rgb/life-coordinator/blob/main/docs/WORKLOG_PROTOCOL.md
> git이 이미 주는 diff·커밋 메시지는 재서술 금지(링크만). `progress`는 근거 필수.
> 이 repo는 이중 하네스로 구동 — Antigravity(`.agents/AGENTS.md`) + Claude Code(`CLAUDE.md`). 채택 강제 = AGENTS.md §5 Worklog Sync 배선(2026-07-24), CLAUDE.md §5(2026-09-04).

---

```yaml
date: 2026-09-06
project: power-of-habit
agent: claude-code (harness-loop-engine, 사용자 동석 세션)
summary: 이하 목표(AT_MOST) 수치 습관 부활 — DB v5 targetType, RecordOutcomes 방향 인지 판정(0=성공·초과=PARTIAL·미기록=미수행), 등록 화면 이상/이하 칩, 상세·입력 다이얼로그 표기, JSON 이전 필드
status: on_track
progress: "PR #33 머지 (근거: testDebugUnitTest 90/90, assembleDebug 성공, 자체 리뷰 10건 전부 반영). 실기기 재검증은 사용자가 Drive apk/로 복사한 빌드로 진행"
changes: ["#33 feat(habit): 이하 목표(AT_MOST) 유형 부활 — DB v5 targetType, 방향 인지 판정, 등록 화면 이상/이하 칩"]
next: 실기기 재검증(기존 설치 위 v4→v5 업데이트·이하 습관 등록·0 입력 성공·초과 PARTIAL 색·위젯) → #32 항목(앱 이름·아이콘·색 필드·Drive 로그인) 재검증 → 사용자 OAuth 클라이언트 등록 후 Drive 백업 확인 → 옛 앱 DB 가져오기 또는 BadgeManager 스트릭 빈도 인지형
blockers: Drive 백업/복원은 OAuth 클라이언트 미등록(사용자 작업). 옛 com.example 앱 데이터 이전 미구현
learning_need: "미기록=성공"이 편해 보여도 통계를 조용히 부풀린다 — 이하 목표는 0을 직접 적게 하고 UI가 그것을 안내하는 쪽이 데이터 신뢰성 면에서 맞다. 판정 함수 하나에 매개변수 하나를 더하면 렌더러·저장 경로 12곳이 한 번에 따라오는 구조가 이번에 값을 했다
synergy: "의미 충돌을 표로 정리하고 역할별 최강 반대를 적은 뒤 결정"하는 결정 기록 형식이 두 번째로 재사용됨 — L2C "제품 의미 설계를 에이전트 회의로" 콘텐츠 원재료
monetization: 아직 0원. 출시 준비는 키스토어·Play Console·OAuth 클라이언트(사용자 몫) 대기
```
## 의미
지난 세션이 "의미 설계 선행"을 조건으로 보류했던 이하 목표를, 사용자 결정 뒤 3인 역할 의견으로 규칙을 확정하고 구현했다.
핵심은 이상 목표의 "0=미수행"과 이하 목표의 "0=성공"을 한 함수 안에서 방향 매개변수로 가르고, 기록하지 않은 날은 어느 방향에서도
성공으로 치지 않는 것이다. DB는 v5로 올렸지만 기본값이 AT_LEAST라 기존 습관·통계는 한 줄도 달라지지 않는다.

---

```yaml
date: 2026-09-05
project: power-of-habit
agent: claude-code (harness-loop-engine, 사용자 동석 세션)
summary: 실기기 피드백 2차(스크린샷 4장+위젯 녹화) 반영 — 앱 이름 '습관의 힘'·벡터 아이콘·위젯 미리보기, 색 필드/랜덤 기본색, 링 게이지 0점, Drive 백업/복원에 Google 로그인 요청 배선(+복원 안전화), 이전 버튼 정렬
status: on_track
progress: "PR #32 머지 (근거: testDebugUnitTest 77/77, assembleDebug 성공, 자체 리뷰 10건 중 9건 반영·1건(PR 크기)은 기록). 실기기 재검증은 사용자가 Drive apk/로 복사한 빌드로 진행"
changes: ["#32 fix(ux): 실기기 피드백 2차 — 앱 이름·아이콘, 색 필드/랜덤 색, 링 게이지, Drive 로그인 요청, 이전 버튼 정렬"]
next: 실기기 재검증(위젯 선택기에 '습관의 힘' 1개만 뜨는지·색 필드·링 0점·버튼 정렬) → 사용자가 Cloud Console OAuth 클라이언트 등록(docs/RELEASE.md §0-4) 후 Drive 백업/복원 로그인 흐름 확인 → 결정 ②(AT_MOST 부활) 구현 PR
blockers: Drive 백업/복원은 OAuth 클라이언트 미등록 상태에서 코드 10으로 실패(사용자 작업). 옛 com.example 앱 데이터 이전은 여전히 미구현
learning_need: "위젯 선택기에 같은 앱이 두 개"는 코드 버그가 아니라 applicationId만 바꾼 두 설치본의 라벨·아이콘 충돌 — 영상 속 설정 화면의 습관 목록(새 앱에 없는 습관)이 결정적 단서였다. 복원은 '검증 후 교체' 순서가 아니면 깨진 다운로드가 빈 DB가 된다
synergy: 스크린샷 주석(빨간 박스+번호 메모) → 원인표 → PR 표 형식은 다른 앱 프로젝트 QA 회차에 그대로 재사용 가능. L2C "실기기 피드백을 PR로 닫는 루프" 원재료
monetization: 아직 0원. 출시 준비는 키스토어·Play Console·OAuth 클라이언트(사용자 몫) 대기
```
## 의미
사용자가 실기기에서 표시한 문제 6건 중 5건은 코드로 닫았고, "위젯 선택기에 앱 두 개"는 옛 앱과 이름·아이콘이 같아서라는 진단 뒤
런처 이름과 아이콘으로 구분되게 했다. 사용자가 "백업하려면 구글 인증이 필요하다고 안내하면 납득한다"고 결정해 로그인 화면을 따로 만들지
않고 버튼 시점에 인증을 요구하는 방식으로 배선했다. 자체 리뷰가 잡은 복원 중 DB 선삭제(깨진 파일 → 빈 DB), 수동·자동 백업 동시 실행,
회전 시 진행 상태 유실은 뮤텍스·검증 후 교체·ViewModel 이관으로 머지 전에 닫았다.

---

```yaml
date: 2026-09-05
project: power-of-habit
agent: claude-code (harness-loop-engine, 사용자 부재 자율 세션 ~4h)
summary: 실기기 관찰 3건 수정(위젯 갱신 신뢰성·삭제 습관 딥링크) + 전문가 3인 회의 결정 3건 구현 — 수치형 3상태(성공/기준미달/미수행), 수치형 위젯 탭 → 투명 입력 액티비티+성공 폭죽, 로컬 JSON 내보내기/가져오기
status: on_track
progress: "위젯·3상태·이전 기능 머지 완료 (근거: #28 ba2de36 · #29 f0c3206 · #30, testDebugUnitTest 73/73, 자체리뷰 3회 총 26건 반영). 실기기 검증은 사용자 귀가 후"
changes: ["#28 fix(widget): 위젯 갱신 신뢰성 + 수치형 3상태 표현 + 삭제 습관 딥링크 안내", "#29 feat(widget): 수치형 1x1 탭 → 투명 입력 액티비티 + 성공 폭죽, 입력 다이얼로그 공용화", "#30 feat(transfer): 로컬 JSON 내보내기/가져오기 — 기기 이전·백업용"]
next: 실기기 확인(위젯 설정 즉시 반영·1x1 탭 즉시 색 변화·수치형 입력 다이얼로그·삭제 후 위젯 탭·JSON 내보내기/가져오기) → 결정 기록의 사용자 결정 3건(기준미달 부분 점수·AT_MOST·옛 앱 debug 여부) → 옛 앱 DB 파일 가져오기
blockers: 옛 com.example 앱 데이터 이전은 아직 불가(DB 파일 가져오기 미구현) — 예전 앱 삭제 금지
learning_need: Glance updateAll()이 내부 리시버→위젯 매핑에 의존해 첫 배치 위젯을 놓치는 동작 — AppWidgetManager id 나열로 우회(재사용 가능한 패턴)
synergy: docs/decisions 의 3인 회의 기록 형식(찬성/최강 반대/합의/근거)은 다른 프로젝트 의사결정에 재사용 가능 — L2C "에이전트 회의로 제품 결정" 콘텐츠 원재료
monetization: 아직 0원. Play 내부 테스트 업로드는 키스토어·Console 준비(사용자) 대기
```
## 의미
사용자 부재 중 "결정이 필요한 것"과 "그냥 고칠 것"을 분리했다. 갱신 버그·삭제 오류는 바로 고쳤고, 3상태 모델·위젯 탭 방식·데이터 이전
수단은 아키텍트/디자이너/QA 역할 3인의 의견서를 받아 합의점과 근거를 `docs/decisions/2026-09-05-…md`에 남긴 뒤 구현했다.
공통 합의였던 "DB 스키마 변경 금지·옛 앱 건드리지 않기"를 지켰고, 파생 상태 함수 하나(RecordOutcomes)로 4개 렌더러와 2개 저장
경로를 통일했다. 자체 리뷰가 잡은 캘린더 빈 날 실패 표시 회귀·자정 경계 이중 기록·트랜잭션 누락 같은 결함은 머지 전에 닫았다.

---

```yaml
date: 2026-09-05
project: power-of-habit
agent: claude-code (harness-loop-engine)
summary: 출시 준비 — applicationId com.woodpeckerai.powerofhabit 확정, 릴리스 서명 설정, versionCode 자동 증가, R8 minify, Play 내부 테스트 업로드(GPP) 배선, RELEASE.md
status: on_track
progress: "Play 내부 테스트 업로드 직전 단계 (근거: #27, testDebugUnitTest 통과, bundleRelease R8 빌드 성공). 키스토어 생성·Play Console 앱 생성은 사용자 작업으로 남음"
changes: ["#27 chore(release): 출시 준비 — applicationId·서명·versionCode·R8·Play 업로드 배선"]
next: 사용자가 release.jks 생성 + Play Console 앱/내부 테스터 등록 → 첫 AAB 수동 업로드. 병행 후속: Google 로그인 흐름 부재로 백업/복원이 동작하지 않음(자체 리뷰 발견) → 로컬 ZIP 내보내기/가져오기 또는 로그인 UI
blockers: 키스토어 비밀번호·Play Console 설정은 사용자 직접 수행(docs/RELEASE.md §0). 기존 com.example 앱 데이터 이전 수단 없음 — 예전 앱 삭제 금지
synergy: 릴리스 파이프라인(서명·버전·R8·GPP)은 baby-naming 등 다른 앱에 그대로 이식 가능 — L2C "Play 내부 테스트 자동화" 콘텐츠 원재료
monetization: 아직 0원. 내부 테스트 → 프로덕션 → 결제 모델은 이후 결정
```
## 의미
"드라이브에 APK 던지기"에서 "Play 내부 테스트로 올리기"로 배포 경로를 바꾸는 전제 작업이다. applicationId는 되돌릴 수 없는
결정이라 브랜드(WoodpeckerAI) 역도메인으로 확정했고, 코드 패키지는 그대로 둬 diff를 최소화했다. versionCode를 커밋 수로 자동화해
업로드마다 손으로 올리는 실수를 없앴고, R8을 켜 출시 빌드에서만 터지는 문제를 지금부터 매 빌드에서 잡는다.
Drive 복사는 옵트인으로 낮췄다. 남은 것은 비밀(키스토어·서비스 계정)을 쥔 사용자 손에서만 끝나는 단계다.

---

```yaml
date: 2026-09-05
project: power-of-habit
agent: claude-code (harness-loop-engine)
summary: 홈 화면 위젯 2종(1x1 체크·2x2 캘린더 글랜스, Jetpack Glance) + DB 관찰 기반 위젯 자동 갱신 + 체크 토글 트랜잭션 단일화
status: on_track
progress: "PRD §1.1.4·§4-1 홈 위젯 구현 완료 (근거: #26, testDebugUnitTest 48/48, assembleDebug 성공, 자체리뷰 10건 반영). 실기기 배치·탭은 미검증"
changes: ["#26 feat(widget): 홈 화면 위젯 — 1x1 체크 + 2x2 캘린더 글랜스 (Glance)"]
next: 실기기에서 위젯 추가·탭·자정 갱신 확인 → 문제 없으면 출시 준비(패키지명·서명·minify) 또는 목표/연속/점수 2x2 위젯 확장
blockers: 에뮬레이터/실기기 미확인 — Glance 렌더링·런처 배치·재설정 흐름은 기기 테스트 필요
synergy: RecordSideEffects·toggleCompletion 트랜잭션·WidgetRefreshObserver는 향후 알림 "완료" 액션·임포트 등 어떤 쓰기 경로에도 재사용
monetization: 아직 0원(Play 배포·수익모델 미설계)
```
## 의미
PRD에서 유일하게 비어 있던 "홈 화면과의 유기적 연결"을 채웠다. 위젯을 붙이면서 기록 쓰기 경로가 화면·위젯 두 갈래가 되므로,
토글 규칙은 DAO 트랜잭션 한 곳으로, 뱃지·백업은 RecordSideEffects 한 곳으로, 위젯 갱신은 Room 무효화 Flow를 관찰하는
옵저버 한 곳으로 모았다. 자체 리뷰가 잡은 회전 시 딥링크 재발화·연타 중복 행·자정 이후 잘못된 날짜 기록·삭제된 습관의
죽은 위젯 같은 실사용 결함도 같이 막았다. 앞으로 어떤 경로로 기록이 바뀌어도 위젯이 따라온다.

---

```yaml
date: 2026-09-05
project: power-of-habit
agent: claude-code (harness-loop-engine)
summary: 빈도 인지형 통계 엔진(#24) + 레퍼런스 역설계 디자인 가이드·토큰 코드화·핵심 화면 적용(#25)
status: on_track
progress: "PRD 통계 정확성 결함 해소 + 디자인 시스템 v1 (근거: #24 머지 fce371d·testDebugUnitTest 44/44, #25 assembleDebug 성공·자체리뷰 2회 반영)"
changes: ["#24 feat(stats): 빈도 인지형 통계 엔진 — 주기별 스트릭·EMA·달성률", "#25 feat(design): 디자인 가이드(레퍼런스 역설계 + 토큰) + 토큰 코드화·핵심 화면 적용"]
next: 홈 화면 위젯(AppWidget/Glance, PRD §1.1.4·§4-1) — 통계 엔진·토큰을 재사용해 1x1 체크 칩·캘린더 글랜스 뷰
blockers: 실기기 스크린샷 미확인 — 다크/라이트 대비는 토큰 값(AA 계산)으로만 검증
synergy: docs/DESIGN_GUIDE.md는 다른 앱(baby-naming 등)에 이식 가능한 토큰 표 — L2C "디자인 시스템 역설계" 콘텐츠 원재료
monetization: 아직 0원(Play 배포·수익모델 미설계). 출시 준비(패키지명·서명·minify)는 후보 #5로 대기
```
## 의미
"예쁘지만 틀린 숫자"를 먼저 고쳤다. 주 n회·월 n회 습관이 매일 기준으로 채점되던 결함을 기간 단위 판정으로 바꾸고,
계산을 Composable 밖 순수 Kotlin으로 빼 테스트 가능하게 했다. 자체 리뷰에서 홈 도넛의 창(window) 불일치·오늘 실패 미반영·
창 위상 이동 같은 실사용 버그가 추가로 잡혔다. 디자인은 레퍼런스의 문법(무채 85-12-3·톤 계단·텍스트 셀)만 추출하고
곡률·배경·타입·층 구조를 새로 정해 토큰으로 코드화했다. 두 산출물 모두 홈 위젯 작업의 재료가 된다.

---

```yaml
date: 2026-09-04
project: power-of-habit
agent: claude-code (harness-loop-engine)
summary: Claude Code 하네스 이중화 + codebase-memory-mcp 인덱싱(power-of-habit, 660 nodes/2076 edges)
status: on_track
progress: "하네스 이중화 완료 — 두 에이전트가 동일 규약 공유 (근거: #23, 메인 체크아웃에서 testDebugUnitTest 11/11 통과, index_repository status=indexed)"
changes: ["#23 chore(harness): Claude Code 하네스 구성 + codebase-memory 인덱싱"]
next: 그래프 기반 탐색으로 다음 기능/이슈 선정(PRD 기준) → 하네스 루프 첫 실작업 PR
synergy: L2C devlog Stop 훅을 이 머신의 Claude Code 로컬 설정에 옵트인(커밋 대상 아님) — 두 에이전트 모두 콘텐츠 원재료를 자동 생성
monetization: 아직 0원(Play 배포·수익모델 미설계)
```
## 의미
Antigravity 전용이던 하네스를 Claude Code에서도 같은 규약으로 구동할 수 있게 했다. 코드 그래프 인덱스가 생기면서
이후 세션은 grep 대신 호출관계로 탐색 비용을 줄인다. 자체 리뷰에서 worktree의 local.properties 부재·detect_changes 사각지대를
찾아 CLAUDE.md에 우회법을 명시했다. 실기능 변경은 없고 다음 세션부터 루프 실작업에 들어간다.

---

```yaml
date: 2026-07-24
project: power-of-habit
agent: antigravity(round9) — 엔트리는 LC catch-up(세션 self-report 전, git 증거 기반)
summary: 비주얼 배치 라운드9(UI 개선·테마 수정) + Antigravity 하네스 L2C 캡처 통합 재작성
status: on_track
progress: "라운드9 완료 (근거: PR#21 머지 db364bd — CheckWidget·MainScreen·BadgesScreen UI 개선·테마 수정 + .agents L2C 캡처 통합 재작성 3파일)"
changes: ["#21 visual batch round 9 improvements and theme fixes"]
next: 다음 세션부터 Stage B [Worklog Sync]로 self-report (AGENTS.md §5 배선 완료)
synergy: L2C 캡처 하네스 통합 완성 — Antigravity 세션이 devlog 원재료를 직접 생성(l2c_capture skill)해 L2C 콘텐츠 파이프에 공급
monetization: 아직 0원(Play 배포·수익모델 미설계)
---
## 의미
LC 오케스트레이터 catch-up 엔트리다. 라운드9 세션이 self-report 전(AGENTS.md에 Worklog 미배선)이라 git 증거로 대신 기록했다.
이번에 AGENTS.md §5에 Worklog Sync를 배선했으므로 다음 세션부터 Antigravity가 직접 append한다. 라운드9는 UI 폴리시 지속 +
Antigravity의 L2C 캡처 하네스 통합을 함께 담았다.

---

```yaml
date: 2026-07-24
project: power-of-habit
agent: claude-code (LC 오케스트레이터 부트스트랩)
summary: WORKLOG 추적 시작 — A1 보고갭 해소로 오케스트레이터가 시드 생성(git 증거 기반, 실작업 세션 아님)
status: on_track
progress: "실기기 피드백 반영 UI 폴리시 단계 (근거: PR#16~19 백업/복원 시스템 + 비주얼 배치 라운드1~8 머지, 최신 커밋 7f27993 HabitDetailScreen 컴파일에러 수정, 현재 CheckWidget·MainScreen·BadgesScreen 미커밋 WIP 존재)"
changes: ["#19 비주얼 배치 라운드5~8", "#18 2차 실기기 피드백(팝업 모달·수치목표 필드)", "#16 백업/복원 자동화", "7f27993 컴파일에러 수정"]
next: 미커밋 UI 변경(CheckWidget·MainScreen·BadgesScreen) 마무리·커밋 → 다음 작업 세션이 이 블록 위에 실 엔트리 append
synergy: L2C 콘텐츠 승수 후보 — 습관앱 개발과정(실기기 피드백 루프·비주얼 배치·백업복원) 자체가 블로그 소재. Wiki Assets Legacy 곳간의 "습관수익어플" 아이디어 원형
monetization: 아직 0원(Play 배포·수익모델 미설계). baby-naming의 Play Billing 자산이 발효 후 참고 가능
```
## 의미
오케스트레이터 부트스트랩 시드다. A1 점검(2026-07-24)에서 power-of-habit이 활발히 개발되는데(최근 커밋 07-22)
LC 대면 보고 산출물이 없어 `/project-scan`이 cold git 폴백에 의존함이 드러나 시드를 심었다. 실제 작업 흔적은
git(PR#16~19)과 미커밋 WIP에 있으며, 여기서 재서술하지 않는다. 앞으로 이 파일 맨 위에 세션 종료 시 실 엔트리를
append하면 스캔이 델타로 빠르게 읽고, 신호 필드(synergy·monetization 등)가 브리핑 인사이트에 직결된다.
