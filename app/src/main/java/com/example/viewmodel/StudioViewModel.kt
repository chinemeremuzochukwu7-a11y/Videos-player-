package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.ClipCraftDatabase
import com.example.data.ConversionConfig
import com.example.data.ConversionPreset
import com.example.data.CutMode
import com.example.data.ExportJobState
import com.example.data.ExportOperation
import com.example.data.ExportRecordEntity
import com.example.data.ExportsFilter
import com.example.data.LibrarySortOrder
import com.example.data.LibraryVideoEntity
import com.example.data.LinkedFolderEntity
import com.example.data.MergeConfig
import com.example.data.VideoMetadata
import com.example.data.VideoMetadataRepository
import com.example.data.WorkspaceTab
import com.example.media.VideoTransformManager
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Immutable
data class PlayerUiState(
  val isPlaying: Boolean = false,
  val positionMs: Long = 0L,
  val durationMs: Long = 0L,
  val playbackSpeed: Float = 1.0f,
  val isMuted: Boolean = false,
  val isLooping: Boolean = false,
  val isBuffering: Boolean = false,
  val isCropToFill: Boolean = false,
  val errorMessage: String? = null,
)

@Immutable
data class StudioUiState(
  val activeVideo: VideoMetadata? = null,
  val workspaceTab: WorkspaceTab = WorkspaceTab.PLAY,
  val timelineThumbnails: List<Bitmap> = emptyList(),
  val isLoadingThumbnails: Boolean = false,
  val isScanningLibrary: Boolean = false,
  val trimStartMs: Long = 0L,
  val trimEndMs: Long = 0L,
  val fastTrimEnabled: Boolean = true,
  val isPreviewingTrim: Boolean = false,
  val cutMode: CutMode = CutMode.REMOVE_MIDDLE,
  val splitPointMs: Long = 0L,
  val conversionConfig: ConversionConfig = ConversionConfig(),
  val mergeQueue: List<VideoMetadata> = emptyList(),
  val mergeConfig: MergeConfig = MergeConfig(),
  val exportJobState: ExportJobState = ExportJobState.Idle,
  val exportsFilter: ExportsFilter = ExportsFilter.ALL,
  val librarySortOrder: LibrarySortOrder = LibrarySortOrder.NEWEST_FIRST,
  val shouldAutoPromptPicker: Boolean = false,
  val isDarkTheme: Boolean = true,
  val snackbarMessage: String? = null,
)

@OptIn(UnstableApi::class)
class StudioViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs = application.getSharedPreferences("clipcraft_prefs", Context.MODE_PRIVATE)
  private val repository = VideoMetadataRepository(application)
  private val db = ClipCraftDatabase.getInstance(application)
  private val exportDao = db.exportRecordDao()
  private val libraryDao = db.libraryVideoDao()
  private val folderDao = db.linkedFolderDao()
  private val transformManager = VideoTransformManager(application)

  val exoPlayer: ExoPlayer =
    ExoPlayer.Builder(application)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
          .build(),
        true,
      )
      .setHandleAudioBecomingNoisy(true)
      .build()

  private val _playerState = MutableStateFlow(PlayerUiState())
  val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

  private val _uiState = MutableStateFlow(StudioUiState())
  val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

  private val _exportThumbnails = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())
  val exportThumbnails: StateFlow<Map<Long, Bitmap>> = _exportThumbnails.asStateFlow()

  private val _libraryThumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
  val libraryThumbnails: StateFlow<Map<String, Bitmap>> = _libraryThumbnails.asStateFlow()

  val libraryVideos: StateFlow<List<VideoMetadata>> =
    combine(
        libraryDao.observeAll(),
        _uiState.map { it.librarySortOrder }.distinctUntilChanged(),
      ) { entities, sortOrder ->
        val mapped = entities.map { it.toVideoMetadata() }
        when (sortOrder) {
          LibrarySortOrder.NEWEST_FIRST -> mapped.sortedByDescending { it.deviceTimestampMs }
          LibrarySortOrder.OLDEST_FIRST -> mapped.sortedBy { it.deviceTimestampMs }
          LibrarySortOrder.LARGEST_FIRST -> mapped.sortedByDescending { it.fileSizeBytes }
          LibrarySortOrder.NAME_ASC -> mapped.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val linkedFolders: StateFlow<List<LinkedFolderEntity>> =
    folderDao
      .observeAll()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val allExports: StateFlow<List<ExportRecordEntity>> =
    exportDao
      .observeAll()
      .map { list -> list.filter { File(it.filePath).exists() } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val filteredExports: StateFlow<List<ExportRecordEntity>> =
    combine(allExports, _uiState.map { it.exportsFilter }) { exports, filter ->
        when (filter) {
          ExportsFilter.ALL -> exports
          ExportsFilter.TRIMMED ->
            exports.filter {
              it.operationType == ExportOperation.TRIM.name ||
                it.operationType == ExportOperation.CUT.name ||
                it.operationType == ExportOperation.TRIM_AND_CONVERT.name
            }
          ExportsFilter.MERGED -> exports.filter { it.operationType == ExportOperation.MERGE.name }
          ExportsFilter.CONVERTED ->
            exports.filter {
              it.operationType == ExportOperation.CONVERT.name ||
                it.operationType == ExportOperation.TRIM_AND_CONVERT.name
            }
          ExportsFilter.AUDIO -> exports.filter { it.operationType == ExportOperation.AUDIO_EXTRACT.name }
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  private val playerListener =
    object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _playerState.update { it.copy(isPlaying = isPlaying) }
      }

      override fun onPlaybackStateChanged(playbackState: Int) {
        val duration = exoPlayer.duration.takeIf { it > 0L } ?: _playerState.value.durationMs
        _playerState.update {
          it.copy(
            isBuffering = playbackState == Player.STATE_BUFFERING,
            durationMs = duration,
          )
        }
        if (playbackState == Player.STATE_READY && duration > 0L) {
          syncDurationFromPlayerIfNeeded(duration)
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        _playerState.update {
          it.copy(
            isPlaying = false,
            isBuffering = false,
            errorMessage = error.localizedMessage ?: "Unable to play this media file.",
          )
        }
      }
    }

  init {
    exoPlayer.addListener(playerListener)
    startPositionPolling()
    observeExportThumbnails()
    observeLibraryThumbnails()
    refreshAutoLibrary(silent = true)
  }

  private fun observeExportThumbnails() {
    viewModelScope.launch {
      allExports.collect { records ->
        for (record in records) {
          if (record.mimeType.startsWith("audio/")) continue
          if (_exportThumbnails.value.containsKey(record.id)) continue
          val bmp = repository.extractSingleThumbnail(record.filePath)
          if (bmp != null) {
            _exportThumbnails.update { current -> current + (record.id to bmp) }
          }
        }
      }
    }
  }

  private fun observeLibraryThumbnails() {
    viewModelScope.launch {
      libraryVideos.collect { videos ->
        for (video in videos.take(100)) {
          if (video.isAudioOnly) continue
          if (_libraryThumbnails.value.containsKey(video.uriString)) continue
          val bmp = repository.extractUriThumbnail(video.uri)
          if (bmp != null) {
            _libraryThumbnails.update { current -> current + (video.uriString to bmp) }
          }
        }
      }
    }
  }

  fun setLibrarySortOrder(order: LibrarySortOrder) {
    _uiState.update { it.copy(librarySortOrder = order) }
  }

  fun markInitialPickerPrompted() {
    prefs.edit().putBoolean(PREF_PROMPTED_INITIAL_PICKER, true).apply()
    _uiState.update { it.copy(shouldAutoPromptPicker = false) }
  }

  fun refreshAutoLibrary(silent: Boolean = false) {
    viewModelScope.launch {
      _uiState.update { it.copy(isScanningLibrary = true) }
      // Prune stale URIs that were removed or revoked on the device
      val existingRows = libraryDao.getAll()
      for (row in existingRows) {
        if (!repository.isUriReadable(Uri.parse(row.uriString))) {
          libraryDao.deleteByUri(row.uriString)
          _libraryThumbnails.update { it - row.uriString }
        }
      }

      val folders = folderDao.getAll()
      val discovered = repository.discoverAccessibleVideos(folders)
      if (discovered.isNotEmpty()) {
        libraryDao.upsertAll(discovered)
        if (_uiState.value.activeVideo == null) {
          loadVideoUri(Uri.parse(discovered.first().uriString), WorkspaceTab.PLAY, autoPlay = false)
        }
      }

      val currentLibrary = libraryDao.getAll()
      val shouldPrompt =
        currentLibrary.isEmpty() && !prefs.getBoolean(PREF_PROMPTED_INITIAL_PICKER, false)
      _uiState.update {
        it.copy(
          isScanningLibrary = false,
          shouldAutoPromptPicker = shouldPrompt,
        )
      }
      if (!silent) {
        showSnackbar(
          if (discovered.isNotEmpty()) "Synced ${discovered.size} videos"
          else "No new videos found in linked folders"
        )
      }
    }
  }

  fun importPickedVideos(
    uris: List<Uri>,
    targetTab: WorkspaceTab = WorkspaceTab.PLAY,
    autoPlay: Boolean = true,
  ) {
    if (uris.isEmpty()) return
    prefs.edit().putBoolean(PREF_PROMPTED_INITIAL_PICKER, true).apply()
    viewModelScope.launch {
      val contentResolver = getApplication<Application>().contentResolver
      val entities = mutableListOf<LibraryVideoEntity>()
      for (uri in uris) {
        runCatching {
          contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val meta = repository.extractMetadata(uri, sourceTag = "Photo Picker")
        entities.add(
          LibraryVideoEntity(
            uriString = meta.uriString,
            displayName = meta.displayName,
            mimeType = meta.mimeType,
            durationMs = meta.durationMs,
            width = meta.width,
            height = meta.height,
            fileSizeBytes = meta.fileSizeBytes,
            bitrateBps = meta.bitrateBps,
            rotationDegrees = meta.rotationDegrees,
            sourceTag = "Photo Picker",
            deviceTimestampMs = meta.deviceTimestampMs,
          )
        )
      }
      if (entities.isNotEmpty()) {
        libraryDao.upsertAll(entities)
        val newest = entities.maxByOrNull { it.deviceTimestampMs } ?: entities.first()
        loadVideoUri(Uri.parse(newest.uriString), targetTab, autoPlay = autoPlay)
        _uiState.update { it.copy(shouldAutoPromptPicker = false) }
        if (entities.size > 1) {
          showSnackbar("Added ${entities.size} videos sorted by date")
        }
      }
    }
  }

  fun linkVideoFolder(treeUri: Uri) {
    prefs.edit().putBoolean(PREF_PROMPTED_INITIAL_PICKER, true).apply()
    viewModelScope.launch {
      val contentResolver = getApplication<Application>().contentResolver
      runCatching {
        contentResolver.takePersistableUriPermission(
          treeUri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
      }
      val folderName = repository.extractTreeFolderName(treeUri)
      folderDao.upsert(
        LinkedFolderEntity(
          treeUriString = treeUri.toString(),
          folderName = folderName,
          linkedAtEpochMs = System.currentTimeMillis(),
        )
      )
      refreshAutoLibrary(silent = false)
    }
  }

  fun unlinkVideoFolder(folder: LinkedFolderEntity) {
    viewModelScope.launch {
      folderDao.deleteByUri(folder.treeUriString)
      showSnackbar("Unlinked folder ${folder.folderName}")
    }
  }

  fun removeLibraryVideo(video: VideoMetadata) {
    viewModelScope.launch {
      libraryDao.deleteByUri(video.uriString)
      _libraryThumbnails.update { it - video.uriString }
      showSnackbar("Removed ${video.displayName} from library")
    }
  }

  private fun startPositionPolling() {
    viewModelScope.launch {
      while (isActive) {
        if (exoPlayer.playbackState != Player.STATE_IDLE) {
          val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
          val dur = exoPlayer.duration.takeIf { it > 0L } ?: _playerState.value.durationMs
          _playerState.update { it.copy(positionMs = pos, durationMs = dur) }

          val state = _uiState.value
          if (
            state.isPreviewingTrim &&
              exoPlayer.isPlaying &&
              state.trimEndMs > state.trimStartMs &&
              pos >= state.trimEndMs
          ) {
            exoPlayer.seekTo(state.trimStartMs)
          }
        }
        delay(80L)
      }
    }
  }

  private fun syncDurationFromPlayerIfNeeded(playerDurationMs: Long) {
    val active = _uiState.value.activeVideo ?: return
    if (active.durationMs <= 0L && playerDurationMs > 0L) {
      val updated = active.copy(durationMs = playerDurationMs)
      _uiState.update { state ->
        state.copy(
          activeVideo = updated,
          trimStartMs = 0L,
          trimEndMs = playerDurationMs,
          splitPointMs = playerDurationMs / 2L,
        )
      }
      loadTimelineThumbnails(updated.uri, playerDurationMs)
    }
  }

  fun loadVideoUri(
    uri: Uri,
    targetTab: WorkspaceTab = WorkspaceTab.PLAY,
    autoPlay: Boolean = true,
  ) {
    viewModelScope.launch {
      _playerState.update { it.copy(errorMessage = null, positionMs = 0L) }
      val meta = repository.extractMetadata(uri)
      val effectiveDuration = meta.durationMs.coerceAtLeast(0L)

      _uiState.update { state ->
        state.copy(
          activeVideo = meta,
          workspaceTab = targetTab,
          timelineThumbnails = emptyList(),
          isLoadingThumbnails = !meta.isAudioOnly && effectiveDuration > 0L,
          trimStartMs = 0L,
          trimEndMs = effectiveDuration,
          splitPointMs = effectiveDuration / 2L,
          isPreviewingTrim = false,
          conversionConfig =
            state.conversionConfig.copy(
              includeTrimRange = false,
              rotationDegrees = 0,
            ),
        )
      }

      libraryDao.upsertAll(
        listOf(
          LibraryVideoEntity(
            uriString = meta.uriString,
            displayName = meta.displayName,
            mimeType = meta.mimeType,
            durationMs = meta.durationMs,
            width = meta.width,
            height = meta.height,
            fileSizeBytes = meta.fileSizeBytes,
            bitrateBps = meta.bitrateBps,
            rotationDegrees = meta.rotationDegrees,
            sourceTag = meta.sourceTag,
            deviceTimestampMs = meta.deviceTimestampMs,
          )
        )
      )

      exoPlayer.setMediaItem(MediaItem.fromUri(uri))
      exoPlayer.prepare()
      exoPlayer.playWhenReady = autoPlay
      _playerState.update { it.copy(durationMs = effectiveDuration, positionMs = 0L) }

      if (!meta.isAudioOnly && effectiveDuration > 0L) {
        loadTimelineThumbnails(uri, effectiveDuration)
      }
    }
  }

  fun openExportedRecord(record: ExportRecordEntity, targetTab: WorkspaceTab = WorkspaceTab.PLAY) {
    val file = File(record.filePath)
    if (!file.exists()) {
      showSnackbar("This exported file is no longer available.")
      return
    }
    loadVideoUri(Uri.fromFile(file), targetTab)
  }

  private fun loadTimelineThumbnails(uri: Uri, durationMs: Long) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingThumbnails = true) }
      val frames = repository.extractThumbnails(uri, durationMs, count = 10)
      if (_uiState.value.activeVideo?.uriString == uri.toString()) {
        _uiState.update { it.copy(timelineThumbnails = frames, isLoadingThumbnails = false) }
      }
    }
  }

  fun setWorkspaceTab(tab: WorkspaceTab) {
    _uiState.update { it.copy(workspaceTab = tab, isPreviewingTrim = false) }
  }

  fun togglePlayPause() {
    if (exoPlayer.isPlaying) {
      exoPlayer.pause()
    } else {
      val state = _uiState.value
      if (state.isPreviewingTrim) {
        if (
          exoPlayer.currentPosition < state.trimStartMs ||
            exoPlayer.currentPosition >= state.trimEndMs
        ) {
          exoPlayer.seekTo(state.trimStartMs)
        }
      } else if (exoPlayer.playbackState == Player.STATE_ENDED) {
        exoPlayer.seekTo(0L)
      }
      exoPlayer.play()
    }
  }

  fun seekTo(positionMs: Long) {
    val dur = _playerState.value.durationMs.coerceAtLeast(0L)
    val clamped = if (dur > 0L) positionMs.coerceIn(0L, dur) else positionMs.coerceAtLeast(0L)
    exoPlayer.seekTo(clamped)
    _playerState.update { it.copy(positionMs = clamped) }
  }

  fun seekBy(deltaMs: Long) {
    seekTo(exoPlayer.currentPosition + deltaMs)
  }

  fun setPlaybackSpeed(speed: Float) {
    exoPlayer.setPlaybackSpeed(speed)
    _playerState.update { it.copy(playbackSpeed = speed) }
  }

  fun toggleMute() {
    val nextMuted = !_playerState.value.isMuted
    exoPlayer.volume = if (nextMuted) 0f else 1f
    _playerState.update { it.copy(isMuted = nextMuted) }
  }

  fun toggleLoop() {
    val nextLoop = !_playerState.value.isLooping
    exoPlayer.repeatMode = if (nextLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    _playerState.update { it.copy(isLooping = nextLoop) }
  }

  fun toggleResizeMode() {
    _playerState.update { it.copy(isCropToFill = !it.isCropToFill) }
  }

  fun updateTrimRange(startMs: Long, endMs: Long, seekToChangedEdge: Long? = null) {
    val total =
      (_uiState.value.activeVideo?.durationMs ?: _playerState.value.durationMs).coerceAtLeast(500L)
    val minClipMs = 500L.coerceAtMost(total)
    val safeStart = startMs.coerceIn(0L, (total - minClipMs).coerceAtLeast(0L))
    val safeEnd = endMs.coerceIn((safeStart + minClipMs).coerceAtMost(total), total)
    _uiState.update {
      it.copy(trimStartMs = safeStart, trimEndMs = safeEnd, isPreviewingTrim = false)
    }
    if (seekToChangedEdge != null) {
      exoPlayer.pause()
      seekTo(seekToChangedEdge.coerceIn(safeStart, safeEnd))
    }
  }

  fun nudgeTrimStart(deltaMs: Long) {
    val state = _uiState.value
    val nextStart = state.trimStartMs + deltaMs
    updateTrimRange(nextStart, state.trimEndMs, seekToChangedEdge = nextStart)
  }

  fun nudgeTrimEnd(deltaMs: Long) {
    val state = _uiState.value
    val nextEnd = state.trimEndMs + deltaMs
    updateTrimRange(state.trimStartMs, nextEnd, seekToChangedEdge = nextEnd)
  }

  fun setFastTrimEnabled(enabled: Boolean) {
    _uiState.update { it.copy(fastTrimEnabled = enabled) }
  }

  fun togglePreviewTrim() {
    val state = _uiState.value
    if (state.isPreviewingTrim) {
      _uiState.update { it.copy(isPreviewingTrim = false) }
      exoPlayer.pause()
    } else {
      _uiState.update { it.copy(isPreviewingTrim = true) }
      exoPlayer.seekTo(state.trimStartMs)
      exoPlayer.play()
    }
  }

  fun setCutMode(mode: CutMode) {
    _uiState.update { it.copy(cutMode = mode, isPreviewingTrim = false) }
  }

  fun setSplitPointMs(ms: Long) {
    val dur = (_uiState.value.activeVideo?.durationMs ?: _playerState.value.durationMs).coerceAtLeast(1000L)
    val clamped = ms.coerceIn(300L, (dur - 300L).coerceAtLeast(300L))
    _uiState.update { it.copy(splitPointMs = clamped) }
    exoPlayer.pause()
    seekTo(clamped)
  }

  fun setSplitAtPlayhead() {
    setSplitPointMs(_playerState.value.positionMs)
  }

  fun updateConversionConfig(transform: (ConversionConfig) -> ConversionConfig) {
    _uiState.update { it.copy(conversionConfig = transform(it.conversionConfig)) }
  }

  fun applyConversionPreset(preset: ConversionPreset) {
    _uiState.update { state ->
      state.copy(
        conversionConfig =
          state.conversionConfig.copy(
            codec = preset.codec,
            resolution = preset.resolution,
            frameRate = preset.frameRate,
          )
      )
    }
  }

  // Video Merger Queue management
  fun addVideoToMergeQueue(video: VideoMetadata) {
    _uiState.update { state -> state.copy(mergeQueue = state.mergeQueue + video) }
  }

  fun addPickedUrisToMergeQueue(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      val contentResolver = getApplication<Application>().contentResolver
      val added = mutableListOf<VideoMetadata>()
      val entities = mutableListOf<LibraryVideoEntity>()
      for (uri in uris) {
        runCatching {
          contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val meta = repository.extractMetadata(uri, sourceTag = "Merger")
        added.add(meta)
        entities.add(
          LibraryVideoEntity(
            uriString = meta.uriString,
            displayName = meta.displayName,
            mimeType = meta.mimeType,
            durationMs = meta.durationMs,
            width = meta.width,
            height = meta.height,
            fileSizeBytes = meta.fileSizeBytes,
            bitrateBps = meta.bitrateBps,
            rotationDegrees = meta.rotationDegrees,
            sourceTag = "Photo Picker",
            deviceTimestampMs = meta.deviceTimestampMs,
          )
        )
      }
      if (entities.isNotEmpty()) libraryDao.upsertAll(entities)
      _uiState.update { state -> state.copy(mergeQueue = state.mergeQueue + added) }
    }
  }

  fun removeFromMergeQueue(index: Int) {
    _uiState.update { state ->
      val list = state.mergeQueue.toMutableList()
      if (index in list.indices) list.removeAt(index)
      state.copy(mergeQueue = list)
    }
  }

  fun moveMergeClip(fromIndex: Int, toIndex: Int) {
    _uiState.update { state ->
      val list = state.mergeQueue.toMutableList()
      if (fromIndex in list.indices && toIndex in list.indices) {
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
      }
      state.copy(mergeQueue = list)
    }
  }

  fun clearMergeQueue() {
    _uiState.update { it.copy(mergeQueue = emptyList()) }
  }

  fun updateMergeConfig(transform: (MergeConfig) -> MergeConfig) {
    _uiState.update { it.copy(mergeConfig = transform(it.mergeConfig)) }
  }

  fun startMergeExport() {
    val queue = _uiState.value.mergeQueue
    if (queue.size < 2) {
      showSnackbar("Add at least 2 videos to merge.")
      return
    }
    val config = _uiState.value.mergeConfig
    exoPlayer.pause()

    val firstBase = queue.first().displayName
    val outFile = repository.createUniqueOutputFile(firstBase, "merged_${queue.size}", "mp4")
    val totalDuration = queue.sumOf { it.durationMs }

    _uiState.update {
      it.copy(
        exportJobState =
          ExportJobState.Running(
            progressPercent = 0,
            statusLabel = "Merging ${queue.size} videos sequentially…",
            operation = ExportOperation.MERGE,
          )
      )
    }

    transformManager.startMerge(
      sourceUris = queue.map { it.uri },
      outputFile = outFile,
      config = config,
      onProgress = { pct ->
        _uiState.update { state ->
          val current = state.exportJobState
          if (current is ExportJobState.Running) {
            state.copy(exportJobState = current.copy(progressPercent = pct))
          } else state
        }
      },
      onSuccess = { file, result ->
        recordExportSuccess(
          file = file,
          operation = ExportOperation.MERGE,
          codecLabel = config.codec.title,
          resolutionLabel = config.resolution.label,
          durationMs = result.durationMs.takeIf { it > 0L } ?: totalDuration,
          mimeType = "video/mp4",
          sourceName = "${queue.size} merged clips",
        )
      },
      onError = { err ->
        _uiState.update { it.copy(exportJobState = ExportJobState.Error(err)) }
      },
    )
  }

  fun startCutExport() {
    val video = _uiState.value.activeVideo ?: return
    val state = _uiState.value
    val totalDuration = video.durationMs.coerceAtLeast(_playerState.value.durationMs)
    if (totalDuration < 800L) {
      showSnackbar("Video is too short to cut.")
      return
    }

    exoPlayer.pause()
    if (state.cutMode == CutMode.REMOVE_MIDDLE) {
      val cutStart = state.trimStartMs
      val cutEnd = state.trimEndMs
      val outFile = repository.createUniqueOutputFile(video.displayName, "cut", "mp4")
      val expectedDuration = (totalDuration - (cutEnd - cutStart)).coerceAtLeast(0L)

      _uiState.update {
        it.copy(
          exportJobState =
            ExportJobState.Running(
              progressPercent = 0,
              statusLabel = "Cutting out selected middle section…",
              operation = ExportOperation.CUT,
            )
        )
      }

      transformManager.startCutOutMiddle(
        sourceUri = video.uri,
        outputFile = outFile,
        cutStartMs = cutStart,
        cutEndMs = cutEnd,
        totalDurationMs = totalDuration,
        onProgress = { pct ->
          _uiState.update { cur ->
            val job = cur.exportJobState
            if (job is ExportJobState.Running) cur.copy(exportJobState = job.copy(progressPercent = pct))
            else cur
          }
        },
        onSuccess = { file, result ->
          recordExportSuccess(
            file = file,
            operation = ExportOperation.CUT,
            codecLabel = "H.264 MP4",
            resolutionLabel = video.resolutionLabel,
            durationMs = result.durationMs.takeIf { it > 0L } ?: expectedDuration,
            mimeType = "video/mp4",
            sourceName = video.displayName,
          )
        },
        onError = { err ->
          _uiState.update { it.copy(exportJobState = ExportJobState.Error(err)) }
        },
      )
    } else {
      // SPLIT_AT_POINT: Export Part 1 then Part 2 sequentially
      val splitMs = state.splitPointMs.coerceIn(300L, totalDuration - 300L)
      val part1File = repository.createUniqueOutputFile(video.displayName, "part1", "mp4")
      val part2File = repository.createUniqueOutputFile(video.displayName, "part2", "mp4")

      _uiState.update {
        it.copy(
          exportJobState =
            ExportJobState.Running(
              progressPercent = 0,
              statusLabel = "Exporting split Part 1 of 2…",
              operation = ExportOperation.CUT,
            )
        )
      }

      transformManager.startTrim(
        sourceUri = video.uri,
        outputFile = part1File,
        startMs = 0L,
        endMs = splitMs,
        fastTrimOptimization = state.fastTrimEnabled,
        onProgress = { pct ->
          _uiState.update { cur ->
            val job = cur.exportJobState
            if (job is ExportJobState.Running) {
              cur.copy(exportJobState = job.copy(progressPercent = (pct / 2).coerceIn(0, 49)))
            } else cur
          }
        },
        onSuccess = { firstFile, firstResult ->
          _uiState.update { cur ->
            val job = cur.exportJobState
            if (job is ExportJobState.Running) {
              cur.copy(
                exportJobState =
                  job.copy(progressPercent = 50, statusLabel = "Exporting split Part 2 of 2…")
              )
            } else cur
          }

          transformManager.startTrim(
            sourceUri = video.uri,
            outputFile = part2File,
            startMs = splitMs,
            endMs = totalDuration,
            fastTrimOptimization = state.fastTrimEnabled,
            onProgress = { pct2 ->
              _uiState.update { cur ->
                val job = cur.exportJobState
                if (job is ExportJobState.Running) {
                  cur.copy(exportJobState = job.copy(progressPercent = 50 + (pct2 / 2).coerceIn(0, 49)))
                } else cur
              }
            },
            onSuccess = { secondFile, secondResult ->
              viewModelScope.launch {
                val r1 = insertExportRecord(
                  file = firstFile,
                  operation = ExportOperation.CUT,
                  codecLabel = "H.264 MP4",
                  resolutionLabel = video.resolutionLabel,
                  durationMs = firstResult.durationMs.takeIf { it > 0L } ?: splitMs,
                  mimeType = "video/mp4",
                  sourceName = "${video.displayName} (Part 1)",
                )
                val r2 = insertExportRecord(
                  file = secondFile,
                  operation = ExportOperation.CUT,
                  codecLabel = "H.264 MP4",
                  resolutionLabel = video.resolutionLabel,
                  durationMs = secondResult.durationMs.takeIf { it > 0L } ?: (totalDuration - splitMs),
                  mimeType = "video/mp4",
                  sourceName = "${video.displayName} (Part 2)",
                )
                _uiState.update {
                  it.copy(
                    exportJobState =
                      ExportJobState.Success(record = r1, secondaryRecord = r2)
                  )
                }
              }
            },
            onError = { err ->
              _uiState.update { it.copy(exportJobState = ExportJobState.Error(err)) }
            },
          )
        },
        onError = { err ->
          _uiState.update { it.copy(exportJobState = ExportJobState.Error(err)) }
        },
      )
    }
  }

  fun startTrimExport() {
    val video = _uiState.value.activeVideo ?: return
    val startMs = _uiState.value.trimStartMs
    val endMs = _uiState.value.trimEndMs
    if (endMs <= startMs + 200L) {
      showSnackbar("Please select at least 0.5 seconds to trim.")
      return
    }

    exoPlayer.pause()
    val outFile = repository.createUniqueOutputFile(video.displayName, "trim", "mp4")
    _uiState.update {
      it.copy(
        exportJobState =
          ExportJobState.Running(
            progressPercent = 0,
            statusLabel = "Trimming video segment…",
            operation = ExportOperation.TRIM,
          )
      )
    }

    transformManager.startTrim(
      sourceUri = video.uri,
      outputFile = outFile,
      startMs = startMs,
      endMs = endMs,
      fastTrimOptimization = _uiState.value.fastTrimEnabled,
      onProgress = { pct ->
        _uiState.update { state ->
          val current = state.exportJobState
          if (current is ExportJobState.Running) {
            state.copy(exportJobState = current.copy(progressPercent = pct))
          } else state
        }
      },
      onSuccess = { file, result ->
        recordExportSuccess(
          file = file,
          operation = ExportOperation.TRIM,
          codecLabel = "H.264 MP4",
          resolutionLabel = video.resolutionLabel,
          durationMs = (endMs - startMs).coerceAtLeast(result.durationMs),
          mimeType = "video/mp4",
          sourceName = video.displayName,
        )
      },
      onError = { errorMsg ->
        _uiState.update { it.copy(exportJobState = ExportJobState.Error(errorMsg)) }
      },
    )
  }

  fun startConvertExport() {
    val video = _uiState.value.activeVideo ?: return
    val config = _uiState.value.conversionConfig
    val state = _uiState.value

    exoPlayer.pause()
    val op =
      when {
        config.codec.isAudioOnly -> ExportOperation.AUDIO_EXTRACT
        config.includeTrimRange -> ExportOperation.TRIM_AND_CONVERT
        else -> ExportOperation.CONVERT
      }
    val tag =
      when {
        config.codec.isAudioOnly -> "audio"
        config.resolution.heightPx != null -> "${config.resolution.heightPx}p"
        else -> config.codec.extension
      }
    val outFile = repository.createUniqueOutputFile(video.displayName, tag, config.codec.extension)

    _uiState.update {
      it.copy(
        exportJobState =
          ExportJobState.Running(
            progressPercent = 0,
            statusLabel =
              if (config.codec.isAudioOnly) "Extracting AAC audio track…"
              else "Converting to ${config.codec.title}…",
            operation = op,
          )
      )
    }

    val resLabel =
      when {
        config.codec.isAudioOnly -> "AAC Audio"
        config.resolution.heightPx != null -> "${config.resolution.heightPx}p"
        else -> video.resolutionLabel
      }

    transformManager.startConvert(
      sourceUri = video.uri,
      outputFile = outFile,
      config = config,
      trimStartMs = if (config.includeTrimRange) state.trimStartMs else null,
      trimEndMs = if (config.includeTrimRange) state.trimEndMs else null,
      sourceHeight = video.height,
      onProgress = { pct ->
        _uiState.update { curState ->
          val curJob = curState.exportJobState
          if (curJob is ExportJobState.Running) {
            curState.copy(exportJobState = curJob.copy(progressPercent = pct))
          } else curState
        }
      },
      onSuccess = { file, result ->
        val expectedDur =
          if (config.includeTrimRange) {
            (state.trimEndMs - state.trimStartMs).coerceAtLeast(0L)
          } else {
            video.durationMs
          }
        recordExportSuccess(
          file = file,
          operation = op,
          codecLabel = config.codec.title,
          resolutionLabel = resLabel,
          durationMs = result.durationMs.takeIf { it > 0L } ?: expectedDur,
          mimeType = config.codec.mimeType,
          sourceName = video.displayName,
        )
      },
      onError = { errorMsg ->
        _uiState.update { it.copy(exportJobState = ExportJobState.Error(errorMsg)) }
      },
    )
  }

  private suspend fun insertExportRecord(
    file: File,
    operation: ExportOperation,
    codecLabel: String,
    resolutionLabel: String,
    durationMs: Long,
    mimeType: String,
    sourceName: String,
  ): ExportRecordEntity {
    val probed = repository.extractMetadata(Uri.fromFile(file))
    val record =
      ExportRecordEntity(
        filePath = file.absolutePath,
        displayName = file.name,
        mimeType = mimeType,
        operationType = operation.name,
        codecLabel = codecLabel,
        resolutionLabel =
          if (probed.width > 0 && probed.height > 0 && !probed.isAudioOnly) {
            "${probed.width}×${probed.height}"
          } else resolutionLabel,
        durationMs = probed.durationMs.takeIf { it > 0L } ?: durationMs,
        fileSizeBytes = file.length(),
        createdAtEpochMs = System.currentTimeMillis(),
        sourceDisplayName = sourceName,
      )
    val id = exportDao.insert(record)
    return record.copy(id = id)
  }

  private fun recordExportSuccess(
    file: File,
    operation: ExportOperation,
    codecLabel: String,
    resolutionLabel: String,
    durationMs: Long,
    mimeType: String,
    sourceName: String,
  ) {
    viewModelScope.launch {
      val savedRecord =
        insertExportRecord(
          file = file,
          operation = operation,
          codecLabel = codecLabel,
          resolutionLabel = resolutionLabel,
          durationMs = durationMs,
          mimeType = mimeType,
          sourceName = sourceName,
        )
      _uiState.update { it.copy(exportJobState = ExportJobState.Success(savedRecord)) }
    }
  }

  fun cancelExport() {
    transformManager.cancel()
    _uiState.update { it.copy(exportJobState = ExportJobState.Idle) }
    showSnackbar("Export cancelled")
  }

  fun dismissExportResult() {
    _uiState.update { it.copy(exportJobState = ExportJobState.Idle) }
  }

  fun saveExportToGallery(record: ExportRecordEntity) {
    viewModelScope.launch {
      val file = File(record.filePath)
      val isAudio = record.mimeType.startsWith("audio/")
      val result =
        repository.saveToDeviceGallery(
          file = file,
          displayName = record.displayName,
          mimeType = record.mimeType,
          isAudio = isAudio,
        )
      result
        .onSuccess { uri ->
          _uiState.update { state ->
            val currentJob = state.exportJobState
            val nextJob =
              if (currentJob is ExportJobState.Success && currentJob.record.id == record.id) {
                currentJob.copy(savedToGalleryUri = uri.toString())
              } else currentJob
            state.copy(
              exportJobState = nextJob,
              snackbarMessage =
                if (isAudio) "Saved to Music/VideosPlayerConverter"
                else "Saved to Movies/VideosPlayerConverter",
            )
          }
        }
        .onFailure { err ->
          showSnackbar(err.localizedMessage ?: "Failed to save to device gallery")
        }
    }
  }

  fun deleteExportRecord(record: ExportRecordEntity) {
    viewModelScope.launch {
      runCatching { File(record.filePath).delete() }
      exportDao.deleteById(record.id)
      _exportThumbnails.update { it - record.id }
      showSnackbar("Deleted ${record.displayName}")
    }
  }

  fun buildShareIntent(record: ExportRecordEntity): Intent {
    return repository.createShareFileIntent(
      file = File(record.filePath),
      mimeType = record.mimeType,
      title = record.displayName,
    )
  }

  fun buildShareVideoIntent(video: VideoMetadata): Intent {
    return repository.createShareUriIntent(
      uri = video.uri,
      mimeType = video.mimeType,
      title = video.displayName,
    )
  }

  fun buildShareActiveVideoIntent(): Intent? {
    val active = _uiState.value.activeVideo ?: return null
    return buildShareVideoIntent(active)
  }

  fun setExportsFilter(filter: ExportsFilter) {
    _uiState.update { it.copy(exportsFilter = filter) }
  }

  fun toggleTheme() {
    _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
  }

  fun showSnackbar(message: String) {
    _uiState.update { it.copy(snackbarMessage = message) }
  }

  fun consumeSnackbar() {
    _uiState.update { it.copy(snackbarMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    transformManager.cancel()
    exoPlayer.removeListener(playerListener)
    exoPlayer.release()
  }

  companion object {
    private const val PREF_PROMPTED_INITIAL_PICKER = "has_prompted_initial_picker"
  }
}
