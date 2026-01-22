package unidesk.com.br.keymanager.core

import java.io.File
import java.util.concurrent.TimeUnit

// Generic result for keytool operations
sealed class KeytoolResult<out T> {
    data class Success<T>(val data: T) : KeytoolResult<T>()
    data class Error(val message: String, val exitCode: Int) : KeytoolResult<Nothing>()
}

// Entry types in a keystore
enum class EntryType {
    PRIVATE_KEY,      // PrivateKeyEntry
    TRUSTED_CERT,     // trustedCertEntry
    SECRET_KEY,       // SecretKeyEntry
    UNKNOWN
}

// Single keystore entry
data class KeystoreEntry(
    val alias: String,
    val creationDate: String,
    val entryType: EntryType,
    val certificateChainLength: Int?,
    val owner: String?,
    val issuer: String?,
    val algorithm: String?,
    val serialNumber: String?,
    val validFrom: String?,
    val validUntil: String?,
    val fingerprint: String?
)

// Keystore listing result
data class KeystoreInfo(
    val type: String,
    val provider: String,
    val entryCount: Int,
    val entries: List<KeystoreEntry>
)

// Certificate info (for printCert)
data class CertificateInfo(
    val owner: String,
    val issuer: String,
    val serialNumber: String,
    val validFrom: String,
    val validUntil: String,
    val algorithm: String,
    val fingerprints: Map<String, String>
)

// CSR info (for printCertReq)
data class CertRequestInfo(
    val subject: String,
    val algorithm: String,
    val extensions: List<String>
)

// CRL info (for printCrl)
data class CrlInfo(
    val issuer: String,
    val thisUpdate: String,
    val nextUpdate: String?,
    val revokedCertificates: List<String>
)

object KeyToolAPI {

    private data class RawResult(val stdout: String, val stderr: String, val exitCode: Int)

    private fun getKeytoolPath(): String {
        val javaHome = System.getProperty("java.home")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val executable = if (isWindows) "keytool.exe" else "keytool"
        return "$javaHome${File.separator}bin${File.separator}$executable"
    }

    private fun execute(vararg args: String): RawResult {
        val command = listOf(getKeytoolPath()) + args.toList()
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(false)

        // Force English locale for consistent keytool output
        val environment = processBuilder.environment()
        environment["LANG"] = "en_US.UTF-8"
        environment["LC_ALL"] = "en_US.UTF-8"

        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val completed = process.waitFor(60, TimeUnit.SECONDS)

        return if (completed) {
            RawResult(stdout, stderr, process.exitValue())
        } else {
            process.destroyForcibly()
            RawResult("", "Process timed out", -1)
        }
    }

    // ===== 1. LIST → KeystoreInfo =====
    fun list(
        keystore: File,
        storepass: String,
        verbose: Boolean = true
    ): KeytoolResult<KeystoreInfo> {
        val args = mutableListOf("-list", "-keystore", keystore.absolutePath, "-storepass", storepass)
        if (verbose) args.add("-v")

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(parseListVerboseOutput(result.stdout))
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 2. GENKEYPAIR → Unit =====
    fun genKeyPair(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String,
        dname: String,
        validity: Int,
        keyalg: String = "RSA",
        keysize: Int = 2048,
        sigalg: String? = null
    ): KeytoolResult<Unit> {
        val args = mutableListOf(
            "-genkeypair",
            "-alias", alias,
            "-dname", dname,
            "-validity", validity.toString(),
            "-keyalg", keyalg,
            "-keysize", keysize.toString(),
            "-keystore", keystore.absolutePath,
            "-storepass", storepass,
            "-keypass", keypass
        )
        if (sigalg != null) {
            args.addAll(listOf("-sigalg", sigalg))
        }

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 3. GENSECKEY → Unit =====
    fun genSecKey(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String,
        keyalg: String,
        keysize: Int
    ): KeytoolResult<Unit> {
        val args = listOf(
            "-genseckey",
            "-alias", alias,
            "-keyalg", keyalg,
            "-keysize", keysize.toString(),
            "-keystore", keystore.absolutePath,
            "-storepass", storepass,
            "-keypass", keypass
        )

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 4. GENCERT → Unit =====
    fun genCert(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String?,
        infile: File,
        outfile: File,
        validity: Int? = null,
        sigalg: String? = null
    ): KeytoolResult<Unit> {
        val args = mutableListOf(
            "-gencert",
            "-alias", alias,
            "-infile", infile.absolutePath,
            "-outfile", outfile.absolutePath,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )
        if (keypass != null) {
            args.addAll(listOf("-keypass", keypass))
        }
        if (validity != null) {
            args.addAll(listOf("-validity", validity.toString()))
        }
        if (sigalg != null) {
            args.addAll(listOf("-sigalg", sigalg))
        }

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 5. CERTREQ → Unit =====
    fun certReq(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String?,
        file: File,
        sigalg: String? = null
    ): KeytoolResult<Unit> {
        val args = mutableListOf(
            "-certreq",
            "-alias", alias,
            "-file", file.absolutePath,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )
        if (keypass != null) {
            args.addAll(listOf("-keypass", keypass))
        }
        if (sigalg != null) {
            args.addAll(listOf("-sigalg", sigalg))
        }

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 6. EXPORTCERT → Unit =====
    fun exportCert(
        keystore: File,
        storepass: String,
        alias: String,
        file: File,
        rfc: Boolean = false
    ): KeytoolResult<Unit> {
        val args = mutableListOf(
            "-exportcert",
            "-alias", alias,
            "-file", file.absolutePath,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )
        if (rfc) {
            args.add("-rfc")
        }

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 7. IMPORTCERT → Unit =====
    fun importCert(
        keystore: File,
        storepass: String,
        alias: String,
        file: File,
        keypass: String? = null,
        trustcacerts: Boolean = false,
        noprompt: Boolean = true
    ): KeytoolResult<Unit> {
        val args = mutableListOf(
            "-importcert",
            "-alias", alias,
            "-file", file.absolutePath,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )
        if (keypass != null) {
            args.addAll(listOf("-keypass", keypass))
        }
        if (trustcacerts) {
            args.add("-trustcacerts")
        }
        if (noprompt) {
            args.add("-noprompt")
        }

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 8. IMPORTKEYSTORE → Unit =====
    fun importKeystore(
        srcKeystore: File,
        srcStorepass: String,
        srcAlias: String?,
        srcKeypass: String?,
        destKeystore: File,
        destStorepass: String,
        destAlias: String? = null,
        destKeypass: String? = null,
        noprompt: Boolean = true
    ): KeytoolResult<Unit> {
        val args = mutableListOf(
            "-importkeystore",
            "-srckeystore", srcKeystore.absolutePath,
            "-srcstorepass", srcStorepass,
            "-destkeystore", destKeystore.absolutePath,
            "-deststorepass", destStorepass
        )
        if (srcAlias != null) {
            args.addAll(listOf("-srcalias", srcAlias))
        }
        if (srcKeypass != null) {
            args.addAll(listOf("-srckeypass", srcKeypass))
        }
        if (destAlias != null) {
            args.addAll(listOf("-destalias", destAlias))
        }
        if (destKeypass != null) {
            args.addAll(listOf("-destkeypass", destKeypass))
        }
        if (noprompt) {
            args.add("-noprompt")
        }

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 9. IMPORTPASS → Unit =====
    fun importPass(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String
    ): KeytoolResult<Unit> {
        val args = listOf(
            "-importpass",
            "-alias", alias,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass,
            "-keypass", keypass
        )

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 10. DELETE → Unit =====
    fun delete(
        keystore: File,
        storepass: String,
        alias: String
    ): KeytoolResult<Unit> {
        val args = listOf(
            "-delete",
            "-alias", alias,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 11. CHANGEALIAS → Unit =====
    fun changeAlias(
        keystore: File,
        storepass: String,
        alias: String,
        destalias: String,
        keypass: String?
    ): KeytoolResult<Unit> {
        val args = mutableListOf(
            "-changealias",
            "-alias", alias,
            "-destalias", destalias,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )
        if (keypass != null) {
            args.addAll(listOf("-keypass", keypass))
        }

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 12. KEYPASSWD → Unit =====
    fun keyPasswd(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String,
        newKeypass: String
    ): KeytoolResult<Unit> {
        val args = listOf(
            "-keypasswd",
            "-alias", alias,
            "-keypass", keypass,
            "-new", newKeypass,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 13. STOREPASSWD → Unit =====
    fun storePasswd(
        keystore: File,
        storepass: String,
        newStorepass: String
    ): KeytoolResult<Unit> {
        val args = listOf(
            "-storepasswd",
            "-new", newStorepass,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 14. PRINTCERT → CertificateInfo =====
    fun printCert(
        file: File,
        verbose: Boolean = true
    ): KeytoolResult<CertificateInfo> {
        val args = mutableListOf("-printcert", "-file", file.absolutePath)
        if (verbose) args.add("-v")

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(parseCertificateOutput(result.stdout))
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 15. PRINTCERTREQ → CertRequestInfo =====
    fun printCertReq(
        file: File,
        verbose: Boolean = true
    ): KeytoolResult<CertRequestInfo> {
        val args = mutableListOf("-printcertreq", "-file", file.absolutePath)
        if (verbose) args.add("-v")

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(parseCertReqOutput(result.stdout))
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 16. PRINTCRL → CrlInfo =====
    fun printCrl(
        file: File,
        verbose: Boolean = true
    ): KeytoolResult<CrlInfo> {
        val args = mutableListOf("-printcrl", "-file", file.absolutePath)
        if (verbose) args.add("-v")

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(parseCrlOutput(result.stdout))
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== PRIVATE PARSING HELPERS =====

    private fun parseListVerboseOutput(output: String): KeystoreInfo {
        val lines = output.lines()

        // Parse header info
        var type = "Unknown"
        var provider = "Unknown"
        var entryCount = 0

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Keystore type:") -> {
                    type = trimmed.substringAfter("Keystore type:").trim()
                }
                trimmed.startsWith("Keystore provider:") -> {
                    provider = trimmed.substringAfter("Keystore provider:").trim()
                }
                trimmed.contains("keystore contains") && trimmed.contains("entr") -> {
                    val match = Regex("(\\d+)\\s+entr").find(trimmed)
                    entryCount = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
            }
        }

        // Split by "Alias name:" to get individual entries
        val entryBlocks = output.split(Regex("(?=Alias name:)")).filter { it.contains("Alias name:") }
        val entries = entryBlocks.map { parseKeystoreEntry(it) }

        // Log warning if entry count mismatch
        if (entries.size != entryCount && entryCount > 0) {
            System.err.println("Warning: Expected $entryCount entries but parsed ${entries.size}")
        }

        // Log if any entries have missing critical fields
        entries.forEach { entry ->
            if (entry.alias.isBlank()) {
                System.err.println("Warning: Parsed entry with blank alias")
            }
            if (entry.entryType == EntryType.UNKNOWN) {
                System.err.println("Warning: Could not determine entry type for alias '${entry.alias}'")
            }
        }

        return KeystoreInfo(type, provider, entryCount, entries)
    }

    private fun parseKeystoreEntry(block: String): KeystoreEntry {
        val lines = block.lines()

        var alias = ""
        var creationDate = ""
        var entryType = EntryType.UNKNOWN
        var chainLength: Int? = null
        var owner: String? = null
        var issuer: String? = null
        var keyAlgorithm: String? = null  // Subject Public Key Algorithm (RSA, EC, etc.)
        var serialNumber: String? = null
        var validFrom: String? = null
        var validUntil: String? = null
        var fingerprint: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Alias name:") -> {
                    alias = trimmed.substringAfter("Alias name:").trim()
                }
                trimmed.startsWith("Creation date:") -> {
                    creationDate = trimmed.substringAfter("Creation date:").trim()
                }
                trimmed.startsWith("Entry type:") -> {
                    val typeStr = trimmed.substringAfter("Entry type:").trim()
                    entryType = when {
                        typeStr.contains("PrivateKey", ignoreCase = true) -> EntryType.PRIVATE_KEY
                        typeStr.contains("trustedCert", ignoreCase = true) -> EntryType.TRUSTED_CERT
                        typeStr.contains("SecretKey", ignoreCase = true) -> EntryType.SECRET_KEY
                        else -> EntryType.UNKNOWN
                    }
                }
                trimmed.startsWith("Certificate chain length:") -> {
                    chainLength = trimmed.substringAfter("Certificate chain length:").trim().toIntOrNull()
                }
                trimmed.startsWith("Owner:") -> {
                    owner = trimmed.substringAfter("Owner:").trim()
                }
                trimmed.startsWith("Issuer:") -> {
                    issuer = trimmed.substringAfter("Issuer:").trim()
                }
                trimmed.startsWith("Serial number:") -> {
                    serialNumber = trimmed.substringAfter("Serial number:").trim()
                }
                trimmed.startsWith("Valid from:") -> {
                    // Format: "Valid from: Mon Jan 22 10:00:00 BRT 2026 until: Mon Jan 22 10:00:00 BRT 2027"
                    val parts = trimmed.substringAfter("Valid from:").split("until:")
                    validFrom = parts.getOrNull(0)?.trim()
                    validUntil = parts.getOrNull(1)?.trim()
                }
                trimmed.contains("SHA256:") || trimmed.contains("SHA-256:") -> {
                    fingerprint = trimmed.substringAfter(":").trim()
                }
                // Extract the key algorithm from "Subject Public Key Algorithm: 2048-bit RSA key"
                trimmed.startsWith("Subject Public Key Algorithm:") -> {
                    val algStr = trimmed.substringAfter("Subject Public Key Algorithm:").trim()
                    // Extract algorithm type (RSA, EC, DSA, etc.) from strings like "2048-bit RSA key" or "256-bit EC key"
                    keyAlgorithm = when {
                        algStr.contains("RSA", ignoreCase = true) -> "RSA"
                        algStr.contains("EC", ignoreCase = true) -> "EC"
                        algStr.contains("DSA", ignoreCase = true) -> "DSA"
                        else -> algStr.substringBefore(" ").trim().ifBlank { null }
                    }
                }
            }
        }

        return KeystoreEntry(
            alias = alias,
            creationDate = creationDate,
            entryType = entryType,
            certificateChainLength = chainLength,
            owner = owner,
            issuer = issuer,
            algorithm = keyAlgorithm,
            serialNumber = serialNumber,
            validFrom = validFrom,
            validUntil = validUntil,
            fingerprint = fingerprint
        )
    }

    private fun parseCertificateOutput(output: String): CertificateInfo {
        val lines = output.lines()

        var owner = ""
        var issuer = ""
        var serialNumber = ""
        var validFrom = ""
        var validUntil = ""
        var algorithm = ""
        val fingerprints = mutableMapOf<String, String>()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Owner:") -> {
                    owner = trimmed.substringAfter("Owner:").trim()
                }
                trimmed.startsWith("Issuer:") -> {
                    issuer = trimmed.substringAfter("Issuer:").trim()
                }
                trimmed.startsWith("Serial number:") -> {
                    serialNumber = trimmed.substringAfter("Serial number:").trim()
                }
                trimmed.startsWith("Valid from:") -> {
                    val parts = trimmed.substringAfter("Valid from:").split("until:")
                    validFrom = parts.getOrNull(0)?.trim() ?: ""
                    validUntil = parts.getOrNull(1)?.trim() ?: ""
                }
                trimmed.startsWith("Signature algorithm name:") -> {
                    algorithm = trimmed.substringAfter("Signature algorithm name:").trim()
                }
                trimmed.contains("SHA256:") || trimmed.contains("SHA-256:") -> {
                    fingerprints["SHA-256"] = trimmed.substringAfter(":").trim()
                }
                trimmed.contains("SHA1:") || trimmed.contains("SHA-1:") -> {
                    fingerprints["SHA-1"] = trimmed.substringAfter(":").trim()
                }
                trimmed.contains("MD5:") -> {
                    fingerprints["MD5"] = trimmed.substringAfter(":").trim()
                }
            }
        }

        return CertificateInfo(owner, issuer, serialNumber, validFrom, validUntil, algorithm, fingerprints)
    }

    private fun parseCertReqOutput(output: String): CertRequestInfo {
        val lines = output.lines()

        var subject = ""
        var algorithm = ""
        val extensions = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Subject:") -> {
                    subject = trimmed.substringAfter("Subject:").trim()
                }
                trimmed.startsWith("Signature algorithm:") || trimmed.startsWith("Signature Algorithm:") -> {
                    algorithm = trimmed.substringAfter(":").trim()
                }
                trimmed.startsWith("Extension:") || trimmed.contains("OID:") -> {
                    extensions.add(trimmed)
                }
            }
        }

        return CertRequestInfo(subject, algorithm, extensions)
    }

    private fun parseCrlOutput(output: String): CrlInfo {
        val lines = output.lines()

        var issuer = ""
        var thisUpdate = ""
        var nextUpdate: String? = null
        val revokedCerts = mutableListOf<String>()

        var inRevokedSection = false

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Issuer:") -> {
                    issuer = trimmed.substringAfter("Issuer:").trim()
                }
                trimmed.startsWith("This Update:") || trimmed.startsWith("ThisUpdate:") -> {
                    thisUpdate = trimmed.substringAfter(":").trim()
                }
                trimmed.startsWith("Next Update:") || trimmed.startsWith("NextUpdate:") -> {
                    nextUpdate = trimmed.substringAfter(":").trim()
                }
                trimmed.contains("Revoked Certificates:") -> {
                    inRevokedSection = true
                }
                inRevokedSection && trimmed.startsWith("Serial Number:") -> {
                    revokedCerts.add(trimmed.substringAfter("Serial Number:").trim())
                }
            }
        }

        return CrlInfo(issuer, thisUpdate, nextUpdate, revokedCerts)
    }
}
