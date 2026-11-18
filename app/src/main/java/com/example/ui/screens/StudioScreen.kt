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
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LibrarySortOrder
import com.example.data.LinkedFolderEntity
import com.example.data.VideoMetadata
import com.example.data.VideoMetadataRepository
import com.example.data.WorkspaceTab
import com.example.ui.components.SpecPill
import com.example.ui.theme.MonoBadgeStyle

@Composable
fun StudioScreen(
  libraryVideos: List<VideoMetadata>,
  libraryThumbnails: Map<String, Bitmap>,
  linkedFolders: List<LinkedFolderEntity>,
  sortOrder: LibrarySortOrder,
  isScanningLibrary: Boolean,
  onSelectSortOrder: (LibrarySortOrder) -> Unit,
  onPickMultipleVideos: (WorkspaceTab) -> Unit,
  onLinkVideoFolder: () -> Unit,
  onUnlinkFolder: (LinkedFolderEntity) -> Unit,
  onRefreshLibrary: () -> Unit,
  onBrowseFiles: (WorkspaceTab) -> Unit,
  onOpenLibraryVideo: (VideoMetadata, WorkspaceTab) -> Unit,
  onAddVideoToMerger: (VideoMetadata) -> Unit,
  onShareLibraryVideo: (VideoMetadata) -> Unit,
  onRemoveLibraryVideo: (VideoMetadata) -> Unit,
  modifier: Modifier = Modifier,
) {
  var sortMenuExpanded by remember { mutableStateOf(false) }

  val isDateSorted =
    sortOrder == LibrarySortOrder.NEWEST_FIRST || sortOrder == LibrarySortOrder.OLDEST_FIRST

  val groupedByDate =
    remember(libraryVideos, sortOrder) {
      if (!isDateSorted) {
        emptyList()
      } else {
        libraryVideos
          .groupBy { VideoMetadataRepository.dateBucketLabel(it.deviceTimestampMs) }
          .entries
          .toList()
      }
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Compact Home Dashboard Header & Quick Picker Bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = "All Videos (${libraryVideos.size})",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (isScanningLibrary) {
              CircularProgressIndicator(
                modifier = Modifier.padding(horizontal = 12.dp).size(20.dp),
                strokeWidth = 2.dp,
              )
            } else {
              IconButton(
                onClick = onRefreshLibrary,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("refresh_library_button"),
              ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh videos")
              }
            }
            FilledTonalButton(
              onClick = { onPickMultipleVideos(WorkspaceTab.PLAY) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("studio_pick_video_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Videos")
            }
          }
        }
      }

      // Secondary Sort / Folder / File Bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box {
            FilterChip(
              selected = true,
              onClick = { sortMenuExpanded = true },
              label = { Text(sortOrder.label) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.Sort,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              },
              modifier = Modifier.height(48.dp).testTag("studio_sort_chip"),
            )

            DropdownMenu(
              expanded = sortMenuExpanded,
              onDismissRequest = { sortMenuExpanded = false },
            ) {
              LibrarySortOrder.entries.forEach { option ->
                DropdownMenuItem(
                  text = { Text(option.label) },
                  onClick = {
                    sortMenuExpanded = false
                    onSelectSortOrder(option)
                  },
                )
              }
            }
          }

          OutlinedButton(
            onClick = onLinkVideoFolder,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("studio_sync_folder_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.CreateNewFolder,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Folder")
          }

          OutlinedButton(
            onClick = { onBrowseFiles(WorkspaceTab.PLAY) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("studio_browse_files_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.FolderOpen,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Files")
          }

          linkedFolders.forEach { folder ->
            InputChip(
              selected = true,
              onClick = onRefreshLibrary,
              label = { Text(folder.folderName) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Rounded.Folder,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              trailingIcon = {
                IconButton(
                  onClick = { onUnlinkFolder(folder) },
                  modifier = Modifier.size(22.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Unlink ${folder.folderName}",
                    modifier = Modifier.size(16.dp),
                  )
                }
              },
              modifier = Modifier.height(48.dp),
            )
          }
        }
      }

      // Main User Videos Dashboard List
      if (libraryVideos.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                  )
                }
              }
              Text(
                text = "No videos yet",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
              )
              Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Button(
                  onClick = { onPickMultipleVideos(WorkspaceTab.PLAY) },
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("empty_pick_videos_button"),
                ) {
                  Text("Pick Videos")
                }
                FilledTonalButton(
                  onClick = onLinkVideoFolder,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("empty_select_folder_button"),
                ) {
                  Text("Select Folder")
                }
              }
            }
          }
        }
      } else if (isDateSorted) {
        groupedByDate.forEach { (dateLabel, videosInBucket) ->
          item(key = "header_$dateLabel") {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "${videosInBucket.size} ${if (videosInBucket.size == 1) "video" else "videos"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          items(videosInBucket, key = { it.uriString }) { video ->
            HomeVideoCard(
              video = video,
              thumbnail = libraryThumbnails[video.uriString],
              onPlay = { onOpenLibraryVideo(video, WorkspaceTab.PLAY) },
              onTrim = { onOpenLibraryVideo(video, WorkspaceTab.TRIM) },
              onCut = { onOpenLibraryVideo(video, WorkspaceTab.CUT) },
              onConvert = { onOpenLibraryVideo(video, WorkspaceTab.CONVERT) },
              onAddToMerger = { onAddVideoToMerger(video) },
              onShare = { onShareLibraryVideo(video) },
              onRemove = { onRemoveLibraryVideo(video) },
            )
          }
        }
      } else {
        items(libraryVideos, key = { it.uriString }) { video ->
          HomeVideoCard(
            video = video,
            thumbnail = libraryThumbnails[video.uriString],
            onPlay = { onOpenLibraryVideo(video, WorkspaceTab.PLAY) },
            onTrim = { onOpenLibraryVideo(video, WorkspaceTab.TRIM) },
            onCut = { onOpenLibraryVideo(video, WorkspaceTab.CUT) },
            onConvert = { onOpenLibraryVideo(video, WorkspaceTab.CONVERT) },
            onAddToMerger = { onAddVideoToMerger(video) },
            onShare = { onShareLibraryVideo(video) },
            onRemove = { onRemoveLibraryVideo(video) },
          )
        }
      }

      item { Spacer(modifier = Modifier.height(14.dp)) }
    }
  }
}

@Composable
private fun HomeVideoCard(
  video: VideoMetadata,
  thumbnail: Bitmap?,
  onPlay: () -> Unit,
  onTrim: () -> Unit,
  onCut: () -> Unit,
  onConvert: () -> Unit,
  onAddToMerger: () -> Unit,
  onShare: () -> Unit,
  onRemove: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    onClick = onPlay,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    modifier = Modifier.fillMaxWidth().testTag("library_video_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Video Thumbnail with Duration Badge
      Box(
        modifier =
          Modifier.size(width = 108.dp, height = 68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
      ) {
        if (thumbnail != null && !video.isAudioOnly) {
          Image(
            bitmap = thumbnail.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            imageVector = if (video.isAudioOnly) Icons.Rounded.GraphicEq else Icons.Rounded.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(28.dp),
          )
        }

        Surface(
          color = Color.Black.copy(alpha = 0.72f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
        ) {
          Text(
            text = VideoMetadataRepository.formatDuration(video.durationMs),
            style = MonoBadgeStyle,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
          )
        }
      }

      // Video Details
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = video.displayName,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        val dateText = VideoMetadataRepository.formatDateTime(video.deviceTimestampMs)
        if (dateText.isNotEmpty()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = dateText,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
          SpecPill(video.resolutionLabel)
          if (video.fileSizeBytes > 0L) {
            SpecPill(VideoMetadataRepository.formatFileSize(video.fileSizeBytes))
          }
          SpecPill(video.sourceTag)
        }
      }

      // Side 3-Dot Overflow Menu with All Video Tools
      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier =
            Modifier.minimumInteractiveComponentSize().testTag("video_more_menu_${video.displayName}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Video tools for ${video.displayName}",
          )
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text("Play") },
            leadingIcon = {
              Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            },
            onClick = {
              menuExpanded = false
              onPlay()
            },
          )
          DropdownMenuItem(
            text = { Text("Trim Video") },
            leadingIcon = {
              Icon(
                Icons.Rounded.ContentCut,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            },
            onClick = {
              menuExpanded = false
              onTrim()
            },
          )
          if (!video.isAudioOnly) {
            DropdownMenuItem(
              text = { Text("Cut / Split Video") },
              leadingIcon = {
                Icon(
                  Icons.Rounded.CallSplit,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
              },
              onClick = {
                menuExpanded = false
                onCut()
              },
            )
            DropdownMenuItem(
              text = { Text("Convert Format") },
              leadingIcon = {
                Icon(
                  Icons.Rounded.Transform,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                )
              },
              onClick = {
                menuExpanded = false
                onConvert()
              },
            )
            DropdownMenuItem(
              text = { Text("Add to Video Merger") },
              leadingIcon = {
                Icon(
                  Icons.Rounded.AutoAwesomeMotion,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              },
              onClick = {
                menuExpanded = false
                onAddToMerger()
              },
            )
          }
          DropdownMenuItem(
            text = { Text("Share Video") },
            leadingIcon = {
              Icon(
                Icons.Rounded.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            },
            onClick = {
              menuExpanded = false
              onShare()
            },
          )
          HorizontalDivider()
          DropdownMenuItem(
            text = {
              Text("Remove", color = MaterialTheme.colorScheme.error)
            },
            leadingIcon = {
              Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
              )
            },
            onClick = {
              menuExpanded = false
              onRemove()
            },
          )
        }
      }
    }
  }
}
