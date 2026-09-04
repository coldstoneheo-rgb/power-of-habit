package com.example.powerofhabit

import android.app.Application
import com.example.powerofhabit.widget.WidgetRefreshObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PowerOfHabitApp : Application() {

    @Inject lateinit var widgetRefreshObserver: WidgetRefreshObserver

    override fun onCreate() {
        super.onCreate()
        // 홈 위젯은 DB 변화를 관찰해 자동 갱신한다(위젯 브로드캐스트로 프로세스가 깨어나도 여기서 시작됨).
        widgetRefreshObserver.start()
    }
}
