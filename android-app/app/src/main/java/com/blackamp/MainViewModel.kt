package com.blackamp

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.blackamp.data.*
import com.blackamp.playback.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class UiState(
    val repos: List<RepoSource> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val loading: Boolean = false,
    val message: String? = null
)

data class PlayerState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _player = MutableStateFlow(PlayerState())
    val player: StateFlow<PlayerState> = _player.asStateFlow()

    private var controller: MediaController? = null
    /** Tracks in the order they were handed to the player, so we can map index -> Track. */
    private var queue: List<Track> = emptyList()

    init {
        viewModelScope.launch {
            val data = store.load()
            _ui.value = _ui.value.copy(
                repos = data.repos,
                tracks = data.tracks,
                playlists = data.playlists
            )
            // refresh sources in the background so new uploads appear
            data.repos.forEach { refreshRepo(it, silent = true) }
        }
        connectToService()
        startProgressTicker()
    }

    // ---------------- player wiring ----------------

    private fun connectToService() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(object : Player.Listener {
                override fun onEvents(p: Player, events: Player.Events) = syncPlayerState()
            })
            syncPlayerState()
        }, MoreExecutors.directExecutor())
    }

    private fun syncPlayerState() {
        val c = controller ?: return
        val idx = c.currentMediaItemIndex
        _player.value = _player.value.copy(
            current = queue.getOrNull(idx),
            isPlaying = c.isPlaying,
            duration = c.duration.coerceAtLeast(0L),
            position = c.currentPosition.coerceAtLeast(0L),
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode
        )
    }

    private fun startProgressTicker() {
        viewModelScope.launch {
            while (true) {
                delay(500)
                val c = controller ?: continue
                if (c.isPlaying) {
                    _player.value = _player.value.copy(
                        position = c.currentPosition.coerceAtLeast(0L),
                        duration = c.duration.coerceAtLeast(0L)
                    )
                }
            }
        }
    }

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(Uri.parse(url))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(repoId)
                    .build()
            )
            .build()

    /** Play [list] starting at [index]. This is what fills the notification. */
    fun play(list: List<Track>, index: Int) {
        val c = controller ?: return
        if (list.isEmpty()) return
        queue = list
        c.setMediaItems(list.map { it.toMediaItem() }, index.coerceIn(0, list.size - 1), 0L)
        c.prepare()
        c.play()
        syncPlayerState()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNextMediaItem().let { syncPlayerState() }
    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun stop() {
        controller?.pause()
        controller?.seekTo(0)
    }

    fun seekTo(ms: Long) = controller?.seekTo(ms).let { syncPlayerState() }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
        syncPlayerState()
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        syncPlayerState()
    }

    // ---------------- repos ----------------

    fun addRepo(input: String) {
        val repo = RepoSource.parse(input)
        if (repo == null) {
            toast("That doesn't look like a GitHub repo URL")
            return
        }
        if (_ui.value.repos.any { it.id == repo.id }) {
            toast("${repo.label} is already added")
            return
        }
        _ui.value = _ui.value.copy(repos = _ui.value.repos + repo)
        persist()
        refreshRepo(repo)
    }

    fun removeRepo(repo: RepoSource) {
        _ui.value = _ui.value.copy(
            repos = _ui.value.repos.filterNot { it.id == repo.id },
            tracks = _ui.value.tracks.filterNot { it.repoId == repo.id }
        )
        persist()
    }

    fun refreshRepo(repo: RepoSource, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _ui.value = _ui.value.copy(loading = true)
            try {
                val fetched = GitHubApi.fetchTracks(repo)
                // replace this repo's tracks, keep everyone else's
                val others = _ui.value.tracks.filterNot { it.repoId == repo.id }
                val merged = (others + fetched).sortedBy { it.title.lowercase() }
                // remember the branch that actually worked
                val repos = _ui.value.repos.map {
                    if (it.id == repo.id && fetched.isNotEmpty())
                        it.copy(branch = fetched.first().id.substringAfter('@').substringBefore(':'))
                    else it
                }
                _ui.value = _ui.value.copy(tracks = merged, repos = repos, loading = false)
                persist()
                if (!silent) toast("${repo.label}: ${fetched.size} songs")
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false)
                if (!silent) toast(e.message ?: "Could not load ${repo.label}")
            }
        }
    }

    fun refreshAll() = _ui.value.repos.forEach { refreshRepo(it) }

    // ---------------- playlists ----------------

    fun createPlaylist(name: String): Playlist {
        val pl = Playlist(id = UUID.randomUUID().toString(), name = name.ifBlank { "Untitled" })
        _ui.value = _ui.value.copy(playlists = _ui.value.playlists + pl)
        persist()
        return pl
    }

    fun deletePlaylist(pl: Playlist) {
        _ui.value = _ui.value.copy(playlists = _ui.value.playlists.filterNot { it.id == pl.id })
        persist()
    }

    fun renamePlaylist(pl: Playlist, name: String) {
        _ui.value = _ui.value.copy(
            playlists = _ui.value.playlists.map { if (it.id == pl.id) it.copy(name = name) else it }
        )
        persist()
    }

    fun addToPlaylist(pl: Playlist, track: Track) {
        _ui.value = _ui.value.copy(playlists = _ui.value.playlists.map {
            if (it.id == pl.id && !it.trackIds.contains(track.id))
                it.copy(trackIds = (it.trackIds + track.id).toMutableList())
            else it
        })
        persist()
        toast("Added to ${pl.name}")
    }

    fun removeFromPlaylist(pl: Playlist, trackId: String) {
        _ui.value = _ui.value.copy(playlists = _ui.value.playlists.map {
            if (it.id == pl.id) it.copy(trackIds = it.trackIds.filterNot { t -> t == trackId }.toMutableList())
            else it
        })
        persist()
    }

    fun tracksOf(pl: Playlist): List<Track> {
        val byId = _ui.value.tracks.associateBy { it.id }
        return pl.trackIds.mapNotNull { byId[it] }
    }

    // ---------------- backup ----------------

    fun currentBundle(): BackupBundle = BackupBundle(
        repos = _ui.value.repos,
        playlists = _ui.value.playlists,
        tracks = _ui.value.tracks
    )

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.let { out ->
                    store.exportTo(out, currentBundle())
                }
            }.onSuccess { toast("Backup exported") }
                .onFailure { toast("Export failed") }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val bundle = runCatching {
                getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.let { store.importFrom(it) }
            }.getOrNull()

            if (bundle == null) {
                toast("That file isn't a valid backup")
                return@launch
            }
            // merge rather than clobber, so importing never loses existing work
            val repos = (_ui.value.repos + bundle.repos).distinctBy { it.id }
            val playlists = (_ui.value.playlists + bundle.playlists).distinctBy { it.id }
            val tracks = (_ui.value.tracks + bundle.tracks).distinctBy { it.id }
            _ui.value = _ui.value.copy(repos = repos, playlists = playlists, tracks = tracks)
            persist()
            toast("Restored ${bundle.playlists.size} playlists, ${bundle.repos.size} repos")
            repos.forEach { refreshRepo(it, silent = true) }
        }
    }

    // ---------------- misc ----------------

    private fun persist() {
        viewModelScope.launch { store.save(currentBundle()) }
    }

    fun toast(msg: String) {
        _ui.value = _ui.value.copy(message = msg)
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }
}
