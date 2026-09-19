package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.PetSprite
import ru.finny.petgame.ui.components.PrimaryButton

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
                PrimaryButton(
                    text = stringResource(R.string.next),
                    onClick = onNextPage,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = stringResource(R.string.intro_title_2),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.intro_body_2),
                    style = MaterialTheme.typography.bodyLarge,
                )
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.intro_decision_1_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.intro_decision_1_body), style = MaterialTheme.typography.bodyLarge)
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.intro_decision_2_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.intro_decision_2_body), style = MaterialTheme.typography.bodyLarge)
                }
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.intro_decision_3_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.intro_decision_3_body), style = MaterialTheme.typography.bodyLarge)
                }
                PrimaryButton(
                    text = stringResource(R.string.start),
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(R.string.intro_page_indicator, page + 1, 2),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}