package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.ChunkySurface
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton
import ru.finny.petgame.ui.theme.FinnyColors

@Composable
fun IntroScreen(
    page: Int,
    onBack: () -> Unit,
    onHint: () -> Unit,
    onNextPage: () -> Unit,
    onFinish: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                showBack = page > 0,
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (page == 0) {
                Text(
                    text = stringResource(R.string.intro_title_1),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                ChunkySurface(
                    onClick = null,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = FinnyColors.SoftBlue,
                    edgeColor = FinnyColors.PrimaryEdge.copy(alpha = 0.35f),
                    borderColor = FinnyColors.Primary.copy(alpha = 0.35f),
                    minHeight = 200.dp,
                ) {
                    PetSprite(species = 0, colorIndex = 0, modifier = Modifier.size(168.dp))
                }
                Text(
                    text = stringResource(R.string.intro_body_1),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                PrimaryButton(
                    text = stringResource(R.string.next),
                    onClick = onNextPage,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = stringResource(R.string.intro_title_2),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.intro_body_2),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                DecisionCard(
                    title = stringResource(R.string.intro_decision_1_title),
                    body = stringResource(R.string.intro_decision_1_body),
                    icon = Icons.Filled.Favorite,
                    tint = FinnyColors.Optional,
                    soft = FinnyColors.SoftOrange,
                    edge = FinnyColors.OptionalEdge,
                )
                DecisionCard(
                    title = stringResource(R.string.intro_decision_2_title),
                    body = stringResource(R.string.intro_decision_2_body),
                    icon = Icons.Filled.Star,
                    tint = FinnyColors.Tasks,
                    soft = FinnyColors.SoftPurple,
                    edge = FinnyColors.TasksEdge,
                )
                DecisionCard(
                    title = stringResource(R.string.intro_decision_3_title),
                    body = stringResource(R.string.intro_decision_3_body),
                    icon = Icons.Filled.Home,
                    tint = FinnyColors.Success,
                    soft = FinnyColors.SoftGreen,
                    edge = FinnyColors.SuccessEdge,
                )
                PrimaryButton(
                    text = stringResource(R.string.start),
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(R.string.intro_page_indicator, page + 1, 2),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DecisionCard(
    title: String,
    body: String,
    icon: ImageVector,
    tint: Color,
    soft: Color,
    edge: Color,
) {
    ChunkySurface(
        onClick = null,
        modifier = Modifier.fillMaxWidth(),
        containerColor = soft,
        edgeColor = edge,
        borderColor = tint,
        borderWidth = 2.dp,
        minHeight = 88.dp,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(36.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = FinnyColors.TextPrimary)
                Text(text = body, style = MaterialTheme.typography.bodyLarge, color = FinnyColors.TextSecondary)
            }
        }
    }
}
