package com.example.powerofhabit.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomHabitColorTest {

    private val palette = PremiumMatteColors.map { it.first }

    @Test
    fun alwaysReturnsPaletteMember() {
        repeat(200) { seed -> assertTrue(randomHabitColorHex(random = Random(seed)) in palette) }
    }

    @Test
    fun variesAcrossSeeds() {
        val picks = (0 until 50).map { randomHabitColorHex(random = Random(it)) }.toSet()
        assertTrue("picks=$picks", picks.size > 5)
    }

    @Test
    fun avoidsExcludedColors_caseInsensitive() {
        val exclude = palette.drop(1).map { it.lowercase() }
        repeat(20) { seed -> assertEquals(palette.first(), randomHabitColorHex(exclude, Random(seed))) }
    }

    @Test
    fun fallsBackToWholePalette_whenEverythingExcluded() {
        assertTrue(randomHabitColorHex(palette, Random(3)) in palette)
    }
}
