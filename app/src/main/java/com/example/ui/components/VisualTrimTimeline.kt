package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.VideoMetadataRepository
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoTimecodeStyle

@Composable
fun VisualTrimTimeline(
  durationMs: Long,
  trimStartMs: Long,
  trimEndMs: Long,
  currentPositionMs: Long,
  thumbnails: List<Bitmap>,
  isLoadingThumbnails: Boolean,
  onTrimRangeChange: (startMs: Long, endMs: Long, seekEdgeMs: Long?) -> Unit,
  onNudgeStart: (Long) -> Unit,
  onNudgeEnd: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val safeDuration = durationMs.coerceAtLeast(1000L)
  val startFrac = (trimStartMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
  val endFrac = (trimEndMs.toFloat() / safeDuration.toFloat()).coerceIn(startFrac, 1f)
  val playheadFrac = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary

  Column(modifier = modifier.fillMaxWidth()) {
    // Timecode badge strip
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TimecodeBadge(
        label = "IN",
        value = VideoMetadataRepository.formatDuration(trimStartMs, includeTenths = true),
        accent = primaryColor,
      )
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(10.dp),
      ) {
        Text(
          text =
            "CLIP ${VideoMetadataRepository.formatDuration((trimEndMs - trimStartMs).coerceAtLeast(0L), includeTenths = true)}",
          style = MonoBadgeStyle,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
      }
      TimecodeBadge(
        label = "OUT",
        value = VideoMetadataRepository.formatDuration(trimEndMs, includeTenths = true),
        accent = primaryColor,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Visual filmstrip with darkened excluded tails & live playhead
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(64.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(14.dp),
          )
    ) {
      if (thumbnails.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxSize()) {
          thumbnails.forEach { bmp ->
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.weight(1f).fillMaxHeight(),
            )
          }
        }
      } else if (isLoadingThumbnails) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 24.dp),
          color = primaryColor,
        )
      }

      Canvas(modifier = Modifier.fillMaxSize()) {
        val leftX = size.width * startFrac
        val rightX = size.width * endFrac
        val playX = size.width * playheadFrac

        // Dim excluded left and right sections
        if (leftX > 0f) {
          drawRect(
            color = Color.Black.copy(alpha = 0.65f),
            topLeft = Offset.Zero,
            size = Size(leftX, size.height),
          )
        }
        if (rightX < size.width) {
          drawRect(
            color = Color.Black.copy(alpha = 0.65f),
            topLeft = Offset(rightX, 0f),
            size = Size(size.width - rightX, size.height),
          )
        }

        // Selected range frame
        val frameWidth = (rightX - leftX).coerceAtLeast(2f)
        drawRect(
          color = primaryColor,
          topLeft = Offset(leftX, 1.5.dp.toPx()),
          size = Size(frameWidth, size.height - 3.dp.toPx()),
          style = Stroke(width = 3.dp.toPx()),
        )

        // Left & right handle bars
        drawRect(
          color = primaryColor,
          topLeft = Offset((leftX - 4.dp.toPx()).coerceAtLeast(0f), 0f),
          size = Size(8.dp.toPx(), size.height),
        )
        drawRect(
          color = primaryColor,
          topLeft = Offset((rightX - 4.dp.toPx()).coerceAtMost(size.width - 8.dp.toPx()), 0f),
          size = Size(8.dp.toPx(), size.height),
        )

        // Playhead line
        drawLine(
          color = secondaryColor,
          start = Offset(playX, 0f),
          end = Offset(playX, size.height),
          strokeWidth = 2.5.dp.toPx(),
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Precision dual-thumb RangeSlider
    RangeSlider(
      value = startFrac..endFrac,
      onValueChange = { range ->
        val newStart = (range.start * safeDuration).toLong()
        val newEnd = (range.endInclusive * safeDuration).toLong()
        val changedEdge =
          when {
            kotlin.math.abs(newStart - trimStartMs) > kotlin.math.abs(newEnd - trimEndMs) -> newStart
            else -> newEnd
          }
        onTrimRangeChange(newStart, newEnd, changedEdge)
      },
      valueRange = 0f..1f,
      colors =
        SliderDefaults.colors(
          thumbColor = primaryColor,
          activeTrackColor = primaryColor,
          inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
      modifier = Modifier.fillMaxWidth().testTag("trim_range_slider"),
    )

    // Fine-tune nudge controls (48dp touch targets)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalButton(
          onClick = { onNudgeStart(-500L) },
          modifier = Modifier.height(48.dp).testTag("trim_in_minus_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("IN -0.5s", style = MonoBadgeStyle)
        }
        FilledTonalButton(
          onClick = { onNudgeStart(500L) },
          modifier = Modifier.height(48.dp).testTag("trim_in_plus_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("IN +0.5s", style = MonoBadgeStyle)
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalButton(
          onClick = { onNudgeEnd(-500L) },
          modifier = Modifier.height(48.dp).testTag("trim_out_minus_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("OUT -0.5s", style = MonoBadgeStyle)
        }
        FilledTonalButton(
          onClick = { onNudgeEnd(500L) },
          modifier = Modifier.height(48.dp).testTag("trim_out_plus_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text("OUT +0.5s", style = MonoBadgeStyle)
        }
      }
    }
  }
}

@Composable
private fun TimecodeBadge(label: String, value: String, accent: Color) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(10.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = label, style = MonoBadgeStyle, color = accent)
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = value,
        style = MonoTimecodeStyle,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
