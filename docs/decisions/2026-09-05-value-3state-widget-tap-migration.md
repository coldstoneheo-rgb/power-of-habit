# 결정 기록 — 수치형 3상태 · 1x1 위젯 탭 · 데이터 이전 (2026-09-05)

사용자 부재(약 4시간 자율 세션) 중 내린 결정. 사용자 지시에 따라 전문가 역할 3인(Android 플랫폼 아키텍트 / 프로덕트 디자이너·PO / QA·데이터 신뢰성 엔지니어)이 각자 의견서를 작성하고, 퍼실리테이터가 찬성 입장·가장 강한 반대·합의점을 취합했다. 원문 브리프와 의견서 전문은 세션 기록에 있으며, 여기에는 결정에 필요한 요약만 남긴다.

## 확인된 사실 (세 의견서가 공통으로 코드에서 검증)
- 기록 status는 자유 문자열(COMPLETED/FAILED/SKIPPED). 수치형은 `value >= targetValue`면 COMPLETED, 아니면 FAILED로 저장되며 **inputValue가 함께 남는다**. 통계 엔진은 `inputValue`/`targetValue`를 전혀 쓰지 않고, 모르는 status 문자열은 **미기록처럼 조용히 무시**한다.
- "FAILED 기간 부분 점수"는 횟수 비율(`completed/required`)이다. 매일 습관에서 기준미달 수행과 미기록은 통계상 완전히 같다(둘 다 0점).
- 등록 화면의 목표 유형(AT_LEAST/AT_MOST) 라디오는 저장되지 않는 죽은 UI다.
- Google 로그인 코드는 git 이력 전체에 존재한 적이 없다 → 옛 앱의 Drive 백업은 **존재하지 않는다**. 복원 코드에는 `cacheDir` 로컬 ZIP 폴백이 이미 있다. 백업 ZIP은 열린 DB의 `-wal/-shm`을 checkpoint 없이 담는다(토른 카피 위험).
- 위젯 갱신 지연(관찰 1·2)의 유력 원인: `updateAll()`이 Glance 내부 리시버→위젯 매핑에 의존해 방금 배치된 위젯을 못 찾음. 관찰 3(삭제 후 오류)은 stale 렌더의 딥링크가 살아 있음.
- 이 머신에는 adb·에뮬레이터가 없다. 이번 세션에서 검증 가능한 것은 JVM 단위 테스트·컴파일·APK/AAB 빌드뿐이다.

## 결정 1 — 수치형 3상태(성공 / 기준미달 수행 / 미수행)

| 역할 | 찬성 입장 | 가장 강한 반대 |
|---|---|---|
| 아키텍트 | A(파생 상태). 새 status를 넣으면 통계·캘린더·뱃지 분기 6곳+와 기존 행 변환이 필요하고, 모르는 문자열은 통계에서 사라진다. 파생 해석은 옛 DB를 변환 없이 즉시 3상태로 보여준다 | "FAILED+value는 사용자가 명시적으로 실패로 바꾼 기록과 구분되지 않는다"(중) → `value < target` 조건으로 대부분 흡수 |
| 디자이너 | A + 단일 판정 함수. 데이터는 이미 3상태를 담고 있고 문제는 표현. 기준미달 색 = `lerp(accent, text.secondary, 0.6)` 후 `accentForText` 보정(두 테마 모두 채도 ≈40% 남은 진회색) | "체크형 FAILED와 수치형 FAILED+value가 같은 문자열을 공유해 불일치가 영구화된다"(중) |
| QA | A + **판정을 status가 아니라 (inputValue, targetValue)에서** 계산. 현행 통계값은 A/B 어느 쪽이든 표시만 바꾸면 변하지 않음을 표로 확인 | "이미 깨진 불변식 위에 짓는다: 롱탭 SKIPPED→FAILED가 값을 보존해 FAILED+9(≥8) 행이 생기고, 성공 칩+빈 값이 FAILED+null이 되고, 목표를 낮추면 stale FAILED가 남는다"(중상) |

**합의:** 선택지 **A**. `domain/ValueOutcome`(가칭) 한 함수가 `(habitType, status, inputValue, targetValue)` → `SUCCESS | PARTIAL | NONE(=미수행) | SKIPPED` 을 결정한다. 규칙: 수치형은 `value`와 `target`으로 판정하고 status는 캐시로만 취급(value ≤ 0 또는 null → 미수행, target 없음 → 값 있으면 SUCCESS). 4개 렌더러(메인 셀·상세 캘린더·1x1 위젯·2x2 위젯)와 2개 저장 경로(메인 다이얼로그·상세 다이얼로그)가 모두 이 함수만 쓴다. QA의 반대는 저장 경로 통일 + 값 기준 판정으로 흡수한다.
**하지 않는 것:** 새 status 문자열, DB v5, 기존 FAILED 행 변환, 통계의 값 기준 부분 점수(+11점 차이는 제품 결정이라 사용자에게 남김), AT_MOST 부활(의미 설계 선행 필요). AT_MOST 라디오는 **숨긴다**(저장되는 척 제거).
**색 규칙(토큰):** 성공 = `accent`; 기준미달 = `partial(accent)` = `lerp(accent, text.secondary, 0.6)` → 앱은 `accentForText`로 AA 보정, 위젯은 다크 토큰 기준 고정값이며 `widget_bg` 대비 ≥3:1을 JVM 테스트로 고정; 미수행 = `text.disabled`(X 또는 "0 단위"). 두 번째 단서로 성공·기준미달은 Bold, 미수행은 Regular.

## 결정 2 — 1x1 위젯 탭 동작

| 역할 | 찬성 입장 | 가장 강한 반대 |
|---|---|---|
| 아키텍트 | 수치형은 A(투명 다이얼로그 액티비티). Compose이므로 사용자가 요구한 "체크 떠오름 → 입력 → 성공 시 폭죽"을 위젯 탭 흐름 안에서 구현 가능. C는 Glance에 롱탭이 없어 불가 | "액티비티가 뜨는 것 자체가 한 단계 느리고 Android 12+ 백그라운드 제한을 받는다"(하) → 사용자 탭 PendingIntent는 제한 대상 아님 |
| 디자이너 | A. 위젯의 존재 이유는 "앱 안 열고 기록". B는 그 가치를 부정. 위젯은 색+정적 마크+제목색 세 겹으로 상태 표현, 앱 셀은 폭죽(accent 2톤, 6~8입자, ≤500ms) | "투명 액티비티 콜드스타트+키보드로 즉시성이 깨지고, 갱신 버그가 남으면 입력 후에도 늦게 바뀐다"(중상) → 해당 glanceId만 직접 update, `stateVisible`로 키보드 즉시 |
| QA | A. C는 "실제 8"과 "탭한 8"이 구분 불가한 합성 데이터를 쌓아 향후 차트를 오염 | "갱신 신뢰성 버그가 남으면 사용자가 입력 성공을 못 믿는다"(중) → provider 기준 직접 update, 자정 가드 복제, 색 대비 테스트 |

**합의:** 체크형 = 탭 즉시 토글(현행) + 갱신 경로를 `AppWidgetManager` id 나열 기반 직접 `update`로 교체. 수치형 = **A** 투명 입력 액티비티(`ValueInputDialog` 컴포저블을 메인 화면과 공유, `Theme.Translucent`, `noHistory/excludeFromRecents`, 자정 가드 복제, 저장 후 해당 위젯 즉시 갱신). 삭제된 습관 딥링크는 "삭제된 습관" 안내 후 자동 복귀. 위젯 3상태 색은 결정 1의 규칙. 앱 셀 애니메이션: 성공 = accent 2톤 폭죽(≤500ms), 기준미달 = 200ms 색 크로스페이드, 미수행 = 변화 없음. **실기기 검증 불가**이므로 PR에 "실기기 미확인"을 명시한다.

## 결정 3 — 옛 앱(com.example) 데이터 이전

| 역할 | 찬성 입장 | 가장 강한 반대 |
|---|---|---|
| 아키텍트 | A(로컬 파일). 옛 앱은 debug 빌드일 확률이 높아 `adb run-as`로 DB를 뽑을 수 있어 재배포 불필요. 가져오기는 `restoreDatabase`의 로컬 ZIP 폴백을 SAF로 잇기만 하면 거의 완성 | "현 백업 형식은 -wal/-shm 미체크포인트 복사라 토른 카피 위험, 검증 없는 복원은 수 개월 데이터를 날린다"(**상**) → checkpoint·헤더·user_version·integrity_check·사전 사본·경고 다이얼로그 |
| 디자이너 | A(JSON). Drive는 사용자 부재 중 검증 불가. JSON은 라운드트립 단위 테스트 가능 | "옛 applicationId 빌드 재배포는 사용자 1명을 위한 릴리스이며 미래 변경이 섞인다"(중) → 새 앱에 가져오기 먼저, 옛 앱은 `run-as` 우선 |
| QA | A(JSON 주, DB 파일 보조). B는 이전 문제와 무관(옮길 백업이 없음). JSON 라운드트립·dedupe·FK 검증은 이번 세션에서 검증되는 유일한 이전 로직 | "옛 applicationId 재배포는 기존 설치와 같은 서명 키여야 하며 다르면 갱신 불가 = 데이터 소실 경로"(상) → 재배포 금지, run-as 경로 문서화 |

**합의:** 선택지 **A**. 순서: ① 새 앱에 **JSON 내보내기/가져오기**(습관·기록·뱃지, id 재매핑, (habitId,date) 중복은 최신 recordId 우선, 모르는 status 보존, 순수 Kotlin으로 JVM 테스트) + SAF(`CREATE_DOCUMENT`/`OPEN_DOCUMENT`) UI. ② 보조로 **DB 파일 가져오기**(SQLite 헤더·`user_version ≤ 현재`·`integrity_check` 통과 시에만, 기존 DB는 `pre_restore_<ts>.db`로 보존, 덮어쓰기 경고). ③ 사용자 귀가 후 첫 작업: `adb shell am force-stop com.example.powerofhabit` → `adb shell run-as com.example.powerofhabit` 로 `databases/power_of_habit.db(-wal,-shm)` pull → 새 앱에서 가져오기. ④ 옛 applicationId 빌드 재배포는 **하지 않는다**(서명 키 불일치 시 데이터 소실). ⑤ Drive 로그인(B)은 출시 후 마일스톤.

## 이번 세션 금지 사항 (세 의견서 공통)
1. DB 스키마 버전 인상, 새 status 문자열 도입, 기존 행 일괄 UPDATE, `fallbackToDestructiveMigration`.
2. 옛 `com.example.powerofhabit` 앱을 설치·덮어쓰기·삭제하거나 사전 사본 없이 DB를 덮어쓰는 복원 경로.

## 사용자에게 남기는 결정 (귀가 후)
- 통계에서 기준미달 수행을 값 비율로 부분 인정할지(예: 5/8잔 → 62.5점). 현재는 0점(미기록과 동일).
- AT_MOST("이하가 목표", 예: 몸무게) 유형을 살릴지 — 살리면 "미수행=0=성공" 의미 충돌을 어떻게 풀지.
- 옛 앱이 debug 빌드인지 확인(`adb shell run-as com.example.powerofhabit ls databases` 가 되면 debug).
