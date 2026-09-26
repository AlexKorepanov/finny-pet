package ru.finny.petgame.ui.model

import ru.finny.petgame.content.ContentCatalog
import ru.finny.petgame.content.openTaskDay
import ru.finny.petgame.content.model.TaskContent
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.economy.model.BudgetDirection

/** Одна подсказка «что делать дальше» по циклу недели: план → нужное → копилка → задание → итог. */
sealed interface NextStep {
    data object MakePlan : NextStep
    data class BuyNeeds(val titles: List<String>) : NextStep
    data class Save(val amount: Long) : NextStep
    data class SolveTask(val task: TaskContent) : NextStep
    data object CloseWeek : NextStep
}

fun missingNeedTitles(snapshot: GameSnapshot, catalog: ContentCatalog?): List<String> {
    val periodIndex = snapshot.currentPeriod?.periodIndex ?: 0
    val bought = snapshot.periodPurchases.map { it.itemId }.toSet()
    return catalog?.needsFor(periodIndex).orEmpty().filter { it.itemId !in bought }.map { it.title }
}

fun nextOpenTask(snapshot: GameSnapshot, catalog: ContentCatalog?): TaskContent? {
    catalog ?: return null
    val periodIndex = snapshot.currentPeriod?.periodIndex ?: 0
    val openDay = snapshot.currentPeriod?.let { openTaskDay(it.createdAt) } ?: 1
    val done = snapshot.completedTasks.filter { it.isCorrect == true }.map { it.taskId }.toSet()
    return catalog.tasks.firstOrNull {
        it.id !in done && catalog.isTaskOpen(it, periodIndex, openDay, snapshot.profile.allTasksOpen)
    }
}

fun nextStep(snapshot: GameSnapshot, catalog: ContentCatalog?): NextStep {
    if (snapshot.currentPeriod?.status != PeriodStatus.ACTIVE.name) return NextStep.MakePlan
    val missing = missingNeedTitles(snapshot, catalog)
    if (missing.isNotEmpty()) return NextStep.BuyNeeds(missing)
    val plannedSavings = snapshot.plan
        .firstOrNull { it.direction == BudgetDirection.SAVINGS.name }
        ?.plannedAmount ?: 0L
    val savedLeft = (plannedSavings - (snapshot.periodFact[BudgetDirection.SAVINGS] ?: 0L))
        .coerceAtMost(snapshot.balance)
    if (savedLeft > 0L) return NextStep.Save(savedLeft)
    nextOpenTask(snapshot, catalog)?.let { return NextStep.SolveTask(it) }
    return NextStep.CloseWeek
}
