package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.SectionAccent
import ru.finny.petgame.ui.components.SectionHeader

@Composable
fun SectionStubScreen(
    title: String,
    accent: SectionAccent,
    petSpecies: Int,
    petColorIndex: Int,
    onBack: () -> Unit,
    onHint: () -> Unit,
) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                accent = accent,
                title = title,
                hint = stringResource(R.string.section_hint_stub),
                petSpecies = petSpecies,
                petColorIndex = petColorIndex,
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.section_stub_text),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}