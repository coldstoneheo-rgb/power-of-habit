# 처리방침 게시 — GitHub Pages(`gh-pages` 가지, 한 파일)

> 2026-11-25에 Vercel 안을 이것으로 바꿨다(LC 가정, 사용자 통보). 공개 레포의 GitHub Pages는 무료다.
> **공개 게시는 사용자 승인 1줄 뒤에만 한다.** reading-diary에 대한 승인은 이 레포에 미치지 않는다.

## 1. 지금 상태(2026-11-25 17:42 실측)
`has_pages=false` · `https://coldstoneheo-rgb.github.io/power-of-habit/privacy-policy.html` 404.

## 2. 순서
1. (사용자) LC에 «power-of-habit 처리방침 게시해도 됨».
2. (워커) 원본 `privacy-policy.html`의 `[게시일 입력]`을 게시하는 날로 채운다 → `gh-pages` 가지에 같은 파일 하나만 올린다(`docs/` 전체를 사이트로 내지 않는다). 작업 폴더를 바꾸지 않는 명령 3줄(2026-11-25에 로컬 빈 저장소로 푸시까지 연습해 파일 1개 · 시행일 치환 · `[ ]` 0을 확인했다). Git Bash에서 레포 폴더, **한 줄씩**:
   ```
   git fetch origin
   D="2026년 11월 00일"; B=$(git show origin/main:docs/privacy/privacy-policy.html | sed "s/\[게시일 입력\]/$D/" | git hash-object -w --stdin); T=$(printf '100644 blob %s\tprivacy-policy.html\n' "$B" | git mktree); C=$(git commit-tree "$T" -m "docs(privacy): 개인정보처리방침 게시"); git show "$C:privacy-policy.html" | grep -n "시행일:\|게시일 입력"; echo "$C"
   git push origin <2번째 줄이 찍은 SHA>:refs/heads/gh-pages
   ```
   2번째 줄의 `D`를 그날 날짜로 바꾼다(셸을 새로 열면 변수가 사라지므로 SHA를 옮겨 적는다). 출력이 `시행일: (그날)` 1줄이고 `게시일 입력`이 없을 때만 3번째 줄로 간다. 원본의 시행일 자리도 앱 주소 PR에서 같은 날짜로 바꾼다.
3. (사용자) 레포 › Settings › Pages › «Deploy from a branch» › `gh-pages` / root › Save.

## 3. 확인
```
curl -s -o /dev/null -w "%{http_code}\n" https://coldstoneheo-rgb.github.io/power-of-habit/privacy-policy.html
```
→ `200`. 시크릿 창에서 시행일이 날짜로, 문의 메일이 주소로 보인다(`[ ]` 자리가 남지 않음).

## 4. 반영할 곳
- 앱: 수리 PR(#42)이 만드는 `ui/legal/PrivacyPolicy.kt`의 `URL` 1줄 PR(**비어 있으면 첫 빌드를 만들지 않는다**)
- 콘솔 앱 콘텐츠 «개인정보처리방침» 칸 · 자세한 설명 마지막 줄(`LISTING-2026-11.md` §3)

## 5. 이후 수정
원본을 고쳐 머지 → `gh-pages` 사본도 같은 내용으로. 새 권한 · 새 전송은 `DATA-SAFETY-2026-11.md`와 같은 PR에서 고친다.
