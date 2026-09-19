package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.PetSprite

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (page == 0) {
                Text(
                    text = stringResource(R.string.intro_title_1),
                    style = MaterialTheme.typography.headlineSmall,
                )
                PetSprite(species = 0, colorIndex = 0, modifier = Modifier.size(140.dp))
                Text(
                    text = stringResource(R.string.intro_body_1),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = onNextPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.next))
                }
            } else {
                Text(
                    text = stringResource(R.string.intro_title_2),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.intro_body_2),
                    style = MaterialTheme.typography.bodyLarge,
                )
                DecisionCard(
                    title = stringResource(R.string.intro_decision_1_title),
                    body = stringResource(R.string.intro_decision_1_body),
                )
                DecisionCard(
                    title = stringResource(R.string.intro_decision_2_title),
                    body = stringResource(R.string.intro_decision_2_body),
                )
                DecisionCard(
                    title = stringResource(R.string.intro_decision_3_title),
                    body = stringResource(R.string.intro_decision_3_body),
                )
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.start))
                }
            }
            Text(
                text = stringResource(R.string.intro_page_indicator, page + 1, 2),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun DecisionCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}