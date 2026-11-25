# 첫 올리기 순서 — 키 만들기부터 «검토로 보내기»까지 (2026-11-25)

한 번에 한 단계씩. 답과 근거는 [CLOSED-TEST-2026-11.md](CLOSED-TEST-2026-11.md)(아래 «답안지»).
콘솔 경로는 도움말 글 기준이고 콘솔 화면을 직접 보고 적은 것이 아니다 — 화면 이름이 다르면 캡처를 보내고 멈춘다. 키 · 서명 명령은 워커가 실행해 보지 않았다(키는 사용자만 만든다).
비밀번호와 키 내용은 채팅 · 문서 · 레포 어디에도 적지 않는다.

| # | 단계 | 분 | 먼저 끝나 있어야 하는 것 |
|---|---|---|---|
| 1 | 업로드 키 만들기 | 10 | 없음 |
| 2 | 키 사본 두 곳 | 5 | 1 |
| 3 | 콘솔에서 앱 만들기 | 10 | 앱 이름 정하기(답안지 §6) |
| 4 | Google Cloud 프로젝트 + Drive API 켜기 | 10 | 없음 |
| 5 | 구글 그룹 | 5 | 없음(reading-diary에서 만들었으면 0분) |
| 6 | 처리방침 게시 | 3 | 사용자 «게시해도 됨» → 워커가 `gh-pages` 가지를 올림 |
| 7 | 등록정보 + 그림 | 15 | 3 · 6 · 워커 그림 |
| 8 | 앱 콘텐츠 · 데이터 보안 | 25 | 3 · 6 |
| 9 | 서명 빌드 | 15 | 1 · 수리 PR 머지 · 앱 안 처리방침 주소 PR 머지 |
| 10 | 비공개 트랙 초안에 올리기(아직 보내지 않음) | 10 | 3 · 5 · 7 · 8 · 9 |
| 11 | Drive 로그인 등록(OAuth) | 25 | 4 · 6 · 10 |
| 12 | «검토를 위해 보내기» | 5 | 7 · 8 · 10 · 11 |
| 13 | 본인 휴대폰 확인 → 모집 글 | 30 | 12의 검토 통과 |

12단계까지 약 2시간 20분. **1 · 2 · 4 · 5는 지금 할 수 있다.** 3은 이름을 정한 뒤(지금 이름으로 만들고 나중에 바꿔도 된다).

## 1. 업로드 키 만들기 — 10분
비밀번호를 먼저 정해 비밀번호 관리자에 저장한다(6자 이상 · 영문과 숫자 위주). 시작 메뉴 › PowerShell 새 창에 1줄:
```
cd ~\AI\antigravity\power-of-habit; & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore release.jks -alias powerofhabit -keyalg RSA -keysize 2048 -validity 10000
```
비밀번호 2번(화면에 안 보임) → 이름 등은 Enter → 마지막 «맞습니까? [no]»에는 `y`. 키 비밀번호를 따로 물으면 Enter(같은 값). **«이미 있습니다»가 나오면 멈춘다**(덮어쓰지 않는다). reading-diary 키와 다른 파일이다.
- **됐는지:** 아래 1줄에 `powerofhabit`과 `PrivateKeyEntry`가 보인다.
```
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -keystore release.jks
```

## 2. 키 사본 두 곳 — 5분
`release.jks`를 ① PC 밖 저장 장치 ② 본인만 보는 클라우드 폴더에 복사한다. 레포 안 다른 폴더 · 채팅 · 메일에는 두지 않는다(공개 레포 — git은 `*.jks`를 무시하지만 이름을 바꾼 사본은 무시하지 않는다).
- **됐는지:** 두 사본의 크기가 원본과 같다.
- 업로드 키는 잃어도 Google이 재설정해 줄 수 있다(9842756 «If compromised or lost, Google can reset it for you»). 재설정을 기다리지 않으려고 사본을 둔다.

## 3. 콘솔에서 앱 만들기 — 10분
play.google.com/console › 앱 만들기 → 이름(정한 이름) · 패키지 이름 `com.woodpeckerai.powerofhabit` · 기본 언어 한국어(ko-KR) · 앱 · 무료 · 연락 메일 `report@woodpecker-ai.com`(공개되는 메일 — 개인 메일을 넣지 않는다) → 선언 2개 · Play 앱 서명 약관 → 앱 만들기.
- 이름은 나중에 바꿀 수 있다(9859152 «You can change this later»). 패키지 이름은 못 바꾼다(«can't be deleted or re-used»).
- **됐는지:** 앱 이름 아래 글자가 `com.woodpeckerai.powerofhabit`이고 기본 언어가 한국어다.

## 4. Google Cloud 프로젝트 + Drive API — 10분
console.cloud.google.com › 프로젝트 만들기(이름 `power-of-habit`) → API 및 서비스 › 라이브러리 › **Google Drive API › 사용 설정**. 예전에 이 앱용으로 만든 프로젝트가 있으면 그것을 쓴다(다른 프로젝트면 백업 공간이 갈린다).
- **돈:** 무료다. «Standard Tier: Enabled by default at no cost» · «Usage on this tier never produces charges on a Google Cloud bill»(developers.google.com/workspace/drive/api/guides/limits, 2026-11-25). **결제 계정을 연결하라거나 Scaled Tier에 들라는 안내가 나오면 누르지 말고 멈춘다.**
- **됐는지:** 라이브러리의 Google Drive API에 «관리» 버튼이 보인다.

## 5. 구글 그룹 — 5분
reading-diary와 같은 그룹 1개를 쓴다(LC 가정). 없으면 groups.google.com › 그룹 만들기 → 가입 «웹상의 모든 사용자», 글쓰기 · 대화 보기 · 구성원 보기는 소유자/관리자만.
- **됐는지:** 그룹 주소(`…@googlegroups.com`)와 가입 링크를 적어 두었고, 시크릿 창의 다른 구글 계정에 가입 버튼이 보인다.

## 6. 처리방침 게시 — 3분
① 사용자가 LC에 «게시해도 됨» → ② 워커가 시행일을 그날로 채워 `gh-pages` 가지(한 파일)를 올림 → ③ GitHub 레포 › Settings › Pages › «Deploy from a branch» › `gh-pages` / root › Save.
- **됐는지:** 시크릿 창에서 `https://coldstoneheo-rgb.github.io/power-of-habit/privacy-policy.html`이 열리고 시행일이 날짜로, 문의 메일이 주소로 보인다 → 워커가 앱 안 주소 1줄 PR을 낸다.

## 7. 등록정보 + 그림 — 15분
사용자 증가 › 스토어 등록정보 › 기본 등록정보: [LISTING-2026-11.md](LISTING-2026-11.md) §1 ~ §3 붙여 넣기 · 아이콘 · 1024×500 · 휴대전화 스크린샷 → 저장. 카테고리 «생산성» · 연락 메일은 스토어 설정에서.
- **됐는지:** 대시보드 «스토어 등록정보 설정»에 완료 표시.

## 8. 앱 콘텐츠 · 데이터 보안 — 25분
정책 및 프로그램 › 앱 콘텐츠에서 위에서 아래로: [DATA-SAFETY-2026-11.md](DATA-SAFETY-2026-11.md) §1 · §2, 콘텐츠 등급과 타겟 연령은 LISTING §9 · §10. 광고 ID «아니요»는 워커가 병합 매니페스트를 확인한 결과를 본 뒤 고른다(DATA-SAFETY §4-2). 처리방침 칸은 6의 주소.
- **됐는지:** «조치 필요» 탭이 비었고, 데이터 보안 미리보기에 **기타 사용자 생성 콘텐츠(수집 · 선택)** 한 가지만 보인다.

## 9. 서명 빌드 — 15분
먼저 LC에 «워커 창이 쉬는지»를 묻는다. **Git Bash 창에서 한 블록씩**(둘을 함께 붙이면 다음 줄이 비밀번호로 들어갈 수 있다):
```
cd ~/AI/antigravity/power-of-habit && [ "$(git branch --show-current)" = main ] && [ -z "$(git status --porcelain -- app gradle build.gradle.kts settings.gradle.kts gradle.properties)" ] && git pull --ff-only && git log -1 --oneline || echo "멈춤: main이 아니거나 고친 파일이 있습니다"
```
```
IFS= read -rsp "키스토어 비밀번호: " POH_STORE_PASSWORD && echo && export POH_STORE_PASSWORD POH_KEY_PASSWORD="$POH_STORE_PASSWORD" POH_STORE_FILE=release.jks POH_KEY_ALIAS=powerofhabit JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" && ./gradlew :app:bundleRelease -PrequireSigning --no-daemon --no-configuration-cache; unset POH_STORE_PASSWORD POH_KEY_PASSWORD POH_STORE_FILE POH_KEY_ALIAS
```
- 비밀번호를 파일(`keystore.properties`)에 적지 않는 방법이다. `--no-daemon --no-configuration-cache`는 비밀번호가 gradle 상주 프로세스나 `.gradle` 캐시에 남는 일을 줄인다(Kotlin 컴파일 프로세스까지는 확인하지 못했다). 비밀번호는 `/`로 시작하지 않게 정한다. `-PrequireSigning`은 서명 값이 없으면 빌드를 멈춘다.
- versionCode는 main 커밋 수라 새 PR이 머지될 때마다 저절로 커진다(올릴 때 번호가 겹치면 LC에 알린다).
- **됐는지:** 첫 블록의 커밋이 워커가 알려 준 SHA와 같다 · `BUILD SUCCESSFUL` · `app/build/outputs/bundle/release/app-release.aab`가 방금 시각이다.

## 10. 비공개 트랙 초안에 올리기 — 10분
테스트 및 출시 › 테스트 › 비공개 테스트 › 트랙 관리: 국가 «대한민국» → 테스터 «Google 그룹스»에 그룹 주소 · 의견 받을 곳 `report@woodpecker-ai.com` → 새 버전 만들기 → AAB 올리기(앱 서명 기본값) → 출시 노트(답안지 §5) → **저장까지만.** «검토를 위해 보내기»는 12단계에서 누른다.
- 미리보기에 **오류**가 있으면 멈추고 캡처, 경고뿐이면 캡처를 보내고 계속.
- **됐는지:** 트랙에 초안 버전이 저장됐다.

## 11. Drive 로그인 등록 — 25분
1. **지문 모으기** — Play 콘솔 › 습관 앱 › Play로 보호됨(Protected with Play) › Play 스토어 보호 › Play 앱 서명 관리 › «앱 서명 키»의 SHA-1. 새 앱은 hybrid signing이 기본이라 **지문이 3개이고 셋 다 등록**한다(9842756) — 하나만 넣으면 일부 기기에서 «코드 10»이 난다. **초안 상태에서 아직 안 보이면** 멈추고 LC에 알린다(초안에서 보인다는 공식 문장은 못 찾았다 — 그때는 12를 먼저 하고 11을 바로 잇는다).
2. **동의 화면** — Cloud 콘솔 › Google Auth Platform › 브랜딩: 앱 이름 = 정한 이름 · 지원 메일 · 개발자 연락처 = `report@woodpecker-ai.com`. **고를 수 없고 개인 메일만 보이면 멈추고 LC에 알린다**(동의 화면은 남이 본다) · **로고는 올리지 않는다.** 대상 = 외부. 데이터 액세스 = `…/auth/drive.appdata` 추가, 분류가 «민감하지 않은 범위»인지 본다.
3. **클라이언트** — 클라이언트 만들기 › 유형 Android › 패키지 `com.woodpeckerai.powerofhabit` › 1의 SHA-1. 지문마다 1개씩.
4. **게시** — 대상 › «앱 게시» → «프로덕션 단계». «테스트»로 두면 100명까지만 되고 로그인이 7일마다 풀린다(15549945).
- 민감하지 않은 범위만 쓰면 앱 확인은 의무가 아니다(13463073 «it is not mandatory»). 동의 화면에 앱 이름 · 로고를 보이게 하려면 «brand verification»이 따로 있다 — 1.0에서는 하지 않는다. «확인 필요»가 뜨면 멈추고 캡처.
- 클라이언트 ID · 비밀은 앱에 넣지 않는다.
- **됐는지:** 클라이언트 목록에 지문 수만큼 보이고 게시 상태가 «프로덕션 단계»다.

## 12. «검토를 위해 보내기» — 5분
게시 개요 › 검토를 위해 보내기.
- **됐는지:** 게시 개요에 «검토 중». 그 뒤로는 검토가 끝날 때까지 아무것도 더 보내지 않는다(9859654 «pushed to the back of the app review queue»).

## 13. 본인 휴대폰 확인 → 모집 글 — 30분
테스터 탭에 참여 링크가 생기면 **모집 글보다 먼저** 본인 휴대폰에서:
1. 개발 중 앱(디버그)이 깔려 있으면 설정 «파일로 내보내기» → JSON이 0바이트가 아닌지 본 뒤 → 디버그 앱 삭제(서명이 달라 덮어 설치할 수 없다). **예전 앱(`com.example.powerofhabit`)은 지우지 않는다.**
2. 그룹 가입 → 참여 링크 → Play로 설치 → «파일에서 가져오기».
3. 습관 1개 만들기 · 오늘 칸 누르기 · 상세 열기 · 홈 화면 위젯 1개 · 알림 1개(2분 뒤) · 설정의 처리방침 링크.
4. **Drive:** 설정 › 백업하기 → 로그인 → «백업이 완료되었습니다.» → **오늘 칸 하나를 바꾼 뒤** → 복원하기 → 바꾸기 전 기록으로 돌아옴. «코드 10»이면 지문 등록이 빠진 것이다.
- **됐는지:** 2 ~ 4가 모두 된다. 하나라도 안 되면 글을 올리지 말고 캡처를 보낸다. 되면 모집 글([TESTER-POST-2026-11.md](TESTER-POST-2026-11.md))을 한 번 올린다.
