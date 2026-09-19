package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppTopBar

@Composable
fun SectionStubScreen(
    title: String,
    onBack: () -> Unit,
    onHint: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                showBack = true,
                onBack = onBack,
                onHint = onHint,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.section_stub_text),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}