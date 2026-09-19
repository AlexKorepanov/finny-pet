package ru.finny.petgame.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.finny.petgame.economy.model.PurchaseCategory
import ru.finny.petgame.economy.model.PurchaseDraft
import ru.finny.petgame.economy.model.PurchaseResult

class PurchaseTest {

    private val engine = EconomyEngine()
    private val food = PurchaseDraft(
        itemId = "food_1",
        title = "Корм",
        category = PurchaseCategory.REQUIRED,
        price = 30L,
    )

    @Test
    fun `successful purchase reduces balance and returns record`() {
        val state = EconomyState(balance = 100L)
        val result = engine.purchase(state, food)

        assertTrue(result is PurchaseResult.Success)
        val success = result as PurchaseResult.Success
        assertEquals(70L, success.state.balance)
        assertEquals("food_1", success.purchase.itemId)
        assertEquals(30L, success.purchase.price)
        assertEquals(PurchaseCategory.REQUIRED, success.purchase.category)
    }

    @Test
    fun `purchase for exact balance leaves zero and never negative`() {
        val state = EconomyState(balance = 30L)
        val result = engine.purchase(state, food)

        assertTrue(result is PurchaseResult.Success)
        assertEquals(0L, (result as PurchaseResult.Success).state.balance)
    }

    @Test
    fun `purchase without enough funds returns explanation and keeps state`() {
        val state = EconomyState(balance = 10L)
        val result = engine.purchase(state, food)

        assertTrue(result is PurchaseResult.InsufficientFunds)
        val failed = result as PurchaseResult.InsufficientFunds
        assertEquals(20L, failed.shortfall)
        assertTrue(failed.explanation.contains("20"))
        assertEquals(10L, failed.balance)
    }

    @Test
    fun `optional purchase without funds explains refusal option`() {
        val toy = PurchaseDraft(
            itemId = "toy_1",
            title = "Мяч",
            category = PurchaseCategory.OPTIONAL,
            price = 50L,
        )
        val state = EconomyState(balance = 15L)
        val result = engine.purchase(state, toy)

        assertTrue(result is PurchaseResult.InsufficientFunds)
        val failed = result as PurchaseResult.InsufficientFunds
        assertEquals(35L, failed.shortfall)
        assertTrue(failed.explanation.contains("Мяч"))
    }

    @Test
    fun `zero or negative price is rejected`() {
        val zeroPrice = PurchaseDraft("x", "X", PurchaseCategory.OPTIONAL, 0L)
        val negativePrice = PurchaseDraft("y", "Y", PurchaseCategory.OPTIONAL, -5L)

        assertTrue(engine.purchase(EconomyState(balance = 100L), zeroPrice) is PurchaseResult.Invalid)
        assertTrue(engine.purchase(EconomyState(balance = 100L), negativePrice) is PurchaseResult.Invalid)
    }
}