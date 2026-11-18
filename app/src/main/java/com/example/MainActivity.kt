package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.WorkspaceTab
import com.example.ui.components.ExportJobDialog
import com.example.ui.components.StudioDrawerContent
import com.example.ui.screens.ExportsScreen
import com.example.ui.screens.FullscreenPlayerScreen
import com.example.ui.screens.MergerScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.ClipCraftTheme
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

const val ROUTE_STUDIO = "studio"
const val ROUTE_WORKSPACE = "workspace"
const val ROUTE_MERGER = "merger"
const val ROUTE_EXPORTS = "exports"
const val ROUTE_FULLSCREEN = "fullscreen"

private data class NavDestinationItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

class MainActivity : ComponentActivity() {

  private val viewModel: StudioViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleIncomingIntent(intent)

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      ClipCraftTheme(darkTheme = uiState.isDarkTheme) { ClipCraftApp(viewModel = viewModel) }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIncomingIntent(intent)
  }

  override fun onStop() {
    super.onStop()
    if (!isChangingConfigurations) {
      viewModel.exoPlayer.pause()
    }
  }

  private fun handleIncomingIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_VIEW) {
      intent.data?.let { uri -> viewModel.loadVideoUri(uri, WorkspaceTab.PLAY) }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipCraftApp(viewModel: StudioViewModel) {
  val context = LocalContext.current
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_STUDIO

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val playerState by viewModel.playerState.collectAsStateWithLifecycle()
  val libraryVideos by viewModel.libraryVideos.collectAsStateWithLifecycle()
  val libraryThumbnails by viewModel.libraryThumbnails.collectAsStateWithLifecycle()
  val linkedFolders by viewModel.linkedFolders.collectAsStateWithLifecycle()
  val allExports by viewModel.allExports.collectAsStateWithLifecycle()
  val filteredExports by viewModel.filteredExports.collectAsStateWithLifecycle()
  val exportThumbnails by viewModel.exportThumbnails.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val coroutineScope = rememberCoroutineScope()

  var pendingTargetTab by remember { mutableStateOf(WorkspaceTab.PLAY) }
  var pendingPickerForMerger by remember { mutableStateOf(false) }
  var pendingStayOnHomeAfterPick by remember { mutableStateOf(false) }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAutoLibrary(silent = true)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeSnackbar()
    }
  }

  fun navigateTopLevel(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val multiPhotoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
      onResult = { uris ->
        val stayHome = pendingStayOnHomeAfterPick
        pendingStayOnHomeAfterPick = false
        if (uris.isNotEmpty()) {
          if (pendingPickerForMerger) {
            viewModel.addPickedUrisToMergeQueue(uris)
            pendingPickerForMerger = false
            navigateTopLevel(ROUTE_MERGER)
          } else {
            viewModel.importPickedVideos(
              uris = uris,
              targetTab = pendingTargetTab,
              autoPlay = !stayHome,
            )
            if (!stayHome) {
              navigateTopLevel(ROUTE_WORKSPACE)
            }
          }
        } else {
          pendingPickerForMerger = false
        }
      },
    )

  val openDocumentLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument(),
      onResult = { uri ->
        val stayHome = pendingStayOnHomeAfterPick
        pendingStayOnHomeAfterPick = false
        if (uri != null) {
          runCatching {
            context.contentResolver.takePersistableUriPermission(
              uri,
              Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
          }
          viewModel.importPickedVideos(
            uris = listOf(uri),
            targetTab = pendingTargetTab,
            autoPlay = !stayHome,
          )
          if (!stayHome) {
            navigateTopLevel(ROUTE_WORKSPACE)
          }
        }
      },
    )

  val openFolderTreeLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocumentTree(),
      onResult = { treeUri ->
        if (treeUri != null) {
          viewModel.linkVideoFolder(treeUri)
        }
      },
    )

  fun launchPickVideos(
    targetTab: WorkspaceTab = WorkspaceTab.PLAY,
    forMerger: Boolean = false,
    stayOnHome: Boolean = false,
  ) {
    pendingTargetTab = targetTab
    pendingPickerForMerger = forMerger
    pendingStayOnHomeAfterPick = stayOnHome
    multiPhotoPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
    )
  }

  fun launchBrowseFiles(
    targetTab: WorkspaceTab = WorkspaceTab.PLAY,
    stayOnHome: Boolean = false,
  ) {
    pendingTargetTab = targetTab
    pendingStayOnHomeAfterPick = stayOnHome
    openDocumentLauncher.launch(arrayOf("video/*"))
  }

  LaunchedEffect(uiState.shouldAutoPromptPicker) {
    if (uiState.shouldAutoPromptPicker && uiState.activeVideo == null) {
      viewModel.markInitialPickerPrompted()
      launchPickVideos(targetTab = WorkspaceTab.PLAY, stayOnHome = true)
    }
  }

  val navItems =
    listOf(
      NavDestinationItem(
        route = ROUTE_STUDIO,
        label = stringResource(R.string.nav_studio),
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
      ),
      NavDestinationItem(
        route = ROUTE_WORKSPACE,
        label = "Editor",
        selectedIcon = Icons.Filled.Movie,
        unselectedIcon = Icons.Outlined.Movie,
      ),
      NavDestinationItem(
        route = ROUTE_MERGER,
        label = "Merge",
        selectedIcon = Icons.Filled.AutoAwesomeMotion,
        unselectedIcon = Icons.Outlined.AutoAwesomeMotion,
      ),
      NavDestinationItem(
        route = ROUTE_EXPORTS,
        label = stringResource(R.string.nav_exports),
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary,
      ),
    )

  val isFullscreen = currentRoute == ROUTE_FULLSCREEN

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = !isFullscreen,
    drawerContent = {
      StudioDrawerContent(
        currentRoute = currentRoute,
        activeWorkspaceTab = uiState.workspaceTab,
        libraryCount = libraryVideos.size,
        exportsCount = allExports.size,
        isDarkTheme = uiState.isDarkTheme,
        onNavigateStudio = {
          coroutineScope.launch { drawerState.close() }
          navigateTopLevel(ROUTE_STUDIO)
        },
        onNavigateWorkspace = { tab ->
          coroutineScope.launch { drawerState.close() }
          viewModel.setWorkspaceTab(tab)
          navigateTopLevel(ROUTE_WORKSPACE)
        },
        onNavigateMerger = {
          coroutineScope.launch { drawerState.close() }
          navigateTopLevel(ROUTE_MERGER)
        },
        onNavigateExports = {
          coroutineScope.launch { drawerState.close() }
          navigateTopLevel(ROUTE_EXPORTS)
        },
        onPickMultipleVideos = {
          coroutineScope.launch { drawerState.close() }
          launchPickVideos(WorkspaceTab.PLAY)
        },
        onLinkVideoFolder = {
          coroutineScope.launch { drawerState.close() }
          openFolderTreeLauncher.launch(null)
        },
        onRefreshLibrary = {
          coroutineScope.launch { drawerState.close() }
          viewModel.refreshAutoLibrary(silent = false)
        },
        onToggleTheme = { viewModel.toggleTheme() },
      )
    },
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 720.dp && !isFullscreen

      Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          if (!isFullscreen) {
            CenterAlignedTopAppBar(
              navigationIcon = {
                IconButton(
                  onClick = { coroutineScope.launch { drawerState.open() } },
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("top_bar_menu_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Open navigation side menu",
                  )
                }
              },
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.MovieCreation,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(6.dp),
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              },
              actions = {
                IconButton(
                  onClick = { viewModel.toggleTheme() },
                  modifier =
                    Modifier.minimumInteractiveComponentSize().testTag("theme_toggle_button"),
                ) {
                  Icon(
                    imageVector =
                      if (uiState.isDarkTheme) Icons.Rounded.LightMode
                      else Icons.Rounded.DarkMode,
                    contentDescription = stringResource(R.string.cd_theme_toggle),
                  )
                }
              },
              colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                  containerColor = MaterialTheme.colorScheme.background
                ),
            )
          }
        },
        bottomBar = {
          if (!isFullscreen && !useNavRail) {
            NavigationBar(
              windowInsets = WindowInsets.navigationBars,
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
              navItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTopLevel(item.route) },
                  icon = {
                    if (item.route == ROUTE_EXPORTS && allExports.isNotEmpty()) {
                      BadgedBox(badge = { Badge { Text("${allExports.size}") } }) {
                        Icon(
                          imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                          contentDescription = item.label,
                        )
                      }
                    } else {
                      Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                      )
                    }
                  },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag("bottom_nav_${item.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useNavRail) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              modifier = Modifier.fillMaxHeight(),
            ) {
              navItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationRailItem(
                  selected = selected,
                  onClick = { navigateTopLevel(item.route) },
                  icon = {
                    if (item.route == ROUTE_EXPORTS && allExports.isNotEmpty()) {
                      BadgedBox(badge = { Badge { Text("${allExports.size}") } }) {
                        Icon(
                          imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                          contentDescription = item.label,
                        )
                      }
                    } else {
                      Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                      )
                    }
                  },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag("rail_nav_${item.route}"),
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            NavHost(navController = navController, startDestination = ROUTE_STUDIO) {
              composable(ROUTE_STUDIO) {
                StudioScreen(
                  libraryVideos = libraryVideos,
                  libraryThumbnails = libraryThumbnails,
                  linkedFolders = linkedFolders,
                  sortOrder = uiState.librarySortOrder,
                  isScanningLibrary = uiState.isScanningLibrary,
                  onSelectSortOrder = { viewModel.setLibrarySortOrder(it) },
                  onPickMultipleVideos = { tab -> launchPickVideos(tab, stayOnHome = true) },
                  onLinkVideoFolder = { openFolderTreeLauncher.launch(null) },
                  onUnlinkFolder = { viewModel.unlinkVideoFolder(it) },
                  onRefreshLibrary = { viewModel.refreshAutoLibrary(silent = false) },
                  onBrowseFiles = { tab -> launchBrowseFiles(tab, stayOnHome = true) },
                  onOpenLibraryVideo = { video, tab ->
                    if (uiState.activeVideo?.uriString == video.uriString) {
                      viewModel.setWorkspaceTab(tab)
                    } else {
                      viewModel.loadVideoUri(video.uri, tab)
                    }
                    navigateTopLevel(ROUTE_WORKSPACE)
                  },
                  onAddVideoToMerger = { video ->
                    viewModel.addVideoToMergeQueue(video)
                    viewModel.showSnackbar("Added ${video.displayName} to Video Merger")
                  },
                  onShareLibraryVideo = { video ->
                    context.startActivity(viewModel.buildShareVideoIntent(video))
                  },
                  onRemoveLibraryVideo = { video -> viewModel.removeLibraryVideo(video) },
                )
              }

              composable(ROUTE_WORKSPACE) {
                WorkspaceScreen(
                  player = viewModel.exoPlayer,
                  playerState = playerState,
                  uiState = uiState,
                  onPickVideo = { launchPickVideos(uiState.workspaceTab) },
                  onBrowseFiles = { launchBrowseFiles(uiState.workspaceTab) },
                  onSelectTab = { viewModel.setWorkspaceTab(it) },
                  onTogglePlayPause = { viewModel.togglePlayPause() },
                  onSeekTo = { viewModel.seekTo(it) },
                  onSeekBy = { viewModel.seekBy(it) },
                  onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
                  onToggleMute = { viewModel.toggleMute() },
                  onToggleLoop = { viewModel.toggleLoop() },
                  onToggleResizeMode = { viewModel.toggleResizeMode() },
                  onEnterFullscreen = { navController.navigate(ROUTE_FULLSCREEN) },
                  onShareActiveVideo = {
                    viewModel.buildShareActiveVideoIntent()?.let { intent ->
                      context.startActivity(intent)
                    }
                  },
                  onTrimRangeChange = { start, end, edge ->
                    viewModel.updateTrimRange(start, end, edge)
                  },
                  onNudgeTrimStart = { viewModel.nudgeTrimStart(it) },
                  onNudgeTrimEnd = { viewModel.nudgeTrimEnd(it) },
                  onToggleFastTrim = { viewModel.setFastTrimEnabled(it) },
                  onTogglePreviewTrim = { viewModel.togglePreviewTrim() },
                  onStartTrimExport = { viewModel.startTrimExport() },
                  onSelectCutMode = { viewModel.setCutMode(it) },
                  onSetSplitPointMs = { viewModel.setSplitPointMs(it) },
                  onSetSplitAtPlayhead = { viewModel.setSplitAtPlayhead() },
                  onStartCutExport = { viewModel.startCutExport() },
                  onApplyPreset = { viewModel.applyConversionPreset(it) },
                  onUpdateConversionConfig = { viewModel.updateConversionConfig(it) },
                  onStartConvertExport = { viewModel.startConvertExport() },
                )
              }

              composable(ROUTE_MERGER) {
                MergerScreen(
                  mergeQueue = uiState.mergeQueue,
                  mergeConfig = uiState.mergeConfig,
                  libraryVideos = libraryVideos,
                  libraryThumbnails = libraryThumbnails,
                  onPickVideosForMerge = { launchPickVideos(forMerger = true) },
                  onAddFromLibrary = { viewModel.addVideoToMergeQueue(it) },
                  onRemoveAt = { viewModel.removeFromMergeQueue(it) },
                  onMoveClip = { from, to -> viewModel.moveMergeClip(from, to) },
                  onClearQueue = { viewModel.clearMergeQueue() },
                  onUpdateMergeConfig = { viewModel.updateMergeConfig(it) },
                  onStartMerge = { viewModel.startMergeExport() },
                )
              }

              composable(ROUTE_EXPORTS) {
                ExportsScreen(
                  exports = filteredExports,
                  activeFilter = uiState.exportsFilter,
                  thumbnails = exportThumbnails,
                  onSelectFilter = { viewModel.setExportsFilter(it) },
                  onPlayExport = { record ->
                    viewModel.openExportedRecord(record, WorkspaceTab.PLAY)
                    navigateTopLevel(ROUTE_WORKSPACE)
                  },
                  onShareExport = { record ->
                    context.startActivity(viewModel.buildShareIntent(record))
                  },
                  onSaveToGallery = { record -> viewModel.saveExportToGallery(record) },
                  onDeleteExport = { record -> viewModel.deleteExportRecord(record) },
                  onPickVideo = { launchPickVideos(WorkspaceTab.PLAY) },
                )
              }

              composable(ROUTE_FULLSCREEN) {
                val active = uiState.activeVideo
                FullscreenPlayerScreen(
                  player = viewModel.exoPlayer,
                  playerState = playerState,
                  title = active?.displayName ?: "Videos Player",
                  isAudioOnly = active?.isAudioOnly == true,
                  onTogglePlayPause = { viewModel.togglePlayPause() },
                  onSeekTo = { viewModel.seekTo(it) },
                  onSeekBy = { viewModel.seekBy(it) },
                  onToggleMute = { viewModel.toggleMute() },
                  onToggleResizeMode = { viewModel.toggleResizeMode() },
                  onExitFullscreen = { navController.popBackStack() },
                )
              }
            }
          }
        }
      }
    }
  }

  ExportJobDialog(
    jobState = uiState.exportJobState,
    onCancel = { viewModel.cancelExport() },
    onDismiss = { viewModel.dismissExportResult() },
    onPlayExport = { record ->
      viewModel.dismissExportResult()
      viewModel.openExportedRecord(record, WorkspaceTab.PLAY)
      navigateTopLevel(ROUTE_WORKSPACE)
    },
    onShareExport = { record -> context.startActivity(viewModel.buildShareIntent(record)) },
    onSaveToGallery = { record -> viewModel.saveExportToGallery(record) },
  )
}
