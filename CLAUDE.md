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
- 멈춰서 묻는 예외(1회로 묶어서): 서명 빌드(키스토어 비밀번호, 사용자가 `!`로 직접), 승인 근거 없는 비가역 행위, 방향이 갈리는 진짜 모호한 결정.
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
| 릴리스 서명 빌드 | 키스토어 비밀번호 필요 → 사용자가 `!`로 직접 실행 |
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
