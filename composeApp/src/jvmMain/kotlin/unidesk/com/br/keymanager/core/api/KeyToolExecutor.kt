package unidesk.com.br.keymanager.core.api
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    internal suspend fun execute(vararg args: String): RawResult = withContext(Dispatchers.IO) {
        val command = listOf(keytoolPath) + args.toList()
        val process = processFactory(command)
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val completed = process.waitFor(60, TimeUnit.SECONDS)
        if (completed) {
            RawResult(stdout, stderr, process.exitValue())
        } else {
            process.destroyForcibly()
            RawResult("", "Process timed out", -1)
        }
    }
}
