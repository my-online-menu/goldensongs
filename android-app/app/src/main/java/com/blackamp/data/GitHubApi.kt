package com.blackamp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads every .mp3 in a public repo using the Git Trees API, which returns the
 * whole file listing in a single request (unlike the per-directory Contents API).
 */
object GitHubApi {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class TreeResponse(
        val tree: List<TreeEntry> = emptyList(),
        val truncated: Boolean = false
    )

    @Serializable
    private data class TreeEntry(
        val path: String = "",
        val type: String = "",
        @SerialName("size") val size: Long = 0
    )

    class ApiException(message: String) : Exception(message)

    /**
     * Fetches the mp3 list for [repo]. If the declared branch 404s, falls back to
     * the other common default so users don't have to know main vs master.
     */
    suspend fun fetchTracks(repo: RepoSource): List<Track> = withContext(Dispatchers.IO) {
        val branches = buildList {
            add(repo.branch)
            if (repo.branch != "main") add("main")
            if (repo.branch != "master") add("master")
        }.distinct()

        var lastError: Exception? = null
        for (branch in branches) {
            try {
                val effective = repo.copy(branch = branch)
                return@withContext loadTree(effective)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: ApiException("Could not read ${repo.label}")
    }

    private fun loadTree(repo: RepoSource): List<Track> {
        val api = "https://api.github.com/repos/${repo.owner}/${repo.name}" +
                "/git/trees/${repo.branch}?recursive=1"

        val conn = (URL(api).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "BlackAmp-Android")
            connectTimeout = 15000
            readTimeout = 20000
        }

        try {
            val code = conn.responseCode
            if (code == 404) throw ApiException("Repo or branch not found (${repo.label}@${repo.branch})")
            if (code == 403) throw ApiException("GitHub rate limit reached — try again in a while")
            if (code != 200) throw ApiException("GitHub returned HTTP $code")

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val parsed = json.decodeFromString<TreeResponse>(body)

            return parsed.tree
                .filter { it.type == "blob" && it.path.endsWith(".mp3", ignoreCase = true) }
                .map { Track.fromPath(repo, it.path) }
                .sortedBy { it.title.lowercase() }
        } finally {
            conn.disconnect()
        }
    }
}
