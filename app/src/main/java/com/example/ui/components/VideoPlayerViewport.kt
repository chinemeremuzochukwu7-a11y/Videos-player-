package com.example.ui.components

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import com.example.R
import com.example.data.VideoMetadataRepository
import com.example.ui.theme.MonoTimecodeStyle
import com.example.viewmodel.PlayerUiState
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerViewport(
  player: Player,
  playerState: PlayerUiState,
  title: String,
  isAudioOnly: Boolean,
  isFullscreen: Boolean,
  onTogglePlayPause: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onSeekBy: (Long) -> Unit,
  onToggleMute: () -> Unit,
  onToggleResizeMode: () -> Unit,
  onToggleFullscreen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var controlsVisible by remember { mutableStateOf(true) }
  var isScrubbing by remember { mutableStateOf(false) }
  var scrubFraction by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(controlsVisible, playerState.isPlaying, isScrubbing) {
    if (controlsVisible && playerState.isPlaying && !isScrubbing) {
      delay(3200L)
      controlsVisible = false
    }
  }

  val durationMs = playerState.durationMs.coerceAtLeast(1L)
  val currentFraction =
    if (isScrubbing) scrubFraction
    else (playerState.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

  val containerModifier =
    if (isFullscreen) {
      modifier.fillMaxSize().background(Color.Black)
    } else {
      modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(20.dp))
        .background(Color(0xFF090B0F))
    }

  Box(
    modifier =
      containerModifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
      ) {
        controlsVisible = !controlsVisible
      }
  ) {
    ContentFrame(
      player = player,
      modifier = Modifier.fillMaxSize(),
      contentScale = if (playerState.isCropToFill) ContentScale.Crop else ContentScale.Fit,
      shutter = {
        Box(
          modifier = Modifier.fillMaxSize().background(Color(0xFF0A0C11)),
          contentAlignment = Alignment.Center,
        ) {
          if (isAudioOnly) {
            AudioTrackBackdrop(title = title)
          }
        }
      },
    )

    if (isAudioOnly) {
      AudioTrackBackdrop(title = title)
    }

    if (playerState.isBuffering) {
      CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center).size(42.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp,
      )
    }

    playerState.errorMessage?.let { err ->
      Surface(
        modifier = Modifier.align(Alignment.Center).padding(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(14.dp),
      ) {
        Text(
          text = err,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
      }
    }

    AnimatedVisibility(
      visible = controlsVisible,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.65f),
                0.35f to Color.Black.copy(alpha = 0.15f),
                0.65f to Color.Black.copy(alpha = 0.20f),
                1f to Color.Black.copy(alpha = 0.78f),
              )
            )
      ) {
        // Top HUD bar
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .align(Alignment.TopCenter)
              .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
          )
          IconButton(
            onClick = onToggleMute,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("player_mute_button"),
          ) {
            Icon(
              imageVector =
                if (playerState.isMuted) Icons.AutoMirrored.Rounded.VolumeOff
                else Icons.AutoMirrored.Rounded.VolumeUp,
              contentDescription = stringResource(R.string.cd_mute),
              tint = Color.White,
            )
          }
          if (!isAudioOnly) {
            IconButton(
              onClick = onToggleResizeMode,
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("player_resize_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.AspectRatio,
                contentDescription = stringResource(R.string.cd_resize_mode),
                tint =
                  if (playerState.isCropToFill) MaterialTheme.colorScheme.secondary
                  else Color.White,
              )
            }
          }
          IconButton(
            onClick = onToggleFullscreen,
            modifier =
              Modifier.minimumInteractiveComponentSize().testTag("player_fullscreen_button"),
          ) {
            Icon(
              imageVector =
                if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
              contentDescription = stringResource(R.string.cd_fullscreen),
              tint = Color.White,
            )
          }
        }

        // Center transport cluster
        Row(
          modifier = Modifier.align(Alignment.Center),
          horizontalArrangement = Arrangement.spacedBy(22.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            onClick = { onSeekBy(-5_000L) },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(48.dp).testTag("player_seek_back_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Replay5,
                contentDescription = stringResource(R.string.cd_seek_back),
                tint = Color.White,
                modifier = Modifier.size(26.dp),
              )
            }
          }

          Surface(
            onClick = onTogglePlayPause,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(60.dp).testTag("player_play_pause_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector =
                  if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.cd_play_pause),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp),
              )
            }
          }

          Surface(
            onClick = { onSeekBy(5_000L) },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(48.dp).testTag("player_seek_forward_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Forward5,
                contentDescription = stringResource(R.string.cd_seek_forward),
                tint = Color.White,
                modifier = Modifier.size(26.dp),
              )
            }
          }
        }

        // Bottom scrubber
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .align(Alignment.BottomCenter)
              .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Slider(
            value = currentFraction,
            onValueChange = { value ->
              isScrubbing = true
              scrubFraction = value
            },
            onValueChangeFinished = {
              isScrubbing = false
              onSeekTo((scrubFraction * durationMs).toLong())
            },
            colors =
              SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.30f),
              ),
            modifier = Modifier.fillMaxWidth().height(28.dp).testTag("player_seek_slider"),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val displayPos =
              if (isScrubbing) (scrubFraction * durationMs).toLong() else playerState.positionMs
            Text(
              text =
                "${VideoMetadataRepository.formatDuration(displayPos)} / ${VideoMetadataRepository.formatDuration(playerState.durationMs)}",
              style = MonoTimecodeStyle,
              color = Color.White,
            )
            if (playerState.playbackSpeed != 1.0f) {
              Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(6.dp),
              ) {
                Text(
                  text = "${playerState.playbackSpeed}x",
                  style = MonoTimecodeStyle,
                  color = Color.Black,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AudioTrackBackdrop(title: String) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.linearGradient(
            listOf(Color(0xFF151923), Color(0xFF0E2B2B), Color(0xFF2B1410))
          )
        ),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.size(64.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(34.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White.copy(alpha = 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
    }
  }
}
