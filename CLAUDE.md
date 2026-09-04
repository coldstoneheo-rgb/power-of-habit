# CLAUDE.md — power-of-habit (Claude Code 하네스)

'습관의 힘' Android 앱. Kotlin + Jetpack Compose(M3) + Hilt + Room, MVVM + Repository.
이 파일은 **Claude Code 세션의 진입 지침**이다. Antigravity 세션은 `.agents/AGENTS.md`를 읽는다.
두 하네스는 같은 규약(Worklog Sync·L2C devlog·PR 루틴)을 공유하며, 규약 원문은 `.agents/AGENTS.md`가 SSOT다.

## 1. 동작 모드 — 하네스 루프 (기본)
- 전역 스킬 `harness-loop-engine` 수칙을 따른다: 중간 상태 텍스트 없이 도구 연쇄 → 완료 시 1회 표 보고.
- 파이프라인: worktree(`.claude/worktrees/<name>`) → 구현 → 검증(§3) → commit → push → `gh pr create --base main`
  → 리뷰 스레드 확인(없으면 `code-review` 스킬 자체검증 후 `gh pr comment`) → `gh pr merge --squash`
  → ExitWorktree(keep) → 메인에서 `git pull --ff-only origin main` + worktree/브랜치 정리.
- **완료 = 머지 + 로컬 main 동기화.** PR 생성에서 멈추지 않는다.
- 멈춰서 묻는 예외(1회로 묶어서): 서명 빌드(키스토어 비밀번호, 사용자가 `!`로 직접), 승인 근거 없는 비가역 행위, 방향이 갈리는 진짜 모호한 결정.
- 1 PR = 1 관심사(`WORKFLOW.md` §3). 브랜치명 `feature/…`, `fix/…`, `chore/…`.

## 2. 코드 탐색 — codebase-memory-mcp 그래프 우선
- 인덱스 프로젝트명: **`power-of-habit`** (루트 `C:/Users/colds/AI/antigravity/power-of-habit`).
- 세션 시작 시 `list_projects`/`index_status`로 확인. 미인덱스·대규모 외부 변경 후에만 `index_repository(repo_path, name="power-of-habit")`.
- 구조 질의는 그래프 먼저: `search_graph`(심볼 찾기) → `trace_path`(호출자/피호출자) → `get_code_snippet`(정확한 소스).
  리터럴/비코드 텍스트는 `search_code` 또는 Grep. 인용·부정 주장 전에 `check_index_coverage`.
- 변경 영향은 `detect_changes()`로 diff→심볼 매핑.

## 3. 검증 커맨드 (Windows, Git Bash 기준)
| 목적 | 커맨드 |
|---|---|
| 컴파일 | `./gradlew.bat compileDebugKotlin` |
| 단위 테스트 | `./gradlew.bat testDebugUnitTest` |
| 디버그 APK | `./gradlew.bat assembleDebug` |
| 릴리스 서명 빌드 | 키스토어 비밀번호 필요 → 사용자가 `!`로 직접 실행 |
- PR 전 최소 기준: 컴파일 + 단위 테스트 통과 + `git diff --check`.
- JDK 17 toolchain(`jvmToolchain(17)`), Gradle 9.4.1 wrapper, compileSdk 37 / minSdk 24.
- `local.properties`·`props.txt`·`.idea/`·`build/`는 커밋 금지(gitignore 적용).

## 4. 구조 요약 (그래프 기준, 2026-09-04)
- `app/src/main/java/com/example/powerofhabit/`
  - `data/` Repository·Room(`AppDatabase`, `HabitDao`, `HabitEntity`, `HabitRecordEntity`, `BadgeEntity`, `SettingsManager`)
  - `di/DatabaseModule` Hilt 바인딩
  - `ui/main/` 메인 대시보드(`MainScreen`, `MainScreenViewModel`)
  - `ui/screens/` 습관 추가/수정·상세·배지 화면 + ViewModel
  - `ui/components/widgets/` 6대 분석 위젯(Check·TargetGoal·Heatmap·Streak·HistoryCalendar·HabitScore)
  - `backup/GoogleDriveBackupManager` 백업/복원(팬인 최상위 핫스팟), `badges/BadgeManager`, `reminder/` 알람
- 테스트: `app/src/test/`(ViewModel·Repository 단위 테스트, Fake 주입), `app/src/androidTest/`(Compose UI).

## 5. 세션 종료 보고 (둘 다 남긴다)
- **WORKLOG.md**: 루트 파일 **맨 위**에 2-Layer 블록 append(형식은 `.agents/AGENTS.md` §5). 오케스트레이터 `/project-scan` 1차 소스.
- **L2C devlog**: `.claude/settings.local.json`의 Stop 훅(`Write-DevLog.ps1`)이 자동 기록. 로컬 전용 파일이라 커밋하지 않는다.
  미설치 머신은 `log-to-contents/.claude/hooks/Install-DevLog.ps1 -Project <이 폴더>`로 옵트인.

## 6. 참고 문서
- `PRD.md` 제품 요구사항 · `README.md` 기능/디자인 콘셉트 · `WORKFLOW.md` 10단계 표준 루틴 · `WORKLOG.md` 세션 보고 로그
- `.agents/skills/` Antigravity 스킬(harness_loop_engine·standard_workflow·l2c_capture) — 규약 참조용
