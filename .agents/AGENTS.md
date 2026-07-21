# AGENTS.md — 루프 엔지니어링 기반 자율 하네스 지침 (60줄)

## 1. 기본 원칙 (Core Principles)
- **에이전틱 지렛대**: 사용자는 프롬프트를 일일이 던지는 실행자가 아니라, 완료 조건을 정의하고 결과를 검토하는 감독관이다.
- **완전 자율 루프(Full Autonomous Loop)**: 자율 루프 시작 후 `git push`, `gradlew.bat` 등 터미널 명령어나 intermediate step마다 절대로 사용자 승인을 묻거나 멈추지 않는다. 완료 시까지 도구를 연속 실행한다.
- **중간 상태 텍스트 출력 절대 금지**: 루프 수행 중 "진행 중입니다", "대기합니다", "커밋 진행 중" 등의 중간 안내 문장을 절대 출력하지 않는다. 오직 모든 과정이 완수된 후 1회의 최종 결과만 보고한다.

## 2. 하네스 6대 요소 (Harness Architecture)
1. **오토메이션 (Automation & Triage)**: 로드맵/이슈 기반 작업 우선순위 자동 분류.
2. **격리 작업공간 (Worktree/Branch)**: 각 작업은 별도 feature 브랜치/작업공간에서 독립 진행.
3. **스킬 (Skills)**: 프로젝트 도메인 지식 및 체크리스트 사전 로딩 (`SKILL.md`).
4. **커넥터 (Connectors)**: Git, GitHub CLI(`gh`), Gradle, Google Drive 백업 등 자동화 손발 연동.
5. **서브 에이전트 (Sub-agents)**: 생성 에이전트(Builder)와 검수 에이전트(Reviewer)를 분리하여 병렬 품질 검증.
6. **상태 파일 (State Memory)**: 에이전트는 잊어도 저장소는 잊지 않는다 (`task.md`, `walkthrough.md`, `implementation_plan.md`).

## 3. 루프 실행 워크플로우 (Loop Engine Workflow)

### Stage A. 로드맵 수립 및 완료 조건 정의 (Definition of Done)
- 작업 시작 전, 사용자가 지정한 범위(단일 작업 또는 PR #N~#M 로드맵)에 대해 **완료 요건(DoD)**을 정밀 질의한다.

### Stage B. 자율 루프 실행 (Autonomous Execution Loop)
- 사용자 승인 완료 후 아래 루프를 **중간 상태 텍스트 및 질문 없이 완수 시까지 100% 도구 연속 호출로 직행**한다:
  1. **[Git Sync & Branch]**: 최신 main에서 작업 브랜치 생성.
  2. **[Parallel Sub-agent Build]**: 구현 담당 subagent와 검수 담당 subagent 병렬 운용.
  3. **[Self-Verification]**: `./gradlew.bat testDebugUnitTest` 및 `assembleDebug` 자동 실행.
  4. **[Auto-Repair Loop]**: 테스트/빌드 실패 시 상태 파일(`task.md`)을 갱신하며 자율 수정 반복.
  5. **[Commit & Push & PR]**: 표준 커밋 작성, 원격 푸시 및 `gh pr create` 발행.
  6. **[Review Bot Resolution]**: `gh api graphql`로 리뷰 코멘트 자동 수집 ➔ 수정 및 thread `Resolved` 처리.
  7. **[Auto Merge & Clean]**: PR 머지 (`gh pr merge --merge --delete-branch`) 후 로컬 `main` 동기화 및 브랜치 삭제.

### Stage C. 루프 완료 보고 & 다음 루프 질의 (Completion & Next Loop)
- 지정된 범위의 루프 완료 후 walkthrough.md 요약 및 APK 결과만 1회 전달.

## 4. 금지 및 예외 규칙 (Strict Constraints)
- **중간 텍스트 핑퐁 금지**: "테스트 진행 중", "커밋 진행 중", "푸시 진행 중" 등의 턴 넘기기용 메시지 출력 절대 금지. 도구를 끝까지 멈춤 없이 연속 실행할 것.
- **오염 방지**: `scratch/`, 개인 환경설정, `.agents/` 임시 작업 파일이 Git 커밋에 포함되지 않도록 스테이징 시 엄격 차단.
