package com.example.powerofhabit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** 현재 시각을 읽는 함수. 시험에서만 바꾼다. */
val LocalNow = staticCompositionLocalOf<() -> LocalDateTime> { { LocalDateTime.now() } }

/**
 * 화면이 쓰는 "오늘". 앱이 메모리에 남은 채 날짜가 바뀌어도 어제 칸에 기록되지 않게,
 * 화면이 다시 보일 때(STARTED)와 켜 둔 채 자정이 지날 때 새 날짜로 바꾼다.
 */
@Composable
fun rememberCurrentDate(now: () -> LocalDateTime = LocalNow.current): LocalDate {
    var today by remember { mutableStateOf(now().toLocalDate()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val current = now()
                today = current.toLocalDate()
                delay(millisUntilNextDay(current))
            }
        }
    }
    return today
}

/** 다음 날 0시 1초까지 남은 시간. 시계가 조금 이르게 깨워도 같은 날을 다시 읽지 않도록 1초를 더한다. */
internal fun millisUntilNextDay(now: LocalDateTime): Long =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1_000
