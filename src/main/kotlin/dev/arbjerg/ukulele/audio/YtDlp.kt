package dev.arbjerg.ukulele.audio

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit

data class Download(val file: Path, val title: String, val url: String)

@Component
class YtDlp {
    private val log = LoggerFactory.getLogger(YtDlp::class.java)
    private val dir: Path = Path.of(System.getProperty("java.io.tmpdir"), "ytdlp")

    init {
        try {
            Files.createDirectories(dir)
            Files.list(dir).use { files ->
                files.filter { Files.isRegularFile(it) }.forEach { Files.deleteIfExists(it) }
            }
        } catch (e: Exception) {
            log.warn("Could not clear {}", dir, e)
        }
    }

    private val youtubeUrl = Regex("^https?://([a-z0-9-]+\\.)?(youtube\\.com|youtu\\.be)/", RegexOption.IGNORE_CASE)

    fun isYoutubeUrl(url: String) = youtubeUrl.containsMatchIn(url)

    suspend fun downloadFromYoutube(target: String): Download = withContext(Dispatchers.IO) {
        Files.createDirectories(dir)

        val command = listOf(
            "yt-dlp",
            "--no-playlist",
            "-f", "bestaudio[ext=webm]/bestaudio",
            "-o", "$dir/%(id)s-${UUID.randomUUID()}.%(ext)s",
            "--encoding", "utf-8",
            "--print", "after_move:%(title)s\t%(filepath)s",
            "--no-simulate",
            "--max-downloads 1",
            "--", target
        )
        log.info("Running yt-dlp for {}", target)

        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).readLines()

        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            throw IllegalStateException("yt-dlp timed out")
        }

        val lastLine = output.lastOrNull { it.isNotBlank() }?.trim()

        val tab = lastLine?.lastIndexOf('\t') ?: -1
        val file = if (process.exitValue() == 0 && lastLine != null && tab >= 0) {
            runCatching { Path.of(lastLine.substring(tab + 1)) }.getOrNull()
        } else null

        if (file == null || !Files.exists(file)) {
            log.error("yt-dlp failed (exit {}):\n{}", process.exitValue(), output.joinToString("\n"))
            throw IllegalStateException("yt-dlp failed: ${lastLine ?: "no output"}")
        }
        Download(file, lastLine!!.substring(0, tab), target)
    }

    fun deleteIfDownloaded(track: AudioTrack) {
        if (track.sourceManager.sourceName != "local") return

        try {
            val path = Path.of(track.info.identifier)
            if (path.startsWith(dir)) Files.deleteIfExists(path)
        } catch (e: Exception) {
            log.warn("Could not delete downloaded file for {}", track.info.identifier, e)
        }
    }
}