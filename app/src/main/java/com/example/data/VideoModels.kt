package com.example.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WorkspaceTab {
  PLAY,
  TRIM,
  CUT,
  CONVERT,
}

enum class LibrarySortOrder(val label: String) {
  NEWEST_FIRST("Newest Date"),
  OLDEST_FIRST("Oldest Date"),
  LARGEST_FIRST("Largest"),
  NAME_ASC("Name A-Z"),
}

enum class CutMode(val label: String, val description: String) {
  REMOVE_MIDDLE(
    label = "Cut Out Middle",
    description = "Removes the selected middle segment and joins the start and end clips.",
  ),
  SPLIT_AT_POINT(
    label = "Split into 2 Clips",
    description = "Splits the video at the cut point into Part 1 and Part 2 MP4 files.",
  ),
}

enum class ExportOperation(val label: String) {
  TRIM("Trimmed"),
  CUT("Cut"),
  MERGE("Merged"),
  CONVERT("Converted"),
  TRIM_AND_CONVERT("Trim + Convert"),
  AUDIO_EXTRACT("Audio Extract"),
}

enum class OutputCodecPreset(
  val title: String,
  val subtitle: String,
  val extension: String,
  val mimeType: String,
  val isAudioOnly: Boolean = false,
) {
  H264_MP4(
    title = "MP4 • H.264",
    subtitle = "Universal compatibility",
    extension = "mp4",
    mimeType = "video/mp4",
  ),
  HEVC_MP4(
    title = "MP4 • H.265 (HEVC)",
    subtitle = "Smaller file, crisp quality",
    extension = "mp4",
    mimeType = "video/mp4",
  ),
  AUDIO_M4A(
    title = "M4A • AAC Audio",
    subtitle = "Extract audio track only",
    extension = "m4a",
    mimeType = "audio/mp4",
    isAudioOnly = true,
  ),
}

enum class ResolutionOption(val label: String, val heightPx: Int?) {
  ORIGINAL("Original", null),
  P1080("1080p FHD", 1080),
  P720("720p HD", 720),
  P480("480p SD", 480),
  P360("360p Web", 360),
}

enum class FrameRateOption(val label: String, val fps: Float?) {
  ORIGINAL("Original", null),
  FPS_60("60 fps", 60f),
  FPS_30("30 fps", 30f),
  FPS_24("24 fps", 24f),
}

enum class ConversionPreset(
  val label: String,
  val description: String,
  val codec: OutputCodecPreset,
  val resolution: ResolutionOption,
  val frameRate: FrameRateOption,
) {
  SOCIAL_SHARE(
    label = "Social 720p",
    description = "MP4 H.264 • 720p • 30fps",
    codec = OutputCodecPreset.H264_MP4,
    resolution = ResolutionOption.P720,
    frameRate = FrameRateOption.FPS_30,
  ),
  COMPACT_HEVC(
    label = "Compact HEVC",
    description = "MP4 H.265 • 1080p",
    codec = OutputCodecPreset.HEVC_MP4,
    resolution = ResolutionOption.P1080,
    frameRate = FrameRateOption.ORIGINAL,
  ),
  DATA_SAVER(
    label = "Data Saver",
    description = "MP4 H.264 • 480p • 24fps",
    codec = OutputCodecPreset.H264_MP4,
    resolution = ResolutionOption.P480,
    frameRate = FrameRateOption.FPS_24,
  ),
  AUDIO_ONLY(
    label = "Audio M4A",
    description = "AAC Audio Extraction",
    codec = OutputCodecPreset.AUDIO_M4A,
    resolution = ResolutionOption.ORIGINAL,
    frameRate = FrameRateOption.ORIGINAL,
  ),
}

data class ConversionConfig(
  val codec: OutputCodecPreset = OutputCodecPreset.H264_MP4,
  val resolution: ResolutionOption = ResolutionOption.ORIGINAL,
  val frameRate: FrameRateOption = FrameRateOption.ORIGINAL,
  val rotationDegrees: Int = 0,
  val muteAudio: Boolean = false,
  val includeTrimRange: Boolean = false,
  val fastTrimOptimization: Boolean = true,
)

data class MergeConfig(
  val codec: OutputCodecPreset = OutputCodecPreset.H264_MP4,
  val resolution: ResolutionOption = ResolutionOption.P720,
)

data class VideoMetadata(
  val uriString: String,
  val displayName: String,
  val mimeType: String,
  val durationMs: Long,
  val width: Int,
  val height: Int,
  val fileSizeBytes: Long,
  val bitrateBps: Long,
  val rotationDegrees: Int,
  val isAudioOnly: Boolean = false,
  val sourceTag: String = "Picker",
  val deviceTimestampMs: Long = System.currentTimeMillis(),
) {
  val uri: Uri
    get() = Uri.parse(uriString)

  val resolutionLabel: String
    get() =
      when {
        isAudioOnly -> "Audio"
        width > 0 && height > 0 -> "${width}×${height}"
        else -> "Video"
      }

  val aspectRatio: Float
    get() {
      if (width <= 0 || height <= 0) return 16f / 9f
      val effectiveW = if (rotationDegrees == 90 || rotationDegrees == 270) height else width
      val effectiveH = if (rotationDegrees == 90 || rotationDegrees == 270) width else height
      return (effectiveW.toFloat() / effectiveH.toFloat()).coerceIn(0.4f, 2.5f)
    }
}

@Entity(tableName = "library_videos")
data class LibraryVideoEntity(
  @PrimaryKey val uriString: String,
  val displayName: String,
  val mimeType: String,
  val durationMs: Long,
  val width: Int,
  val height: Int,
  val fileSizeBytes: Long,
  val bitrateBps: Long,
  val rotationDegrees: Int,
  val sourceTag: String,
  val deviceTimestampMs: Long,
) {
  fun toVideoMetadata(): VideoMetadata =
    VideoMetadata(
      uriString = uriString,
      displayName = displayName,
      mimeType = mimeType,
      durationMs = durationMs,
      width = width,
      height = height,
      fileSizeBytes = fileSizeBytes,
      bitrateBps = bitrateBps,
      rotationDegrees = rotationDegrees,
      isAudioOnly = mimeType.startsWith("audio/"),
      sourceTag = sourceTag,
      deviceTimestampMs = deviceTimestampMs,
    )
}

@Entity(tableName = "linked_folders")
data class LinkedFolderEntity(
  @PrimaryKey val treeUriString: String,
  val folderName: String,
  val linkedAtEpochMs: Long,
)

@Entity(tableName = "export_records")
data class ExportRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val filePath: String,
  val displayName: String,
  val mimeType: String,
  val operationType: String,
  val codecLabel: String,
  val resolutionLabel: String,
  val durationMs: Long,
  val fileSizeBytes: Long,
  val createdAtEpochMs: Long,
  val sourceDisplayName: String,
)

sealed interface ExportJobState {
  data object Idle : ExportJobState

  data class Running(
    val progressPercent: Int,
    val statusLabel: String,
    val operation: ExportOperation,
  ) : ExportJobState

  data class Success(
    val record: ExportRecordEntity,
    val savedToGalleryUri: String? = null,
    val secondaryRecord: ExportRecordEntity? = null,
  ) : ExportJobState

  data class Error(val message: String) : ExportJobState
}

enum class ExportsFilter(val label: String) {
  ALL("All"),
  TRIMMED("Trim & Cut"),
  MERGED("Merged"),
  CONVERTED("Converted"),
  AUDIO("Audio"),
}
