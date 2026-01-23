package unidesk.com.br.keymanager.core

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object KeyToolAPI {

    private data class RawResult(val stdout: String, val stderr: String, val exitCode: Int)

    private val keytoolPath: String by lazy {
        val javaHome = System.getProperty("java.home")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val executable = if (isWindows) "keytool.exe" else "keytool"
        "$javaHome${File.separator}bin${File.separator}$executable"
    }

    // For testing purposes - can be overridden to inject mock processes
    internal var processFactory: (List<String>) -> Process = { command ->
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(false)

        // Force English locale for consistent keytool output
        val environment = processBuilder.environment()
        environment["LANG"] = "en_US.UTF-8"
        environment["LC_ALL"] = "en_US.UTF-8"

        processBuilder.start()
    }

    private suspend fun execute(vararg args: String): RawResult = withContext(Dispatchers.IO) {
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

    // ===== 1. LIST → KeystoreInfo =====
    suspend fun list(
        keystore: File,
        storepass: String,
        verbose: Boolean = true
    ): KeytoolResult<KeystoreInfo> {
        val args = mutableListOf("-list", "-keystore", keystore.absolutePath, "-storepass", storepass)
        if (verbose) args.add("-v")

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            try {
                val parsed = parseListVerboseOutput(result.stdout)
                KeytoolResult.Success(parsed)
            } catch (e: Exception) {
                KeytoolResult.Error("Failed to parse keystore list output: ${e.message}", -1)
            }
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 2. GENKEYPAIR → Unit =====
    suspend fun genKeyPair(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String,
        dname: String,
        validity: Int,
        keyAlgorithm: KeyAlgorithm = KeyAlgorithm.RSA,
        keySize: Int? = null,
        signatureAlgorithm: SignatureAlgorithm? = null,
        ecCurve: ECCurve? = null
    ): KeytoolResult<Unit> {
        // Determine final key size
        val finalKeySize = keySize ?: keyAlgorithm.defaultKeySize

        // Validate key size for algorithm
        if (!keyAlgorithm.isValidKeySize(finalKeySize)) {
            return KeytoolResult.Error(
                "Key size $finalKeySize is not valid for ${keyAlgorithm.displayName} (valid range: ${keyAlgorithm.supportedKeySizes})",
                -1
            )
        }

        // Auto-select signature algorithm if not provided
        val finalSigAlg = signatureAlgorithm ?: SignatureAlgorithm.selectDefault(keyAlgorithm, finalKeySize)

        val args = mutableListOf(
            "-genkeypair",
            "-alias", alias,
            "-dname", dname,
            "-validity", validity.toString(),
            "-keyalg", keyAlgorithm.cliName,
            "-keysize", finalKeySize.toString(),
            "-sigalg", finalSigAlg.cliName,
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

    // ===== 3. GENSECKEY → Unit =====
    suspend fun genSecKey(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String,
        keyAlgorithm: KeyAlgorithm,
        keySize: Int? = null
    ): KeytoolResult<Unit> {
        // Determine final key size
        val finalKeySize = keySize ?: keyAlgorithm.defaultKeySize

        // Validate key size for algorithm
        if (!keyAlgorithm.isValidKeySize(finalKeySize)) {
            return KeytoolResult.Error(
                "Key size $finalKeySize is not valid for ${keyAlgorithm.displayName} (valid range: ${keyAlgorithm.supportedKeySizes})",
                -1
            )
        }

        // Validate algorithm supports symmetric keys
        if (keyAlgorithm !in listOf(KeyAlgorithm.AES, KeyAlgorithm.TRIPLE_DES)) {
            return KeytoolResult.Error(
                "${keyAlgorithm.displayName} is not a symmetric key algorithm (valid: AES, TripleDES)",
                -1
            )
        }

        val args = listOf(
            "-genseckey",
            "-alias", alias,
            "-keyalg", keyAlgorithm.cliName,
            "-keysize", finalKeySize.toString(),
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
    suspend fun genCert(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String?,
        infile: File,
        outfile: File,
        validity: Int? = null,
        signatureAlgorithm: SignatureAlgorithm? = null
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
        if (signatureAlgorithm != null) {
            args.addAll(listOf("-sigalg", signatureAlgorithm.cliName))
        }

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 5. CERTREQ → Unit =====
    suspend fun certReq(
        keystore: File,
        storepass: String,
        alias: String,
        keypass: String?,
        file: File,
        signatureAlgorithm: SignatureAlgorithm? = null
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
        if (signatureAlgorithm != null) {
            args.addAll(listOf("-sigalg", signatureAlgorithm.cliName))
        }

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 6. EXPORTCERT → Unit =====
    suspend fun exportCert(
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
    suspend fun importCert(
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
    suspend fun importKeystore(
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
    suspend fun importPass(
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
    suspend fun delete(
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
    suspend fun changeAlias(
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
    suspend fun keyPasswd(
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
    suspend fun storePasswd(
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
    suspend fun printCert(
        file: File,
        verbose: Boolean = true
    ): KeytoolResult<CertificateInfo> {
        val args = mutableListOf("-printcert", "-file", file.absolutePath)
        if (verbose) args.add("-v")

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            try {
                val parsed = parseCertificateOutput(result.stdout)
                KeytoolResult.Success(parsed)
            } catch (e: Exception) {
                KeytoolResult.Error("Failed to parse certificate output: ${e.message}", -1)
            }
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 15. PRINTCERTREQ → CertRequestInfo =====
    suspend fun printCertReq(
        file: File,
        verbose: Boolean = true
    ): KeytoolResult<CertRequestInfo> {
        val args = mutableListOf("-printcertreq", "-file", file.absolutePath)
        if (verbose) args.add("-v")

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            try {
                val parsed = parseCertReqOutput(result.stdout)
                KeytoolResult.Success(parsed)
            } catch (e: Exception) {
                KeytoolResult.Error("Failed to parse certificate request output: ${e.message}", -1)
            }
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 16. PRINTCRL → CrlInfo =====
    suspend fun printCrl(
        file: File,
        verbose: Boolean = true
    ): KeytoolResult<CrlInfo> {
        val args = mutableListOf("-printcrl", "-file", file.absolutePath)
        if (verbose) args.add("-v")

        val result = execute(*args.toTypedArray())

        return if (result.exitCode == 0) {
            try {
                val parsed = parseCrlOutput(result.stdout)
                KeytoolResult.Success(parsed)
            } catch (e: Exception) {
                KeytoolResult.Error("Failed to parse CRL output: ${e.message}", -1)
            }
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

        // Validate keystore has critical information
        if (type.isEmpty() || type == "Unknown") {
            throw IllegalArgumentException("Could not determine keystore type from output")
        }

        // Log if any entries have missing critical fields
        entries.forEach { entry ->
            if (entry.alias.isBlank()) {
                throw IllegalArgumentException("Parsed entry with blank alias")
            }
            if (entry.entryType == EntryType.UNKNOWN) {
                throw IllegalArgumentException("Could not determine entry type for alias '${entry.alias}'")
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
                trimmed.contains("SHA512:") || trimmed.contains("SHA-512:") -> {
                    fingerprints["SHA-512"] = trimmed.substringAfter(":").trim()
                }
                trimmed.contains("SHA1:") || trimmed.contains("SHA-1:") -> {
                    fingerprints["SHA-1"] = trimmed.substringAfter(":").trim()
                }
                trimmed.contains("MD5:") -> {
                    fingerprints["MD5"] = trimmed.substringAfter(":").trim()
                }
            }
        }

        // Validate critical fields are present
        if (owner.isEmpty() || issuer.isEmpty() || serialNumber.isEmpty()) {
            throw IllegalArgumentException("Missing critical certificate fields: owner=$owner, issuer=$issuer, serialNumber=$serialNumber")
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

        // Validate critical fields are present
        if (subject.isEmpty() || algorithm.isEmpty()) {
            throw IllegalArgumentException("Missing critical certificate request fields: subject=$subject, algorithm=$algorithm")
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

        // Validate critical fields are present
        if (issuer.isEmpty() || thisUpdate.isEmpty()) {
            throw IllegalArgumentException("Missing critical CRL fields: issuer=$issuer, thisUpdate=$thisUpdate")
        }

        return CrlInfo(issuer, thisUpdate, nextUpdate, revokedCerts)
    }
}
