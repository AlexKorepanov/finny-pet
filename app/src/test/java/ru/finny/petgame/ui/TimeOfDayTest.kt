package ru.finny.petgame.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.ui.theme.TimeOfDay

class TimeOfDayTest {

    @Test
    fun hoursMapToScenes() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.forHour(0))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.forHour(4))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.forHour(5))
        assertEquals(TimeOfDay.DAY, TimeOfDay.forHour(7))
        assertEquals(TimeOfDay.DAY, TimeOfDay.forHour(17))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.forHour(18))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.forHour(21))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.forHour(22))
    }

    @Test
    fun everySceneHasFullLeafPalette() {
        TimeOfDay.entries.forEach { assertTrue(it.name, it.leafColors.size == 6) }
    }
}
