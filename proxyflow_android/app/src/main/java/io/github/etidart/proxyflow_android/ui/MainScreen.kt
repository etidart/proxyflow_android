package io.github.etidart.proxyflow_android.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.etidart.proxyflow_android.R
import io.github.etidart.proxyflow_android.SourceMode

private data class SourceOption(
    val mode: SourceMode,
    val titleRes: Int,
    val subtitleRes: Int,
)

private val options = listOf(
    SourceOption(SourceMode.MANUAL, R.string.source_manual, R.string.source_manual_subtitle),
    SourceOption(SourceMode.FILE, R.string.source_file, R.string.source_file_subtitle),
    SourceOption(SourceMode.URL, R.string.source_url, R.string.source_url_subtitle),
)

@Composable
fun MainScreen(
    selectedMode: SourceMode,
    manualText: String,
    lastDownloadUrl: String?,
    isConnected: Boolean,
    isDownloading: Boolean,
    manualDraft: String,
    urlDraft: String,
    onManualDraftChange: (String) -> Unit,
    onUrlDraftChange: (String) -> Unit,
    onSelectMode: (SourceMode) -> Unit,
    onChooseFile: () -> Unit,
    onConnectToggle: () -> Unit,
    onSubmitUrl: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.forEach { option ->
                    SourceCard(
                        modifier = Modifier.weight(1f),
                        option = option,
                        selected = option.mode == selectedMode,
                        enabled = !isConnected,
                        detail = when (option.mode) {
                            SourceMode.MANUAL -> manualText
                                .lineSequence()
                                .count { it.isNotBlank() }
                                .takeIf { it > 0 }
                                ?.let { stringResourceCompat(R.string.source_manual_count, it) }
                            SourceMode.URL -> lastDownloadUrl
                            SourceMode.FILE -> null
                        },
                        onClick = { onSelectMode(option.mode) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            when (selectedMode) {
                SourceMode.MANUAL -> ManualSourceEditor(
                    text = manualDraft,
                    enabled = !isConnected,
                    onTextChange = onManualDraftChange,
                )
                SourceMode.URL -> UrlSourceEditor(
                    url = urlDraft,
                    isDownloading = isDownloading,
                    enabled = !isConnected,
                    onUrlChange = onUrlDraftChange,
                    onSubmit = onSubmitUrl,
                )
                SourceMode.FILE -> FileSourcePicker(
                    enabled = !isConnected,
                    onChooseFile = onChooseFile,
                )
            }

            Spacer(Modifier.weight(1f))
            ConnectButton(
                isConnected = isConnected,
                enabled = !isDownloading,
                onClick = onConnectToggle,
            )
        }
    }
}

@Composable
private fun SourceCard(
    modifier: Modifier = Modifier,
    option: SourceOption,
    selected: Boolean,
    enabled: Boolean,
    detail: String?,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Card(
        modifier = modifier
            .height(72.dp)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResourceCompat(option.titleRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail ?: stringResourceCompat(option.subtitleRes),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ConnectButton(
    isConnected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(160.dp),
        shape = CircleShape,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = if (isConnected) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
        ),
    ) {
        Text(
            text = stringResourceCompat(
                if (isConnected) R.string.disconnect else R.string.connect
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ManualSourceEditor(
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        placeholder = { Text(stringResourceCompat(R.string.proxy_list_hint)) },
    )
}

@Composable
private fun UrlSourceEditor(
    url: String,
    isDownloading: Boolean,
    enabled: Boolean,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            enabled = enabled && !isDownloading,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResourceCompat(R.string.proxy_url_hint)) },
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSubmit,
            enabled = enabled && !isDownloading && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isDownloading) {
                    stringResourceCompat(R.string.downloading_proxy_list)
                } else {
                    stringResourceCompat(R.string.download_label)
                }
            )
        }
    }
}

@Composable
private fun FileSourcePicker(
    enabled: Boolean,
    onChooseFile: () -> Unit,
) {
    Button(
        onClick = onChooseFile,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(stringResourceCompat(R.string.choose_file_label))
    }
}

@Composable
private fun stringResourceCompat(id: Int, vararg args: Any): String {
    return if (args.isEmpty()) {
        androidx.compose.ui.res.stringResource(id)
    } else {
        androidx.compose.ui.res.stringResource(id, *args)
    }
}

