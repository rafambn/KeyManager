package unidesk.com.br.keymanager.keytool

import unidesk.com.br.keymanager.keytool.enums.ECCurve
import unidesk.com.br.keymanager.keytool.enums.KeyAlgorithm
import unidesk.com.br.keymanager.keytool.enums.SignatureAlgorithm
import unidesk.com.br.keymanager.keytool.model.CertRequestInfo
import unidesk.com.br.keymanager.keytool.model.CertificateInfo
import unidesk.com.br.keymanager.keytool.model.CrlInfo
import unidesk.com.br.keymanager.keytool.model.KeystoreInfo
import java.io.File

object KeyToolAPI {

    // ===== 1. LIST → KeystoreInfo =====
    suspend fun list(
        keystore: File,
        storepass: String,
        verbose: Boolean = true
    ): KeytoolResult<KeystoreInfo> {
        val args = mutableListOf("-list", "-keystore", keystore.absolutePath, "-storepass", storepass)
        if (verbose) args.add("-v")
        val result = KeyToolExecutor.execute(*args.toTypedArray())
        return if (result.exitCode == 0) {
            try {
                val parsed = KeyToolParser.parseListVerboseOutput(result.stdout)
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

        val finalKeySize = keySize ?: keyAlgorithm.defaultKeySize

        if (!keyAlgorithm.isValidKeySize(finalKeySize)) {
            return KeytoolResult.Error(
                "Key size $finalKeySize is not valid for ${keyAlgorithm.displayName} (valid range: ${keyAlgorithm.supportedKeySizes})",
                -1
            )
        }

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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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

        val finalKeySize = keySize ?: keyAlgorithm.defaultKeySize

        if (!keyAlgorithm.isValidKeySize(finalKeySize)) {
            return KeytoolResult.Error(
                "Key size $finalKeySize is not valid for ${keyAlgorithm.displayName} (valid range: ${keyAlgorithm.supportedKeySizes})",
                -1
            )
        }

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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
        return if (result.exitCode == 0) {
            try {
                val parsed = KeyToolParser.parseCertificateOutput(result.stdout)
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
        return if (result.exitCode == 0) {
            try {
                val parsed = KeyToolParser.parseCertReqOutput(result.stdout)
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
        val result = KeyToolExecutor.execute(*args.toTypedArray())
        return if (result.exitCode == 0) {
            try {
                val parsed = KeyToolParser.parseCrlOutput(result.stdout)
                KeytoolResult.Success(parsed)
            } catch (e: Exception) {
                KeytoolResult.Error("Failed to parse CRL output: ${e.message}", -1)
            }
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }
}
