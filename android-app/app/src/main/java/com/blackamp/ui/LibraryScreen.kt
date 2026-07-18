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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blackamp.data.Playlist
import com.blackamp.data.Track
import com.blackamp.ui.theme.Accent
import com.blackamp.ui.theme.TextDim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    playlists: List<Playlist>,
    nowPlayingId: String?,
    loading: Boolean,
    onPlay: (List<Track>, Int) -> Unit,
    onAddToPlaylist: (Playlist, Track) -> Unit,
    onRefresh: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sheetTrack by remember { mutableStateOf<Track?>(null) }

    val filtered = remember(tracks, query) {
        if (query.isBlank()) tracks
        else tracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search songs") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, "Refresh", tint = Accent)
            }
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent)

        if (filtered.isEmpty()) {
            EmptyHint(
                icon = Icons.Filled.LibraryMusic,
                title = if (tracks.isEmpty()) "No songs yet" else "No matches",
                body = if (tracks.isEmpty())
                    "Add a GitHub repo in the Repos tab to load music."
                else "Try a different search."
            )
        } else {
            Text(
                "${filtered.size} songs",
                style = MaterialTheme.typography.labelMedium,
                color = TextDim,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { track ->
                    val index = filtered.indexOf(track)
                    TrackRow(
                        track = track,
                        isPlaying = track.id == nowPlayingId,
                        onClick = { onPlay(filtered, index) },
                        onMore = { sheetTrack = track }
                    )
                }
            }
        }
    }

    // "add to playlist" picker
    sheetTrack?.let { track ->
        ModalBottomSheet(onDismissRequest = { sheetTrack = null }) {
            Text(
                "Add to playlist",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            if (playlists.isEmpty()) {
                Text(
                    "No playlists yet — create one in the Playlists tab.",
                    color = TextDim,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                playlists.forEach { pl ->
                    ListItem(
                        headlineContent = { Text(pl.name) },
                        supportingContent = { Text("${pl.trackIds.size} songs") },
                        leadingContent = { Icon(Icons.Filled.PlaylistPlay, null, tint = Accent) },
                        modifier = Modifier.clickable {
                            onAddToPlaylist(pl, track)
                            sheetTrack = null
                        }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: (() -> Unit)? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    ListItem(
        headlineContent = {
            Text(
                track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) Accent else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextDim)
        },
        leadingContent = {
            Icon(
                if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                null,
                tint = if (isPlaying) Accent else TextDim
            )
        },
        trailingContent = onMore?.let {
            {
                IconButton(onClick = it) {
                    Icon(trailingIcon ?: Icons.Filled.MoreVert, "More", tint = TextDim)
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun EmptyHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = TextDim, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDim,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
