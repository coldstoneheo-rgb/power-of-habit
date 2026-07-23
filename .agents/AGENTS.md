# AGENTS.md — 루프 엔지니어링 기반 자율 하네스 지침 (60줄)

## 1. 기본 원칙 (Core Principles)
- **에이전틱 지렛대**: 사용자는 프롬프트를 일일이 던지는 실행자가 아니라, 완료 조건을 정의하고 결과를 검토하는 감독관이다.
- **완전 자율 루프(Full Autonomous Loop)**: 자율 루프 시작 후 터미널 명령어나 intermediate step마다 절대로 사용자 승인을 묻거나 멈추지 않는다. 완료 시까지 도구를 연속 실행한다.
- **L2C 자동 캡처 연동**: 세션 실행 중/종료 시 프롬프트, 편집파일, 실행명령을 L2C devlog 원재료 포맷으로 자동 기록/갱신한다.

## 2. 하네스 6대 요소 (Harness Architecture)
1. **오토메이션 (Automation & Triage)**: 로드맵/이슈 기반 작업 우선순위 자동 분류.
2. **격리 작업공간 (Worktree/Branch)**: 각 작업은 별도 feature 브랜치/작업공간에서 독립 진행.
3. **스킬 (Skills)**: 프로젝트 도메인 지식, L2C 캡처 규칙 및 표준 워크플로우 사전 로딩 (`SKILL.md`).
4. **커넥터 (Connectors)**: Git, GitHub CLI(`gh`), L2C Logging Pipeline 등 자동화 손발 연동.
5. **서브 에이전트 (Sub-agents)**: 생성 에이전트(Builder)와 검수 에이전트(Reviewer)를 분리하여 병렬 품질 검증.
6. **상태 파일 (State Memory)**: 에이전트는 잊어도 저장소는 잊지 않는다 (`task.md`, `walkthrough.md`, `implementation_plan.md`, devlog).

## 3. 루프 실행 워크플로우 (Loop Engine Workflow)

### Stage A. 로드맵 수립 및 완료 조건 정의 (Definition of Done)
- 작업 시작 전, 사용자가 지정한 범위에 대해 **완료 요건(DoD)**을 정밀 질의한다:
  - 기능적 요구사항 & UX 기준
  - 빌드/테스트 성공 조건 & 자동화 검증 명령어
  - L2C devlog 로그 산출 및 시각 자산(이미지/Mermaid) 대상 확인

### Stage B. 자율 루프 실행 (Autonomous Execution Loop)
- 사용자 승인 완료 후 아래 루프를 **승인 요청 및 질문 없이 완수 시까지 자율 연속 실행**한다:
  1. **[Git Sync & Branch]**: 최신 main에서 작업 브랜치 생성.
  2. **[Parallel Sub-agent Build]**: 구현 담당 subagent와 검수 담당 subagent 병렬 운용.
  3. **[Self-Verification]**: 프로젝트 검증 커맨드 자동 실행.
  4. **[Auto-Repair Loop]**: 테스트/빌드 실패 시 상태 파일(`task.md`)을 갱신하며 자율 수정 반복.
  5. **[Commit & Push & PR]**: 표준 커밋 작성, 원격 푸시 및 PR 발행.
  6. **[L2C Devlog Sync]**: L2C 캡처 지침에 맞춰 devlog 파일 Direct Write 및 멱등 교체.

## 4. L2C Devlog 캡처 규칙 (L2C Integration & 정식 프로젝트명 통합)
- **저장 경로**: `D:\AI\claude\L2C\devlog\<정식프로젝트명>\<YYYY-MM-DD>.md` (Direct Write, 아티팩트 저장 절대 금지).
- **논리 프로젝트 통합 수칙**: 작업 폴더명이 변형 폴더(예: `-by-agy`, `-antigravity`)라 하더라도 devlog 디렉터리 및 세션 제목의 `[프로젝트]` 태그는 접미사를 뗀 **정식 이름**을 사용한다.
- **이미지 규격**: `D:\AI\claude\L2C\devlog\<정식프로젝트명>\shots\<YYYYMMDD>_<세션ID8자>_<설명>.png` (`shots/` 하위 폴더 위치 & 본문 `![설명](shots/<파일명>.png)` 첨부).
- **origin 필드**: `Antigravity · 생성` (필수 표기, 출처 추적의 시작점).
- **동시쓰기 안전 & 멱등성**: 쓰기 직전 파일을 재읽어 동일 세션 ID 블록만 교체 (덮어쓰기로 인한 타 세션 유실 방지).
- **Claude Code 100% 호환**: 기존 `/l2c-draft` 페르소나 파이프라인 호환성 완전 유지.
- **Antigravity 특장점 시각화**:
  - **인포그래픽 / 이미지 캡처**: 관련 이미지/인포그래픽 생성 시 렌더링된 PNG를 `shots/` 폴더에 저장하고 devlog 내 상대경로 포인터로 첨부.
  - **Mermaid 다이어그램**: 원문 devlog 흐름 보강용으로 작성하되 독자 대상 시각자료는 렌더링된 PNG로도 확보.
- **마스킹**: 쓰기 전 비밀번호/API 키/토큰은 `***REDACTED***`로 가린다.

## 5. 금지 및 예외 규칙 (Strict Constraints)
- **모든 중간 질의 금지**: 자율 루프 도중 중간 빌드/커밋마다 "진행할까요?" 질의 절대 금지.
- **오염 방지**: `scratch/`, 개인 환경설정, `.agents/` 임시 작업 파일이 Git 커밋에 포함되지 않도록 스테이징 시 엄격 차단.
- **불확실성 차단**: 명세가 모호할 경우 Stage A에서 질문을 통해 명확히 정제한 후 루프 진입.
