package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoMetadataRepository(private val context: Context) {

  val exportsDirectory: File
    get() = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }

  suspend fun extractMetadata(uri: Uri, sourceTag: String = "Picker"): VideoMetadata =
    withContext(Dispatchers.IO) {
      var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "video_clip"
      var fileSizeBytes = 0L
      var deviceTimestampMs = 0L

      if (uri.scheme == "file" && uri.path != null) {
        val file = File(uri.path!!)
        displayName = file.name
        fileSizeBytes = file.length()
        deviceTimestampMs = file.lastModified()
      } else {
        runCatching {
          context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
              val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
              if (nameIdx >= 0) {
                cursor.getString(nameIdx)?.let { displayName = it }
              }
              if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                fileSizeBytes = cursor.getLong(sizeIdx)
              }

              val lastModIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
              val dateModifiedIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
              val dateAddedIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
              val dateTakenIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)

              deviceTimestampMs =
                when {
                  lastModIdx >= 0 && !cursor.isNull(lastModIdx) -> cursor.getLong(lastModIdx)
                  dateModifiedIdx >= 0 && !cursor.isNull(dateModifiedIdx) ->
                    cursor.getLong(dateModifiedIdx) * 1000L
                  dateAddedIdx >= 0 && !cursor.isNull(dateAddedIdx) ->
                    cursor.getLong(dateAddedIdx) * 1000L
                  dateTakenIdx >= 0 && !cursor.isNull(dateTakenIdx) -> cursor.getLong(dateTakenIdx)
                  else -> 0L
                }
            }
          }
        }
      }

      val mimeType =
        context.contentResolver.getType(uri)
          ?: if (displayName.endsWith(".m4a", ignoreCase = true)) "audio/mp4" else "video/mp4"

      var durationMs = 0L
      var width = 0
      var height = 0
      var bitrate = 0L
      var rotation = 0
      var hasVideo = true

      val retriever = MediaMetadataRetriever()
      try {
        if (uri.scheme == "file" && uri.path != null) {
          retriever.setDataSource(uri.path)
        } else {
          retriever.setDataSource(context, uri)
        }
        durationMs =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?: 0L
        width =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            ?: 0
        height =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            ?: 0
        bitrate =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            ?: 0L
        rotation =
          retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull() ?: 0
        val hasVideoFlag = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
        hasVideo = hasVideoFlag == "yes" || (width > 0 && height > 0)

        if (deviceTimestampMs <= 0L) {
          val dateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
          deviceTimestampMs = parseMetadataDate(dateStr) ?: 0L
        }
      } catch (_: Exception) {
        // Fallback if metadata retriever fails on an unusual stream
      } finally {
        runCatching { retriever.release() }
      }

      if (deviceTimestampMs <= 0L) {
        deviceTimestampMs = System.currentTimeMillis()
      }

      VideoMetadata(
        uriString = uri.toString(),
        displayName = displayName,
        mimeType = mimeType,
        durationMs = durationMs.coerceAtLeast(0L),
        width = width,
        height = height,
        fileSizeBytes = fileSizeBytes,
        bitrateBps = bitrate,
        rotationDegrees = rotation,
        isAudioOnly = !hasVideo || mimeType.startsWith("audio/"),
        sourceTag = sourceTag,
        deviceTimestampMs = deviceTimestampMs,
      )
    }

  private fun parseMetadataDate(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    val patterns = listOf("yyyyMMdd'T'HHmmss.SSS'Z'", "yyyyMMdd'T'HHmmss'Z'", "yyyy-MM-dd HH:mm:ss")
    for (p in patterns) {
      runCatching {
        return SimpleDateFormat(p, Locale.US).parse(raw)?.time
      }
    }
    return null
  }

  /**
   * Automatically scans videos accessible under Google Play's zero-permission scoped model:
   * 1) Scoped MediaStore Video & Downloads entries accessible without READ_MEDIA_VIDEO
   * 2) User-linked SAF folders (via OpenDocumentTree persistent permissions)
   * Ordered by device download/received/modified date descending.
   */
  suspend fun discoverAccessibleVideos(linkedFolders: List<LinkedFolderEntity>): List<LibraryVideoEntity> =
    withContext(Dispatchers.IO) {
      val discovered = mutableListOf<LibraryVideoEntity>()
      val seenUris = HashSet<String>()

      // 1. Scan scoped MediaStore videos visible to this app without broad permissions
      runCatching {
        val projection =
          arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATE_ADDED,
          )
        context.contentResolver
          .query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC, ${MediaStore.Video.Media.DATE_ADDED} DESC",
          )
          ?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val wCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val hCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext() && discovered.size < 120) {
              val id = cursor.getLong(idCol)
              val contentUri =
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
              val uriStr = contentUri.toString()
              val modSec = cursor.getLong(modCol)
              val addSec = cursor.getLong(addedCol)
              val tsMs = maxOf(modSec, addSec) * 1000L

              if (seenUris.add(uriStr)) {
                discovered.add(
                  LibraryVideoEntity(
                    uriString = uriStr,
                    displayName = cursor.getString(nameCol) ?: "Video_$id.mp4",
                    mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                    durationMs = cursor.getLong(durCol).coerceAtLeast(0L),
                    width = cursor.getInt(wCol),
                    height = cursor.getInt(hCol),
                    fileSizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                    bitrateBps = 0L,
                    rotationDegrees = 0,
                    sourceTag = "Device",
                    deviceTimestampMs = if (tsMs > 0L) tsMs else System.currentTimeMillis(),
                  )
                )
              }
            }
          }
      }

      // 2. Scan scoped Downloads table on Android 10+ (app-accessible downloaded videos)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
          val dlProjection =
            arrayOf(
              MediaStore.Downloads._ID,
              MediaStore.Downloads.DISPLAY_NAME,
              MediaStore.Downloads.MIME_TYPE,
              MediaStore.Downloads.SIZE,
              MediaStore.Downloads.DATE_MODIFIED,
              MediaStore.Downloads.DATE_ADDED,
            )
          context.contentResolver
            .query(
              MediaStore.Downloads.EXTERNAL_CONTENT_URI,
              dlProjection,
              "${MediaStore.Downloads.MIME_TYPE} LIKE ?",
              arrayOf("video/%"),
              "${MediaStore.Downloads.DATE_MODIFIED} DESC, ${MediaStore.Downloads.DATE_ADDED} DESC",
            )
            ?.use { cursor ->
              val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
              val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
              val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
              val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
              val modCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
              val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)

              while (cursor.moveToNext() && discovered.size < 140) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                val uriStr = uri.toString()
                if (seenUris.add(uriStr)) {
                  val probed = probeQuickDurationAndDimensions(uri)
                  val tsMs = maxOf(cursor.getLong(modCol), cursor.getLong(addedCol)) * 1000L
                  discovered.add(
                    LibraryVideoEntity(
                      uriString = uriStr,
                      displayName = cursor.getString(nameCol) ?: "Download_$id.mp4",
                      mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                      durationMs = probed.first,
                      width = probed.second,
                      height = probed.third,
                      fileSizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L),
                      bitrateBps = 0L,
                      rotationDegrees = 0,
                      sourceTag = "Downloads",
                      deviceTimestampMs = if (tsMs > 0L) tsMs else System.currentTimeMillis(),
                    )
                  )
                }
              }
            }
        }
      }

      // 3. Scan all user-linked SAF folders (via OpenDocumentTree)
      for (folder in linkedFolders) {
        val treeUri = Uri.parse(folder.treeUriString)
        runCatching {
          val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
          scanSafFolderRecursive(
            treeUri = treeUri,
            parentDocumentId = rootDocId,
            depth = 0,
            maxDepth = 2,
            sourceTag = folder.folderName,
            seenUris = seenUris,
            out = discovered,
          )
        }
      }

      discovered.sortedByDescending { it.deviceTimestampMs }
    }

  private fun scanSafFolderRecursive(
    treeUri: Uri,
    parentDocumentId: String,
    depth: Int,
    maxDepth: Int,
    sourceTag: String,
    seenUris: MutableSet<String>,
    out: MutableList<LibraryVideoEntity>,
  ) {
    if (out.size >= 160) return
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
    val projection =
      arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
      )
    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
      val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
      val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
      val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
      val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
      val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

      while (cursor.moveToNext() && out.size < 160) {
        if (idIdx < 0 || mimeIdx < 0) continue
        val docId = cursor.getString(idIdx) ?: continue
        val mime = cursor.getString(mimeIdx) ?: ""
        val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "video.mp4" else "video.mp4"
        val size = if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
        val lastMod =
          if (modIdx >= 0 && !cursor.isNull(modIdx)) cursor.getLong(modIdx)
          else System.currentTimeMillis()

        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
          if (depth < maxDepth) {
            scanSafFolderRecursive(
              treeUri = treeUri,
              parentDocumentId = docId,
              depth = depth + 1,
              maxDepth = maxDepth,
              sourceTag = sourceTag,
              seenUris = seenUris,
              out = out,
            )
          }
        } else if (mime.startsWith("video/") || isVideoExtension(name)) {
          val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
          val uriStr = docUri.toString()
          if (seenUris.add(uriStr)) {
            val probed = probeQuickDurationAndDimensions(docUri)
            out.add(
              LibraryVideoEntity(
                uriString = uriStr,
                displayName = name,
                mimeType = if (mime.startsWith("video/")) mime else "video/mp4",
                durationMs = probed.first,
                width = probed.second,
                height = probed.third,
                fileSizeBytes = size,
                bitrateBps = 0L,
                rotationDegrees = 0,
                sourceTag = sourceTag,
                deviceTimestampMs = if (lastMod > 0L) lastMod else System.currentTimeMillis(),
              )
            )
          }
        }
      }
    }
  }

  private fun probeQuickDurationAndDimensions(uri: Uri): Triple<Long, Int, Int> {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(context, uri)
      val dur =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
          ?: 0L
      val w =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
          ?: 0
      val h =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
          ?: 0
      Triple(dur, w, h)
    } catch (_: Exception) {
      Triple(0L, 0, 0)
    } finally {
      runCatching { retriever.release() }
    }
  }

  private fun isVideoExtension(name: String): Boolean {
    val lower = name.lowercase(Locale.US)
    return lower.endsWith(".mp4") ||
      lower.endsWith(".mkv") ||
      lower.endsWith(".mov") ||
      lower.endsWith(".webm") ||
      lower.endsWith(".3gp") ||
      lower.endsWith(".avi") ||
      lower.endsWith(".m4v")
  }

  fun extractTreeFolderName(treeUri: Uri): String {
    return runCatching {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        docId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Synced Folder" }
      }
      .getOrDefault("Synced Folder")
  }

  suspend fun isUriReadable(uri: Uri): Boolean =
    withContext(Dispatchers.IO) {
      if (uri.scheme == "file" && uri.path != null) {
        return@withContext File(uri.path!!).exists()
      }
      runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false }
        .getOrDefault(false)
    }

  suspend fun extractThumbnails(
    uri: Uri,
    durationMs: Long,
    count: Int = 8,
    targetHeightPx: Int = 140,
  ): List<Bitmap> =
    withContext(Dispatchers.IO) {
      if (durationMs <= 0L || count <= 0) return@withContext emptyList()
      val frames = mutableListOf<Bitmap>()
      val retriever = MediaMetadataRetriever()
      try {
        if (uri.scheme == "file" && uri.path != null) {
          retriever.setDataSource(uri.path)
        } else {
          retriever.setDataSource(context, uri)
        }
        val stepUs = (durationMs * 1000L) / count.coerceAtLeast(1)
        for (i in 0 until count) {
          val timeUs = (i * stepUs + stepUs / 2L).coerceIn(0L, durationMs * 1000L)
          val frame =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
              retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                targetHeightPx * 2,
                targetHeightPx,
              )
            } else {
              retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                raw ->
                val aspect = raw.width.toFloat() / raw.height.coerceAtLeast(1).toFloat()
                val targetW = (targetHeightPx * aspect).toInt().coerceAtLeast(48)
                Bitmap.createScaledBitmap(raw, targetW, targetHeightPx, true).also {
                  if (it != raw) raw.recycle()
                }
              }
            }
          if (frame != null) {
            frames.add(frame)
          }
        }
      } catch (_: Exception) {
        // Ignore frame extraction errors on unsupported streams
      } finally {
        runCatching { retriever.release() }
      }
      frames
    }

  suspend fun extractSingleThumbnail(filePath: String): Bitmap? =
    withContext(Dispatchers.IO) {
      val retriever = MediaMetadataRetriever()
      try {
        retriever.setDataSource(filePath)
        val durationMs =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?: 0L
        val timeUs = (durationMs * 1000L / 4L).coerceAtLeast(0L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
          retriever.getScaledFrameAtTime(
            timeUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            320,
            180,
          )
        } else {
          retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
      } catch (_: Exception) {
        null
      } finally {
        runCatching { retriever.release() }
      }
    }

  suspend fun extractUriThumbnail(uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
      val retriever = MediaMetadataRetriever()
      try {
        if (uri.scheme == "file" && uri.path != null) {
          retriever.setDataSource(uri.path)
        } else {
          retriever.setDataSource(context, uri)
        }
        val durationMs =
          retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?: 0L
        val timeUs = (durationMs * 1000L / 4L).coerceAtLeast(0L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
          retriever.getScaledFrameAtTime(
            timeUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            320,
            180,
          )
        } else {
          retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
      } catch (_: Exception) {
        null
      } finally {
        runCatching { retriever.release() }
      }
    }

  suspend fun saveToDeviceGallery(
    file: File,
    displayName: String,
    mimeType: String,
    isAudio: Boolean,
  ): Result<Uri> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!file.exists()) error("Exported file not found")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
          error("Saving directly to MediaStore requires Android 10+. Use Share instead.")
        }
        val collection =
          if (isAudio) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
          } else {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
          }
        val relativeDir =
          if (isAudio) {
            "${Environment.DIRECTORY_MUSIC}/VideosPlayerConverter"
          } else {
            "${Environment.DIRECTORY_MOVIES}/VideosPlayerConverter"
          }
        val values =
          ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
          }
        val resolver = context.contentResolver
        val itemUri =
          resolver.insert(collection, values) ?: error("Could not create MediaStore entry")
        resolver.openOutputStream(itemUri)?.use { out ->
          FileInputStream(file).use { input -> input.copyTo(out) }
        } ?: error("Could not open output stream")

        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(itemUri, values, null, null)
        itemUri
      }
    }

  fun createShareFileIntent(file: File, mimeType: String, title: String): Intent {
    val contentUri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    return Intent.createChooser(sendIntent, "Share $title")
  }

  fun createShareUriIntent(uri: Uri, mimeType: String, title: String): Intent {
    if (uri.scheme == "file" && uri.path != null) {
      return createShareFileIntent(File(uri.path!!), mimeType, title)
    }
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    return Intent.createChooser(sendIntent, "Share $title")
  }

  fun createUniqueOutputFile(baseName: String, suffixTag: String, extension: String): File {
    val cleanBase =
      baseName
        .substringBeforeLast('.')
        .replace(Regex("[^A-Za-z0-9_-]"), "_")
        .take(28)
        .ifBlank { "clip" }
    val timestamp = System.currentTimeMillis() % 1_000_000L
    return File(exportsDirectory, "${cleanBase}_${suffixTag}_$timestamp.$extension")
  }

  companion object {
    fun formatDuration(ms: Long, includeTenths: Boolean = false): String {
      val clamped = ms.coerceAtLeast(0L)
      val totalSeconds = clamped / 1000L
      val hours = totalSeconds / 3600L
      val minutes = (totalSeconds % 3600L) / 60L
      val seconds = totalSeconds % 60L
      val tenths = (clamped % 1000L) / 100L
      return when {
        hours > 0 ->
          String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        includeTenths ->
          String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
        else ->
          String.format(Locale.US, "%02d:%02d", minutes, seconds)
      }
    }

    fun formatFileSize(bytes: Long): String {
      if (bytes <= 0L) return "—"
      val kb = bytes / 1024.0
      val mb = kb / 1024.0
      val gb = mb / 1024.0
      return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb.coerceAtLeast(1.0))
      }
    }

    fun formatDateTime(epochMs: Long): String {
      if (epochMs <= 0L) return ""
      val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
      return fmt.format(Date(epochMs))
    }

    fun dateBucketLabel(epochMs: Long): String {
      if (epochMs <= 0L) return "Recent"
      val nowCal = Calendar.getInstance()
      val itemCal = Calendar.getInstance().apply { timeInMillis = epochMs }

      val sameYear = nowCal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
      val dayDiff = nowCal.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR)

      return when {
        sameYear && dayDiff == 0 -> "Today"
        sameYear && dayDiff == 1 -> "Yesterday"
        sameYear && dayDiff in 2..6 -> "This Week"
        else -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(epochMs))
      }
    }
  }
}
