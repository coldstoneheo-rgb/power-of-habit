# 처리방침 게시 — GitHub Pages(`gh-pages` 가지, 한 파일)

> 2026-11-25에 Vercel 안을 이것으로 바꿨다(LC 가정, 사용자 통보). 공개 레포의 GitHub Pages는 무료다.
> **공개 게시는 사용자 승인 1줄 뒤에만 한다.** reading-diary에 대한 승인은 이 레포에 미치지 않는다.

## 1. 지금 상태(2026-11-25 17:42 실측)
`has_pages=false` · `https://coldstoneheo-rgb.github.io/power-of-habit/privacy-policy.html` 404.

## 2. 순서
1. (사용자) LC에 «power-of-habit 처리방침 게시해도 됨».
2. (워커) 원본 `privacy-policy.html`의 `[게시일 입력]`을 게시하는 날로 채운다 → `gh-pages` 가지에 같은 파일 하나만 올린다(`docs/` 전체를 사이트로 내지 않는다).
3. (사용자) 레포 › Settings › Pages › «Deploy from a branch» › `gh-pages` / root › Save.

## 3. 확인
```
curl -s -o /dev/null -w "%{http_code}\n" https://coldstoneheo-rgb.github.io/power-of-habit/privacy-policy.html
```
→ `200`. 시크릿 창에서 시행일이 날짜로, 문의 메일이 주소로 보인다(`[ ]` 자리가 남지 않음).

## 4. 반영할 곳
- 앱: `ui/legal/PrivacyPolicy.kt`의 `URL` 1줄 PR(**비어 있으면 첫 빌드를 만들지 않는다**)
- 콘솔 앱 콘텐츠 «개인정보처리방침» 칸 · 자세한 설명 마지막 줄(`LISTING-2026-11.md` §3)

## 5. 이후 수정
원본을 고쳐 머지 → `gh-pages` 사본도 같은 내용으로. 새 권한 · 새 전송은 `DATA-SAFETY-2026-11.md`와 같은 PR에서 고친다.
