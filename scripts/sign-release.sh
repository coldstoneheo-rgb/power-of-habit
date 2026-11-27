#!/usr/bin/env bash
# 서명 릴리스 AAB 한 번에: 커밋 확인 → bundleRelease → 업로드 키 ↔ AAB 서명 인증서 대조.
# 비밀번호는 프롬프트(read -s)로만 받는다 — 파일 · 인자 · 셸 기록에 남기지 않는다.
#
# 쓰는 법(Git Bash, 레포 루트):
#   scripts/sign-release.sh --expect <커밋SHA>         # 실제 서명(키스토어 release.jks · 별칭 powerofhabit)
#   scripts/sign-release.sh --expect <SHA> --keystore path/to.jks --alias name
#   scripts/sign-release.sh --self-test                # 키 없이: 임시 키를 만들어 같은 길을 끝까지 돌린다(대조 성공 + 다른 키 대조 실패 확인)
#   scripts/sign-release.sh --check-only               # 커밋 확인만
#
# 끝 코드: 0 = 대조 일치 · 1 = 커밋/작업트리 확인 실패 · 2 = 빌드 실패 · 3 = 인증서 불일치 · 4 = 사용법/도구 오류
set -uo pipefail

EXPECT=""
KEYSTORE="release.jks"
ALIAS="powerofhabit"
MODE="sign"
while [ $# -gt 0 ]; do
  case "$1" in
    --expect) EXPECT="${2:-}"; shift 2 ;;
    --keystore) KEYSTORE="${2:-}"; shift 2 ;;
    --alias) ALIAS="${2:-}"; shift 2 ;;
    --self-test) MODE="self-test"; shift ;;
    --check-only) MODE="check-only"; shift ;;
    -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
    *) echo "모르는 인자: $1" >&2; exit 4 ;;
  esac
done

ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "레포 안에서 실행하세요" >&2; exit 4; }
cd "$ROOT" || exit 4

: "${JAVA_HOME:=/c/Program Files/Android/Android Studio/jbr}"
export JAVA_HOME
KEYTOOL="$JAVA_HOME/bin/keytool"
[ -x "$KEYTOOL" ] || [ -x "$KEYTOOL.exe" ] || { echo "keytool 없음: $KEYTOOL (JAVA_HOME 확인)" >&2; exit 4; }
GRADLEW="./gradlew"; [ -f ./gradlew.bat ] && [ "${OS:-}" = "Windows_NT" ] && GRADLEW="./gradlew.bat"
AAB="app/build/outputs/bundle/release/app-release.aab"

# ---------------------------------------------------------------- 1. 커밋 확인
check_commit() {
  local branch head origin dirty
  branch="$(git branch --show-current)"
  git fetch -q origin main || { echo "[커밋] git fetch 실패" >&2; return 1; }
  head="$(git rev-parse HEAD)"; origin="$(git rev-parse origin/main)"
  dirty="$(git status --porcelain -- app gradle build.gradle.kts settings.gradle.kts gradle.properties)"
  echo "[커밋] 가지=$branch HEAD=${head:0:7} origin/main=${origin:0:7} versionCode(예상)=$(git rev-list --count origin/main)"
  if [ "$MODE" = "sign" ]; then
    [ "$branch" = "main" ] || { echo "[커밋] main 가지가 아닙니다" >&2; return 1; }
    [ "$head" = "$origin" ] || { echo "[커밋] HEAD가 origin/main과 다릅니다 — git pull --ff-only origin main 뒤 다시" >&2; return 1; }
    [ -n "$EXPECT" ] || { echo "[커밋] --expect <SHA>가 필요합니다(워커가 알려 준 SHA)" >&2; return 1; }
  fi
  if [ -n "$EXPECT" ]; then
    case "$head" in "$EXPECT"*) ;; *) echo "[커밋] HEAD ${head:0:7} ≠ 기대 $EXPECT" >&2; return 1 ;; esac
  fi
  [ -z "$dirty" ] || { echo "[커밋] 빌드 파일에 커밋 안 된 변경:"; echo "$dirty"; return 1; } >&2
  echo "[커밋] 확인 끝"
}

# ---------------------------------------------------------------- 2. 빌드
build_signed() {  # 환경변수 POH_* 가 이미 있어야 한다
  echo "[빌드] bundleRelease -PrequireSigning (--no-daemon --no-configuration-cache)"
  local before; before="$(date +%s)"
  "$GRADLEW" :app:bundleRelease -PrequireSigning --no-daemon --no-configuration-cache -q || return 2
  [ -f "$AAB" ] || { echo "[빌드] $AAB 없음" >&2; return 2; }
  local mtime; mtime="$(date -r "$AAB" +%s)"
  [ "$mtime" -ge "$before" ] || { echo "[빌드] $AAB 가 방금 만든 파일이 아닙니다" >&2; return 2; }
  echo "[빌드] $AAB ($(wc -c < "$AAB") 바이트)"
}

# ---------------------------------------------------------------- 3. 대조
sha256_of_key() {  # $1 keystore  $2 alias  — 비밀번호는 환경변수 POH_STORE_PASSWORD
  "$KEYTOOL" -list -v -keystore "$1" -alias "$2" -storepass:env POH_STORE_PASSWORD 2>/dev/null \
    | tr -d '\r' | sed -n 's/^[[:space:]]*SHA256:[[:space:]]*//p' | head -1
}
sha256_of_aab() {
  "$KEYTOOL" -printcert -jarfile "$1" 2>/dev/null | tr -d '\r' | sed -n 's/^[[:space:]]*SHA256:[[:space:]]*//p' | head -1
}
compare() {  # $1 keystore $2 alias $3 aab
  local k a
  k="$(sha256_of_key "$1" "$2")"; a="$(sha256_of_aab "$3")"
  echo "[대조] 업로드 키 SHA-256 = ${k:-(읽기 실패)}"
  echo "[대조] AAB 서명    SHA-256 = ${a:-(서명 없음)}"
  if [ -n "$k" ] && [ "$k" = "$a" ]; then echo "[대조] 일치"; return 0; fi
  echo "[대조] 불일치" >&2; return 3
}

cleanup() { unset POH_STORE_PASSWORD POH_KEY_PASSWORD POH_STORE_FILE POH_KEY_ALIAS; }
trap cleanup EXIT

case "$MODE" in
  check-only)
    check_commit || exit 1
    ;;
  sign)
    check_commit || exit 1
    [ -f "$KEYSTORE" ] || { echo "키스토어 없음: $KEYSTORE" >&2; exit 4; }
    IFS= read -rsp "키스토어 비밀번호: " POH_STORE_PASSWORD; echo
    export POH_STORE_PASSWORD POH_KEY_PASSWORD="$POH_STORE_PASSWORD" POH_STORE_FILE="$KEYSTORE" POH_KEY_ALIAS="$ALIAS"
    build_signed || exit 2
    compare "$KEYSTORE" "$ALIAS" "$AAB" || exit 3
    echo "끝: 서명 AAB 준비됨 — 올리기는 사용자 손(Play Console 또는 publishReleaseBundle)"
    ;;
  self-test)
    check_commit || exit 1
    TMP="$(mktemp -d)"; trap 'cleanup; rm -rf "$TMP"' EXIT
    # 키스토어 파일은 루트 기준 상대 경로로 읽힌다(rootProject.file) — 절대 경로를 넘긴다.
    POH_STORE_PASSWORD="selftest-$RANDOM$RANDOM"; export POH_STORE_PASSWORD
    for n in a b; do
      "$KEYTOOL" -genkeypair -keystore "$TMP/$n.jks" -alias selftest -keyalg RSA -keysize 2048 -validity 1 \
        -dname "CN=self-test-$n" -storepass:env POH_STORE_PASSWORD -keypass:env POH_STORE_PASSWORD >/dev/null 2>&1 \
        || { echo "[자체시험] 임시 키 생성 실패" >&2; exit 4; }
    done
    echo "[자체시험] 임시 키 2개 만듦(1일짜리 · 끝나면 지움)"
    export POH_KEY_PASSWORD="$POH_STORE_PASSWORD" POH_STORE_FILE="$TMP/a.jks" POH_KEY_ALIAS=selftest
    build_signed || exit 2
    compare "$TMP/a.jks" selftest "$AAB" || { echo "[자체시험] 같은 키인데 불일치 — 실패" >&2; exit 3; }
    if compare "$TMP/b.jks" selftest "$AAB" 2>/dev/null; then
      echo "[자체시험] 다른 키인데 일치로 나옴 — 실패" >&2; exit 3
    fi
    echo "[자체시험] 다른 키는 불일치로 잡음"
    echo "[자체시험] 통과 — 이 AAB는 임시 키로 서명됐으니 올리지 않는다(지금 지움)"
    rm -f "$AAB"
    ;;
esac
