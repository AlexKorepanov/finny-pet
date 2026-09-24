package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Test

class PetEffectsTest {

    private val engine = EconomyEngine()

    @Test
    fun `pet effect is clamped to 10 and 100`() {
        assertEquals(100, engine.applyPetEffect(95, 10))
        assertEquals(10, engine.applyPetEffect(5, -10))
        assertEquals(10, engine.applyPetEffect(20, -30))
        assertEquals(80, engine.applyPetEffect(70, 10))
        assertEquals(70, engine.applyPetEffect(70, 0))
    }
}