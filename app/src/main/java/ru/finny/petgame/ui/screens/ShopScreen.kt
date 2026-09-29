package ru.finny.petgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.finny.petgame.R
import ru.finny.petgame.content.ContentCatalog
import ru.finny.petgame.content.ContentLoader
import ru.finny.petgame.content.model.ShopItemContent
import ru.finny.petgame.data.entity.PurchaseEntity
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.PeriodStatus
import ru.finny.petgame.data.model.ShopPurchaseResult
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.model.BudgetDirection
import ru.finny.petgame.economy.model.PurchaseCategory
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.StatusBadge
import ru.finny.petgame.ui.components.coinsAmount
import ru.finny.petgame.ui.model.toPetLook
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
    var tab by rememberSaveable { mutableStateOf(ShopTab.REQUIRED) }

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
                title = stringResource(R.string.section_shop),
                showBack = true,
                onBack = onBack,
                onHint = onHint,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // Финни и кошелёк всегда наверху — не уезжают при прокрутке товаров
            ShopPreview(
                snapshot = snapshot,
                planActive = planActive,
                requiredLeft = envelopeLeft(BudgetDirection.REQUIRED),
                optionalLeft = envelopeLeft(BudgetDirection.OPTIONAL),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FinnyColors.SoftBlue)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )

            ShopTabRow(
                selected = tab,
                boughtCount = snapshot.periodPurchases.size,
                onSelect = { tab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FinnyColors.SurfaceVariant)
                    .padding(vertical = 10.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(tab.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = FinnyColors.TextPrimary,
                )
                if (tab != ShopTab.HISTORY) {
                    Text(
                        text = stringResource(R.string.shop_tap_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FinnyColors.TextSecondary,
                    )
                }
            }

            val items = shopItems
            if (items == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (tab) {
                        ShopTab.REQUIRED, ShopTab.OPTIONAL -> {
                            val shown = items
                                .filter { it.category == tab.category }
                                .sortedByDescending { it.id in weekNeedIds }
                            items(shown, key = { it.id }) { item ->
                                ShopItemTile(
                                    item = item,
                                    weekNeed = item.id in weekNeedIds,
                                    bought = item.id in boughtIds,
                                    affordable = item.price <= snapshot.balance,
                                    onClick = { confirmItem = item },
                                )
                            }
                        }
                        ShopTab.HISTORY -> {
                            if (snapshot.periodPurchases.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    HistoryEmpty()
                                }
                            } else {
                                val emojiById = items.associate { it.id to it.emoji }
                                items(snapshot.periodPurchases, key = { it.id }) { purchase ->
                                    HistoryTile(
                                        purchase = purchase,
                                        emoji = emojiById[purchase.itemId] ?: ShopItemContent.DEFAULT_EMOJI,
                                    )
                                }
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

/** Вкладка магазина: [emoji] — картинка на плитке вкладки, [category] — какие товары в ней. */
private enum class ShopTab(
    val shortRes: Int,
    val titleRes: Int,
    val emoji: String,
    val category: PurchaseCategory?,
) {
    REQUIRED(R.string.shop_tab_short_required, R.string.shop_tab_required, "🥣", PurchaseCategory.REQUIRED),
    OPTIONAL(R.string.shop_tab_optional, R.string.shop_tab_optional, "🎈", PurchaseCategory.OPTIONAL),
    HISTORY(R.string.shop_tab_short_history, R.string.shop_history_title, "🧺", null),
}

@Composable
private fun ShopPreview(
    snapshot: GameSnapshot,
    planActive: Boolean,
    requiredLeft: Long,
    optionalLeft: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PetSprite(
            look = snapshot.profile.toPetLook(),
            stage = snapshot.profile.petStage,
            modifier = Modifier.size(112.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.shop_balance_title),
                style = MaterialTheme.typography.labelLarge,
                color = FinnyColors.TextSecondary,
            )
            Text(
                text = "🪙 " + coinsAmount(snapshot.balance),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FinnyColors.TextPrimary,
            )
            if (planActive) {
                Text(
                    text = stringResource(R.string.shop_envelopes_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = FinnyColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                PlanPill(label = stringResource(R.string.shop_category_required), left = requiredLeft)
                PlanPill(label = stringResource(R.string.shop_category_optional), left = optionalLeft)
            }
        }
    }
}

@Composable
private fun PlanPill(label: String, left: Long) {
    val over = left < 0L
    val amount = if (over) {
        stringResource(R.string.shop_envelope_over, coinsAmount(-left))
    } else {
        stringResource(R.string.shop_envelope_left, coinsAmount(left))
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (over) FinnyColors.BadgeAttentionContainer else FinnyColors.Surface)
            .border(1.dp, FinnyColors.CardBorder, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (over) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = FinnyColors.BadgeAttentionContent,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = stringResource(R.string.shop_plan_line, label, amount),
            style = MaterialTheme.typography.labelLarge,
            color = if (over) FinnyColors.BadgeAttentionContent else FinnyColors.TextPrimary,
        )
    }
}

@Composable
private fun ShopTabRow(
    selected: ShopTab,
    boughtCount: Int,
    onSelect: (ShopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShopTab.entries.forEach { tab ->
            ShopTabTile(
                tab = tab,
                label = if (tab == ShopTab.HISTORY) {
                    stringResource(tab.shortRes, boughtCount)
                } else {
                    stringResource(tab.shortRes)
                },
                selected = tab == selected,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ShopTabTile(
    tab: ShopTab,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (selected) FinnyColors.SoftBlue else FinnyColors.Surface)
            .border(
                width = if (selected) 3.dp else 2.dp,
                color = if (selected) FinnyColors.Primary else FinnyColors.CardBorder,
                shape = shape,
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = tab.emoji, fontSize = 28.sp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) FinnyColors.Primary else FinnyColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ShopItemTile(
    item: ShopItemContent,
    weekNeed: Boolean,
    bought: Boolean,
    affordable: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val required = item.category == PurchaseCategory.REQUIRED
    val borderColor = when {
        bought -> FinnyColors.Success
        weekNeed -> FinnyColors.Optional
        else -> FinnyColors.CardBorder
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .clip(shape)
            .background(FinnyColors.Surface)
            .border(width = if (bought || weekNeed) 3.dp else 2.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TileTag(
            text = when {
                bought -> stringResource(R.string.shop_tile_bought)
                weekNeed -> stringResource(R.string.shop_tile_need_week)
                required -> stringResource(R.string.shop_category_required)
                else -> stringResource(R.string.shop_category_optional)
            },
            icon = when {
                bought -> Icons.Filled.Done
                weekNeed -> Icons.Filled.Warning
                required -> Icons.Filled.Check
                else -> Icons.Filled.Star
            },
            kind = when {
                bought -> BadgeKind.POSITIVE
                weekNeed -> BadgeKind.ATTENTION
                else -> BadgeKind.NEUTRAL
            },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(if (required) FinnyColors.SoftGreen else FinnyColors.SoftOrange),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.emoji, fontSize = 40.sp)
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = FinnyColors.TextPrimary,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        PricePill(price = item.price, affordable = affordable)
    }
}

@Composable
private fun PricePill(price: Long, affordable: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (affordable) FinnyColors.Primary else FinnyColors.BadgeNeutralContainer,
        contentColor = if (affordable) FinnyColors.OnPrimary else FinnyColors.BadgeNeutralContent,
    ) {
        Text(
            text = "🪙 " + coinsAmount(price),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun TileTag(text: String, icon: ImageVector, kind: BadgeKind) {
    val (container: Color, content: Color) = when (kind) {
        BadgeKind.POSITIVE -> FinnyColors.BadgePositiveContainer to FinnyColors.BadgePositiveContent
        BadgeKind.ATTENTION -> FinnyColors.BadgeAttentionContainer to FinnyColors.BadgeAttentionContent
        BadgeKind.NEUTRAL -> FinnyColors.BadgeNeutralContainer to FinnyColors.BadgeNeutralContent
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryTile(purchase: PurchaseEntity, emoji: String) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FinnyColors.Surface)
            .border(width = 2.dp, color = FinnyColors.CardBorder, shape = shape)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = emoji, fontSize = 36.sp)
        Text(
            text = purchase.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = FinnyColors.TextPrimary,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "🪙 " + coinsAmount(purchase.price),
            style = MaterialTheme.typography.labelLarge,
            color = FinnyColors.TextSecondary,
        )
    }
}

@Composable
private fun HistoryEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "🧺", fontSize = 48.sp)
        Text(
            text = stringResource(R.string.shop_history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = FinnyColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
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
        icon = { Text(text = item.emoji, fontSize = 48.sp) },
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
