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
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.finny.petgame.R
import ru.finny.petgame.content.ContentLoader
import ru.finny.petgame.content.model.ShopItemContent
import ru.finny.petgame.data.model.GameSnapshot
import ru.finny.petgame.data.model.ShopPurchaseResult
import ru.finny.petgame.data.repository.GameRepository
import ru.finny.petgame.economy.model.PurchaseCategory
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.BadgeKind
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.StatusBadge

@Composable
fun ShopScreen(
    repository: GameRepository,
    snapshot: GameSnapshot,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onShopChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var shopItems by remember { mutableStateOf<List<ShopItemContent>?>(null) }
    var confirmItem by remember { mutableStateOf<ShopItemContent?>(null) }
    var successResult by remember { mutableStateOf<ShopPurchaseResult.Success?>(null) }
    var failure by remember { mutableStateOf<ShopPurchaseResult.InsufficientFunds?>(null) }

    LaunchedEffect(Unit) {
        shopItems = withContext(Dispatchers.IO) {
            ContentLoader(context.assets).loadCatalog().shopItems
        }
    }

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
                petSpecies = snapshot.profile.petSpecies,
                petColorIndex = snapshot.profile.petColor,
            )
            Text(
                text = stringResource(R.string.shop_balance_line, snapshot.balance),
                style = MaterialTheme.typography.titleMedium,
            )
            val items = shopItems
            if (items == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ShopSection(
                    title = stringResource(R.string.shop_tab_required),
                    items = items.filter { it.category == PurchaseCategory.REQUIRED },
                    onBuy = { confirmItem = it },
                )
                ShopSection(
                    title = stringResource(R.string.shop_tab_optional),
                    items = items.filter { it.category == PurchaseCategory.OPTIONAL },
                    onBuy = { confirmItem = it },
                )
                Text(
                    text = stringResource(R.string.shop_history_title),
                    style = MaterialTheme.typography.titleMedium,
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
                                    text = purchase.price.toString(),
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
        ConfirmPurchaseDialog(
            item = item,
            balance = snapshot.balance,
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
                        is ShopPurchaseResult.InsufficientFunds -> failure = result
                        is ShopPurchaseResult.Invalid -> failure = ShopPurchaseResult.InsufficientFunds(
                            balance = 0L,
                            shortfall = 0L,
                            explanation = result.explanation,
                        )
                    }
                }
            },
            onDismiss = { confirmItem = null },
        )
    }

    successResult?.let { result ->
        AlertDialog(
            onDismissRequest = { successResult = null },
            title = { Text(text = stringResource(R.string.shop_done_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.shop_bought, result.title, result.price))
                    if (result.moodDelta != 0) {
                        Text(text = stringResource(R.string.shop_mood_line, result.moodDelta))
                    }
                    if (result.satietyDelta != 0) {
                        Text(text = stringResource(R.string.shop_saturation_line, result.satietyDelta))
                    }
                    Text(text = stringResource(R.string.shop_left_line, result.balance))
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.hint_close),
                    onClick = { successResult = null },
                )
            },
        )
    }

    failure?.let { fail ->
        AlertDialog(
            onDismissRequest = { failure = null },
            title = { Text(text = stringResource(R.string.shop_not_enough_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = fail.explanation)
                    Text(text = stringResource(R.string.shop_not_enough_balance, fail.balance))
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.hint_close),
                    onClick = { failure = null },
                )
            },
        )
    }
}

@Composable
private fun ShopSection(title: String, items: List<ShopItemContent>, onBuy: (ShopItemContent) -> Unit) {
    Text(text = title, style = MaterialTheme.typography.titleLarge)
    items.forEach { item ->
        ShopItemCard(item = item, onBuy = onBuy)
    }
}

@Composable
private fun ShopItemCard(item: ShopItemContent, onBuy: (ShopItemContent) -> Unit) {
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
                    text = stringResource(R.string.shop_price, item.price),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            StatusBadge(
                text = if (item.category == PurchaseCategory.REQUIRED) {
                    stringResource(R.string.shop_category_required)
                } else {
                    stringResource(R.string.shop_category_optional)
                },
                icon = if (item.category == PurchaseCategory.REQUIRED) Icons.Filled.Check else Icons.Filled.Star,
                kind = BadgeKind.NEUTRAL,
            )
            Text(text = item.effect, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ConfirmPurchaseDialog(
    item: ShopItemContent,
    balance: Long,
    onBuy: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = item.title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.shop_confirm_price, item.price))
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
                Text(text = stringResource(R.string.shop_confirm_after, balance - item.price))
            }
        },
        confirmButton = {
            PrimaryButton(text = stringResource(R.string.shop_buy), onClick = onBuy)
        },
        dismissButton = {
            SecondaryButton(text = stringResource(R.string.shop_cancel), onClick = onDismiss)
        },
    )
}