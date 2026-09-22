package ru.finny.petgame.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R

@Composable
fun HintDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(text = stringResource(R.string.hint_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.hint_body))
                Text(text = stringResource(R.string.hint_item_1))
                Text(text = stringResource(R.string.hint_item_2))
                Text(text = stringResource(R.string.hint_item_3))
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.hint_close),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}