package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Movie
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MergeConfig
import com.example.data.OutputCodecPreset
import com.example.data.ResolutionOption
import com.example.data.VideoMetadata
import com.example.data.VideoMetadataRepository
import com.example.ui.components.SpecPill
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoTimecodeStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MergerScreen(
  mergeQueue: List<VideoMetadata>,
  mergeConfig: MergeConfig,
  libraryVideos: List<VideoMetadata>,
  libraryThumbnails: Map<String, Bitmap>,
  onPickVideosForMerge: () -> Unit,
  onAddFromLibrary: (VideoMetadata) -> Unit,
  onRemoveAt: (Int) -> Unit,
  onMoveClip: (Int, Int) -> Unit,
  onClearQueue: () -> Unit,
  onUpdateMergeConfig: ((MergeConfig) -> MergeConfig) -> Unit,
  onStartMerge: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val totalDurationMs = mergeQueue.sumOf { it.durationMs }
  val videoOnlyLibrary = libraryVideos.filter { !it.isAudioOnly }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header Summary Card
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(42.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.AutoAwesomeMotion,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
                Column {
                  Text("Video Merger", style = MaterialTheme.typography.titleLarge)
                  Text(
                    text = "${mergeQueue.size} clips • Total ${VideoMetadataRepository.formatDuration(totalDurationMs)}",
                    style = MonoTimecodeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              if (mergeQueue.isNotEmpty()) {
                TextButton(
                  onClick = onClearQueue,
                  modifier = Modifier.height(48.dp).testTag("merger_clear_button"),
                ) {
                  Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Clear")
                }
              }
            }

            FilledTonalButton(
              onClick = onPickVideosForMerge,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("merger_pick_videos_button"),
            ) {
              Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Select Videos from Photo Picker")
            }
          }
        }
      }

      // Quick Add from Auto-Synced Library
      if (videoOnlyLibrary.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "Tap to Add from Your Library",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              items(videoOnlyLibrary, key = { it.uriString }) { vid ->
                val bmp = libraryThumbnails[vid.uriString]
                Card(
                  onClick = { onAddFromLibrary(vid) },
                  shape = RoundedCornerShape(14.dp),
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                  modifier = Modifier.width(152.dp).testTag("merger_library_add_${vid.displayName}"),
                ) {
                  Column {
                    Box(
                      modifier =
                        Modifier.fillMaxWidth()
                          .height(78.dp)
                          .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                      contentAlignment = Alignment.Center,
                    ) {
                      if (bmp != null) {
                        Image(
                          bitmap = bmp.asImageBitmap(),
                          contentDescription = null,
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxSize(),
                        )
                      } else {
                        Icon(
                          imageVector = Icons.Rounded.Movie,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.secondary,
                        )
                      }
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.Add,
                          contentDescription = "Add clip",
                          tint = MaterialTheme.colorScheme.onPrimary,
                          modifier = Modifier.padding(3.dp).size(16.dp),
                        )
                      }
                    }
                    Column(modifier = Modifier.padding(8.dp)) {
                      Text(
                        text = vid.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = VideoMetadataRepository.formatDuration(vid.durationMs),
                        style = MonoBadgeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Merge Queue Items
      if (mergeQueue.isEmpty()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text("Merge Timeline is Empty", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "Add at least 2 videos above or from the Photo Picker, then reorder clips to join them seamlessly into one MP4.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        item {
          Text(
            text = "Merge Sequence Order",
            style = MaterialTheme.typography.titleMedium,
          )
        }
        itemsIndexed(mergeQueue, key = { idx, item -> "${idx}_${item.uriString}" }) { index, clip ->
          val bmp = libraryThumbnails[clip.uriString]
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth().testTag("merger_queue_item_$index"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(30.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "${index + 1}",
                    style = MonoBadgeStyle,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }

              Box(
                modifier =
                  Modifier.size(width = 68.dp, height = 46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
              ) {
                if (bmp != null) {
                  Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                } else {
                  Icon(Icons.Rounded.Movie, contentDescription = null)
                }
              }

              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = clip.displayName,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  SpecPill(VideoMetadataRepository.formatDuration(clip.durationMs))
                  SpecPill(clip.resolutionLabel)
                }
              }

              IconButton(
                onClick = { onMoveClip(index, index - 1) },
                enabled = index > 0,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("merger_up_$index"),
              ) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move clip up")
              }
              IconButton(
                onClick = { onMoveClip(index, index + 1) },
                enabled = index < mergeQueue.lastIndex,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("merger_down_$index"),
              ) {
                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move clip down")
              }
              IconButton(
                onClick = { onRemoveAt(index) },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("merger_remove_$index"),
              ) {
                Icon(
                  Icons.Rounded.Close,
                  contentDescription = "Remove clip",
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }
        }
      }

      // Output Settings + Merge CTA
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text("Merge Output Settings", style = MaterialTheme.typography.titleSmall)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              listOf(OutputCodecPreset.H264_MP4, OutputCodecPreset.HEVC_MP4).forEach { codec ->
                FilterChip(
                  selected = mergeConfig.codec == codec,
                  onClick = { onUpdateMergeConfig { it.copy(codec = codec) } },
                  label = { Text(codec.title) },
                  modifier = Modifier.height(48.dp),
                )
              }
            }
            Text("Normalize Resolution", style = MaterialTheme.typography.labelLarge)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              listOf(
                ResolutionOption.P720,
                ResolutionOption.P1080,
                ResolutionOption.P480,
                ResolutionOption.ORIGINAL,
              ).forEach { res ->
                FilterChip(
                  selected = mergeConfig.resolution == res,
                  onClick = { onUpdateMergeConfig { it.copy(resolution = res) } },
                  label = { Text(res.label) },
                  modifier = Modifier.height(48.dp),
                )
              }
            }
          }
        }
      }

      item {
        Button(
          onClick = onStartMerge,
          enabled = mergeQueue.size >= 2,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("merger_export_button"),
        ) {
          Icon(
            Icons.Rounded.AutoAwesomeMotion,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              if (mergeQueue.size >= 2) "Merge ${mergeQueue.size} Videos into MP4"
              else "Add at least 2 videos to merge",
            style = MaterialTheme.typography.titleSmall,
          )
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}
