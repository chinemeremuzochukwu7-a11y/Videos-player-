package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.R
import com.example.data.ConversionConfig
import com.example.data.ConversionPreset
import com.example.data.CutMode
import com.example.data.FrameRateOption
import com.example.data.OutputCodecPreset
import com.example.data.ResolutionOption
import com.example.data.VideoMetadata
import com.example.data.VideoMetadataRepository
import com.example.data.WorkspaceTab
import com.example.ui.components.SpecPill
import com.example.ui.components.VideoPlayerViewport
import com.example.ui.components.VisualTrimTimeline
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.MonoTimecodeStyle
import com.example.viewmodel.PlayerUiState
import com.example.viewmodel.StudioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
  player: Player,
  playerState: PlayerUiState,
  uiState: StudioUiState,
  onPickVideo: () -> Unit,
  onBrowseFiles: () -> Unit,
  onSelectTab: (WorkspaceTab) -> Unit,
  onTogglePlayPause: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onSeekBy: (Long) -> Unit,
  onSetPlaybackSpeed: (Float) -> Unit,
  onToggleMute: () -> Unit,
  onToggleLoop: () -> Unit,
  onToggleResizeMode: () -> Unit,
  onEnterFullscreen: () -> Unit,
  onShareActiveVideo: () -> Unit,
  onTrimRangeChange: (Long, Long, Long?) -> Unit,
  onNudgeTrimStart: (Long) -> Unit,
  onNudgeTrimEnd: (Long) -> Unit,
  onToggleFastTrim: (Boolean) -> Unit,
  onTogglePreviewTrim: () -> Unit,
  onStartTrimExport: () -> Unit,
  onSelectCutMode: (CutMode) -> Unit,
  onSetSplitPointMs: (Long) -> Unit,
  onSetSplitAtPlayhead: () -> Unit,
  onStartCutExport: () -> Unit,
  onApplyPreset: (ConversionPreset) -> Unit,
  onUpdateConversionConfig: ((ConversionConfig) -> ConversionConfig) -> Unit,
  onStartConvertExport: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeVideo = uiState.activeVideo
  if (activeVideo == null) {
    EmptyWorkspaceState(
      onPickVideo = onPickVideo,
      onBrowseFiles = onBrowseFiles,
      modifier = modifier,
    )
    return
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isWideLayout = maxWidth >= 760.dp

    if (isWideLayout) {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Column(
          modifier = Modifier.weight(1.1f).fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          VideoPlayerViewport(
            player = player,
            playerState = playerState,
            title = activeVideo.displayName,
            isAudioOnly = activeVideo.isAudioOnly,
            isFullscreen = false,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSeekBy = onSeekBy,
            onToggleMute = onToggleMute,
            onToggleResizeMode = onToggleResizeMode,
            onToggleFullscreen = onEnterFullscreen,
          )
          ActiveClipMetadataStrip(video = activeVideo, onPickVideo = onPickVideo)
        }

        Column(
          modifier =
            Modifier.weight(0.95f)
              .fillMaxHeight()
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          WorkspaceModeSwitcher(
            selectedTab = uiState.workspaceTab,
            isAudioOnly = activeVideo.isAudioOnly,
            onSelectTab = onSelectTab,
          )
          WorkspaceTabBody(
            uiState = uiState,
            playerState = playerState,
            activeVideo = activeVideo,
            onSetPlaybackSpeed = onSetPlaybackSpeed,
            onToggleLoop = onToggleLoop,
            onShareActiveVideo = onShareActiveVideo,
            onPickVideo = onPickVideo,
            onTrimRangeChange = onTrimRangeChange,
            onNudgeTrimStart = onNudgeTrimStart,
            onNudgeTrimEnd = onNudgeTrimEnd,
            onToggleFastTrim = onToggleFastTrim,
            onTogglePreviewTrim = onTogglePreviewTrim,
            onStartTrimExport = onStartTrimExport,
            onSelectCutMode = onSelectCutMode,
            onSetSplitPointMs = onSetSplitPointMs,
            onSetSplitAtPlayhead = onSetSplitAtPlayhead,
            onStartCutExport = onStartCutExport,
            onApplyPreset = onApplyPreset,
            onUpdateConversionConfig = onUpdateConversionConfig,
            onStartConvertExport = onStartConvertExport,
          )
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    } else {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = 640.dp)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          VideoPlayerViewport(
            player = player,
            playerState = playerState,
            title = activeVideo.displayName,
            isAudioOnly = activeVideo.isAudioOnly,
            isFullscreen = false,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSeekBy = onSeekBy,
            onToggleMute = onToggleMute,
            onToggleResizeMode = onToggleResizeMode,
            onToggleFullscreen = onEnterFullscreen,
          )

          ActiveClipMetadataStrip(video = activeVideo, onPickVideo = onPickVideo)

          WorkspaceModeSwitcher(
            selectedTab = uiState.workspaceTab,
            isAudioOnly = activeVideo.isAudioOnly,
            onSelectTab = onSelectTab,
          )

          WorkspaceTabBody(
            uiState = uiState,
            playerState = playerState,
            activeVideo = activeVideo,
            onSetPlaybackSpeed = onSetPlaybackSpeed,
            onToggleLoop = onToggleLoop,
            onShareActiveVideo = onShareActiveVideo,
            onPickVideo = onPickVideo,
            onTrimRangeChange = onTrimRangeChange,
            onNudgeTrimStart = onNudgeTrimStart,
            onNudgeTrimEnd = onNudgeTrimEnd,
            onToggleFastTrim = onToggleFastTrim,
            onTogglePreviewTrim = onTogglePreviewTrim,
            onStartTrimExport = onStartTrimExport,
            onSelectCutMode = onSelectCutMode,
            onSetSplitPointMs = onSetSplitPointMs,
            onSetSplitAtPlayhead = onSetSplitAtPlayhead,
            onStartCutExport = onStartCutExport,
            onApplyPreset = onApplyPreset,
            onUpdateConversionConfig = onUpdateConversionConfig,
            onStartConvertExport = onStartConvertExport,
          )

          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceModeSwitcher(
  selectedTab: WorkspaceTab,
  isAudioOnly: Boolean,
  onSelectTab: (WorkspaceTab) -> Unit,
) {
  val tabs =
    if (isAudioOnly) listOf(WorkspaceTab.PLAY, WorkspaceTab.TRIM)
    else listOf(WorkspaceTab.PLAY, WorkspaceTab.TRIM, WorkspaceTab.CUT, WorkspaceTab.CONVERT)

  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    tabs.forEachIndexed { index, tab ->
      val label =
        when (tab) {
          WorkspaceTab.PLAY -> "Play"
          WorkspaceTab.TRIM -> "Trim"
          WorkspaceTab.CUT -> "Cut"
          WorkspaceTab.CONVERT -> "Convert"
        }
      val icon =
        when (tab) {
          WorkspaceTab.PLAY -> Icons.Rounded.PlayArrow
          WorkspaceTab.TRIM -> Icons.Rounded.ContentCut
          WorkspaceTab.CUT -> Icons.Rounded.CallSplit
          WorkspaceTab.CONVERT -> Icons.Rounded.Transform
        }
      SegmentedButton(
        selected = selectedTab == tab,
        onClick = { onSelectTab(tab) },
        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
        icon = {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        modifier = Modifier.height(48.dp).testTag("workspace_tab_${tab.name.lowercase()}"),
      ) {
        Text(label, maxLines = 1)
      }
    }
  }
}

@Composable
private fun ActiveClipMetadataStrip(
  video: VideoMetadata,
  onPickVideo: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
    ) {
      SpecPill(video.resolutionLabel)
      SpecPill(VideoMetadataRepository.formatDuration(video.durationMs))
      if (video.fileSizeBytes > 0L) {
        SpecPill(VideoMetadataRepository.formatFileSize(video.fileSizeBytes))
      }
    }
    Spacer(modifier = Modifier.width(8.dp))
    FilledTonalButton(
      onClick = onPickVideo,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.height(48.dp).testTag("workspace_change_video_button"),
    ) {
      Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Switch")
    }
  }
}

@Composable
private fun WorkspaceTabBody(
  uiState: StudioUiState,
  playerState: PlayerUiState,
  activeVideo: VideoMetadata,
  onSetPlaybackSpeed: (Float) -> Unit,
  onToggleLoop: () -> Unit,
  onShareActiveVideo: () -> Unit,
  onPickVideo: () -> Unit,
  onTrimRangeChange: (Long, Long, Long?) -> Unit,
  onNudgeTrimStart: (Long) -> Unit,
  onNudgeTrimEnd: (Long) -> Unit,
  onToggleFastTrim: (Boolean) -> Unit,
  onTogglePreviewTrim: () -> Unit,
  onStartTrimExport: () -> Unit,
  onSelectCutMode: (CutMode) -> Unit,
  onSetSplitPointMs: (Long) -> Unit,
  onSetSplitAtPlayhead: () -> Unit,
  onStartCutExport: () -> Unit,
  onApplyPreset: (ConversionPreset) -> Unit,
  onUpdateConversionConfig: ((ConversionConfig) -> ConversionConfig) -> Unit,
  onStartConvertExport: () -> Unit,
) {
  when (uiState.workspaceTab) {
    WorkspaceTab.PLAY ->
      PlayControlsPanel(
        video = activeVideo,
        playerState = playerState,
        onSetPlaybackSpeed = onSetPlaybackSpeed,
        onToggleLoop = onToggleLoop,
        onShareActiveVideo = onShareActiveVideo,
        onPickVideo = onPickVideo,
      )

    WorkspaceTab.TRIM ->
      TrimControlsPanel(
        video = activeVideo,
        uiState = uiState,
        currentPositionMs = playerState.positionMs,
        onTrimRangeChange = onTrimRangeChange,
        onNudgeStart = onNudgeTrimStart,
        onNudgeEnd = onNudgeTrimEnd,
        onToggleFastTrim = onToggleFastTrim,
        onTogglePreviewTrim = onTogglePreviewTrim,
        onStartTrimExport = onStartTrimExport,
      )

    WorkspaceTab.CUT ->
      CutControlsPanel(
        video = activeVideo,
        uiState = uiState,
        currentPositionMs = playerState.positionMs,
        onSelectCutMode = onSelectCutMode,
        onTrimRangeChange = onTrimRangeChange,
        onNudgeStart = onNudgeTrimStart,
        onNudgeEnd = onNudgeTrimEnd,
        onSetSplitPointMs = onSetSplitPointMs,
        onSetSplitAtPlayhead = onSetSplitAtPlayhead,
        onStartCutExport = onStartCutExport,
      )

    WorkspaceTab.CONVERT ->
      ConvertControlsPanel(
        video = activeVideo,
        uiState = uiState,
        onApplyPreset = onApplyPreset,
        onUpdateConfig = onUpdateConversionConfig,
        onStartConvertExport = onStartConvertExport,
      )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayControlsPanel(
  video: VideoMetadata,
  playerState: PlayerUiState,
  onSetPlaybackSpeed: (Float) -> Unit,
  onToggleLoop: () -> Unit,
  onShareActiveVideo: () -> Unit,
  onPickVideo: () -> Unit,
) {
  val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("Playback Speed", style = MaterialTheme.typography.titleSmall)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          speeds.forEach { spd ->
            FilterChip(
              selected = playerState.playbackSpeed == spd,
              onClick = { onSetPlaybackSpeed(spd) },
              label = { Text("${spd}x", style = MonoBadgeStyle) },
              modifier = Modifier.height(48.dp).testTag("speed_chip_${spd}"),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Repeat,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
            )
            Column {
              Text("Loop Playback", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "Repeat video continuously",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = playerState.isLooping,
            onCheckedChange = { onToggleLoop() },
            modifier = Modifier.testTag("play_loop_switch"),
          )
        }
      }
    }

    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("Clip Inspector", style = MaterialTheme.typography.titleSmall)
        InspectorRow("File Name", video.displayName)
        InspectorRow("Format", video.mimeType)
        InspectorRow("Resolution", video.resolutionLabel)
        InspectorRow(
          "Duration",
          VideoMetadataRepository.formatDuration(video.durationMs, includeTenths = true),
        )
        InspectorRow("File Size", VideoMetadataRepository.formatFileSize(video.fileSizeBytes))
        if (video.bitrateBps > 0L) {
          InspectorRow("Bitrate", "${video.bitrateBps / 1000L} kbps")
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Button(
        onClick = onShareActiveVideo,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).height(50.dp).testTag("play_share_button"),
      ) {
        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Share Video")
      }
      OutlinedButton(
        onClick = onPickVideo,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).height(50.dp),
      ) {
        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Pick Another")
      }
    }
  }
}

@Composable
private fun InspectorRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MonoTimecodeStyle,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun TrimControlsPanel(
  video: VideoMetadata,
  uiState: StudioUiState,
  currentPositionMs: Long,
  onTrimRangeChange: (Long, Long, Long?) -> Unit,
  onNudgeStart: (Long) -> Unit,
  onNudgeEnd: (Long) -> Unit,
  onToggleFastTrim: (Boolean) -> Unit,
  onTogglePreviewTrim: () -> Unit,
  onStartTrimExport: () -> Unit,
) {
  val duration = video.durationMs.coerceAtLeast(1000L)

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("Trim & Keep Selected Range", style = MaterialTheme.typography.titleSmall)
        VisualTrimTimeline(
          durationMs = duration,
          trimStartMs = uiState.trimStartMs,
          trimEndMs = uiState.trimEndMs,
          currentPositionMs = currentPositionMs,
          thumbnails = uiState.timelineThumbnails,
          isLoadingThumbnails = uiState.isLoadingThumbnails,
          onTrimRangeChange = onTrimRangeChange,
          onNudgeStart = onNudgeStart,
          onNudgeEnd = onNudgeEnd,
        )
      }
    }

    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text("Fast Trim Optimization", style = MaterialTheme.typography.titleSmall)
          Text(
            text = "Avoids full re-encoding when encoder matches source MP4",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = uiState.fastTrimEnabled,
          onCheckedChange = onToggleFastTrim,
          modifier = Modifier.testTag("trim_fast_switch"),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      FilledTonalButton(
        onClick = onTogglePreviewTrim,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).height(52.dp).testTag("trim_preview_button"),
      ) {
        Icon(
          imageVector = if (uiState.isPreviewingTrim) Icons.Rounded.Stop else Icons.Rounded.Loop,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (uiState.isPreviewingTrim) "Stop Preview" else "Preview Cut")
      }

      Button(
        onClick = onStartTrimExport,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1.2f).height(52.dp).testTag("trim_export_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.ContentCut,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Trim & Export")
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CutControlsPanel(
  video: VideoMetadata,
  uiState: StudioUiState,
  currentPositionMs: Long,
  onSelectCutMode: (CutMode) -> Unit,
  onTrimRangeChange: (Long, Long, Long?) -> Unit,
  onNudgeStart: (Long) -> Unit,
  onNudgeEnd: (Long) -> Unit,
  onSetSplitPointMs: (Long) -> Unit,
  onSetSplitAtPlayhead: () -> Unit,
  onStartCutExport: () -> Unit,
) {
  val duration = video.durationMs.coerceAtLeast(1000L)

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Video Cutter Mode", style = MaterialTheme.typography.titleSmall)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CutMode.entries.forEach { mode ->
            FilterChip(
              selected = uiState.cutMode == mode,
              onClick = { onSelectCutMode(mode) },
              label = { Text(mode.label) },
              modifier = Modifier.height(48.dp).testTag("cut_mode_${mode.name.lowercase()}"),
            )
          }
        }
        Text(
          text = uiState.cutMode.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (uiState.cutMode == CutMode.REMOVE_MIDDLE) {
      val cutLength = (uiState.trimEndMs - uiState.trimStartMs).coerceAtLeast(0L)
      val outputLength = (duration - cutLength).coerceAtLeast(0L)

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Select Unwanted Middle Section to Remove", style = MaterialTheme.typography.titleSmall)
          VisualTrimTimeline(
            durationMs = duration,
            trimStartMs = uiState.trimStartMs,
            trimEndMs = uiState.trimEndMs,
            currentPositionMs = currentPositionMs,
            thumbnails = uiState.timelineThumbnails,
            isLoadingThumbnails = uiState.isLoadingThumbnails,
            onTrimRangeChange = onTrimRangeChange,
            onNudgeStart = onNudgeStart,
            onNudgeEnd = onNudgeEnd,
          )
          Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Removing ${VideoMetadataRepository.formatDuration(cutLength, true)}",
                style = MonoTimecodeStyle,
                color = MaterialTheme.colorScheme.error,
              )
              Text(
                text = "Output ${VideoMetadataRepository.formatDuration(outputLength, true)}",
                style = MonoTimecodeStyle,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
          }
        }
      }
    } else {
      val splitFrac = (uiState.splitPointMs.toFloat() / duration.toFloat()).coerceIn(0.05f, 0.95f)
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Split Point", style = MaterialTheme.typography.titleSmall)
            SpecPill(VideoMetadataRepository.formatDuration(uiState.splitPointMs, includeTenths = true))
          }

          Slider(
            value = splitFrac,
            onValueChange = { frac -> onSetSplitPointMs((frac * duration).toLong()) },
            valueRange = 0.05f..0.95f,
            modifier = Modifier.fillMaxWidth().testTag("cut_split_slider"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Part 1: ${VideoMetadataRepository.formatDuration(uiState.splitPointMs, true)}",
              style = MonoTimecodeStyle,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text =
                "Part 2: ${VideoMetadataRepository.formatDuration((duration - uiState.splitPointMs).coerceAtLeast(0L), true)}",
              style = MonoTimecodeStyle,
              color = MaterialTheme.colorScheme.secondary,
            )
          }

          FilledTonalButton(
            onClick = onSetSplitAtPlayhead,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cut_set_playhead_button"),
          ) {
            Text("Set Split at Current Playhead")
          }
        }
      }
    }

    Button(
      onClick = onStartCutExport,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().height(54.dp).testTag("cut_export_button"),
    ) {
      Icon(Icons.Rounded.CallSplit, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text =
          if (uiState.cutMode == CutMode.REMOVE_MIDDLE) "Cut Out Middle & Export MP4"
          else "Split & Export 2 Clips",
        style = MaterialTheme.typography.titleSmall,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConvertControlsPanel(
  video: VideoMetadata,
  uiState: StudioUiState,
  onApplyPreset: (ConversionPreset) -> Unit,
  onUpdateConfig: ((ConversionConfig) -> ConversionConfig) -> Unit,
  onStartConvertExport: () -> Unit,
) {
  val config = uiState.conversionConfig
  val hasActiveTrim =
    video.durationMs > 0L && (uiState.trimStartMs > 0L || uiState.trimEndMs < video.durationMs)

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Quick Presets
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Quick Presets", style = MaterialTheme.typography.titleSmall)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ConversionPreset.entries.forEach { preset ->
            val selected =
              config.codec == preset.codec &&
                config.resolution == preset.resolution &&
                config.frameRate == preset.frameRate
            FilterChip(
              selected = selected,
              onClick = { onApplyPreset(preset) },
              label = { Text(preset.label) },
              modifier = Modifier.height(48.dp).testTag("convert_preset_${preset.name.lowercase()}"),
            )
          }
        }
      }
    }

    // Output Format & Codec
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Format & Codec", style = MaterialTheme.typography.titleSmall)
        OutputCodecPreset.entries.forEach { codec ->
          val selected = config.codec == codec
          Surface(
            onClick = { onUpdateConfig { it.copy(codec = codec) } },
            shape = RoundedCornerShape(14.dp),
            color =
              if (selected) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().testTag("codec_option_${codec.name.lowercase()}"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  text = codec.title,
                  style = MaterialTheme.typography.titleSmall,
                  color =
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = codec.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (selected) {
                SpecPill("SELECTED")
              }
            }
          }
        }
      }
    }

    // Video Parameters (hidden when Audio Only is chosen)
    if (!config.codec.isAudioOnly) {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Resolution", style = MaterialTheme.typography.titleSmall)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ResolutionOption.entries.forEach { res ->
              FilterChip(
                selected = config.resolution == res,
                onClick = { onUpdateConfig { it.copy(resolution = res) } },
                label = { Text(res.label) },
                modifier = Modifier.height(48.dp).testTag("res_chip_${res.name.lowercase()}"),
              )
            }
          }

          Text("Frame Rate", style = MaterialTheme.typography.titleSmall)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FrameRateOption.entries.forEach { fps ->
              FilterChip(
                selected = config.frameRate == fps,
                onClick = { onUpdateConfig { it.copy(frameRate = fps) } },
                label = { Text(fps.label) },
                modifier = Modifier.height(48.dp).testTag("fps_chip_${fps.name.lowercase()}"),
              )
            }
          }

          Text("Rotate Video", style = MaterialTheme.typography.titleSmall)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(0, 90, 180, 270).forEach { deg ->
              FilterChip(
                selected = config.rotationDegrees == deg,
                onClick = { onUpdateConfig { it.copy(rotationDegrees = deg) } },
                label = { Text("${deg}°", style = MonoBadgeStyle) },
                modifier = Modifier.height(48.dp).testTag("rotate_chip_$deg"),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text("Mute / Strip Audio", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "Export silent video stream",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = config.muteAudio,
              onCheckedChange = { checked -> onUpdateConfig { it.copy(muteAudio = checked) } },
              modifier = Modifier.testTag("convert_mute_switch"),
            )
          }
        }
      }
    }

    if (hasActiveTrim) {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text("Apply Trim Selection", style = MaterialTheme.typography.titleSmall)
            Text(
              text =
                "${VideoMetadataRepository.formatDuration(uiState.trimStartMs, true)} → ${VideoMetadataRepository.formatDuration(uiState.trimEndMs, true)}",
              style = MonoTimecodeStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = config.includeTrimRange,
            onCheckedChange = { checked ->
              onUpdateConfig { it.copy(includeTrimRange = checked) }
            },
            modifier = Modifier.testTag("convert_apply_trim_switch"),
          )
        }
      }
    }

    Button(
      onClick = onStartConvertExport,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().height(54.dp).testTag("convert_export_button"),
    ) {
      Icon(Icons.Rounded.Transform, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text =
          if (config.codec.isAudioOnly) "Extract Audio (.m4a)"
          else "Convert & Export (${config.codec.extension.uppercase()})",
        style = MaterialTheme.typography.titleSmall,
      )
    }
  }
}

@Composable
private fun EmptyWorkspaceState(
  onPickVideo: () -> Unit,
  onBrowseFiles: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
    Column(
      modifier = Modifier.widthIn(max = 420.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(76.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp),
          )
        }
      }
      Text(
        text = stringResource(R.string.empty_workspace_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(R.string.empty_workspace_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onPickVideo,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(50.dp).testTag("empty_workspace_pick_button"),
        ) {
          Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.action_pick_video))
        }
        FilledTonalButton(
          onClick = onBrowseFiles,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(50.dp).testTag("empty_workspace_browse_button"),
        ) {
          Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.action_browse_files))
        }
      }
    }
  }
}
