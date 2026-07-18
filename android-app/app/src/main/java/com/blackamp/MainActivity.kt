package com.blackamp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blackamp.data.Store
import com.blackamp.ui.LibraryScreen
import com.blackamp.ui.PlayerScreen
import com.blackamp.ui.PlaylistsScreen
import com.blackamp.ui.ReposScreen
import com.blackamp.ui.theme.BlackAmpTheme

private enum class Tab(val label: String, val icon: ImageVector) {
    Player("Player", Icons.Filled.PlayCircle),
    Library("Library", Icons.Filled.LibraryMusic),
    Playlists("Playlists", Icons.Filled.QueueMusic),
    Repos("Repos", Icons.Filled.Folder)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            BlackAmpTheme {
                val vm: MainViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(vm: MainViewModel) {
    val ui by vm.ui.collectAsState()
    val playerState by vm.player.collectAsState()
    var tab by remember { mutableStateOf(Tab.Player) }
    val snackbar = remember { SnackbarHostState() }

    // notification permission is required on Android 13+ for the media notification
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* playback still works; the notification just won't show if denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // surface one-off messages
    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    // backup export / import via the system file picker
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportTo(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importFrom(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.Player -> PlayerScreen(
                    state = playerState,
                    queueSize = ui.tracks.size,
                    queueIndex = ui.tracks.indexOfFirst { it.id == playerState.current?.id }
                        .coerceAtLeast(0),
                    onPlayPause = vm::togglePlayPause,
                    onNext = vm::next,
                    onPrev = vm::previous,
                    onStop = vm::stop,
                    onSeek = vm::seekTo,
                    onShuffle = vm::toggleShuffle,
                    onRepeat = vm::cycleRepeat
                )

                Tab.Library -> LibraryScreen(
                    tracks = ui.tracks,
                    playlists = ui.playlists,
                    nowPlayingId = playerState.current?.id,
                    loading = ui.loading,
                    onPlay = { list, i -> vm.play(list, i); tab = Tab.Player },
                    onAddToPlaylist = vm::addToPlaylist,
                    onRefresh = vm::refreshAll
                )

                Tab.Playlists -> PlaylistsScreen(
                    playlists = ui.playlists,
                    tracksOf = vm::tracksOf,
                    nowPlayingId = playerState.current?.id,
                    onCreate = { vm.createPlaylist(it) },
                    onDelete = vm::deletePlaylist,
                    onRename = vm::renamePlaylist,
                    onPlay = { list, i -> vm.play(list, i); tab = Tab.Player },
                    onRemoveTrack = vm::removeFromPlaylist,
                    onExport = { exportLauncher.launch(Store.defaultExportName()) },
                    onImport = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                )

                Tab.Repos -> ReposScreen(
                    repos = ui.repos,
                    tracks = ui.tracks,
                    loading = ui.loading,
                    onAdd = vm::addRepo,
                    onRemove = vm::removeRepo,
                    onRefresh = { vm.refreshRepo(it) }
                )
            }
        }
    }
}
