# 습관의 힘 — 디자인 가이드 (Design Tokens v1)

> 레퍼런스 캡처 7장(`D:\사진\power-of-habit\references`, 2026-05-29 ~ 07-23)을 역설계해 얻은 분석과,
> 그 분석을 **표절 없이 재해석한** 이 앱의 디자인 토큰 정의. 코드 반영 위치: `app/src/main/java/com/example/powerofhabit/ui/theme/`.
> 레퍼런스는 오픈소스 습관 트래커의 다크 모드 화면(리스트·상세·런처 위젯 프리뷰)이다. 여기서 "그대로 가져온 것"은 없다 — 무드·문법을 추출하고 값·형태는 새로 정했다.

---

## Part A. 레퍼런스 역설계 분석

### A1. 비주얼 테마 & 브랜드 무드
**키워드 5**: 절제된(Restrained) · 계기판적(Instrumental) · 무광 다크(Matte Dark) · 데이터 우선(Data-first) · 조용한 컬러 포인트(Quiet Accent)

무드를 만드는 시각 장치:
- **거의 검은 무채 배경 위 단 하나의 유채색**: 화면당 색은 습관 고유색 1개(+회색 계조)뿐. 색이 "장식"이 아니라 "식별자"로 작동한다.
- **텍스트가 곧 UI**: 아이콘·일러스트가 거의 없다. X/✓ 마크와 숫자+단위(0 kg, 3 km)가 셀을 채운다. 인풋 필드도 밑줄 없는 텍스트다.
- **얕은 깊이감**: 그림자 없음. 배경(#1E1E1E대)과 카드(#2A2A2A대)의 명도 차 5~6%와 1px 구분선으로만 층을 나눈다.
- **직각에 가까운 곡률**: 카드·바 모서리 2~4dp. 딱딱하지만 "계기판" 인상을 만든다.
- **밀도**: 리스트 행 높이 ≈ 56dp, 한 화면에 12~14개. 여백은 넉넉하지 않고 "정렬"로 정돈감을 만든다.

### A2. 컬러 팔레트 & 사용 규칙 (추정값)
| 역할 | 추정 Hex | 관찰 |
|---|---|---|
| Background | `#1F1F1F` | 리스트 배경. 순흑이 아닌 짙은 회색 |
| App bar | `#121212` | 배경보다 더 어두워 상단이 가라앉음 |
| Surface(카드/행) | `#2A2A2A` ~ `#2C2C2C` | 행 사이 1px `#181818` 구분선 |
| Text primary | `#FFFFFF` (약 90% 불투명) | 제목·숫자 |
| Text secondary | `#9E9E9E` | 요일·단위·축 레이블 |
| Disabled mark | `#5C5C5C` | 미완료 X, 0 값 |
| Accent(습관별) | `#64B5F6`(블루), `#FFB74D`(오렌지), `#81C784`(그린), `#BA68C8`(퍼플), `#F48FB1`(핑크), `#D7CCC8`(베이지) 등 | Material 300톤 수준의 저채도 파스텔 |
| Accent tint | Accent × 50~60% | 스트릭 바의 비최고 구간, 이력 막대 |
| Error | (관찰 안 됨) | — |

사용 비율: 무채 배경/표면 **≈ 85%**, 흰·회색 텍스트 **≈ 12%**, 액센트 **≈ 3%**. 60-30-10보다 훨씬 극단적인 **85-12-3**. 액센트는 (1) 습관 이름 (2) 섹션 제목 (3) 완료 마크·차트 요소에만 쓰이고 버튼·앱바에는 쓰지 않는다.

대비/가독성: 흰 텍스트 vs `#2A2A2A` ≈ 12:1, 회색 `#9E9E9E` vs `#2A2A2A` ≈ 5.6:1(AA 통과). 액센트 텍스트(`#64B5F6` vs `#2A2A2A`) ≈ 7:1. 라이트 모드는 관찰되지 않았다.

### A3. 타이포그래피 & 텍스트 위계
- 폰트: 시스템 산세리프(Roboto + 한글 Noto Sans CJK). 기하학적이라기보다 **휴머니스트-중립**. 자간 조정 없음.
- 위계는 크기보다 **색·굵기**로 만든다.

| 레벨 | 추정 크기 | 굵기 | 색 |
|---|---|---|---|
| App bar title | 20sp | Medium | 흰색 |
| 습관 이름(리스트) | 16sp | Regular | 액센트 |
| 섹션 제목(상세) | 16sp | Medium | 액센트 |
| 본문/숫자 | 14sp | Regular/Medium | 흰색 |
| 캡션(요일·단위·축) | 11~12sp | Regular | 회색 |

행간은 시스템 기본(≈1.4). 숫자는 셀 안에서 세로로 두 줄(값 / 단위) 스택.

### A4. 컴포넌트 스타일 & 레이아웃 문법
- **Corner radius**: 카드 2~4dp(거의 Sharp), 프로그레스/스트릭 바 2dp, 런처 위젯 프리뷰 16dp(런처 규칙).
- **Elevation**: 그림자 0. **면 분할(Flat color block) + 1px 구분선** 방식. 카드 테두리 없음.
- **Spacing**: 8dp 그리드. 리스트 행 56dp, 좌우 패딩 16dp, 상세 섹션 간 8dp(구분선), 섹션 내부 패딩 16dp.
- **체크 셀**: 4열 고정, 열 폭 ≈ 56dp, X는 회색, ✓는 액센트, 수치는 "값(굵게) / 단위(회색)" 2줄.
- **차트**: 격자선 1px 회색 10% 불투명, 축 레이블 회색, 데이터 점은 액센트 원 + 연결선.
- **스트릭 바**: 좌우에 날짜 텍스트, 가운데 막대. 최장 기록만 밝은 액센트, 나머지는 어두운 틴트.
- **빈도 도트 매트릭스**: 요일 7행 × 월 12열, 빈도에 따라 원 반지름 3단계.

### A5. 정보 구조(IA) & 안드로이드 UX 패턴
- **Top App Bar 단일 진입**: `+`(추가), 필터, 오버플로 메뉴. Bottom Nav·FAB 없음. 화면 수 = 리스트 → 상세 → 편집, 3단.
- **리스트 = 대시보드**: 행 좌측(이름+도넛) 탭 → 상세, 우측 셀 탭/롱탭 → 기록 토글. 한 행이 두 종류 터치 타깃을 가진다.
- **상세 = 세로 스택 카드**: 목표 → 점수 → 이력 → 캘린더 → 최고 연속 → 빈도. 각 카드에 기간 드롭다운(주/월/…).
- **Material 접점**: 앱바·오버플로·드롭다운은 Material 2 문법. 변형 포인트는 (1) 색 역할을 "브랜드"가 아니라 "항목 식별"에 쓰는 것, (2) 카드 elevation 제거, (3) 컬러 섹션 제목.

---

## Part B. 최종 산출물 — 습관의 힘 디자인 가이드 (재해석)

레퍼런스에서 **가져오는 것**: 85-12-3 무채 지배 + 항목 식별 액센트, 그림자 없는 명도 층, 텍스트 중심 셀, 세로 스택 상세.
**바꾸는 것**: 곡률(직각 → 스퀘어클 12dp), 배경(중성 회색 → 웜 블랙), 타입 스케일(색 위계 → 크기·자간 위계 병행), 층 구조(구분선 → 톤 레이어 4단 + 헤어라인), 라이트 모드 정의.

### B1. 무드 키워드
**웜 블랙(Warm Black) · 스퀘어클(Squircle) · 헤어라인(Hairline) · 단색 포인트(Mono-accent) · 여백 호흡(Breathing Room)**

### B2. 컬러 토큰

#### 다크(기본)
| 토큰 | 값 | 역할 |
|---|---|---|
| `bg.base` | `#101012` | 화면 바닥. 파란기 도는 웜 블랙(순흑 아님) |
| `bg.layer1` | `#17171A` | 앱바·하단 영역 |
| `bg.layer2` | `#1E1E22` | 카드·리스트 행 |
| `bg.layer3` | `#26262B` | 카드 내부 강조 블록, 입력 필드, 바 트랙 |
| `line.hair` | `#FFFFFF` @ 8% | 구분선. 항상 알파, 절대 불투명 회색 금지 |
| `line.focus` | `#FFFFFF` @ 16% | 포커스/선택 테두리 |
| `line.strong` | `#FFFFFF` @ 28% | 입력 필드 등 "있어야 보이는" 테두리 (M3 `outlineVariant`) |
| `text.primary` | `#F2F2F7` | 제목·숫자 |
| `text.secondary` | `#9A9AA3` | 캡션·단위·축 |
| `text.disabled` | `#55555E` | 미완료 마크, 비활성 |
| `accent.habit` | 습관별 36색(기존 `PremiumMatteColors`) | 이름·완료 마크·차트·섹션 제목 |
| `accent.habit.dim` | accent @ 45% | 바 트랙 위 비강조 구간, 이력 막대 |
| `accent.habit.glow` | accent @ 12% | 선택 셀 배경, 캘린더 오늘 |
| `status.success` | accent.habit | 별도 초록 쓰지 않음(색 = 습관) |
| `status.skip` | `#9A9AA3` | 건너뜀 `–` |
| `status.fail` | `#D26A6A` | 사용자가 "실패"로 표시한 날(캘린더). 저채도 적색, 시스템 오류색과 구분 |
| `on.accent` | 액센트 명도 > 0.4 → `#1C1C1E`, 아니면 `#F2F2F7` | 액센트로 채운 면(헤더·선택 칩·완료 날짜) 위의 잉크 |
| `status.error` | `#FF6B6B` | 삭제·복원 실패 등 시스템 오류만 |

#### 라이트
| 토큰 | 값 |
|---|---|
| `bg.base` `#F4F4F6` · `bg.layer1` `#FFFFFF` · `bg.layer2` `#FFFFFF` · `bg.layer3` `#ECECF0` |
| `line.hair` `#000000` @ 8% · `line.strong` `#000000` @ 24% · `text.primary` `#1C1C1E` · `text.secondary` `#6B6B75` · `text.disabled` `#B5B5BD` · `status.fail` `#B94A4A` |
| 액센트는 다크와 동일 팔레트. 바탕 위 액센트 글자는 `accentForText`가 4.5:1(AA)에 닿을 때까지 다크는 밝게·라이트는 어둡게 당겨 자동 보정 |

#### 비율 규칙
- 화면 면적 **85%** 무채(bg.*), **12%** 텍스트(text.*), **3%** 액센트. 액센트 면적이 커지는 순간(큰 버튼·앱바 배경) 이 앱이 아니다.
- 액센트를 쓰는 곳은 정확히 5군데: 습관 이름 / 완료 마크 / 차트 데이터 / 섹션 제목 / 도넛 진행. **버튼 배경·앱바·FAB에 금지**.
- 한 화면에 액센트 색은 리스트에서 N개(행마다), 상세에서 1개.

### B3. 타이포그래피 토큰
폰트: 시스템 산세리프(Roboto/Noto Sans KR) 유지. 대신 **자간을 음수로** 당겨 헤드라인의 밀도를 높이고, 숫자는 `tabular-nums`로 열 정렬.

| 토큰 | 크기/행간 | 굵기 | 자간 | 용도 |
|---|---|---|---|---|
| `display` | 28/34sp | SemiBold | −0.8sp | 상세 상단 큰 숫자(점수) |
| `headline` | 20/26sp | SemiBold | −0.5sp | 앱바 제목 |
| `title` | 16/22sp | SemiBold | −0.3sp | 섹션 제목, 다이얼로그 제목 |
| `body` | 14/20sp | Regular | −0.2sp | 습관 이름, 본문 |
| `bodyStrong` | 14/20sp | SemiBold | −0.2sp | 셀 숫자, 강조 값 |
| `label` | 12/16sp | Medium | 0 | 요일 헤더, 탭, 칩 |
| `caption` | 11/14sp | Regular | 0 | 단위, 축 레이블, 힌트 |

비율: display:headline:title:body:caption ≈ 2.5 : 1.8 : 1.45 : 1.27 : 1 (1.15~1.2 배수 스케일). 위계는 **크기 + 굵기 + 색** 세 축을 모두 쓰되, 한 요소에서 두 축 이상을 동시에 최대치로 올리지 않는다.

### B4. 형태(Shape) 토큰
| 토큰 | 값 | 용도 |
|---|---|---|
| `radius.xs` | 4dp | 체크 셀, 캘린더 날짜 칩 |
| `radius.sm` | 8dp | 입력 필드, 드롭다운, 작은 칩 |
| `radius.md` | 12dp | **카드·리스트 행(기본)** |
| `radius.lg` | 20dp | 다이얼로그, 바텀시트 |
| `radius.pill` | 999dp | 프로그레스/스트릭 바, 필터 칩 |
스퀘어클 인상은 12dp 카드 + 8dp 내부 요소의 **2단 곡률 차**에서 나온다. 레퍼런스의 2dp 직각은 쓰지 않는다.

### B5. 간격(Spacing) 토큰
4dp 베이스, 8dp 리듬. `space.1`=4 · `space.2`=8 · `space.3`=12 · `space.4`=16 · `space.5`=20 · `space.6`=24 · `space.8`=32
- 화면 좌우 패딩 `space.5`(20dp) — 레퍼런스(16)보다 한 단계 넓혀 "호흡".
- 카드 내부 패딩 `space.4`(16dp), 카드 사이 `space.3`(12dp) — 구분선 대신 간격으로 분리.
- 리스트 행 높이 **48dp**(레퍼런스 56보다 슬림, PRD "12개+ 한 화면"), 행 사이 `space.1`(4dp).
- 체크 열 폭 38dp × 4열 = 152dp 고정(현행 유지).

### B6. 깊이(Elevation) 규칙
- 그림자 **0**. 층은 `bg.layer0→3` 톤 계단으로만 표현한다.
- 층 위에 층을 올릴 때는 반드시 한 단계만 올린다(layer2 위에 layer3, layer1 위에 layer2).
- 경계가 필요한 곳만 `line.hair`(알파 8%). 카드 외곽 테두리는 기본 없음, 포커스 시 `line.focus`.
- 기존 `MetalBorderBrush`(그라데이션 테두리)는 **layer2 카드에서 제거**하고 뱃지 화면의 메달 표현에만 남긴다.

### B7. 컴포넌트 규격
| 컴포넌트 | 규격 |
|---|---|
| 리스트 행 | bg.base 위 flat(카드 없음, 밀도 우선 — PRD "12개+ 한 화면") · 행 사이 line.hair · 높이 48 · 좌: 도넛 18dp + 이름(bodyStrong, text.primary) · 우: 체크 셀 4열 |
| 체크 셀 | 38×40 · radius.xs · 미완료 `×` text.disabled · 완료 `✓` accent · 건너뜀 `–` status.skip · 수치는 bodyStrong 값 / caption 단위 2줄 · 탭 피드백 accent.glow 배경 |
| 섹션 카드(상세) | layer2 · radius.md · 패딩 space.4 · 제목 title(accent) + 우측 기간 드롭다운(label) |
| 프로그레스 바 | 높이 8 · radius.pill · 트랙 layer3 · 채움 accent · 우측 % bodyStrong |
| 스트릭 바 | 높이 24 · radius.pill · 최고 기록 accent, 나머지 accent.dim · 막대 안 숫자 bodyStrong |
| 도트 매트릭스 | 셀 24 · 원 반지름 빈도 비례 · 색 accent, 빈도 0은 line.focus 헤어라인 점(격자 위치 안내) |
| 점수 라인 | 격자선 line.hair · 축 캡션 · 데이터 점 accent 4dp · 선 1.5dp |
| 캘린더 칩 | 28×28 · radius.xs · 완료 accent 채움 + on.accent · 실패 status.fail @16% 채움 + status.fail 글자 · 건너뜀 line.hair 채움 · 오늘 accent.glow 테두리 |
| 입력 필드 | layer3 · radius.sm · 높이 48 · 라벨 caption 상단 · 밑줄 없음 |
| 버튼(주) | text 버튼 우선. 채움 버튼이 꼭 필요하면 layer3 배경 + text.primary(액센트 배경 금지) |
| 다이얼로그 | layer2 · radius.lg · 패딩 space.6 |

### B8. IA & 인터랙션 원칙
- 화면 3단(리스트 → 상세 → 편집) 유지. Bottom Nav·FAB 도입 금지(밀도·무채 규칙과 충돌).
- 리스트 행 좌측 탭 = 상세, 셀 탭 = 완료 토글, 셀 롱탭 = 건너뜀, 수치형 셀 탭 = 입력 팝업(PRD 3.1).
- 상세는 세로 스택, 순서 고정: 한눈에 보기 → 점수 → 연속 → 목표 → 이력 → 빈도.
- Material 3 접점: 컴포넌트 API(M3)를 쓰되 `colorScheme`은 위 토큰으로 오버라이드. `surfaceVariant`=layer3, `outline`=line.hair, `primary`=accent(습관 컨텍스트 밖에서는 `#F2F2F7` 중립).

### B9. 적용 현황
적용됨 (PR feature/design-guide-tokens)
- `ui/theme/Tokens.kt` — 색(다크/라이트)·간격·곡률 토큰, `LocalHabitTokens`/`HabitTheme.colors`, `HabitShapes`, `accentDim/accentGlow/accentForText`
- `Type.kt` — B3 타입 스케일을 M3 Typography에 매핑 · `Color.kt`/`Theme.kt` — colorScheme을 토큰에서 파생, 레거시 색 이름은 토큰 별칭
- 메인: 구분선 line.hair, 캡션 text.secondary, 곡률 토큰 · 체크 셀: 미완료 text.disabled, 건너뜀 status.skip, 수치 text.primary/secondary
- 상세: `CardSection` layer2 + radius.md + 액센트 제목(테두리 제거), `StatCard`·힌트 블록 layer3, 헤더(on.accent 잉크)/다이얼로그/칩 토큰화, 캘린더 완료 = on.accent·실패 = status.fail
- M3 매핑: `surface*`·`surfaceContainer*`를 톤 계단에, `outline`=text.disabled, `outlineVariant`=line.strong, `shapes`=HabitShapes(카드 medium·내부 small·다이얼로그 large)
- 프로그레스/스트릭 바: pill + 트랙 layer3 + accentDim

후속
- `primary`(HabitOrange)가 아직 버튼·스위치·로딩에 남아 있음 → B2 "버튼 배경 액센트 금지"에 맞춰 layer3 버튼으로 교체
- 등록/수정 화면·뱃지 화면·백업 다이얼로그는 토큰 미적용(`Color.White` 등 하드코딩 잔존)
- 실기기 스크린샷으로 다크/라이트 대비 확인
