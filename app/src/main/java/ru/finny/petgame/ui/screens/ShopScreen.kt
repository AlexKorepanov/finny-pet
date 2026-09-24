package ru.finny.petgame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.finny.petgame.R
import ru.finny.petgame.content.ContentCatalog
import ru.finny.petgame.content.ContentLoader
import ru.finny.petgame.content.model.ShopItemContent
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.data.model.ShopPurchaseResult
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.PurchaseCategory
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.model.toPetLook
import ru.finny.petgame.ui.components.SectionTitle
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.coinsAmount
import ru.finny.petgame.ui.theme.FinnyColors

@Composable
fun ShopScreen(
    repository: GameRepository,
    snapshot: GameSnapshot,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onShopChanged: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenSavings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var catalog by remember { mutableStateOf<ContentCatalog?>(null) }
    var confirmItem by remember { mutableStateOf<ShopItemContent?>(null) }
    var successResult by remember { mutableStateOf<ShopPurchaseResult.Success?>(null) }
    var failure by remember { mutableStateOf<ShopPurchaseResult.InsufficientFunds?>(null) }
    var failedItem by remember { mutableStateOf<ShopItemContent?>(null) }

    LaunchedEffect(Unit) {
        catalog = withContext(Dispatchers.IO) {
            ContentLoader(context.assets).loadCatalog()
        }
    }
    val shopItems = catalog?.shopItems
    val periodIndex = snapshot.currentPeriod?.periodIndex ?: 0
    val weekNeedIds = catalog?.needsFor(periodIndex).orEmpty().map { it.itemId }.toSet()
    val boughtIds = snapshot.periodPurchases.map { it.itemId }.toSet()
    val planActive = snapshot.currentPeriod?.status == PeriodStatus.ACTIVE.name
    val planned = snapshot.plan.associate { BudgetDirection.valueOf(it.direction) to it.plannedAmount }
    fun envelopeLeft(direction: BudgetDirection): Long =
        (planned[direction] ?: 0L) - (snapshot.periodFact[direction] ?: 0L)

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                showBack = true,
                onBack = onBack,
                onHint = onHint,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                accent = SectionAccent.SHOP,
                title = stringResource(R.string.section_shop),
                hint = stringResource(R.string.shop_header_hint),
                petLook = snapshot.profile.toPetLook(),
                petStage = snapshot.profile.petStage,
            )
            Text(
                text = stringResource(R.string.shop_balance_line, snapshot.balance),
                style = MaterialTheme.typography.titleMedium,
            )
            if (planActive) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = stringResource(R.string.shop_envelopes_title), icon = Icons.Filled.List)
                    EnvelopeLine(
                        title = stringResource(R.string.plan_direction_required),
                        left = envelopeLeft(BudgetDirection.REQUIRED),
                    )
                    EnvelopeLine(
                        title = stringResource(R.string.plan_direction_optional),
                        left = envelopeLeft(BudgetDirection.OPTIONAL),
                    )
                }
            }
            val items = shopItems
            if (items == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val required = items.filter { it.category == PurchaseCategory.REQUIRED }
                ShopSection(
                    title = stringResource(R.string.shop_tab_required),
                    icon = Icons.Filled.Check,
                    items = required.sortedByDescending { it.id in weekNeedIds },
                    weekNeedIds = weekNeedIds,
                    boughtIds = boughtIds,
                    onBuy = { confirmItem = it },
                )
                ShopSection(
                    title = stringResource(R.string.shop_tab_optional),
                    icon = Icons.Filled.Star,
                    items = items.filter { it.category == PurchaseCategory.OPTIONAL },
                    weekNeedIds = weekNeedIds,
                    boughtIds = boughtIds,
                    onBuy = { confirmItem = it },
                )
                SectionTitle(
                    text = stringResource(R.string.shop_history_title),
                    accent = SectionAccent.SHOP,
                    icon = Icons.Filled.List,
                )
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    if (snapshot.periodPurchases.isEmpty()) {
                        Text(
                            text = stringResource(R.string.shop_history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        snapshot.periodPurchases.forEach { purchase ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = purchase.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = coinsAmount(purchase.price),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    confirmItem?.let { item ->
        val direction = if (item.category == PurchaseCategory.REQUIRED) BudgetDirection.REQUIRED else BudgetDirection.OPTIONAL
        ConfirmPurchaseDialog(
            item = item,
            balance = snapshot.balance,
            overPlan = repository.overPlanAmount(snapshot, direction, item.price),
            onBuy = {
                confirmItem = null
                scope.launch {
                    when (
                        val result = repository.purchaseShopItem(
                            itemId = item.id,
                            title = item.title,
                            category = item.category,
                            price = item.price,
                            moodDelta = item.moodDelta,
                            satietyDelta = item.satietyDelta,
                        )
                    ) {
                        is ShopPurchaseResult.Success -> {
                            successResult = result
                            onShopChanged()
                        }
                        is ShopPurchaseResult.InsufficientFunds -> {
                            failedItem = item
                            failure = result
                        }
                        is ShopPurchaseResult.Invalid -> {
                            failedItem = null
                            failure = ShopPurchaseResult.InsufficientFunds(
                                balance = 0L,
                                shortfall = 0L,
                                explanation = result.explanation,
                            )
                        }
                    }
                }
            },
            onDismiss = { confirmItem = null },
        )
    }

    successResult?.let { result ->
        AlertDialog(
            onDismissRequest = { successResult = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.shop_done_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.shop_bought, result.title, coinsAmount(result.price)))
                    if (result.moodDelta != 0) {
                        Text(text = stringResource(R.string.shop_mood_line, result.moodDelta))
                    }
                    if (result.satietyDelta != 0) {
                        Text(text = stringResource(R.string.shop_saturation_line, result.satietyDelta))
                    }
                    Text(text = stringResource(R.string.shop_left_line, coinsAmount(result.balance)))
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.hint_close),
                    onClick = { successResult = null },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }

    failure?.let { fail ->
        AlertDialog(
            onDismissRequest = { failure = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text(text = stringResource(R.string.shop_not_enough_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = fail.explanation)
                    Text(text = stringResource(R.string.shop_not_enough_balance, coinsAmount(fail.balance)))
                    when (failedItem?.category) {
                        PurchaseCategory.REQUIRED -> Text(text = stringResource(R.string.shop_not_enough_required_tip))
                        PurchaseCategory.OPTIONAL -> Text(text = stringResource(R.string.shop_not_enough_optional_tip))
                        null -> {}
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (failedItem != null) {
                        PrimaryButton(
                            text = stringResource(R.string.shop_not_enough_tasks),
                            onClick = {
                                failure = null
                                onOpenTasks()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (failedItem?.category == PurchaseCategory.REQUIRED && snapshot.savingsTotal > 0L) {
                        SecondaryButton(
                            text = stringResource(R.string.shop_not_enough_savings),
                            onClick = {
                                failure = null
                                onOpenSavings()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    SecondaryButton(
                        text = if (failedItem?.category == PurchaseCategory.OPTIONAL) {
                            stringResource(R.string.shop_not_enough_wait)
                        } else {
                            stringResource(R.string.shop_not_enough_cheaper)
                        },
                        onClick = { failure = null },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }
}

@Composable
private fun EnvelopeLine(title: String, left: Long) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = if (left >= 0L) {
                stringResource(R.string.shop_envelope_left, coinsAmount(left))
            } else {
                stringResource(R.string.shop_envelope_over, coinsAmount(-left))
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ShopSection(
    title: String,
    icon: ImageVector,
    items: List<ShopItemContent>,
    weekNeedIds: Set<String>,
    boughtIds: Set<String>,
    onBuy: (ShopItemContent) -> Unit,
) {
    SectionTitle(text = title, accent = SectionAccent.SHOP, icon = icon)
    items.forEach { item ->
        ShopItemCard(
            item = item,
            weekNeed = item.id in weekNeedIds,
            bought = item.id in boughtIds,
            onBuy = onBuy,
        )
    }
}

@Composable
private fun ShopItemCard(item: ShopItemContent, weekNeed: Boolean, bought: Boolean, onBuy: (ShopItemContent) -> Unit) {
    Surface(
        onClick = { onBuy(item) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = coinsAmount(item.price),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(
                    text = if (item.category == PurchaseCategory.REQUIRED) {
                        stringResource(R.string.shop_category_required)
                    } else {
                        stringResource(R.string.shop_category_optional)
                    },
                    icon = if (item.category == PurchaseCategory.REQUIRED) Icons.Filled.Check else Icons.Filled.Star,
                    kind = BadgeKind.NEUTRAL,
                )
                when {
                    weekNeed && bought -> StatusBadge(
                        text = stringResource(R.string.shop_need_bought),
                        icon = Icons.Filled.Done,
                        kind = BadgeKind.POSITIVE,
                    )
                    weekNeed -> StatusBadge(
                        text = stringResource(R.string.shop_need_week),
                        icon = Icons.Filled.Warning,
                        kind = BadgeKind.ATTENTION,
                    )
                }
            }
            Text(text = item.effect, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ConfirmPurchaseDialog(
    item: ShopItemContent,
    balance: Long,
    overPlan: Long,
    onBuy: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(text = item.title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.shop_confirm_price, coinsAmount(item.price)))
                StatusBadge(
                    text = if (item.category == PurchaseCategory.REQUIRED) {
                        stringResource(R.string.shop_category_required)
                    } else {
                        stringResource(R.string.shop_category_optional)
                    },
                    icon = if (item.category == PurchaseCategory.REQUIRED) Icons.Filled.Check else Icons.Filled.Star,
                    kind = BadgeKind.NEUTRAL,
                )
                Text(text = stringResource(R.string.shop_confirm_mood, item.moodDelta))
                if (item.satietyDelta > 0) {
                    Text(text = stringResource(R.string.shop_confirm_saturation, item.satietyDelta))
                }
                Text(text = item.effect, style = MaterialTheme.typography.bodyLarge)
                Text(text = stringResource(R.string.shop_confirm_balance_now, balance))
                Text(text = stringResource(R.string.shop_confirm_after, coinsAmount(balance - item.price)))
                if (overPlan > 0L) {
                    StatusBadge(
                        text = stringResource(R.string.shop_confirm_over_plan, coinsAmount(overPlan)),
                        icon = Icons.Filled.Warning,
                        kind = BadgeKind.ATTENTION,
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.shop_buy),
                    onClick = onBuy,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(R.string.shop_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        dismissButton = {},
    )
}