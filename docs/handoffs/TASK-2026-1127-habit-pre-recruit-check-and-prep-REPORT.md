# REPORT — TASK-2026-1127 습관의 힘: 모집 전 참여 경로 확인 · 14일 구간 준비

> 발주: LC `life-coordinator-ae` · origin:lc · 2026-11-27 · 워커 창 `power-of-habit-bf` · 콘솔은 읽기만 함(저장 · 게시 · 테스터 그룹 수정 0)

## LC가 사용자에게 올릴 것
1. **모집 글은 올려도 됩니다**(조건 1 표). 글의 «(그룹 가입 링크)»는 테스터 그룹 정보 화면 주소, «(참여 링크)»는 `https://play.google.com/apps/testing/com.woodpeckerai.powerofhabit`.
2. **01-01 공개에 닿으려면 12명이 12-03까지 차야 합니다**(검토 7일씩 = 통상 상한, 보장 아님 · 11-28이면 여유 5일, 12-05면 이틀 늦음). 신청은 12명이 된 날을 0일째로 세어 +15일(RELEASE-PLAN §3-4).
3. 오늘 저녁 본인 폰에 설치하면 DEVICE-QA «사용자 폰 확인 순서»대로 1 ~ 5번(약 25분)을 봐 주세요 — 13번 Drive 백업은 참가자도 누를 수 있어 가장 중요합니다.
4. 결정 2건: (a) effort 설정 · 미커밋 `settings.json` 12줄 처리 (b) 습관의 힘 국가를 문장서랍처럼 일본까지 넓힐지(지금은 대한민국 1개).

## 조건 1 — 모집 전 참여 경로(2026-11-27 12:1x ~ 12:3x 실측 · 소유자 계정 화면)
| 확인 | 화면 글자 · 값 | 판정 |
|---|---|---|
| ① 웹 옵트인 `play.google.com/apps/testing/com.woodpeckerai.powerofhabit` | «WoodpeckerAI has invited you to a testing program for an unreleased version of the 습관의 힘 - 하루 한 번 기록 app» · «Become a tester» 버튼(누르지 않음) · 의견 `report@woodpecker-ai.com` | 열림 |
| ① Android 꼴 `play.google.com/store/apps/details?id=com.woodpeckerai.powerofhabit` | 제목 «습관의 힘 - 하루 한 번 기록» · «0+ 다운로드» · «설치» | 열림(실제 Android 폰은 미확인) |
| ② 국가/지역 | «타겟팅됨(1개) · 대한민국» — 문장서랍은 «타겟팅됨(2개) · 대한민국 · 일본» | 다름(막지 않음 · 사용자 결정 4b) |
| ② 기기 제외 | 기기 카탈로그 = 첫 진입 안내 화면(«시작하기») — 수동 제외 설정 없음 | 제외 0 |
| ② 테스터 그룹 | Google 그룹스 2개(테스터 그룹 · 맞테스트 그룹) · 의견 메일 — 문장서랍 트랙과 같음 | 같음 |
| ② 그룹 가입 | 두 그룹 정보 화면 모두 «웹의 모든 사용자에게 공개 · 그룹에 가입할 수 있음» | 누구나 가입 |
| ③ 게시 개요 | «관리형 게시가 사용 중지됨 · 최근 게시일: 2026년 11월 26일», 검토 중 · 보내지 않은 변경 섹션 없음 | 남은 변경 0 |
| ④ 모집 글 | 링크 자리 2개 = 위 두 주소 · 앱 이름 «습관의 힘»(스토어 제목 앞부분) · 부탁 1줄 «14일 동안 나가지 말기 + 습관 하나» · 메일 일치 | 일치 |
| 대시보드 | «비공개 테스트 버전 게시 ✔ · 현재 참여를 선택한 테스터 0명» | 의도된 0 |

**모집 글을 올려도 됨** — 경로 4단(그룹 가입 → 테스터 되기 → 설치)이 모두 열려 있고 고칠 값이 없다. 한계: 소유자(그룹 회원) 계정으로 본 화면이라 비회원 화면과 실제 Android 폰은 사용자 설치 때 처음 본다.
12:28 대시보드 카드 글자 «현재 참여를 선택한 테스터 0명»은 JS로 읽은 화면 텍스트(캡처 파일 없음).
캡처: `C:\Users\colds\AppData\Local\Temp\claude-chrome-screenshots-5tOENE\screenshot-1795750317882-0.jpg`(옵트인) · `…-1795750403851-1.jpg`(테스터 탭) · `…-1795750432397-2.jpg`(게시 개요) — 임시 폴더, 레포에 커밋하지 않음.

## 완료 조건 체크리스트
- [x] 0. effort 정본 §4 적용 확인 — **미적용 · 사용자 답 대기**: 커밋본 `.claude/settings.json`에 `modelSettings` 없음(`git show HEAD:.claude/settings.json | grep -i effort` 0줄) · `.claude/agents/` 없음. LC REPLY 11-27로 보류(settings.json 손대지 않음). `/effort status`는 워커가 slash 명령을 직접 칠 수 없어 미기록 — 사용자 손
- [x] 1. 모집 전 참여 경로 확인 — «모집 글을 올려도 됨»(LC에 12:3x 1줄 보냄)
- [x] 2. 일정표 · 옵트인 장부 — #53 머지 `f717fa2`(LC PASS-관찰 · 보정 head `89b40e5`)
- [x] 3. 서명 스크립트 — #54(게이트 요청) · `--self-test` exit 0(임시 키 일치 / 다른 키 불일치) · 실제 키 실행은 사용자 손
- [x] 4. 백업 규칙 — #55(게이트 요청) · testDebugUnitTest 129/0 · 병합 매니페스트 :27 · :29 · :32
- [x] 5. DEVICE-QA — LC 변경 조건대로: **3 / 15**(01 · 09 · 11 JVM 확인) · 부분 8 · 실기기 전용 4 · 사용자 폰 확인 순서 표(이 PR)
- [x] 6. 접근 신청서 초안 — #53 §5-0(«질문 미리보기» = 14151465 Apply 절로 연결 · 틀에 없던 2문항)
- [x] 7. REPORT · WORKLOG · AGENT_STATUS — 이 PR

## 한 일 / 발견 / 필요한 것
**한 일**
| PR | 내용 | 검증 |
|---|---|---|
| #53 `f717fa2` | RELEASE-PLAN §3-4 도달일별 일정 · OPTIN-LEDGER · §5-0 질문 미리보기 대조 | LC PASS-관찰 → 보정 2 + 관찰 3 반영 뒤 머지 |
| #54 | `scripts/sign-release.sh` + `.gitattributes` | `bash -n` · `--check-only` exit 0 · 잘못된 가지/SHA exit 1 · `--self-test` exit 0(9분 9초) |
| #55 | 백업 규칙(로그인 저장소 제외) + `BackupRulesTest` | `testDebugUnitTest` exit 0 · 129/0 · 병합 매니페스트 확인 |
| 이 PR | REPORT · WORKLOG · AGENT_STATUS · DEVICE-QA 미리보기 절 | `git diff --check` |

**발견**
- 습관의 힘 트랙은 국가 1개(대한민국), 문장서랍은 2개(대한민국 · 일본) — 사용자 결정(LC가 올림).
- 콘솔 «질문 미리보기»는 별도 화면이 아니라 도움말 14151465 Apply 절로 연결된다. 그 절에 «모집 방법» 문항은 없고 «모든 기능 사용 여부» · «예상 사용과의 차이»가 있다(§5-0).
- #55 뒤 처리방침 `privacy-policy.html:165` «Google 로그인을 했다면 4항의 로그인 정보 포함»이 새 빌드부터 사실보다 넓다 — 개정 문안은 사용자에게 보인 뒤 별도 게시.
- 레포 versionCode(origin/main 커밋 수)는 지금 94 — 새 빌드는 89보다 크다.
- 정리: `.claude/worktrees/plan-1127` 폴더가 Windows 잠금으로 남음(git worktree 등록은 해제됨 · 다음 창에서 지움).

**LC · 사용자에게 필요한 것**: 맨 위 «LC가 사용자에게 올릴 것» 1 ~ 4 + #54 · #55 게이트.

## effort
세션 medium(기본) 그대로 — 올린 곳 없음. 서브에이전트 0.
