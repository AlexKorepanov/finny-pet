package ru.finny.petgame.content

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ru.finny.petgame.content.model.SavingsGoalContent
import ru.finny.petgame.content.model.ShopItemContent
import ru.finny.petgame.content.model.TaskContent
import ru.finny.petgame.content.model.TaskOption
import ru.finny.petgame.content.model.TaskTheme
import ru.finny.petgame.content.model.TaskType
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.PurchaseCategory

class ContentFormatException(val problems: List<String>) :
    IllegalArgumentException("Ошибки в учебном контенте:\n" + problems.joinToString("\n"))

object ContentParser {

    fun parseTasks(json: String): List<TaskContent> {
        val array = rootArray(json, "tasks")
        val problems = mutableListOf<String>()
        val tasks = mutableListOf<TaskContent>()
        val ids = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val prefix = "tasks[$index]"
            val obj = objectAt(array, index, prefix, problems) ?: continue
            parseTask(obj, prefix, problems)?.let { task ->
                if (!ids.add(task.id)) {
                    problems += "$prefix: повторяется id ${task.id}"
                } else {
                    tasks += task
                }
            }
        }
        failIfProblems(problems)
        return tasks
    }

    fun parseShopItems(json: String): List<ShopItemContent> {
        val array = rootArray(json, "items")
        val problems = mutableListOf<String>()
        val items = mutableListOf<ShopItemContent>()
        val ids = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val prefix = "items[$index]"
            val obj = objectAt(array, index, prefix, problems) ?: continue
            parseShopItem(obj, prefix, problems)?.let { item ->
                if (!ids.add(item.id)) {
                    problems += "$prefix: повторяется id ${item.id}"
                } else {
                    items += item
                }
            }
        }
        failIfProblems(problems)
        return items
    }

    fun parseGoals(json: String): List<SavingsGoalContent> {
        val array = rootArray(json, "goals")
        val problems = mutableListOf<String>()
        val goals = mutableListOf<SavingsGoalContent>()
        val ids = mutableSetOf<String>()
        for (index in 0 until array.length()) {
            val prefix = "goals[$index]"
            val obj = objectAt(array, index, prefix, problems) ?: continue
            parseGoal(obj, prefix, problems)?.let { goal ->
                if (!ids.add(goal.id)) {
                    problems += "$prefix: повторяется id ${goal.id}"
                } else {
                    goals += goal
                }
            }
        }
        failIfProblems(problems)
        return goals
    }

    private fun parseTask(obj: JSONObject, prefix: String, problems: MutableList<String>): TaskContent? {
        val local = mutableListOf<String>()
        val id = requireString(obj, "id", prefix, local)
        val title = requireString(obj, "title", prefix, local)
        val story = requireString(obj, "story", prefix, local)
        val question = requireString(obj, "question", prefix, local)
        val reward = requirePositiveLong(obj, "reward", prefix, local)
        val correctExplanation = requireString(obj, "correctExplanation", prefix, local)
        val wrongExplanation = requireString(obj, "wrongExplanation", prefix, local)
        val themeName = requireEnum(obj, "theme", TaskTheme.entries.map { it.name }, prefix, local)
        val typeName = requireEnum(obj, "type", TaskType.entries.map { it.name }, prefix, local)

        val options: List<TaskOption> = if (typeName == TaskType.CHOICE.name) {
            collectOptions(obj, prefix, local)
        } else {
            emptyList()
        }
        val sum: Long? = if (typeName == TaskType.DISTRIBUTE.name) {
            requirePositiveLong(obj, "sum", prefix, local)
        } else {
            0L
        }
        val minimums = parseMinimums(obj, prefix, local)

        problems += local
        if (local.isNotEmpty()) return null

        return TaskContent(
            id = id!!,
            theme = TaskTheme.valueOf(themeName!!),
            type = TaskType.valueOf(typeName!!),
            title = title!!,
            story = story!!,
            question = question!!,
            reward = reward!!,
            options = options,
            sum = sum!!,
            minimums = minimums,
            correctExplanation = correctExplanation!!,
            wrongExplanation = wrongExplanation!!,
        )
    }

    private fun collectOptions(obj: JSONObject, prefix: String, local: MutableList<String>): List<TaskOption> {
        if (!obj.has("options")) {
            local += "$prefix: для задания типа CHOICE нужен массив options"
            return emptyList()
        }
        val array = obj.optJSONArray("options")
        if (array == null) {
            local += "$prefix: поле options должно быть массивом"
            return emptyList()
        }
        val options = mutableListOf<TaskOption>()
        for (index in 0 until array.length()) {
            val optionPrefix = "$prefix.options[$index]"
            val optionObj = objectAt(array, index, optionPrefix, local) ?: continue
            val optionLocal = mutableListOf<String>()
            val optionId = requireString(optionObj, "id", optionPrefix, optionLocal)
            val text = requireString(optionObj, "text", optionPrefix, optionLocal)
            val result = requireString(optionObj, "result", optionPrefix, optionLocal)
            val isGood = requireBoolean(optionObj, "isGood", optionPrefix, optionLocal)
            local += optionLocal
            if (optionLocal.isEmpty()) {
                options += TaskOption(optionId!!, text!!, result!!, isGood!!)
            }
        }
        if (options.size < 2) {
            local += "$prefix: нужно минимум два варианта ответа"
        }
        if (options.none { it.isGood }) {
            local += "$prefix: нужен хотя бы один правильный вариант (isGood: true)"
        }
        return options
    }

    private fun parseMinimums(obj: JSONObject, prefix: String, problems: MutableList<String>): Map<BudgetDirection, Long> {
        if (!obj.has("minimums")) return emptyMap()
        val raw = obj.optJSONObject("minimums")
        if (raw == null) {
            problems += "$prefix: поле minimums должно быть объектом"
            return emptyMap()
        }
        val result = mutableMapOf<BudgetDirection, Long>()
        val keys = raw.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val direction = BudgetDirection.entries.firstOrNull { it.name == key }
            if (direction == null) {
                problems += "$prefix.minimums: неизвестное направление $key (допустимо: ${BudgetDirection.entries.joinToString { it.name }})"
                continue
            }
            try {
                val value = raw.getLong(key)
                if (value < 0L) {
                    problems += "$prefix.minimums: значение для $key не может быть меньше нуля"
                } else {
                    result[direction] = value
                }
            } catch (e: JSONException) {
                problems += "$prefix.minimums: значение для $key должно быть числом"
            }
        }
        return result
    }

    private fun parseShopItem(obj: JSONObject, prefix: String, problems: MutableList<String>): ShopItemContent? {
        val local = mutableListOf<String>()
        val id = requireString(obj, "id", prefix, local)
        val title = requireString(obj, "title", prefix, local)
        val categoryName = requireEnum(obj, "category", PurchaseCategory.entries.map { it.name }, prefix, local)
        val price = requirePositiveLong(obj, "price", prefix, local)
        val effect = requireString(obj, "effect", prefix, local)
        val moodDelta = optionalInt(obj, "moodDelta", prefix, local)
        val satietyDelta = optionalInt(obj, "satietyDelta", prefix, local)

        problems += local
        if (local.isNotEmpty()) return null

        return ShopItemContent(
            id = id!!,
            title = title!!,
            category = PurchaseCategory.valueOf(categoryName!!),
            price = price!!,
            effect = effect!!,
            moodDelta = moodDelta,
            satietyDelta = satietyDelta,
        )
    }

    private fun parseGoal(obj: JSONObject, prefix: String, problems: MutableList<String>): SavingsGoalContent? {
        val local = mutableListOf<String>()
        val id = requireString(obj, "id", prefix, local)
        val title = requireString(obj, "title", prefix, local)
        val cost = requirePositiveLong(obj, "cost", prefix, local)
        val description = requireString(obj, "description", prefix, local)

        problems += local
        if (local.isNotEmpty()) return null

        return SavingsGoalContent(
            id = id!!,
            title = title!!,
            cost = cost!!,
            description = description!!,
        )
    }

    private fun rootArray(json: String, key: String): JSONArray {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw ContentFormatException(listOf("Файл не является корректным JSON: ${e.message}"))
        }
        if (!root.has(key)) throw ContentFormatException(listOf("Не найден массив $key"))
        val array = root.optJSONArray(key) ?: throw ContentFormatException(listOf("Поле $key должно быть массивом"))
        return array
    }

    private fun objectAt(array: JSONArray, index: Int, prefix: String, problems: MutableList<String>): JSONObject? =
        try {
            array.getJSONObject(index)
        } catch (e: JSONException) {
            problems += "$prefix: элемент должен быть объектом"
            null
        }

    private fun requireString(obj: JSONObject, field: String, prefix: String, problems: MutableList<String>): String? {
        if (!obj.has(field)) {
            problems += "$prefix: нет поля $field"
            return null
        }
        val value = obj.optString(field, "")
        if (value.isBlank()) {
            problems += "$prefix: поле $field пустое"
            return null
        }
        return value
    }

    private fun requireBoolean(obj: JSONObject, field: String, prefix: String, problems: MutableList<String>): Boolean? {
        if (!obj.has(field)) {
            problems += "$prefix: нет поля $field"
            return null
        }
        return try {
            obj.getBoolean(field)
        } catch (e: JSONException) {
            problems += "$prefix: поле $field должно быть true или false"
            null
        }
    }

    private fun requirePositiveLong(obj: JSONObject, field: String, prefix: String, problems: MutableList<String>): Long? {
        if (!obj.has(field)) {
            problems += "$prefix: нет поля $field"
            return null
        }
        return try {
            val value = obj.getLong(field)
            if (value <= 0L) {
                problems += "$prefix: поле $field должно быть больше нуля"
                null
            } else {
                value
            }
        } catch (e: JSONException) {
            problems += "$prefix: поле $field должно быть числом"
            null
        }
    }

    private fun optionalInt(obj: JSONObject, field: String, prefix: String, problems: MutableList<String>): Int {
        if (!obj.has(field)) return 0
        return try {
            obj.getInt(field)
        } catch (e: JSONException) {
            problems += "$prefix: поле $field должно быть целым числом"
            0
        }
    }

    private fun requireEnum(obj: JSONObject, field: String, allowed: List<String>, prefix: String, problems: MutableList<String>): String? {
        val value = requireString(obj, field, prefix, problems) ?: return null
        if (value !in allowed) {
            problems += "$prefix: поле $field = $value, допустимые значения: ${allowed.joinToString()}"
            return null
        }
        return value
    }

    private fun failIfProblems(problems: List<String>) {
        if (problems.isNotEmpty()) throw ContentFormatException(problems)
    }
}