package com.banner.logs.reader

import com.banner.logs.data.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.coroutines.coroutineContext

object LogcatReader {

    fun stream(): Flow<LogEntry> = flow {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "logcat -v threadtime"))
        val reader = BufferedReader(InputStreamReader(process.inputStream), 16 * 1024)
        var lastEntry: LogEntry? = null
        try {
            while (coroutineContext.isActive) {
                val line = reader.readLine() ?: break
                if (line.isBlank() || line.startsWith("---------")) continue
                val entry = LogEntry.parse(line)
                if (entry != null) {
                    lastEntry = entry
                    emit(entry)
                } else if (lastEntry != null) {
                    // Continuation line (stack trace, multi-line message)
                    val cont = lastEntry.copy(
                        id = LogEntry.nextId(),
                        message = "    $line",
                        raw = line
                    )
                    emit(cont)
                }
            }
        } finally {
            reader.close()
            try { process.destroy() } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Reads /proc/<pid>/cmdline directly — world-readable on Android, no root needed.
     * This avoids spawning a new `su` process on every refresh (which triggers SU popups).
     */
    fun getPidMap(): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        try {
            File("/proc").listFiles { f ->
                f.isDirectory && f.name.all { it.isDigit() }
            }?.forEach { pidDir ->
                val pid = pidDir.name.toIntOrNull() ?: return@forEach
                val cmdlineFile = File(pidDir, "cmdline")
                if (!cmdlineFile.canRead()) return@forEach
                val raw = cmdlineFile.readBytes()
                // cmdline is null-delimited; first segment is the process/package name
                val name = raw.takeWhile { it != 0.toByte() }
                    .toByteArray()
                    .decodeToString()
                    .trim()
                if (name.isNotEmpty()) map[pid] = name
            }
        } catch (_: Exception) {}
        return map
    }

    suspend fun checkRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val result = process.inputStream.bufferedReader().readLine() ?: ""
            process.waitFor()
            result.contains("uid=0")
        } catch (_: Exception) {
            false
        }
    }
}
