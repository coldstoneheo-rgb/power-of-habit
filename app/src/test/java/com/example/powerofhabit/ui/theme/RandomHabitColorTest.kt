package com.example.powerofhabit.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomHabitColorTest {

    private val palette = PremiumMatteColors.map { it.first }

    @Test
    fun alwaysReturnsPaletteMember() {
        repeat(200) { seed -> assertTrue(randomHabitColorHex(Random(seed)) in palette) }
    }

    @Test
    fun variesAcrossSeeds() {
        val picks = (0 until 50).map { randomHabitColorHex(Random(it)) }.toSet()
        assertTrue("picks=$picks", picks.size > 5)
    }
}
