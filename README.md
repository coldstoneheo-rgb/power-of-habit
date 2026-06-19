# 습관의 힘 (The Power of Habit)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Database-Room%20DB-3DDC84?logo=sqlite&logoColor=white" alt="Room" />
</p>

**습관의 힘(The Power of Habit)**은 사용자가 일상적인 습관을 등록하고, 직관적인 체크와 다채로운 시각적 통계 피드백(점수, 히트맵, 스트릭)을 통해 지속적인 동기부여를 얻는 Android 앱입니다. "하루에 책 10쪽 읽기", "하루에 물 1L 마시기" 등 목표로 한 습관이 '진짜 습관'이 될 수 있도록 추적, 기록, 관리해 줍니다.

---

## 🎨 핵심 디자인 콘셉트

> **"최소한의 요소로 최대한의 정보 시각화"**

* **다크 모드 기반 '포인트 컬러 시스템'**: 완전한 블랙에 가까운 다크 그레이 계열 배경을 사용해 눈의 피로도를 낮추고, 습관마다 고유의 테마 색상(예: 이불 개기는 오렌지, 수학 문제는 스카이블루 등)을 매칭하여 시각적 인지(Cognitive Parsing)를 극대화했습니다.
* **여백 중심(Whitespace-Driven) 레이아웃**: 불필요하게 굵은 테두리와 경계선을 배제하고, 투명도를 지닌 얇은 구분선과 명도 차이를 통해 공간을 분할합니다.
* **부드러운 모서리(Rounded Corner)**: 모든 카드와 입력 폼 등에 스퀘어클(Squircle) 형태의 라운드 힌트를 주어 트렌디하고 소프트한 감성을 연출했습니다.

---

## 🚀 핵심 기능

### 1. 초슬림 메인 대시보드
* **12개+ 한 화면 노출**: 세로 여백 및 요소를 슬림화하여 스크롤 없이도 한 화면에 **12개 이상의 습관 리스트**를 한눈에 볼 수 있도록 최적화했습니다.
* **컴팩트 4일 체크 영역**: 우측 체크 영역 가로폭을 `152.dp`로 밀도 있게 구성하여 왼쪽 습관 이름이 `'...'`로 잘리는 현상을 완벽히 방지했습니다.
* **직관적인 체크 인터페이스**: 날짜별 아이콘을 단일 탭하면 완료(`O`), 다시 탭하면 미완료(`X`), 롱 탭 시 건너뛰기(`-`)로 상태가 전환되며 수치형 습관은 팝업을 통해 숫자를 입력합니다.

### 2. 6x6 (36색) 완전한 원형 테마 피커
* 습관 등록/수정 시 가로 6개, 세로 6개 구조의 **총 36종 매트 컬러 팔레트**를 제공합니다.
* 다이얼로그의 너비 제약에 맞춰 원이 세로 타원으로 찌그러지던 버그를 `requiredSize(32.dp)`로 고정하여 모든 디바이스에서 완전한 원형으로 깔끔하게 정렬됩니다.

### 3. 감성적인 6대 분석 위젯
* **체크 위젯**: 오늘 및 최근 4일간의 성취도를 체크하고 상태를 업데이트하는 코어 위젯.
* **목표 위젯**: 주/월/분/연 단위 탭을 넘나들며 목표 대비 달성률(%)을 프로그레스 바로 제공.
* **빈도 히트맵 위젯**: 요일과 월이 교차하는 지점에 성공 빈도에 따라 원의 크기가 커지는 도트 매트릭스 차트.
* **연속 수행 위젯**: '현재 스트릭' 및 '최고 스트릭' 연속 달성 일수를 둥근 알약 형태(Pill-shaped)의 막대로 시각화.
* **이력 위젯**: 성공(테마색 원), 실패(작은 회색점) 등의 상태를 마킹하는 컴팩트한 월간 달력 그리드.
* **습관 점수 위젯**: 그리드 선을 숨긴 채 지수 이동 평균(EMA) 알고리즘을 사용해 학습 동향 및 상승/하락 트렌드를 꺾은선으로 투영.

---

## 🛠 기술 스택

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose (Material 3)
* **Architecture**: MVVM Architecture + Repository Pattern
* **Dependency Injection**: Hilt (hilt-navigation-compose)
* **Database**: Room DB (SQLite Wrapper)
* **Asynchronous**: Kotlin Coroutines & StateFlow / SharedFlow
* **Build System**: Gradle Kotlin DSL (`.gradle.kts`)

---

## 🗄 데이터베이스(DB) 구조

SQLite / Room DB 스키마 구조는 다음과 같이 긴밀하게 설계되었습니다.

### 1. `Habits` (습관 마스터 테이블)
| 필드명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| `habit_id` | Integer (PK) | 습관 고유 ID (자동 증가) |
| `title` | String | 습관 이름 (예: 이불 개기) |
| `question` | String | 수행 여부를 묻는 질문 (예: 잠자리 정리 했나?) |
| `frequency_type` | String | 주기 종류 (`DAILY` / `INTERVAL` / `WEEKLY_COUNT` 등) |
| `frequency_value`| String | 주기에 따른 세부 값 (예: 요일, 숫자 등) |
| `reminder_time` | Time | 알림 시간 (Null 허용, 기본 "09:00") |
| `theme_color` | String | 고유 테마 헥사코드 (예: #FF5722) |
| `habit_type` | String | 기록 형태 (단순 체크형 `CHECK` / 수치 입력형 `VALUE`) |
| `unit` | String | 수치 입력형일 때의 단위 (예: 쪽, kg, 회 - Null 허용) |

### 2. `HabitRecords` (습관 수행 이력 테이블)
| 필드명 | 데이터 타입 | 설명 |
| :--- | :--- | :--- |
| `record_id` | Integer (PK) | 기록 고유 ID |
| `habit_id` | Integer (FK) | `Habits` 테이블 참조 |
| `date` | Date | 수행 기준 날짜 (YYYY-MM-DD) |
| `status` | String | 수행 상태 (`COMPLETED`: 완료, `FAILED`: 미완료, `SKIPPED`: 건너뛰기) |
| `input_value` | Float | 수치 입력형일 때의 실제 기록 값 (Null 허용) |

---

## 📦 빌드 및 디버깅 자동화 가이드

본 프로젝트에는 디버그 APK 빌드가 완료되는 즉시 구글 드라이브로 파일을 저장하고 정돈해 주는 커스텀 빌드 태스크가 탑재되어 있습니다.

### Google Drive 자동 복사 활성화 방법
1. 프로젝트 루트 디렉토리의 `local.properties` 파일을 엽니다.
2. 아래와 같이 APK 파일이 최종적으로 복사될 로컬 Google Drive 동기화 디렉토리 경로를 추가합니다:
   ```properties
   google.drive.apk.dir=G:/내 드라이브/AI-outputs/Android Studio/powerofhabit/apk
   ```
3. Gradle 빌드를 실행합니다:
   ```bash
   ./gradlew.bat assembleDebug
   ```
4. 빌드가 완료되면 `app/build/outputs/apk/` 하위에 위치한 디버그 APK가 설정된 디렉토리에 **타임스탬프와 빌드 코드(VersionCode)를 조합한 파일명**으로 안전하게 자동 복제됩니다.
   * 복제 예시: `power-of-habit-v1.0_c1_20260619_2147-debug.apk`
   * *만약 `local.properties` 경로가 비어있고 디폴트 구글 드라이브 마운트 경로도 탐색할 수 없다면, 빌드가 에러로 중단되지 않고 복사 단계를 건너뛴 후 정상 완료됩니다.*
