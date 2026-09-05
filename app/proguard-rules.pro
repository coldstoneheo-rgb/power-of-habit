# ---------------------------------------------------------------------------
# R8 규칙 (release: minify + shrinkResources). Room·Hilt·Compose·Glance는 라이브러리 consumer 규칙으로 충분하다.
# 여기에는 리플렉션/직렬화로 클래스 이름이나 멤버가 살아 있어야 하는 것만 적는다.
# ---------------------------------------------------------------------------

# --- Navigation3 NavKey ---
# kotlinx.serialization 규칙은 라이브러리 consumer 규칙(-if @Serializable 가드)으로 충분하므로 여기 복사하지 않는다.
# NavKey는 클래스 이름으로 다형 직렬화되어 saved state에 들어간다 — 이름 유지
-keepnames class * implements androidx.navigation3.runtime.NavKey

# --- Google API client / Drive (GenericJson 리플렉션) ---
-keepclassmembers class * extends com.google.api.client.json.GenericJson { *; }
-keepclassmembers class * extends com.google.api.client.util.GenericData { *; }
-keep class com.google.api.services.drive.model.** { *; }
-keep class com.google.api.client.googleapis.json.** { *; }
-dontwarn org.apache.http.**
-dontwarn javax.naming.**
-dontwarn com.google.api.client.extensions.**
-dontwarn org.ietf.jgss.**
-dontwarn org.joda.time.**

# --- 진단용: 크래시 스택의 줄 번호 보존 (mapping.txt와 함께 Play에 업로드) ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
