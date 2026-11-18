package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ExportOperation
import com.example.data.ExportRecordEntity
import com.example.data.ExportsFilter
import com.example.data.VideoMetadataRepository
import com.example.ui.components.SpecPill
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoTimecodeStyle

@Composable
fun ExportsScreen(
  exports: List<ExportRecordEntity>,
  activeFilter: ExportsFilter,
  thumbnails: Map<Long, Bitmap>,
  onSelectFilter: (ExportsFilter) -> Unit,
  onPlayExport: (ExportRecordEntity) -> Unit,
  onShareExport: (ExportRecordEntity) -> Unit,
  onSaveToGallery: (ExportRecordEntity) -> Unit,
  onDeleteExport: (ExportRecordEntity) -> Unit,
  onPickVideo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 760.dp)) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ExportsFilter.entries.forEach { filter ->
          FilterChip(
            selected = activeFilter == filter,
            onClick = { onSelectFilter(filter) },
            label = { Text(filter.label) },
            modifier = Modifier.height(48.dp).testTag("exports_filter_${filter.name.lowercase()}"),
          )
        }
      }

      if (exports.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            modifier = Modifier.widthIn(max = 400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.secondaryContainer,
              modifier = Modifier.size(72.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.VideoLibrary,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(34.dp),
                )
              }
            }
            Text(
              text = stringResource(R.string.empty_exports_title),
              style = MaterialTheme.typography.headlineSmall,
              textAlign = TextAlign.Center,
            )
            Text(
              text = stringResource(R.string.empty_exports_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            Button(
              onClick = onPickVideo,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.padding(top = 6.dp).height(50.dp).testTag("exports_pick_video_button"),
            ) {
              Text(stringResource(R.string.action_pick_video))
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(exports, key = { it.id }) { record ->
            ExportRecordCard(
              record = record,
              thumbnail = thumbnails[record.id],
              onPlay = { onPlayExport(record) },
              onShare = { onShareExport(record) },
              onSaveToGallery = { onSaveToGallery(record) },
              onDelete = { onDeleteExport(record) },
            )
          }
          item { Spacer(modifier = Modifier.height(14.dp)) }
        }
      }
    }
  }
}

@Composable
private fun ExportRecordCard(
  record: ExportRecordEntity,
  thumbnail: Bitmap?,
  onPlay: () -> Unit,
  onShare: () -> Unit,
  onSaveToGallery: () -> Unit,
  onDelete: () -> Unit,
) {
  val isAudio = record.mimeType.startsWith("audio/")
  val opLabel =
    runCatching { ExportOperation.valueOf(record.operationType).label }.getOrDefault("Exported")

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    modifier = Modifier.fillMaxWidth().testTag("export_card_${record.id}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(width = 104.dp, height = 68.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          contentAlignment = Alignment.Center,
        ) {
          if (thumbnail != null && !isAudio) {
            Image(
              bitmap = thumbnail.asImageBitmap(),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Icon(
              imageVector = if (isAudio) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(30.dp),
            )
          }
          Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
          ) {
            Text(
              text = VideoMetadataRepository.formatDuration(record.durationMs),
              style = MonoBadgeStyle,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
          }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(
            text = record.displayName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "$opLabel • from ${record.sourceDisplayName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
          ) {
            SpecPill(record.codecLabel)
            SpecPill(record.resolutionLabel)
            SpecPill(VideoMetadataRepository.formatFileSize(record.fileSizeBytes))
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilledTonalButton(
          onClick = onPlay,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("export_play_${record.id}"),
        ) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Play")
        }

        Button(
          onClick = onShare,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("export_share_${record.id}"),
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Share")
        }

        IconButton(
          onClick = onSaveToGallery,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("export_save_${record.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Download,
            contentDescription = stringResource(R.string.action_save_gallery),
            tint = MaterialTheme.colorScheme.secondary,
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("export_delete_${record.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = stringResource(R.string.action_delete),
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}
