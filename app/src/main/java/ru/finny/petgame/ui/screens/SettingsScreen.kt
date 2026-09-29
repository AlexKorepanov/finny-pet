package ru.finny.petgame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.finny.petgame.R
import ru.finny.petgame.ui.components.AppCard
import ru.finny.petgame.ui.components.AppTopBar
import ru.finny.petgame.ui.components.SecondaryButton
import ru.finny.petgame.ui.components.SectionTitle
import ru.finny.petgame.ui.theme.FinnyColors
import kotlin.math.roundToInt

private const val VOLUME_STEP = 0.1f

@Composable
fun SettingsScreen(
    musicOn: Boolean,
    volume: Float,
    onMusicOnChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
    soundsOn: Boolean,
    onSoundsOnChange: (Boolean) -> Unit,
    vibrationOn: Boolean,
    onVibrationOnChange: (Boolean) -> Unit,
    animationsOn: Boolean,
    systemAnimationsOff: Boolean,
    onAnimationsOnChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onHint: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_title),
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
            AppCard {
                SectionTitle(text = stringResource(R.string.settings_music_title), icon = Icons.Filled.Settings)
                SwitchRow(
                    title = stringResource(R.string.settings_music_switch),
                    status = stringResource(if (musicOn) R.string.settings_music_on else R.string.settings_music_off),
                    checked = musicOn,
                    onCheckedChange = onMusicOnChange,
                )

                val percent = (volume * 100).roundToInt()
                val volumeLabel = stringResource(R.string.settings_volume_value, percent)
                Text(
                    text = volumeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (musicOn) MaterialTheme.colorScheme.onSurface else FinnyColors.TextSecondary,
                )
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    onValueChangeFinished = onVolumeChangeFinished,
                    enabled = musicOn,
                    colors = SliderDefaults.colors(
                        thumbColor = FinnyColors.SplashBarFill,
                        activeTrackColor = FinnyColors.SplashBarFill,
                        inactiveTrackColor = FinnyColors.OutlineSoft,
                        inactiveTickColor = FinnyColors.SplashInk,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = volumeLabel },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        text = stringResource(R.string.settings_volume_down),
                        onClick = {
                            onVolumeChange(stepped(volume - VOLUME_STEP))
                            onVolumeChangeFinished()
                        },
                        enabled = musicOn && volume > 0f,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.settings_volume_up),
                        onClick = {
                            onVolumeChange(stepped(volume + VOLUME_STEP))
                            onVolumeChangeFinished()
                        },
                        enabled = musicOn && volume < 1f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AppCard {
                SectionTitle(text = stringResource(R.string.settings_sounds_title), icon = Icons.Filled.Notifications)
                SwitchRow(
                    title = stringResource(R.string.settings_sounds_switch),
                    status = stringResource(if (soundsOn) R.string.settings_sounds_on else R.string.settings_sounds_off),
                    checked = soundsOn,
                    onCheckedChange = onSoundsOnChange,
                )
            }
            AppCard {
                SectionTitle(text = stringResource(R.string.settings_vibration_title), icon = Icons.Filled.Phone)
                SwitchRow(
                    title = stringResource(R.string.settings_vibration_switch),
                    status = stringResource(
                        if (vibrationOn) R.string.settings_vibration_on else R.string.settings_vibration_off,
                    ),
                    checked = vibrationOn,
                    onCheckedChange = onVibrationOnChange,
                )
            }
            AppCard {
                SectionTitle(text = stringResource(R.string.settings_animations_title), icon = Icons.Filled.Star)
                SwitchRow(
                    title = stringResource(R.string.settings_animations_switch),
                    status = stringResource(
                        when {
                            !animationsOn -> R.string.settings_animations_off
                            systemAnimationsOff -> R.string.settings_animations_system_off
                            else -> R.string.settings_animations_on
                        },
                    ),
                    checked = animationsOn,
                    onCheckedChange = onAnimationsOnChange,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, status: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = status, style = MaterialTheme.typography.bodyMedium, color = FinnyColors.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun stepped(value: Float): Float = ((value / VOLUME_STEP).roundToInt() * VOLUME_STEP).coerceIn(0f, 1f)
