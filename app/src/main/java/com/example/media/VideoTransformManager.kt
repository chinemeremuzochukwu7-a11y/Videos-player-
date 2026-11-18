package com.example.media

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.ConversionConfig
import com.example.data.MergeConfig
import com.example.data.OutputCodecPreset
import java.io.File

@OptIn(UnstableApi::class)
class VideoTransformManager(private val context: Context) {

  private val mainHandler = Handler(Looper.getMainLooper())
  private var activeTransformer: Transformer? = null
  private var activeOutputFile: File? = null
  private var progressRunnable: Runnable? = null

  fun startTrim(
    sourceUri: Uri,
    outputFile: File,
    startMs: Long,
    endMs: Long,
    fastTrimOptimization: Boolean,
    onProgress: (Int) -> Unit,
    onSuccess: (File, ExportResult) -> Unit,
    onError: (String) -> Unit,
  ) {
    val clippedMediaItem =
      MediaItem.Builder()
        .setUri(sourceUri)
        .setClippingConfiguration(
          MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs.coerceAtLeast(0L))
            .setEndPositionMs(endMs.coerceAtLeast(startMs + 200L))
            .build()
        )
        .build()

    val editedMediaItem = EditedMediaItem.Builder(clippedMediaItem).build()

    val transformerBuilder =
      Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)

    if (fastTrimOptimization) {
      transformerBuilder.experimentalSetTrimOptimizationEnabled(true)
    }

    runSingleItemTransformation(
      transformerBuilder = transformerBuilder,
      editedMediaItem = editedMediaItem,
      outputFile = outputFile,
      onProgress = onProgress,
      onSuccess = onSuccess,
      onError = onError,
    )
  }

  /**
   * Video Cutter: removes the middle segment [cutStartMs .. cutEndMs] and joins
   * [0 .. cutStartMs] + [cutEndMs .. totalDurationMs] seamlessly via Composition.
   */
  fun startCutOutMiddle(
    sourceUri: Uri,
    outputFile: File,
    cutStartMs: Long,
    cutEndMs: Long,
    totalDurationMs: Long,
    onProgress: (Int) -> Unit,
    onSuccess: (File, ExportResult) -> Unit,
    onError: (String) -> Unit,
  ) {
    val keepStart = cutStartMs > 250L
    val keepEnd = cutEndMs < (totalDurationMs - 250L)

    if (!keepStart && !keepEnd) {
      onError("Cannot cut out the entire video. Leave at least 0.3s before or after the cut.")
      return
    }

    val segments = mutableListOf<EditedMediaItem>()
    if (keepStart) {
      val firstPart =
        MediaItem.Builder()
          .setUri(sourceUri)
          .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
              .setStartPositionMs(0L)
              .setEndPositionMs(cutStartMs)
              .build()
          )
          .build()
      segments.add(EditedMediaItem.Builder(firstPart).build())
    }
    if (keepEnd) {
      val secondPart =
        MediaItem.Builder()
          .setUri(sourceUri)
          .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
              .setStartPositionMs(cutEndMs)
              .setEndPositionMs(totalDurationMs)
              .build()
          )
          .build()
      segments.add(EditedMediaItem.Builder(secondPart).build())
    }

    val transformerBuilder =
      Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)

    if (segments.size == 1) {
      runSingleItemTransformation(
        transformerBuilder = transformerBuilder,
        editedMediaItem = segments.first(),
        outputFile = outputFile,
        onProgress = onProgress,
        onSuccess = onSuccess,
        onError = onError,
      )
    } else {
      val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(segments)
      val composition = Composition.Builder(sequence).build()
      runCompositionTransformation(
        transformerBuilder = transformerBuilder,
        composition = composition,
        outputFile = outputFile,
        onProgress = onProgress,
        onSuccess = onSuccess,
        onError = onError,
      )
    }
  }

  /**
   * Video Merger: sequentially concatenates 2 or more videos into a single MP4 via Media3 Composition.
   */
  fun startMerge(
    sourceUris: List<Uri>,
    outputFile: File,
    config: MergeConfig,
    onProgress: (Int) -> Unit,
    onSuccess: (File, ExportResult) -> Unit,
    onError: (String) -> Unit,
  ) {
    if (sourceUris.size < 2) {
      onError("Select at least 2 videos to merge.")
      return
    }

    val targetHeight = config.resolution.heightPx
    val editedItems =
      sourceUris.map { uri ->
        val builder = EditedMediaItem.Builder(MediaItem.fromUri(uri))
        if (targetHeight != null) {
          builder.setEffects(Effects(emptyList(), listOf(Presentation.createForHeight(targetHeight))))
        }
        builder.build()
      }

    val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(editedItems)
    val composition = Composition.Builder(sequence).build()

    val transformerBuilder =
      Transformer.Builder(context)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)
        .setVideoMimeType(
          if (config.codec == OutputCodecPreset.HEVC_MP4) MimeTypes.VIDEO_H265
          else MimeTypes.VIDEO_H264
        )

    runCompositionTransformation(
      transformerBuilder = transformerBuilder,
      composition = composition,
      outputFile = outputFile,
      onProgress = onProgress,
      onSuccess = onSuccess,
      onError = onError,
    )
  }

  fun startConvert(
    sourceUri: Uri,
    outputFile: File,
    config: ConversionConfig,
    trimStartMs: Long?,
    trimEndMs: Long?,
    sourceHeight: Int,
    onProgress: (Int) -> Unit,
    onSuccess: (File, ExportResult) -> Unit,
    onError: (String) -> Unit,
  ) {
    val mediaItemBuilder = MediaItem.Builder().setUri(sourceUri)
    if (
      config.includeTrimRange &&
        trimStartMs != null &&
        trimEndMs != null &&
        trimEndMs > trimStartMs + 200L
    ) {
      mediaItemBuilder.setClippingConfiguration(
        MediaItem.ClippingConfiguration.Builder()
          .setStartPositionMs(trimStartMs.coerceAtLeast(0L))
          .setEndPositionMs(trimEndMs)
          .build()
      )
    }

    val videoEffects = mutableListOf<Effect>()
    if (!config.codec.isAudioOnly) {
      val targetHeight = config.resolution.heightPx
      if (targetHeight != null && (sourceHeight <= 0 || targetHeight < sourceHeight)) {
        videoEffects.add(Presentation.createForHeight(targetHeight))
      }
      if (config.rotationDegrees != 0) {
        videoEffects.add(
          ScaleAndRotateTransformation.Builder()
            .setRotationDegrees(config.rotationDegrees.toFloat())
            .build()
        )
      }
      val targetFps = config.frameRate.fps
      if (targetFps != null) {
        videoEffects.add(FrameDropEffect.createDefaultFrameDropEffect(targetFps))
      }
    }

    val editedItemBuilder =
      EditedMediaItem.Builder(mediaItemBuilder.build()).apply {
        if (config.codec.isAudioOnly) {
          setRemoveVideo(true)
        } else {
          if (config.muteAudio) {
            setRemoveAudio(true)
          }
          if (videoEffects.isNotEmpty()) {
            setEffects(Effects(emptyList(), videoEffects))
          }
        }
      }

    val transformerBuilder = Transformer.Builder(context).setAudioMimeType(MimeTypes.AUDIO_AAC)

    when (config.codec) {
      OutputCodecPreset.H264_MP4 -> transformerBuilder.setVideoMimeType(MimeTypes.VIDEO_H264)
      OutputCodecPreset.HEVC_MP4 -> transformerBuilder.setVideoMimeType(MimeTypes.VIDEO_H265)
      OutputCodecPreset.AUDIO_M4A -> Unit
    }

    runSingleItemTransformation(
      transformerBuilder = transformerBuilder,
      editedMediaItem = editedItemBuilder.build(),
      outputFile = outputFile,
      onProgress = onProgress,
      onSuccess = onSuccess,
      onError = onError,
    )
  }

  private fun runSingleItemTransformation(
    transformerBuilder: Transformer.Builder,
    editedMediaItem: EditedMediaItem,
    outputFile: File,
    onProgress: (Int) -> Unit,
    onSuccess: (File, ExportResult) -> Unit,
    onError: (String) -> Unit,
  ) {
    executeTransformer(
      transformerBuilder = transformerBuilder,
      outputFile = outputFile,
      onProgress = onProgress,
      onSuccess = onSuccess,
      onError = onError,
      starter = { transformer, path -> transformer.start(editedMediaItem, path) },
    )
  }

  private fun runCompositionTransformation(
    transformerBuilder: Transformer.Builder,
    composition: Composition,
    outputFile: File,
    onProgress: (Int) -> Unit,
    onSuccess: (File, ExportResult) -> Unit,
    onError: (String) -> Unit,
  ) {
    executeTransformer(
      transformerBuilder = transformerBuilder,
      outputFile = outputFile,
      onProgress = onProgress,
      onSuccess = onSuccess,
      onError = onError,
      starter = { transformer, path -> transformer.start(composition, path) },
    )
  }

  private fun executeTransformer(
    transformerBuilder: Transformer.Builder,
    outputFile: File,
    onProgress: (Int) -> Unit,
    onSuccess: (File, ExportResult) -> Unit,
    onError: (String) -> Unit,
    starter: (Transformer, String) -> Unit,
  ) {
    mainHandler.post {
      cancelInternal(deleteFile = false)
      activeOutputFile = outputFile
      outputFile.parentFile?.mkdirs()
      if (outputFile.exists()) outputFile.delete()

      val transformer =
        transformerBuilder
          .addListener(
            object : Transformer.Listener {
              override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                stopProgressPolling()
                activeTransformer = null
                activeOutputFile = null
                onProgress(100)
                onSuccess(outputFile, exportResult)
              }

              override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
              ) {
                stopProgressPolling()
                activeTransformer = null
                activeOutputFile = null
                runCatching { if (outputFile.exists()) outputFile.delete() }
                onError(
                  exportException.localizedMessage
                    ?: "Export failed (code ${exportException.errorCode})."
                )
              }
            }
          )
          .build()

      activeTransformer = transformer
      try {
        starter(transformer, outputFile.absolutePath)
        startProgressPolling(transformer, onProgress)
      } catch (e: Exception) {
        stopProgressPolling()
        activeTransformer = null
        activeOutputFile = null
        runCatching { if (outputFile.exists()) outputFile.delete() }
        onError(e.localizedMessage ?: "Unable to start media transformation.")
      }
    }
  }

  private fun startProgressPolling(transformer: Transformer, onProgress: (Int) -> Unit) {
    stopProgressPolling()
    val progressHolder = ProgressHolder()
    val runnable =
      object : Runnable {
        override fun run() {
          if (activeTransformer !== transformer) return
          val state = transformer.getProgress(progressHolder)
          if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
            onProgress(progressHolder.progress.coerceIn(0, 99))
          }
          if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
            mainHandler.postDelayed(this, 220L)
          }
        }
      }
    progressRunnable = runnable
    mainHandler.postDelayed(runnable, 150L)
  }

  private fun stopProgressPolling() {
    progressRunnable?.let { mainHandler.removeCallbacks(it) }
    progressRunnable = null
  }

  fun cancel() {
    mainHandler.post { cancelInternal(deleteFile = true) }
  }

  private fun cancelInternal(deleteFile: Boolean) {
    stopProgressPolling()
    runCatching { activeTransformer?.cancel() }
    activeTransformer = null
    if (deleteFile) {
      activeOutputFile?.let { file -> runCatching { if (file.exists()) file.delete() } }
    }
    activeOutputFile = null
  }
}
