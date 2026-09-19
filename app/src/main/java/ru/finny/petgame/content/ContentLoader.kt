package ru.finny.petgame.content

import android.content.res.AssetManager

class ContentLoader(private val assets: AssetManager) {

    fun loadCatalog(): ContentCatalog = ContentCatalog(
        tasks = ContentParser.parseTasks(readText(TASKS_FILE)),
        shopItems = ContentParser.parseShopItems(readText(SHOP_FILE)),
        goals = ContentParser.parseGoals(readText(GOALS_FILE)),
    )

    private fun readText(path: String): String =
        assets.open(path).use { stream -> stream.bufferedReader().readText() }

    companion object {
        const val TASKS_FILE = "content/tasks.json"
        const val SHOP_FILE = "content/shop.json"
        const val GOALS_FILE = "content/goals.json"
    }
}