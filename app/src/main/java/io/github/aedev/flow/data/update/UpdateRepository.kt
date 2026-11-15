package io.github.aedev.flow.data.update

import android.os.Build
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.data.local.LocalDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val STABLE_RELEASE_API = "https://api.github.com/repos/saimway/SuperTube/releases/latest"
private const val NIGHTLY_RELEASE_API = "https://api.github.com/repos/saimway/SuperTube/releases/tags/nightly"
private const val STABLE_RELEASE_WEB = "https://github.com/saimway/SuperTube/releases/latest"
private const val NIGHTLY_RELEASE_WEB = "https://github.com/saimway/SuperTube/releases/tag/nightly"
private const val RELEASES_ATOM_URL = "https://github.com/saimway/SuperTube/releases.atom"
private const val RELEASES_WEB_BASE = "https://github.com/saimway/SuperTube/releases"
private const val NIGHTLY_CHANNEL = "nightly"
private const val GITHUB_JSON = "application/vnd.github+json"

/** Where an automatic check may surface a release; each announces a version once. */
enum class UpdateAnnouncement { LAUNCH_PAGE, NOTIFICATION }

/** How the automatic checks space themselves out and which releases they may show. */
internal object UpdateSchedule {
    val COOLDOWN_MS: Long = TimeUnit.HOURS.toMillis(12)

    fun isDue(
        lastCheckMs: Long,
        nowMs: Long,
    ): Boolean = nowMs < lastCheckMs || nowMs - lastCheckMs >= COOLDOWN_MS

    fun mayAnnounce(
        version: String,
        skippedVersion: String?,
        announcedVersion: String?,
    ): Boolean = version != skippedVersion && version != announcedVersion
}

/**
 * The one place Flow asks GitHub about releases. The launch check, the background worker and the
 * Settings button all come through here, so a cold start never fetches twice and a skipped version
 * stays skipped everywhere.
 */
@Singleton
class UpdateRepository
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val dataManager: LocalDataManager,
    ) {
        private val mutex = Mutex()
        private val isNightly = BuildConfig.UPDATE_CHANNEL == NIGHTLY_CHANNEL
        private val _latest = MutableStateFlow<AppRelease?>(null)

        /** The newest release found this process, or null when none is known or this build is current. */
        val latest: StateFlow<AppRelease?> = _latest.asStateFlow()

        /** Asks GitHub now. Throws when the request fails, so a check the user started can say so. */
        suspend fun fetch(): AppRelease? =
            mutex.withLock {
                try {
                    fetchLocked()
                } catch (e: Exception) {
                    _latest.value ?: throw e
                }
            }

        /**
         * For the automatic checks: fetches only once the cooldown has passed, and returns a release
         * only if [announcement] has not shown this version before and the user has not skipped it.
         */
        suspend fun releaseToAnnounce(
            announcement: UpdateAnnouncement,
            nowMs: Long = System.currentTimeMillis(),
        ): AppRelease? =
            mutex.withLock {
                if (!UpdateSchedule.isDue(dataManager.lastUpdateCheck.first(), nowMs)) return@withLock null
                val release = runCatching { fetchLocked() }.getOrNull() ?: return@withLock null
                val announced =
                    when (announcement) {
                        UpdateAnnouncement.LAUNCH_PAGE -> dataManager.promptedUpdateVersion.first()
                        UpdateAnnouncement.NOTIFICATION -> dataManager.notifiedUpdateVersion.first()
                    }
                if (!UpdateSchedule.mayAnnounce(release.version, dataManager.skippedUpdateVersion.first(), announced)) {
                    return@withLock null
                }
                when (announcement) {
                    UpdateAnnouncement.LAUNCH_PAGE -> dataManager.setPromptedUpdateVersion(release.version)
                    UpdateAnnouncement.NOTIFICATION -> dataManager.setNotifiedUpdateVersion(release.version)
                }
                release
            }

        suspend fun skip(version: String) = dataManager.setSkippedUpdateVersion(version)

        /**
         * Multi-tier update fetch:
         * Tier 1: Query GitHub REST API. Handles HTTP 403 (unauthenticated rate limit 60/hr on cellular/Wi-Fi).
         * Tier 2: Query GitHub web release redirect with followRedirects=false to extract latest tag.
         *         If latestTag is not newer than current build, returns null immediately (UpToDate).
         * Tier 3: If latestTag is newer (or nightly), query Atom feed and expanded assets for release notes and APK.
         * Throws only if device has zero connectivity (both API and web fallbacks fail).
         */
        private suspend fun fetchLocked(): AppRelease? {
            val userAgent = "SuperTube/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE})"
            var tier1Exception: Exception? = null

            // Tier 1: Query GitHub API
            try {
                val body =
                    withContext(Dispatchers.IO) {
                        val request =
                            Request
                                .Builder()
                                .url(if (isNightly) NIGHTLY_RELEASE_API else STABLE_RELEASE_API)
                                .header("Accept", GITHUB_JSON)
                                .header("User-Agent", userAgent)
                                .cacheControl(CacheControl.FORCE_NETWORK)
                                .build()
                        client.newCall(request).execute().use { response ->
                            if (response.code == 404) return@withContext null
                            if (response.code == 403) {
                                // GitHub rate limit hit (HTTP 403). Fall through to Tier 2 web fallback.
                                throw IOException("GitHub API rate limit exceeded (HTTP 403)")
                            }
                            if (!response.isSuccessful) throw IOException("Update check failed: HTTP ${response.code}")
                            response.body.string()
                        }
                    }

                if (body != null) {
                    val parsed = parseGitHubRelease(body)
                    val release =
                        if (isNightly) {
                            parsed.toNightlyReleaseIfNewer(BuildConfig.NIGHTLY_RUN)
                        } else {
                            parsed.toAppReleaseIfNewer(BuildConfig.VERSION_NAME, Build.SUPPORTED_ABIS.toList())
                        }
                    dataManager.setLastUpdateCheck(System.currentTimeMillis())
                    _latest.value = release
                    return release
                } else if (!isNightly) {
                    // Release API returned 404: no release published
                    dataManager.setLastUpdateCheck(System.currentTimeMillis())
                    _latest.value = null
                    return null
                }
            } catch (e: Exception) {
                tier1Exception = e
            }

            // Tier 2: GitHub web release redirect (rate-limit immune)
            var tier2Exception: Exception? = null
            try {
                val latestTag = fetchWebLatestTag(userAgent)
                if (!latestTag.isNullOrBlank()) {
                    if (!isNightly && !AppVersions.isNewer(latestTag, BuildConfig.VERSION_NAME)) {
                        // Current version is up to date! Return null cleanly without network error.
                        dataManager.setLastUpdateCheck(System.currentTimeMillis())
                        _latest.value = null
                        return null
                    }

                    // Tier 3: Newer version available (or nightly). Construct genuine AppRelease.
                    val release = fetchWebRelease(latestTag, userAgent)
                    dataManager.setLastUpdateCheck(System.currentTimeMillis())
                    _latest.value = release
                    return release
                }
            } catch (e: Exception) {
                tier2Exception = e
            }

            // Zero connectivity: both GitHub API and web fallbacks failed
            throw (tier2Exception ?: tier1Exception ?: IOException("Failed to check for updates"))
        }

        private suspend fun fetchWebLatestTag(userAgent: String): String? =
            withContext(Dispatchers.IO) {
                val webUrl = if (isNightly) NIGHTLY_RELEASE_WEB else STABLE_RELEASE_WEB
                val noRedirectClient = client.newBuilder().followRedirects(false).build()

                // 1. Try HEAD request to detect redirect
                try {
                    val headRequest =
                        Request.Builder()
                            .url(webUrl)
                            .head()
                            .header("User-Agent", userAgent)
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .build()
                    noRedirectClient.newCall(headRequest).execute().use { response ->
                        if (response.isRedirect || response.code in 300..399) {
                            val location = response.header("Location")
                            val tag = extractTagFromLocation(location)
                            if (!tag.isNullOrBlank()) return@withContext tag
                        } else if (response.isSuccessful && isNightly) {
                            return@withContext NIGHTLY_CHANNEL
                        }
                    }
                } catch (_: Exception) {
                    // Fall through to GET request or Atom feed
                }

                // 2. Try GET request with redirects disabled if HEAD was not supported
                try {
                    val getRequest =
                        Request.Builder()
                            .url(webUrl)
                            .get()
                            .header("User-Agent", userAgent)
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .build()
                    noRedirectClient.newCall(getRequest).execute().use { response ->
                        if (response.isRedirect || response.code in 300..399) {
                            val location = response.header("Location")
                            val tag = extractTagFromLocation(location)
                            if (!tag.isNullOrBlank()) return@withContext tag
                        } else if (response.isSuccessful && isNightly) {
                            return@withContext NIGHTLY_CHANNEL
                        }
                    }
                } catch (_: Exception) {
                    // Fall through to Atom feed
                }

                // 3. Fallback: inspect Atom feed for latest tag
                fetchLatestTagFromAtom(userAgent)
            }

        private fun extractTagFromLocation(location: String?): String? {
            if (location.isNullOrBlank()) return null
            return location
                .substringBefore('?')
                .substringBefore('#')
                .trimEnd('/')
                .substringAfterLast('/')
                .takeIf { it.isNotBlank() }
        }

        private suspend fun fetchLatestTagFromAtom(userAgent: String): String? =
            withContext(Dispatchers.IO) {
                val request =
                    Request.Builder()
                        .url(RELEASES_ATOM_URL)
                        .header("User-Agent", userAgent)
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body.string()
                    val match = Regex("""/releases/tag/([^"/?#\s]+)""").find(body)
                    match?.groupValues?.get(1)?.trim()
                }
            }

        private suspend fun fetchWebRelease(
            latestTag: String,
            userAgent: String,
        ): AppRelease? =
            withContext(Dispatchers.IO) {
                var publishedAt: String? = null
                var notes = ""

                // 1. Fetch release notes and publish date from Atom feed
                try {
                    val atomRequest =
                        Request.Builder()
                            .url(RELEASES_ATOM_URL)
                            .header("User-Agent", userAgent)
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .build()
                    client.newCall(atomRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val xml = response.body.string()
                            val entries = Regex("""<entry\b[^>]*>(.*?)</entry>""", RegexOption.DOT_MATCHES_ALL).findAll(xml)
                            val matchingEntry =
                                entries.firstOrNull { it.value.contains("/releases/tag/$latestTag") }
                                    ?: entries.firstOrNull()
                            if (matchingEntry != null) {
                                val entryXml = matchingEntry.value
                                val updatedMatch = Regex("""<updated>([^<]+)</updated>""").find(entryXml)
                                publishedAt = updatedMatch?.groupValues?.get(1)?.trim()
                                val contentMatch =
                                    Regex("""<content\b[^>]*>(.*?)</content>""", RegexOption.DOT_MATCHES_ALL).find(entryXml)
                                if (contentMatch != null) {
                                    notes = htmlToMarkdown(contentMatch.groupValues[1])
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Atom feed failed; proceed with fallback notes
                }

                // 2. Fetch assets from expanded_assets
                val assets = mutableListOf<GitHubAsset>()
                try {
                    val assetsUrl = "$RELEASES_WEB_BASE/expanded_assets/$latestTag"
                    val assetsRequest =
                        Request.Builder()
                            .url(assetsUrl)
                            .header("User-Agent", userAgent)
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .build()
                    client.newCall(assetsRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val html = response.body.string()
                            val items = Regex("""<li\b[^>]*>(.*?)</li>""", RegexOption.DOT_MATCHES_ALL).findAll(html)
                            for (item in items) {
                                val itemHtml = item.value
                                val linkMatch =
                                    Regex("""href="(/saimway/SuperTube/releases/download/[^/]+/([^"]+\.apk))"""")
                                        .find(itemHtml)
                                if (linkMatch != null) {
                                    val path = linkMatch.groupValues[1]
                                    val apkName = linkMatch.groupValues[2]
                                    val shaMatch = Regex("""sha256:([0-9a-fA-F]{64})""").find(itemHtml)
                                    val digest = shaMatch?.value
                                    val sizeBytes = parseAssetSizeBytes(itemHtml)
                                    assets += GitHubAsset(
                                        name = apkName,
                                        downloadUrl = "https://github.com$path",
                                        size = sizeBytes,
                                        digest = digest,
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // expanded_assets failed; will fall back to constructed assets
                }

                // 3. Fallback assets if none discovered from expanded_assets
                if (assets.isEmpty()) {
                    assets += GitHubAsset(
                        name = "supertube.apk",
                        downloadUrl = "$RELEASES_WEB_BASE/download/$latestTag/supertube.apk",
                    )
                    for (abi in Build.SUPPORTED_ABIS) {
                        assets += GitHubAsset(
                            name = "supertube-$abi.apk",
                            downloadUrl = "$RELEASES_WEB_BASE/download/$latestTag/supertube-$abi.apk",
                        )
                    }
                }

                val gitHubRelease =
                    GitHubRelease(
                        tagName = latestTag,
                        body = notes,
                        htmlUrl = "$RELEASES_WEB_BASE/tag/$latestTag",
                        publishedAt = publishedAt,
                        assets = assets,
                    )

                val release =
                    if (isNightly) {
                        gitHubRelease.toNightlyReleaseIfNewer(BuildConfig.NIGHTLY_RUN)
                    } else {
                        gitHubRelease.toAppReleaseIfNewer(BuildConfig.VERSION_NAME, Build.SUPPORTED_ABIS.toList())
                    }

                // If nightly is not newer, release is null -> return null (up to date)
                if (isNightly && release == null) {
                    return@withContext null
                }

                // If release matched but APK selector found no split, fallback to constructed ReleaseApk
                if (release != null && release.apk == null) {
                    val preferredAbi = Build.SUPPORTED_ABIS.firstOrNull()
                    val fallbackName = if (preferredAbi != null) "supertube-$preferredAbi.apk" else "supertube.apk"
                    val fallbackUrl = "$RELEASES_WEB_BASE/download/$latestTag/$fallbackName"
                    return@withContext release.copy(
                        apk = ReleaseApk(
                            name = fallbackName,
                            url = fallbackUrl,
                            sizeBytes = 0L,
                            sha256 = null,
                        ),
                    )
                }

                release ?: AppRelease(
                    version = AppVersions.normalize(latestTag),
                    tag = latestTag,
                    notes = notes,
                    publishedAt = publishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
                    pageUrl = "$RELEASES_WEB_BASE/tag/$latestTag",
                    apk = assets.firstOrNull()?.let {
                        ReleaseApk(
                            name = it.name,
                            url = it.downloadUrl,
                            sizeBytes = it.size,
                            sha256 = it.digest?.removePrefix("sha256:"),
                        )
                    },
                )
            }

        private fun parseAssetSizeBytes(html: String): Long {
            val match = Regex(""">(\d+(?:\.\d+)?)\s*(MB|KB|B)<""", RegexOption.IGNORE_CASE).find(html) ?: return 0L
            val value = match.groupValues[1].toDoubleOrNull() ?: return 0L
            val unit = match.groupValues[2].uppercase()
            return when (unit) {
                "MB" -> (value * 1024 * 1024).toLong()
                "KB" -> (value * 1024).toLong()
                else -> value.toLong()
            }
        }

        private fun unescapeHtml(text: String): String {
            var result = text
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
            val numericRegex = Regex("""&#(\d+);""")
            result = numericRegex.replace(result) { match ->
                val code = match.groupValues[1].toIntOrNull()
                if (code != null) code.toChar().toString() else match.value
            }
            val hexRegex = Regex("""&#x([0-9a-fA-F]+);""")
            result = hexRegex.replace(result) { match ->
                val code = match.groupValues[1].toIntOrNull(16)
                if (code != null) code.toChar().toString() else match.value
            }
            return result
        }

        private fun htmlToMarkdown(html: String): String {
            val unescaped = unescapeHtml(html)
            var md = unescaped
            md = Regex("""<h1\b[^>]*>(.*?)</h1>""", RegexOption.DOT_MATCHES_ALL).replace(md) { "# ${it.groupValues[1].trim()}\n" }
            md = Regex("""<h2\b[^>]*>(.*?)</h2>""", RegexOption.DOT_MATCHES_ALL).replace(md) { "## ${it.groupValues[1].trim()}\n" }
            md = Regex("""<h3\b[^>]*>(.*?)</h3>""", RegexOption.DOT_MATCHES_ALL).replace(md) { "### ${it.groupValues[1].trim()}\n" }
            md = Regex("""<li\b[^>]*>(.*?)</li>""", RegexOption.DOT_MATCHES_ALL).replace(md) { "- ${it.groupValues[1].trim()}\n" }
            md = Regex("""<p\b[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL).replace(md) { "${it.groupValues[1].trim()}\n\n" }
            md = Regex("""<(?:strong|b)\b[^>]*>(.*?)</(?:strong|b)>""", RegexOption.DOT_MATCHES_ALL).replace(md) { "**${it.groupValues[1]}**" }
            md = Regex("""<(?:em|i)\b[^>]*>(.*?)</(?:em|i)>""", RegexOption.DOT_MATCHES_ALL).replace(md) { "*${it.groupValues[1]}*" }
            md = Regex("""<code\b[^>]*>(.*?)</code>""", RegexOption.DOT_MATCHES_ALL).replace(md) { "`${it.groupValues[1]}`" }
            md = Regex("""<a\s+[^>]*href="([^"]+)"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL).replace(md) { "[${it.groupValues[2]}](${it.groupValues[1]})" }
            md = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE).replace(md, "\n")
            md = Regex("""<hr\s*/?>""", RegexOption.IGNORE_CASE).replace(md, "\n---\n")
            md = Regex("""<[^>]+>""").replace(md, "")
            md = Regex("""\n{3,}""").replace(md, "\n\n")
            return md.trim()
        }
    }
