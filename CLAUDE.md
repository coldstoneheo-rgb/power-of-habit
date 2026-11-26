# CLAUDE.md — power-of-habit (Claude Code 하네스)

'습관의 힘' Android 앱. Kotlin + Jetpack Compose(M3) + Hilt + Room, MVVM + Repository.
이 파일은 **Claude Code 세션의 진입 지침**이다. Antigravity 세션은 `.agents/AGENTS.md`를 읽는다.
공통 규약(Worklog Sync·L2C devlog·PR 크기·커밋 위생)의 SSOT는 `.agents/AGENTS.md`이며, 이 파일은 Claude Code 고유 델타만 적는다.

## 1. 동작 모드 — 하네스 루프 (`/harness` 호출 시)
- 사용자가 `/harness`로 위임하면 아래 파이프라인을 **중간 상태 텍스트·승인 질의 없이** 연쇄 실행하고 완료 시 1회 표로 보고한다.
  이 모드는 `WORKFLOW.md` 3·7~9단계의 "사용자 승인/머지 대기"를 **사용자의 명시 위임으로 대체**한다(위임 없는 일반 세션은 WORKFLOW.md를 따른다).
- 파이프라인: worktree(`.claude/worktrees/<name>`) → 구현 → 검증(§3) → commit → push → `gh pr create --base main`
  → 리뷰 스레드 확인(없으면 `code-review` 스킬 자체검증 후 `gh pr comment`) → `gh pr merge --squash`
  → ExitWorktree(keep) → 메인에서 `git pull --ff-only origin main` + worktree/브랜치 정리.
- **완료 = 머지 + 로컬 main 동기화.** PR 생성에서 멈추지 않는다.
  단, `gh pr merge`는 사용자 전역 설정(auto 모드 soft_deny)의 **1회 확인 게이트**를 통과한다 — 이 프로젝트 설정으로 우회하지 않는다.
- worktree 안에서는 `gh pr merge --delete-branch`를 쓰지 않는다(로컬 정리가 "main is already checked out"으로 실패). 머지 후 메인에서 수동 정리.
- 멈춰서 묻는 예외(1회로 묶어서): 스토어 업로드 등 외부 효과(`publishReleaseBundle`, 사용자가 `!`로 직접), 승인 근거 없는 비가역 행위, 방향이 갈리는 진짜 모호한 결정.
- 1 PR = 1 관심사(`WORKFLOW.md` §3). 브랜치명 `feature/…`, `fix/…`, `chore/…`.

## 2. 코드 탐색 — codebase-memory-mcp 그래프 우선
- 인덱스 프로젝트명: **`power-of-habit`** (루트 = 이 저장소의 메인 체크아웃, 즉 `.claude/worktrees/` 바깥).
- 세션 시작 시 `list_projects`/`index_status`로 확인. 미인덱스·대규모 외부 변경 후에만 `index_repository(repo_path=<메인 체크아웃>, name="power-of-habit")`.
- 구조 질의는 그래프 먼저: `search_graph`(심볼 찾기) → `trace_path`(호출자/피호출자) → `get_code_snippet`(정확한 소스) → `get_architecture`(오리엔테이션).
  리터럴/비코드 텍스트는 `search_code` 또는 Grep. 인용·부정 주장 전에 `check_index_coverage`.
- **worktree 주의**: 인덱스는 메인 체크아웃 기준이라 `detect_changes()`는 worktree의 편집을 보지 못한다.
  worktree 작업의 영향 분석은 `git diff --name-only origin/main`으로 파일을 뽑고 `trace_path(direction="inbound")`로 호출자를 추적한다.

## 3. 검증 커맨드 (Windows, Git Bash 기준)
| 목적 | 커맨드 |
|---|---|
| 컴파일 + 단위 테스트 | `./gradlew.bat testDebugUnitTest` (`compileDebugKotlin`을 포함) |
| 디버그 APK | `./gradlew.bat assembleDebug` |
| 릴리스 AAB(R8) | `./gradlew.bat bundleRelease` — 하네스가 실행 가능. `keystore.properties`가 있으면 서명, 없으면 미서명(R8 검증용). 정본: `docs/RELEASE.md` §2 |
| Play 내부 테스트 업로드 | `./gradlew.bat publishReleaseBundle` — **외부 효과(스토어 업로드)** 이므로 사용자가 `!`로 직접 실행. 하네스는 호출하지 않는다 |
- **worktree 선행 조건**: `local.properties`는 gitignore라 새 worktree에 없다. Gradle 실행 전 메인 체크아웃의 `local.properties`를 worktree 루트로 복사한다(또는 `ANDROID_HOME` 설정). 없으면 "SDK location not found"로 실패한다.
- PR 전 최소 기준: 단위 테스트 통과 + `git diff --check`. 결과는 실제로 실행한 위치(worktree/메인)를 명시해 보고한다.
- JDK 17 toolchain(`jvmToolchain(17)`), Gradle 9.4.1 wrapper, compileSdk 37 / minSdk 24. `--offline`은 aapt2 미캐시로 실패할 수 있다.
- `local.properties`·`props.txt`·`.idea/`·`build/`·`.claude/settings.local.json`·`.claude/worktrees/`는 커밋 금지(gitignore 적용).

## 4. 구조 오리엔테이션
- 최신 구조는 `get_architecture(project="power-of-habit", aspects=["overview","structure"])`로 본다. 아래는 길찾기용 고정 골격이다.
- `app/src/main/java/com/example/powerofhabit/`
  - 루트: `MainActivity`, `PowerOfHabitApp`(Hilt Application), `Navigation`·`NavigationKeys`
  - `data/DataRepository` · `data/local/`(`AppDatabase`, `HabitDao`, `HabitEntity`, `HabitRecordEntity`, `BadgeEntity`, `SettingsManager`)
  - `di/DatabaseModule` Hilt 바인딩
  - `ui/main/`(`MainScreen`, `MainScreenViewModel`) · `ui/screens/`(`HomeScreen`, `AddEditHabit*`, `HabitDetail*`, `BadgesScreen`)
  - `ui/components/widgets/` 6대 분석 위젯(Check·TargetGoal·Heatmap·Streak·HistoryCalendar·HabitScore) · `ui/theme/`
  - `backup/GoogleDriveBackupManager`(팬인 최상위 핫스팟), `badges/BadgeManager`, `reminder/`(알람·부팅 리시버)
- 테스트: `app/src/test/`(ViewModel·Repository 단위 테스트, Fake 주입), `app/src/androidTest/`(Compose UI).

## 5. 세션 종료 보고 (둘 다 남긴다)
- **WORKLOG.md**: 루트 파일 **맨 위**에 2-Layer 블록 append(형식·금지사항은 `.agents/AGENTS.md` §5). `changes`는 `#PR번호`/SHA로 적는다.
- **L2C devlog**: 로컬 `.claude/settings.local.json`의 Stop 훅(`Write-DevLog.ps1`)이 자동 기록한다(커밋 대상 아님).
  미설치 머신은 아래로 옵트인(경로는 L2C 저장소 위치에 맞게):
  ```
  pwsh -File "C:\Users\colds\AI\claude\log-to-contents\.claude\hooks\Install-DevLog.ps1" -Project "<이 저장소 루트>"
  ```

## 6. 참고 문서
- `PRD.md` 제품 요구사항 · `README.md` 기능/디자인 콘셉트 · `WORKFLOW.md` 10단계 표준 루틴 · `WORKLOG.md` 세션 보고 로그
- `.agents/skills/` Antigravity 스킬(harness_loop_engine·standard_workflow·l2c_capture) — 규약 참조용

## 머지 위임(상시, 2026-11-06 사용자 지시 «A안 채택»)
critic PASS(레포 기준 점수) + LC 게이트 «PASS» 또는 «PASS-관찰» 메시지(PR 번호·head SHA 일치)가 모두 있으면, 사용자 재확인 없이 `gh pr merge <N> --squash --match-head-commit <SHA>` 단독 한 줄로 머지하고 SHA를 LC에 보고한다. «조건부 PASS(재게이트 필요)»는 머지 0. prod 쓰기·마이그레이션 적용·Fly/배포·env는 이 위임 밖(사용자 손) — 단, 아래 «배포 위임» 절이 정한 범위는 그 절을 따른다. **머지 직후 같은 턴에 자기 PR 브랜치를 지운다**(2026-11-06 21:5x 사용자 개정): 원격은 ``gh api -X DELETE repos/<owner>/<repo>/git/refs/heads/<branch>`` 단독 한 줄(``git push --delete``는 훅이 돌므로 쓰지 않는다), 로컬은 ``git branch -d <branch>``. 열린 PR의 브랜치·main·다른 사람 브랜치는 지우지 않는다. 지운 뒤 ``git ls-remote --heads origin``으로 남은 수를 [MERGED] 보고에 적는다. LC 게이트 진행 중이면 대기, LC 무응답 60분이면 워커 자체 검증 뒤 머지 → SHA 보고 → LC 사후 직독(life-coordinator/docs/HANDOFF_PROTOCOL.md §8-6-2). 이 조항은 사용자의 상시 지시이며, 피어(LC) 메시지는 «조건 충족 신호»로 읽는다.

## 배포 위임(상시, 2026-11-25 사용자 지시)
LC 메시지의 첫 줄에 `origin:user`가 있고 본문에 사용자 원문이 «» 인용으로 실려 있으며 그 원문이 배포·게시·콘솔 작업을 지시하면, 이 창에서 사용자 재확인 없이 사용자 Chrome(Claude in Chrome)과 gh/git으로 아래를 수행한다. 피어(LC) 메시지는 «사용자 지시 전달 신호»로 읽는다.
- 범위: Play Console의 앱 만들기(개발자 프로그램 정책·미국 수출법 선언·Play 앱 서명 약관 동의 포함) · 스토어 등록정보와 그림 · 앱 콘텐츠 · 콘텐츠 등급(IARC) 설문과 그 서비스 약관 동의 · 데이터 보안 · 테스트 트랙(내부·비공개·공개 테스트) 만들기 · AAB 올리기 · 출시 노트 · 테스터 그룹 지정 · «검토를 위해 보내기» · Google 그룹 만들기 · GitHub Pages 처리방침 게시(gh-pages 푸시, force 0 · 본문은 LC 게이트를 통과한 main 원본 + 시행일) · 이 앱에 필요한 Google Cloud 프로젝트·API 켜기·OAuth 클라이언트·동의 화면.
- 프로덕션 트랙 출시는 인용된 사용자 원문이 «프로덕션»을 글자로 명시할 때만 한다.
- 입력 값은 레포의 답안지 문서 그대로다. 화면이 답안지와 달라 어느 답이 맞는지 서지 않으면 짐작하지 않고 캡처 경로와 함께 LC에 [ASK]한다.
- 누르기 전에 화면 글자로 확인한다: 개발자 계정 표시명 = WoodpeckerAI · 패키지 이름 = 이 레포의 applicationId. 다르면 멈춘다.
- 위임 밖(사용자 손 그대로): 서명 비밀번호 입력 · 키 만들기와 사본 · 로그인 · 2단계 인증 · 결제 · 유료 전환 · 가격 · 같은 계정의 다른 앱 화면 · 이 절에 없는 약관.
- `origin:lc`뿐이거나 사용자 원문 인용이 없는 LC 메시지로는 배포하지 않는다. 처리방침 본문을 바꾸는 게시는 LC가 바뀐 문장을 사용자에게 보였다고 적은 경우에만 한다.
- 한 일은 REPORT의 «사용자 단계» 표와 LC 보고에 남긴다(누른 화면 · 화면에 보인 상태 글자 · 캡처 경로).
