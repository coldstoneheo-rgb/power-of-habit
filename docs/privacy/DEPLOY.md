# 처리방침 배포 — Vercel 무료 정적 프로젝트 `power-of-habit-privacy`

> **실행은 사용자/로컬 세션.** 클라우드 세션은 Vercel에 접근하지 않는다(HO 규약 6).
> 원본: [`privacy-policy.html`](privacy-policy.html) — 정적 HTML 1개, 외부 스크립트·스타일시트·이미지 0(`grep -c "<script\|src=\|<link"` = 0).
> 블록 B의 선행 단계다. 결과 URL(HTTP 200)을 등록정보 자세한 설명 마지막 줄과 콘솔 «개인정보처리방침» 칸에 넣는다.

## 0. 준비(1회)
- Node.js 18+ (`node -v`), Vercel 계정(무료 Hobby — baby `baby-naming-privacy`와 같은 계정).
- 아래 명령은 **Windows Git Bash** 기준, 저장소 루트에서 실행.

## 1. 배포 폴더 만들기 (저장소 밖 — 커밋하지 않는다)
```bash
mkdir -p ../power-of-habit-privacy && cp docs/privacy/privacy-policy.html ../power-of-habit-privacy/index.html
```

## 2. 연락처 이메일 치환
HTML의 `__CONTACT_EMAIL__` 2곳을 **Play 콘솔 개발자 연락처와 같은 이메일**로 바꾼다(이 저장소에는 이메일을 적지 않는다).
```bash
sed -i 's/__CONTACT_EMAIL__/여기에-이메일/g' ../power-of-habit-privacy/index.html
```
```bash
grep -c "__CONTACT_EMAIL__" ../power-of-habit-privacy/index.html
```
→ `0`이어야 한다(1 이상이면 치환 누락 — 배포하지 않는다).

## 3. 배포 (외부 쓰기)
```bash
cd ../power-of-habit-privacy && npx vercel@latest link --yes --project power-of-habit-privacy
```
```bash
npx vercel@latest deploy --prod --yes
```
- 첫 `link`에서 프로젝트가 없으면 같은 이름으로 만든다. 이름이 이미 쓰였으면 Vercel이 다른 도메인(예: `power-of-habit-privacy-xxxx.vercel.app`)을 준다 — 그때는 아래 URL을 실제 값으로 바꿔 쓴다.
- 프레임워크 감지 질문이 나오면 «Other»(정적), 빌드 명령 없음, 출력 디렉터리 `.`.

## 4. 확인 (증거 = `B-1-privacy-url-200.png` 또는 출력 텍스트)
```bash
curl -s -o /dev/null -w "%{http_code} %{size_download}\n" https://power-of-habit-privacy.vercel.app
```
→ `200 <바이트>`.
```bash
curl -s https://power-of-habit-privacy.vercel.app | grep -c "습관의 힘"
```
→ 1 이상.
- Vercel «Deployment Protection»(Vercel Authentication)이 켜져 있으면 비로그인 접근이 401이 된다 → 프로젝트 Settings → Deployment Protection에서 **프로덕션 도메인은 공개**로 둔다.

## 5. 반영할 곳
| 위치 | 값 |
|---|---|
| Play 콘솔 → 앱 콘텐츠 → 개인정보처리방침 | 실측 URL |
| `docs/store-listing/LISTING-2026-11.md` §3 자세한 설명 마지막 줄 | 실측 URL(맨 URL, 꺾쇠 금지) |
| 처리방침 시행일 | 배포일이 11-02와 다르면 `privacy-policy.html`의 «시행일»을 배포일로 고쳐 커밋 후 재배포 |

## 6. 이후 수정
`docs/privacy/privacy-policy.html`을 고쳐 커밋 → 1~4를 다시 실행. 내용이 넓어지는 변경(새 권한·새 전송)은 데이터 보안 선언(`docs/store-listing/DATA-SAFETY-2026-11.md`)과 같은 PR에서 함께 고친다.
