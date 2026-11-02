# CLOUD-2026-1102 — power-of-habit 무료 프로덕션 출시 문서 묶음 REPORT

> origin:lc · 클라우드 9건째 · 브랜치 `cloud/habit-release-docs`(origin/main `96e01ff`에서 분기) · 코드 0(`app/` 변경 0).
> 정본 계획(LC `docs/PLAN-2026-1003-portfolio-implementation.md` §5, `docs/meetings/MEETING-2026-1003-power-of-habit.md`)은 이 VM에서 읽을 수 없다 — 결정 사항은 HO 인라인을 근거로 삼았다.
> 시작 2026-11-02 15:18 KST(`date -Iseconds` → `2026-11-02T06:18:53+00:00`).

## 체크리스트 (완료 조건 번호)

| # | 항목 | 상태 | 산출 |
|---|---|---|---|
| 1 | RELEASE-PLAN (DoD 9 · A~G · 재역산 일정) | ✅ | `docs/RELEASE-PLAN-2026-11.md` |
| 2 | 등록정보 10항 + 원칙 2 | ✅ | `docs/store-listing/LISTING-2026-11.md` |
| 3 | 처리방침 html + 배포 문서 | ✅ | `docs/privacy/privacy-policy.html` · `docs/privacy/DEPLOY.md` |
| 4 | 데이터 안전 + 앱 콘텐츠 + 권한 실측표 | ✅ | `docs/store-listing/DATA-SAFETY-2026-11.md` |
| 5 | Drive OAuth 블록 D 체크리스트 | ✅ | `docs/DRIVE-OAUTH-CHECKLIST.md` |
| 6 | 실기기 QA 15행 틀 | ✅ | `docs/DEVICE-QA-2026-11.md` |
| 7 | README 목적·F6·출시 문서 링크 | ✅ | `README.md` |
| 8 | AGENT_STATUS.json + WORKLOG 블록 | ✅ | `AGENT_STATUS.json` · `WORKLOG.md` |
| 9 | app/ 변경 0 · PR [cloud] · 본문 | ⬜ | PR |
| 10 | 이 REPORT · 마지막 줄 경과·모델 | ⬜ | 이 파일 |

## 한 일
| 산출 | 요지 | 근거 |
|---|---|---|
| `docs/RELEASE-PLAN-2026-11.md` | DoD 9항(현재 상태 칸) · 블록 A~G(선행·소요·증거 파일명) · 12-31 역산 일정(권장/최종 마감 2열) · 리스크 · 신청서 3부 답안 틀 | baby 실측: 첫 심사 12일(`POST-LAUNCH-vc19-2026-1002.md:46,56`) · 신청→승인 4일(`PLAY-PRODUCTION-ACCESS-2026-0916.md:141`) · 콘솔 14일 카운터 1일 지연(같은 문서 §0) |
| `docs/store-listing/LISTING-2026-11.md` | 10항 + 원칙 2 + 기능 문장별 코드 근거 부록 | 짧은 설명 52/80 · 자세한 설명 1171/4000(Python `len`) · 꺾쇠 URL 0 |
| `docs/privacy/privacy-policy.html` · `DEPLOY.md` | baby 틀(스타일 블록) 재사용, 내용 전면 교체 · 외부 리소스 0 · 이메일은 `__CONTACT_EMAIL__` 치환 토큰(배포 전 grep 0 확인 단계 포함) | `grep -c "<script\|src=\|<link"` = 0 · HTMLParser 태그 짝 오류 0 · 9,260 B |
| `docs/store-listing/DATA-SAFETY-2026-11.md` | 데이터 보안 답안(수집=예·선택 / 공유=아니요 / 전송 암호화=예 / 삭제 경로) · 앱 콘텐츠 선언표 · 권한 실측표(실측 3 + 추정 8 + 미확정 1) | `AndroidManifest.xml:4-6` · `app/build.gradle.kts:175,206,227-233` · `GoogleDriveBackupManager.kt:59-60,174-181,191-237` |
| `docs/DRIVE-OAUTH-CHECKLIST.md` | RELEASE §0-4 → 블록 D 실행 순서 · SHA-1 3종 · 같은 Cloud 프로젝트 · 동의 화면 문안 · «프로덕션» 전환 · 실패 토스트↔원인표 | 토스트 원문 `MainScreenViewModel.kt:225,228` · `GoogleDriveBackupManager.kt:149-150` |
| `docs/DEVICE-QA-2026-11.md` | 15행(RELEASE §5 스모크 + 알림·정확한 알람·재부팅·Drive·마이그레이션·테마), 결정 기록 3건 기대값 | 뱃지 10종 = `BadgeRules.kt` 1+6+3 |
| `README.md` | 목적 4항 · F6 재검토 조항 · 출시 문서 링크 표 | 상대 링크 깨짐 0(스크립트 검사) |
| `AGENT_STATUS.json` · `WORKLOG.md` | 상태 파일 신설(필드 as_of·phase·current_task·waiting_on·next_actions·metrics·last_commit) · 2-Layer 블록 1개(synergy·monetization·learning_need·idea) | `python3 -m json.tool` OK |

## 발견
1. **`INTERNET` 권한 미선언 위험** — 소스 매니페스트 선언은 3개뿐이고 저장소 이력 전체 `git log --all -S INTERNET` 0건. Drive는 `NetHttpTransport`로 REST 호출(`GoogleDriveBackupManager.kt:174-181`)하므로 필요하다. 지금까지는 OAuth 미등록으로 로그인 단계에서 멈춰 HTTP 경로가 한 번도 실행되지 않았다. 의존성이 더해 주는지는 미확정(아래 3) → 블록 A 직후 `DATA-SAFETY §4-3` 명령 1회.
2. **Android 시스템 자동 백업 활성** — `AndroidManifest.xml:10` `allowBackup="true"`, `backup_rules.xml`·`data_extraction_rules.xml`은 매니페스트에 미연결(샘플 그대로). 처리방침 §5에 고지, 데이터 보안 답은 영향 없음.
3. **권한 실측 한계** — `maven.google.com`/`dl.google.com` 프록시 403·연결 거부(11-02 `curl`) → 라이브러리 기여 권한은 전부 «추정» 표기.
4. **README 기능 설명이 코드와 어긋남** — README «목표 위젯: 주/월/분/연 단위 탭»이지만 상세 화면은 «이번 달 달성률»/«현재 주기 n/m회» 단일 바(`HabitDetailScreen.kt:491-499`). 등록정보는 코드 기준으로 썼고 README 본문은 이 PR에서 고치지 않았다.
5. **AI 애셋 선언 판단 지점** — 아이콘 벡터 XML은 코딩 에이전트가 #32(77710a7)에서 작성. 이미지 생성 모델 출력이 아니므로 HO 기준 «해당 없음»으로 적되, 콘솔 정의가 코드 보조까지 포함하면 라벨로 바꾸도록 LISTING §11에 명시.
6. **레거시 Google 로그인 API** — `play-services-auth:20.7.0` `GoogleSignIn` 사용(`build.gradle.kts:227`). 지금은 동작, 출시 후 백로그 후보.
7. **Drive 자동 백업** — 연결된 동안 기록 변경 5초 뒤 자동 업로드(`GoogleDriveBackupManager.kt:159-170`). 처리방침 §4·데이터 보안 메모에 반영.
8. **baby clone 경로** — `gh repo clone` = GraphQL 403, `git clone` = 인증 프롬프트 실패 → 세션에 저장소 추가 뒤 `git clone --depth 1` 성공(`05c431d`, 읽기 전용).
9. **12명×14일 요건** — baby가 같은 개인 개발자 계정이면(추정) 이 앱에도 적용될 가능성이 높다. 확정은 콘솔 과제 표시 뒤.

## LC·사용자에게 필요한 것
- **LC**: 이 PR 게이트 → 통과 시 사용자 머지 1줄.
- **사용자(블록 A, 권장 11-06)**: 키스토어 생성 + 서명 `bundleRelease` → 이어서 병합 매니페스트 `INTERNET` 확인(없으면 1줄 코드 PR 요청).
- **사용자(블록 B, 권장 11-09)**: 처리방침 배포(`docs/privacy/DEPLOY.md`) → 앱 만들기 → «프로덕션 액세스» 과제 표시 여부를 LC에 1줄 보고(⑥ 적용 확정).
- **로컬 세션**: 아이콘 512·피처 1024×500 렌더(LISTING §6·§7).
- 기본값으로 진행되는 판단(막지 않음): 이메일 = 수집 아님 · AI 애셋 = 해당 없음 · 카테고리 = 생산성 · 타겟 = 18+.

effort: 세션 기본값 그대로(올린 곳 없음).
