package com.example.powerofhabit.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class RingProgressTest {

    @Test
    fun noScore_or_zero_drawsNothing() {
        assertEquals(0f, scoreToRingProgress(null), 0f)
        assertEquals(0f, scoreToRingProgress(0f), 0f)
        assertEquals(0f, scoreToRingProgress(Float.NaN), 0f)
    }

    @Test
    fun score_mapsLinearly_andClamps() {
        assertEquals(0.5f, scoreToRingProgress(50f), 1e-6f)
        assertEquals(0.07f, scoreToRingProgress(7f), 1e-6f)
        assertEquals(1f, scoreToRingProgress(100f), 0f)
        assertEquals(1f, scoreToRingProgress(140f), 0f)
        assertEquals(0f, scoreToRingProgress(-3f), 0f)
    }
}
