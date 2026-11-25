# HO-2026-1125 — power-of-habit 새해 출시 REPORT

> origin:user(LC 전달) · 시작 2026-11-25 17:41 · 기준 main `17e270b` · 예산 240분. 진행 중 — 항목이 끝날 때마다 갱신한다.

## 체크리스트(완료 조건)
| # | 항목 | 상태 | 근거 |
|---|---|---|---|
| 1 | 지금 상태 대조 + 미커밋 2파일 요지 | ✅ | 아래 «상태 대조» · 2파일은 LC에 1줄 보고(17:44) → LC «그대로 둔다» |
| 2 | Play 요건 사실표 | ✅ | `docs/store-listing/CLOSED-TEST-2026-11.md` §1(17:49 curl — 요청한 Play 글 8개 중 7개 200, 16594066은 404라 쓰지 않음) |
| 3 | 역산 일정표 | ✅ | 같은 문서 §2 · `RELEASE-PLAN-2026-11.md` §3-2 |
| 4 | 사용자 손 순서 + allowBackup | ✅ | `FIRST-UPLOAD-2026-11.md` 13단계 · allowBackup = 그대로(LC 가정) |
| 5 | 완성도 한 바퀴 | ⏳ | 목록 = CLOSED-TEST §4 · 수리 PR `fix/newyear-prelaunch` — **시험을 아직 돌리지 못했다** |
| 6 | 등록 준비물 전수표 | ✅(그림 일부) | CLOSED-TEST §3 · 아이콘 512와 이름 없는 1024×500을 만듦, 스크린샷은 수리 PR 시험 뒤 |
| 7 | 테스터 안내문 | ✅ | `TESTER-POST-2026-11.md` |
| 8 | PR · 게이트 · AGENT_STATUS | ⏳ | |

## 상태 대조(main `17e270b`, 2026-11-25 17:42)
| 항목 | 판정 | 근거 |
|---|---|---|
| DoD ① 키 | 사용자 손 | 루트 `*.jks` 0 · `keystore.properties` 0(`ls`) |
| ② 서명 AAB | 사용자 손 | 미서명 R8 빌드만 09-06 기록(WORKLOG) |
| ③ 실기기 15행 | 남음 | `DEVICE-QA-2026-11.md` 결과 칸 15개 빈칸 |
| ④ 콘솔 앱 | 사용자 손 | 레포 기록 0 |
| ⑤ 내부 테스트 | 건너뜀 | 14151465 «optional» · LC 수용 |
| ⑥ 12명 × 14일 · ⑦ 접근 신청 · ⑧ 게시 | 남음 | — |
| ⑨ 스토어 URL | 남음 | 스토어 주소 404 · Pages 404(`has_pages=false`) |
| 블록 A ~ G | 전부 미착수 | `AGENT_STATUS.json`(11-02) `user_blocks_done: 0` |

## 환경 사실
- 에뮬레이터: SDK에 `emulator`는 있으나 시스템 이미지 · AVD 0개 → 화면 시험은 Robolectric(JVM). 위젯 · 실제 알림 · Drive 로그인은 못 본다.
- CI 없음(`.github/workflows` 0) → 시험 기록은 PR 댓글.
- `.claude/agents` 없음 → effort 프런트매터를 붙일 정의 파일 0(REF §4). 설정 · 정의 파일을 새로 만들지 않았다(사용자 손 1줄이 먼저).
- 17:54에 reading-diary 창과 gradle이 겹쳐 시작됐다 → 이 창 것만 멈추고 양보(그 실행의 exit 1은 강제 종료이지 시험 결과가 아니다).

## effort
세션 수준을 올리지 않았다. critic은 서브에이전트 1회(읽기 전용).
