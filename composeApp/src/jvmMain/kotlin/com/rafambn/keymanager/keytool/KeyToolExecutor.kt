package com.rafambn.keymanager.keytool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

internal object KeyToolExecutor {
    internal data class RawResult(val stdout: String, val stderr: String, val exitCode: Int)

    private val keytoolPath: String by lazy {
        val javaHome = System.getProperty("java.home")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val executable = if (isWindows) "keytool.exe" else "keytool"
        "$javaHome${File.separator}bin${File.separator}$executable"
    }

    internal var processFactory: (List<String>) -> Process = { command ->
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(false)

        val environment = processBuilder.environment()
        environment["LANG"] = "en_US.UTF-8"
        environment["LC_ALL"] = "en_US.UTF-8"
        processBuilder.start()
    }

    internal suspend fun execute(vararg args: String, stdin: String? = null): RawResult = withContext(Dispatchers.IO) {
        val command = listOf(keytoolPath) + args.toList()
        val process = processFactory(command)

        if (stdin != null) {
            process.outputStream.bufferedWriter().use { it.write(stdin) }
        } else {
            process.outputStream.close()
        }

        val stdoutDeferred = async { process.inputStream.bufferedReader().use { it.readText() } }
        val stderrDeferred = async { process.errorStream.bufferedReader().use { it.readText() } }

        val result = withTimeoutOrNull(60_000L) {
            val exitCode = process.waitFor()
            val stdout = stdoutDeferred.await()
            val stderr = stderrDeferred.await()
            RawResult(stdout, stderr, exitCode)
        }

        if (result != null) {
            result
        } else {
            process.destroyForcibly()
            stdoutDeferred.cancel()
            stderrDeferred.cancel()
            RawResult("", "Process timed out", -1)
        }
    }
}
