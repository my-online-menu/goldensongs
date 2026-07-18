package com.blackamp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackamp.data.Playlist
import com.blackamp.data.Track
import com.blackamp.ui.theme.Accent
import com.blackamp.ui.theme.TextDim

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    tracksOf: (Playlist) -> List<Track>,
    nowPlayingId: String?,
    onCreate: (String) -> Unit,
    onDelete: (Playlist) -> Unit,
    onRename: (Playlist, String) -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onRemoveTrack: (Playlist, String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    var open by remember { mutableStateOf<Playlist?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }

    // ---- detail view of one playlist ----
    val current = open
    if (current != null) {
        val songs = tracksOf(current)
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { open = null }) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(current.name, style = MaterialTheme.typography.titleMedium)
                    Text("${songs.size} songs", style = MaterialTheme.typography.bodySmall, color = TextDim)
                }
                IconButton(
                    onClick = { if (songs.isNotEmpty()) onPlay(songs, 0) },
                    enabled = songs.isNotEmpty()
                ) {
                    Icon(Icons.Filled.PlayCircle, "Play all", tint = Accent)
                }
            }
            HorizontalDivider(color = DividerColor)

            if (songs.isEmpty()) {
                EmptyHint(
                    icon = Icons.Filled.QueueMusic,
                    title = "Empty playlist",
                    body = "Add songs from the Library tab using the ⋮ menu."
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(songs, key = { it.id }) { t ->
                        val idx = songs.indexOf(t)
                        TrackRow(
                            track = t,
                            isPlaying = t.id == nowPlayingId,
                            onClick = { onPlay(songs, idx) },
                            onMore = { onRemoveTrack(current, t.id) },
                            trailingIcon = Icons.Filled.RemoveCircleOutline
                        )
                    }
                }
            }
        }
        return
    }

    // ---- list of playlists ----
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { showCreate = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("New")
            }
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(6.dp)); Text("Export")
            }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Download, null); Spacer(Modifier.width(6.dp)); Text("Import")
            }
        }

        if (playlists.isEmpty()) {
            EmptyHint(
                icon = Icons.Filled.QueueMusic,
                title = "No playlists",
                body = "Create one, then add songs from your Library."
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(playlists, key = { it.id }) { pl ->
                    ListItem(
                        headlineContent = { Text(pl.name) },
                        supportingContent = { Text("${pl.trackIds.size} songs", color = TextDim) },
                        leadingContent = { Icon(Icons.Filled.PlaylistPlay, null, tint = Accent) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { renaming = pl }) {
                                    Icon(Icons.Filled.Edit, "Rename", tint = TextDim)
                                }
                                IconButton(onClick = { onDelete(pl) }) {
                                    Icon(Icons.Filled.Delete, "Delete", tint = TextDim)
                                }
                            }
                        },
                        modifier = Modifier.clickable { open = pl }
                    )
                    HorizontalDivider(color = DividerColor)
                }
            }
        }
    }

    if (showCreate) {
        TextPrompt(
            title = "New playlist",
            initial = "",
            confirmLabel = "Create",
            onDismiss = { showCreate = false },
            onConfirm = { onCreate(it); showCreate = false }
        )
    }

    renaming?.let { pl ->
        TextPrompt(
            title = "Rename playlist",
            initial = pl.name,
            confirmLabel = "Save",
            onDismiss = { renaming = null },
            onConfirm = { onRename(pl, it); renaming = null }
        )
    }
}

@Composable
private fun TextPrompt(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                placeholder = { Text("Playlist name") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }, enabled = value.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private val DividerColor = androidx.compose.ui.graphics.Color(0xFF222222)
