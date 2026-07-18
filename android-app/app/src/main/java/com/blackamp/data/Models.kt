package com.blackamp.data

import kotlinx.serialization.Serializable

/** A GitHub repo the user has added as a music source. */
@Serializable
data class RepoSource(
    val owner: String,
    val name: String,
    val branch: String = "main"
) {
    val id: String get() = "$owner/$name"
    val label: String get() = "$owner/$name"

    companion object {
        /**
         * Accepts any of:
         *   https://github.com/owner/repo
         *   github.com/owner/repo/tree/branch
         *   owner/repo
         */
        fun parse(input: String): RepoSource? {
            var s = input.trim()
            if (s.isEmpty()) return null
            s = s.removePrefix("https://").removePrefix("http://")
            s = s.removePrefix("www.").removePrefix("github.com/")
            s = s.removeSuffix(".git").trim('/')

            val parts = s.split('/').filter { it.isNotBlank() }
            if (parts.size < 2) return null

            val owner = parts[0]
            val name = parts[1]
            // .../tree/<branch>
            val branch = if (parts.size >= 4 && parts[2] == "tree") parts[3] else "main"
            return RepoSource(owner, name, branch)
        }
    }
}

/** One playable song discovered in a repo. */
@Serializable
data class Track(
    val id: String,        // stable: "owner/repo@branch:path"
    val title: String,
    val artist: String,
    val url: String,       // raw.githubusercontent streaming URL
    val repoId: String,
    val path: String
) {
    companion object {
        fun fromPath(repo: RepoSource, path: String): Track {
            val file = path.substringAfterLast('/')
            val base = file.removeSuffix(".mp3").removeSuffix(".MP3")
            // "Artist - Title" is the common shape; split when present
            val dash = base.indexOf(" - ")
            val artist = if (dash > 0) base.substring(0, dash).trim() else repo.name
            val title = if (dash > 0) base.substring(dash + 3).trim() else base

            val encoded = path.split('/').joinToString("/") { seg ->
                java.net.URLEncoder.encode(seg, "UTF-8").replace("+", "%20")
            }
            val url = "https://raw.githubusercontent.com/${repo.owner}/${repo.name}/${repo.branch}/$encoded"

            return Track(
                id = "${repo.id}@${repo.branch}:$path",
                title = title,
                artist = artist,
                url = url,
                repoId = repo.id,
                path = path
            )
        }
    }
}

/** A user-created playlist referencing track ids. */
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: MutableList<String> = mutableListOf(),
    val created: Long = System.currentTimeMillis()
)

/** Everything we persist — also the shape of an exported backup file. */
@Serializable
data class BackupBundle(
    val version: Int = 1,
    val exported: Long = System.currentTimeMillis(),
    val repos: List<RepoSource> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    /** Snapshot of track metadata so a backup restores even if a repo is offline. */
    val tracks: List<Track> = emptyList()
)
