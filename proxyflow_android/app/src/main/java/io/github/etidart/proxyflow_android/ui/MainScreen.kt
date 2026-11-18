package io.github.etidart.proxyflow_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.etidart.proxyflow_android.R
import io.github.etidart.proxyflow_android.SourceMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    lastDownloadTimestamp: Long,
    isConnected: Boolean,
    isDownloading: Boolean,
    showManualDialog: Boolean,
    showUrlDialog: Boolean,
    submitManual: Boolean,
    submitUrl: Boolean,
    manualDraft: String,
    urlDraft: String,
    onManualDraftChange: (String) -> Unit,
    onUrlDraftChange: (String) -> Unit,
    onSelectMode: (SourceMode) -> Unit,
    onConnectToggle: () -> Unit,
    onDismissManualDialog: () -> Unit,
    onSubmitManual: () -> Unit,
    onDismissUrlDialog: () -> Unit,
    onSubmitUrl: () -> Unit,
    onManualSubmitted: (String) -> Unit,
    onUrlSubmitted: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceCompat(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResourceCompat(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            StatusRow(isConnected = isConnected, lastDownloadTimestamp = lastDownloadTimestamp)

            Spacer(Modifier.height(24.dp))
            options.forEach { option ->
                SourceCard(
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
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.weight(1f))
            ConnectButton(
                isConnected = isConnected,
                enabled = !isDownloading,
                onClick = onConnectToggle,
            )
        }
    }

    if (showManualDialog) {
        ManualDialog(
            text = manualDraft,
            submit = submitManual,
            onTextChange = onManualDraftChange,
            onDismiss = onDismissManualDialog,
            onConfirm = onSubmitManual,
            onSubmitted = onManualSubmitted,
        )
    }

    if (showUrlDialog) {
        UrlDialog(
            url = urlDraft,
            isDownloading = isDownloading,
            submit = submitUrl,
            onUrlChange = onUrlDraftChange,
            onDismiss = onDismissUrlDialog,
            onConfirm = onSubmitUrl,
            onSubmitted = onUrlSubmitted,
        )
    }
}

@Composable
private fun StatusRow(isConnected: Boolean, lastDownloadTimestamp: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                )
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = stringResourceCompat(
                    if (isConnected) R.string.status_connected else R.string.status_disconnected
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            if (lastDownloadTimestamp > 0L) {
                Text(
                    text = stringResourceCompat(R.string.last_download_at, formatTimestamp(lastDownloadTimestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SourceCard(
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
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResourceCompat(option.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail ?: stringResourceCompat(option.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (detail == null) Int.MAX_VALUE else 1,
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
private fun ManualDialog(
    text: String,
    submit: Boolean,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onSubmitted: (String) -> Unit,
) {
    LaunchedEffect(submit) {
        if (submit) onSubmitted(text)
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResourceCompat(R.string.manual_proxy_list_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                placeholder = { Text(stringResourceCompat(R.string.proxy_list_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = text.isNotBlank()) {
                Text(stringResourceCompat(R.string.save_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResourceCompat(R.string.cancel_label))
            }
        },
    )
}

@Composable
private fun UrlDialog(
    url: String,
    isDownloading: Boolean,
    submit: Boolean,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onSubmitted: (String) -> Unit,
) {
    LaunchedEffect(submit) {
        if (submit) onSubmitted(url)
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResourceCompat(R.string.proxy_url_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResourceCompat(R.string.proxy_url_hint)) },
                )
                if (isDownloading) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResourceCompat(R.string.downloading_proxy_list))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDownloading && url.isNotBlank(),
            ) {
                Text(stringResourceCompat(R.string.download_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResourceCompat(R.string.cancel_label))
            }
        },
    )
}

@Composable
private fun stringResourceCompat(id: Int, vararg args: Any): String {
    return if (args.isEmpty()) {
        androidx.compose.ui.res.stringResource(id)
    } else {
        androidx.compose.ui.res.stringResource(id, *args)
    }
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
