package com.blackamp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.blackamp.data.RepoSource
import com.blackamp.data.Track
import com.blackamp.ui.theme.Accent
import com.blackamp.ui.theme.TextDim

@Composable
fun ReposScreen(
    repos: List<RepoSource>,
    tracks: List<Track>,
    loading: Boolean,
    onAdd: (String) -> Unit,
    onRemove: (RepoSource) -> Unit,
    onRefresh: (RepoSource) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf<RepoSource?>(null) }

    Column(Modifier.fillMaxSize()) {

        Card(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Add a music repo", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Paste any public GitHub repo URL. Every .mp3 inside it becomes playable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDim
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("github.com/user/repo") },
                    leadingIcon = { Icon(Icons.Filled.Link, null) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onAdd(input); input = "" },
                    enabled = input.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add repo")
                }
            }
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent)

        if (repos.isEmpty()) {
            EmptyHint(
                icon = Icons.Filled.CloudDownload,
                title = "No repos added",
                body = "Add a public GitHub repo above to start building your library."
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(repos, key = { it.id }) { repo ->
                    val count = tracks.count { it.repoId == repo.id }
                    ListItem(
                        headlineContent = { Text(repo.name) },
                        supportingContent = {
                            Text("${repo.owner} · ${repo.branch} · $count songs", color = TextDim)
                        },
                        leadingContent = { Icon(Icons.Filled.Folder, null, tint = Accent) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onRefresh(repo) }) {
                                    Icon(Icons.Filled.Refresh, "Refresh", tint = TextDim)
                                }
                                IconButton(onClick = { confirmRemove = repo }) {
                                    Icon(Icons.Filled.Delete, "Remove", tint = TextDim)
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = Color_Divider)
                }
            }
        }
    }

    confirmRemove?.let { repo ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove ${repo.name}?") },
            text = { Text("Its songs will be removed from your library. Playlists keep their entries, but those songs won't play until you re-add the repo.") },
            confirmButton = {
                TextButton(onClick = { onRemove(repo); confirmRemove = null }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) { Text("Cancel") }
            }
        )
    }
}

private val Color_Divider = androidx.compose.ui.graphics.Color(0xFF222222)
