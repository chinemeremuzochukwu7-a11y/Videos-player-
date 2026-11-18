package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ROUTE_EXPORTS
import com.example.ROUTE_MERGER
import com.example.ROUTE_STUDIO
import com.example.ROUTE_WORKSPACE
import com.example.data.WorkspaceTab
import com.example.ui.theme.MonoBadgeStyle

@Composable
fun StudioDrawerContent(
  currentRoute: String,
  activeWorkspaceTab: WorkspaceTab,
  libraryCount: Int,
  exportsCount: Int,
  isDarkTheme: Boolean,
  onNavigateStudio: () -> Unit,
  onNavigateWorkspace: (WorkspaceTab) -> Unit,
  onNavigateMerger: () -> Unit,
  onNavigateExports: () -> Unit,
  onPickMultipleVideos: () -> Unit,
  onLinkVideoFolder: () -> Unit,
  onRefreshLibrary: () -> Unit,
  onToggleTheme: () -> Unit,
) {
  ModalDrawerSheet(
    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.width(308.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)
    ) {
      // Studio Brand Header
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(
              Brush.linearGradient(
                listOf(
                  MaterialTheme.colorScheme.primaryContainer,
                  MaterialTheme.colorScheme.secondaryContainer,
                )
              )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(46.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.MovieCreation,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(26.dp),
                )
              }
            }
            Column {
              Text(
                text = "Videos Player",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = "Video Converter • com.geevideoplayer",
                style = MonoBadgeStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecPill(text = "$libraryCount VIDEOS")
            SpecPill(text = "$exportsCount EXPORTS")
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      DrawerSectionLabel("WORKSPACE")

      NavigationDrawerItem(
        label = { Text("Home & Video Library") },
        selected = currentRoute == ROUTE_STUDIO,
        onClick = onNavigateStudio,
        icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_studio"),
      )

      NavigationDrawerItem(
        label = { Text("Video Player") },
        selected = currentRoute == ROUTE_WORKSPACE && activeWorkspaceTab == WorkspaceTab.PLAY,
        onClick = { onNavigateWorkspace(WorkspaceTab.PLAY) },
        icon = { Icon(Icons.Rounded.PlayCircle, contentDescription = null) },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_play"),
      )

      NavigationDrawerItem(
        label = { Text("Video Trimmer") },
        selected = currentRoute == ROUTE_WORKSPACE && activeWorkspaceTab == WorkspaceTab.TRIM,
        onClick = { onNavigateWorkspace(WorkspaceTab.TRIM) },
        icon = { Icon(Icons.Rounded.ContentCut, contentDescription = null) },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_trim"),
      )

      NavigationDrawerItem(
        label = { Text("Video Cutter") },
        selected = currentRoute == ROUTE_WORKSPACE && activeWorkspaceTab == WorkspaceTab.CUT,
        onClick = { onNavigateWorkspace(WorkspaceTab.CUT) },
        icon = { Icon(Icons.Rounded.CallSplit, contentDescription = null) },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_cut"),
      )

      NavigationDrawerItem(
        label = { Text("Video Merger") },
        selected = currentRoute == ROUTE_MERGER,
        onClick = onNavigateMerger,
        icon = { Icon(Icons.Rounded.AutoAwesomeMotion, contentDescription = null) },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_merge"),
      )

      NavigationDrawerItem(
        label = { Text("Format Converter") },
        selected = currentRoute == ROUTE_WORKSPACE && activeWorkspaceTab == WorkspaceTab.CONVERT,
        onClick = { onNavigateWorkspace(WorkspaceTab.CONVERT) },
        icon = { Icon(Icons.Rounded.Transform, contentDescription = null) },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_convert"),
      )

      NavigationDrawerItem(
        label = { Text("Exports Vault") },
        selected = currentRoute == ROUTE_EXPORTS,
        onClick = onNavigateExports,
        icon = { Icon(Icons.Rounded.VideoLibrary, contentDescription = null) },
        badge = {
          if (exportsCount > 0) {
            Badge { Text("$exportsCount") }
          }
        },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag("drawer_exports"),
      )

      HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp))

      DrawerSectionLabel("PLAY-COMPLIANT LIBRARY")

      NavigationDrawerItem(
        label = { Text("Pick Videos (Photo Picker)") },
        selected = false,
        onClick = onPickMultipleVideos,
        icon = { Icon(Icons.Rounded.VideoLibrary, contentDescription = null) },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag("drawer_pick_videos"),
      )

      NavigationDrawerItem(
        label = { Text("Auto-Sync Video Folder") },
        selected = false,
        onClick = onLinkVideoFolder,
        icon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag("drawer_sync_folder"),
      )

      NavigationDrawerItem(
        label = { Text("Refresh Library") },
        selected = false,
        onClick = onRefreshLibrary,
        icon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
        modifier =
          Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag("drawer_refresh_library"),
      )

      HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp))

      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text("Dark Studio Mode", style = MaterialTheme.typography.titleSmall)
        }
        Switch(
          checked = isDarkTheme,
          onCheckedChange = { onToggleTheme() },
          modifier = Modifier.testTag("drawer_theme_switch"),
        )
      }

      Text(
        text = "Uses Android Photo Picker & SAF (zero broad storage permissions).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
      )
    }
  }
}

@Composable
private fun DrawerSectionLabel(text: String) {
  Text(
    text = text,
    style = MonoBadgeStyle,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
  )
}
