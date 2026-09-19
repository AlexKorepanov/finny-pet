package ru.finny.petgame.content

import ru.finny.petgame.content.model.SavingsGoalContent
import ru.finny.petgame.content.model.ShopItemContent
import ru.finny.petgame.content.model.TaskContent

class ContentCatalog(
    val tasks: List<TaskContent>,
    val shopItems: List<ShopItemContent>,
    val goals: List<SavingsGoalContent>,
) {

    fun taskById(id: String): TaskContent? = tasks.firstOrNull { it.id == id }

    fun shopItemById(id: String): ShopItemContent? = shopItems.firstOrNull { it.id == id }

    fun goalById(id: String): SavingsGoalContent? = goals.firstOrNull { it.id == id }
}