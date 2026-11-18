package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExportJobState
import com.example.data.ExportRecordEntity
import com.example.data.VideoMetadataRepository
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoTimecodeStyle

@Composable
fun ExportJobDialog(
  jobState: ExportJobState,
  onCancel: () -> Unit,
  onDismiss: () -> Unit,
  onPlayExport: (ExportRecordEntity) -> Unit,
  onShareExport: (ExportRecordEntity) -> Unit,
  onSaveToGallery: (ExportRecordEntity) -> Unit,
) {
  when (jobState) {
    ExportJobState.Idle -> Unit

    is ExportJobState.Running -> {
      AlertDialog(
        onDismissRequest = {},
        title = {
          Text(
            text = jobState.operation.label,
            style = MaterialTheme.typography.titleLarge,
          )
        },
        text = {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = jobState.statusLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
              progress = { (jobState.progressPercent / 100f).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(8.dp),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Hardware encoding in progress",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${jobState.progressPercent}%",
                style = MonoTimecodeStyle,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        },
        confirmButton = {
          TextButton(
            onClick = onCancel,
            modifier = Modifier.height(48.dp).testTag("export_cancel_button"),
          ) {
            Text("Cancel")
          }
        },
      )
    }

    is ExportJobState.Success -> {
      val record = jobState.record
      val second = jobState.secondaryRecord
      AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(56.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp),
              )
            }
          }
        },
        title = {
          Text(
            text = if (second != null) "Split Complete (2 Clips)" else "Export Complete",
            style = MaterialTheme.typography.headlineSmall,
          )
        },
        text = {
          Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExportSummaryBox(record = record)

            if (second != null) {
              ExportSummaryBox(record = second)
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = { onShareExport(record) },
                modifier = Modifier.weight(1f).height(48.dp).testTag("export_success_share_button"),
              ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (second != null) "Share Part 1" else "Share")
              }
              FilledTonalButton(
                onClick = {
                  onSaveToGallery(record)
                  if (second != null) onSaveToGallery(second)
                },
                enabled = jobState.savedToGalleryUri == null,
                modifier = Modifier.weight(1f).height(48.dp).testTag("export_success_save_button"),
              ) {
                Icon(
                  Icons.Rounded.DownloadDone,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (jobState.savedToGalleryUri != null) "Saved" else "Save")
              }
            }

            OutlinedButton(
              onClick = { onPlayExport(record) },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_success_play_button"),
            ) {
              Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (second != null) "Play Part 1" else "Play Exported Clip")
            }
          }
        },
        confirmButton = {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.height(48.dp).testTag("export_success_done_button"),
          ) {
            Text("Done")
          }
        },
      )
    }

    is ExportJobState.Error -> {
      AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
          Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(36.dp),
          )
        },
        title = { Text("Export Error", style = MaterialTheme.typography.titleLarge) },
        text = {
          Text(
            text = jobState.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        confirmButton = {
          Button(
            onClick = onDismiss,
            modifier = Modifier.height(48.dp).testTag("export_error_dismiss_button"),
          ) {
            Text("Close")
          }
        },
      )
    }
  }
}

@Composable
private fun ExportSummaryBox(record: ExportRecordEntity) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = record.displayName,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SpecPill(text = record.codecLabel)
        SpecPill(text = record.resolutionLabel)
        SpecPill(text = VideoMetadataRepository.formatFileSize(record.fileSizeBytes))
      }
      Text(
        text = "Duration ${VideoMetadataRepository.formatDuration(record.durationMs, includeTenths = true)}",
        style = MonoTimecodeStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun SpecPill(text: String, modifier: Modifier = Modifier) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier,
  ) {
    Text(
      text = text,
      style = MonoBadgeStyle,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}
