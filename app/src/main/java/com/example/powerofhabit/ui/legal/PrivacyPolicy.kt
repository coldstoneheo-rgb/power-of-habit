package com.example.powerofhabit.ui.legal

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** 앱 안 개인정보처리방침 링크. Play 정책은 등록정보와 앱 안 양쪽에 링크를 요구한다. */
object PrivacyPolicy {
    /** 공개 주소. 게시 뒤 200을 확인한 값으로 채운다. 비어 있으면 열지 않고 안내만 한다. */
    const val URL = ""

    fun open(context: Context, url: String = URL) {
        if (url.isBlank()) {
            Toast.makeText(context, "개인정보처리방침 페이지를 준비하고 있습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "브라우저를 열 수 없습니다: $url", Toast.LENGTH_LONG).show()
        }
    }
}
