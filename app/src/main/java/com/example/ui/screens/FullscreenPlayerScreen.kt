package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.Player
import com.example.ui.components.VideoPlayerViewport
import com.example.viewmodel.PlayerUiState

@Composable
fun FullscreenPlayerScreen(
  player: Player,
  playerState: PlayerUiState,
  title: String,
  isAudioOnly: Boolean,
  onTogglePlayPause: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onSeekBy: (Long) -> Unit,
  onToggleMute: () -> Unit,
  onToggleResizeMode: () -> Unit,
  onExitFullscreen: () -> Unit,
) {
  BackHandler { onExitFullscreen() }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Color.Black)
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    VideoPlayerViewport(
      player = player,
      playerState = playerState,
      title = title,
      isAudioOnly = isAudioOnly,
      isFullscreen = true,
      onTogglePlayPause = onTogglePlayPause,
      onSeekTo = onSeekTo,
      onSeekBy = onSeekBy,
      onToggleMute = onToggleMute,
      onToggleResizeMode = onToggleResizeMode,
      onToggleFullscreen = onExitFullscreen,
      modifier = Modifier.fillMaxSize(),
    )
  }
}
