package ru.finny.petgame.data.model

enum class LedgerKind { INCOME, PURCHASE, TO_SAVINGS, FROM_SAVINGS }

/** Строка журнала монет: откуда пришли или куда ушли монеты баланса. */
data class LedgerEntry(
    val periodIndex: Int,
    val kind: LedgerKind,
    val title: String,
    /** Изменение баланса: плюс — пришло, минус — ушло. */
    val amount: Long,
    val createdAt: Long,
)
