package com.rafambn.keymanager.keytool

import com.rafambn.keymanager.keytool.enums.ECCurve
import com.rafambn.keymanager.keytool.enums.KeyAlgorithm
import com.rafambn.keymanager.keytool.enums.SignatureAlgorithm
import com.rafambn.keymanager.keytool.model.CertRequestInfo
import com.rafambn.keymanager.keytool.model.CertificateInfo
import com.rafambn.keymanager.keytool.model.CrlInfo
import com.rafambn.keymanager.keytool.model.KeystoreInfo
import com.rafambn.keymanager.keytool.model.TlsInfo
import java.io.File

object KeyToolAPI {

    private fun buildKeystoreArgs(keystore: File, storepass: String, cacerts: Boolean): List<String> {
        return if (cacerts) {
            listOf("-cacerts", "-storepass", storepass)
        } else {
            listOf("-keystore", keystore.absolutePath, "-storepass", storepass)
        }
    }

    // ===== 1. LIST → KeystoreInfo =====
    suspend fun list(
        keystore: File,
        storepass: String,
        verbose: Boolean = true,
        alias: String? = null,
        storetype: String? = null,
        rfc: Boolean = false,
        cacerts: Boolean = false
    ): KeytoolResult<KeystoreInfo> {
        val args = mutableListOf("-list")
        args.addAll(buildKeystoreArgs(keystore, storepass, cacerts))
        if (rfc) {
            args.add("-rfc")
        } else if (verbose) {
            args.add("-v")
        }
        if (alias != null) args.addAll(listOf("-alias", alias))
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
        val result = KeyToolExecutor.execute(*args.toTypedArray())
        return if (result.exitCode == 0) {
            try {
                val parsed = KeyToolParser.parseListOutput(result.stdout, verbose && !rfc)
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
        ecCurve: ECCurve? = null,
        ext: List<String> = emptyList(),
        startdate: String? = null,
        storetype: String? = null,
        signer: String? = null,
        signerKeypass: String? = null
    ): KeytoolResult<Unit> {

        val args = mutableListOf(
            "-genkeypair",
            "-alias", alias,
            "-dname", dname,
            "-validity", validity.toString(),
            "-keyalg", keyAlgorithm.cliName,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass,
            "-keypass", keypass
        )

        if (ecCurve != null) {
            args.addAll(listOf("-groupname", ecCurve.cliName))
        } else if (keyAlgorithm.defaultKeySize != -1 &&
            keyAlgorithm.supportedKeySizes.first != keyAlgorithm.supportedKeySizes.last
        ) {
            val finalKeySize = keySize ?: keyAlgorithm.defaultKeySize
            if (!keyAlgorithm.isValidKeySize(finalKeySize)) {
                return KeytoolResult.Error(
                    "Key size $finalKeySize is not valid for ${keyAlgorithm.displayName} (valid range: ${keyAlgorithm.supportedKeySizes})",
                    -1
                )
            }
            args.addAll(listOf("-keysize", finalKeySize.toString()))
        }

        val effectiveKeySize = ecCurve?.bitLength ?: keySize ?: keyAlgorithm.defaultKeySize
        val finalSigAlg = signatureAlgorithm ?: SignatureAlgorithm.selectDefault(keyAlgorithm, effectiveKeySize)
        args.addAll(listOf("-sigalg", finalSigAlg.cliName))

        ext.forEach { args.addAll(listOf("-ext", it)) }
        if (startdate != null) args.addAll(listOf("-startdate", startdate))
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
        if (signer != null) args.addAll(listOf("-signer", signer))
        if (signerKeypass != null) args.addAll(listOf("-signerkeypass", signerKeypass))

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
        keySize: Int? = null,
        storetype: String? = null
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
        val args = mutableListOf(
            "-genseckey",
            "-alias", alias,
            "-keyalg", keyAlgorithm.cliName,
            "-keysize", finalKeySize.toString(),
            "-keystore", keystore.absolutePath,
            "-storepass", storepass,
            "-keypass", keypass
        )
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
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
        signatureAlgorithm: SignatureAlgorithm? = null,
        ext: List<String> = emptyList(),
        rfc: Boolean = false,
        dname: String? = null,
        startdate: String? = null,
        storetype: String? = null
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
        ext.forEach { args.addAll(listOf("-ext", it)) }
        if (rfc) args.add("-rfc")
        if (dname != null) args.addAll(listOf("-dname", dname))
        if (startdate != null) args.addAll(listOf("-startdate", startdate))
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
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
        signatureAlgorithm: SignatureAlgorithm? = null,
        ext: List<String> = emptyList(),
        dname: String? = null,
        storetype: String? = null
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
        ext.forEach { args.addAll(listOf("-ext", it)) }
        if (dname != null) args.addAll(listOf("-dname", dname))
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
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
        rfc: Boolean = false,
        storetype: String? = null,
        cacerts: Boolean = false
    ): KeytoolResult<Unit> {
        val args = mutableListOf("-exportcert", "-alias", alias, "-file", file.absolutePath)
        args.addAll(buildKeystoreArgs(keystore, storepass, cacerts))
        if (rfc) {
            args.add("-rfc")
        }
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
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
        noprompt: Boolean = true,
        storetype: String? = null,
        cacerts: Boolean = false
    ): KeytoolResult<Unit> {
        val args = mutableListOf("-importcert", "-alias", alias, "-file", file.absolutePath)
        args.addAll(buildKeystoreArgs(keystore, storepass, cacerts))
        if (keypass != null) {
            args.addAll(listOf("-keypass", keypass))
        }
        if (trustcacerts) {
            args.add("-trustcacerts")
        }
        if (noprompt) {
            args.add("-noprompt")
        }
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
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
        noprompt: Boolean = true,
        srcStoretype: String? = null,
        destStoretype: String? = null,
        srcProvidername: String? = null,
        destProvidername: String? = null
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
        if (srcStoretype != null) args.addAll(listOf("-srcstoretype", srcStoretype))
        if (destStoretype != null) args.addAll(listOf("-deststoretype", destStoretype))
        if (srcProvidername != null) args.addAll(listOf("-srcprovidername", srcProvidername))
        if (destProvidername != null) args.addAll(listOf("-destprovidername", destProvidername))
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
        keypass: String,
        keyAlgorithm: KeyAlgorithm? = null,
        keySize: Int? = null,
        storetype: String? = null
    ): KeytoolResult<Unit> {
        val args = mutableListOf(
            "-importpass",
            "-alias", alias,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )
        if (keyAlgorithm != null) args.addAll(listOf("-keyalg", keyAlgorithm.cliName))
        if (keySize != null) args.addAll(listOf("-keysize", keySize.toString()))
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
        val result = KeyToolExecutor.execute(*args.toTypedArray(), stdin = "$keypass\n$keypass\n")
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
        alias: String,
        storetype: String? = null,
        cacerts: Boolean = false
    ): KeytoolResult<Unit> {
        val args = mutableListOf("-delete", "-alias", alias)
        args.addAll(buildKeystoreArgs(keystore, storepass, cacerts))
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
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
        keypass: String?,
        storetype: String? = null,
        cacerts: Boolean = false
    ): KeytoolResult<Unit> {
        val args = mutableListOf("-changealias", "-alias", alias, "-destalias", destalias)
        args.addAll(buildKeystoreArgs(keystore, storepass, cacerts))
        if (keypass != null) {
            args.addAll(listOf("-keypass", keypass))
        }
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
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
        newKeypass: String,
        storetype: String? = null
    ): KeytoolResult<Unit> {
        val args = mutableListOf(
            "-keypasswd",
            "-alias", alias,
            "-keypass", keypass,
            "-new", newKeypass,
            "-keystore", keystore.absolutePath,
            "-storepass", storepass
        )
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
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
        newStorepass: String,
        storetype: String? = null,
        cacerts: Boolean = false
    ): KeytoolResult<Unit> {
        val args = mutableListOf("-storepasswd", "-new", newStorepass)
        args.addAll(buildKeystoreArgs(keystore, storepass, cacerts))
        if (storetype != null) args.addAll(listOf("-storetype", storetype))
        val result = KeyToolExecutor.execute(*args.toTypedArray())
        return if (result.exitCode == 0) {
            KeytoolResult.Success(Unit)
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 14. PRINTCERT → CertificateInfo =====
    suspend fun printCert(
        file: File? = null,
        verbose: Boolean = true,
        rfc: Boolean = false,
        sslserver: String? = null,
        jarfile: File? = null
    ): KeytoolResult<CertificateInfo> {
        val sourceCount = listOfNotNull(file, sslserver, jarfile).size
        if (sourceCount != 1) {
            return KeytoolResult.Error(
                "Exactly one of file, sslserver, or jarfile must be specified",
                -1
            )
        }

        val args = mutableListOf("-printcert")
        if (file != null) args.addAll(listOf("-file", file.absolutePath))
        if (sslserver != null) args.addAll(listOf("-sslserver", sslserver))
        if (jarfile != null) args.addAll(listOf("-jarfile", jarfile.absolutePath))
        if (rfc) {
            args.add("-rfc")
        } else if (verbose) {
            args.add("-v")
        }
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

    // ===== 17. SHOWINFO -TLS → TlsInfo =====
    suspend fun showInfoTls(): KeytoolResult<TlsInfo> {
        val result = KeyToolExecutor.execute("-showinfo", "-tls")
        return if (result.exitCode == 0) {
            try {
                val parsed = KeyToolParser.parseTlsInfoOutput(result.stdout)
                KeytoolResult.Success(parsed)
            } catch (e: Exception) {
                KeytoolResult.Error("Failed to parse TLS info output: ${e.message}", -1)
            }
        } else {
            KeytoolResult.Error(result.stderr.ifBlank { result.stdout }, result.exitCode)
        }
    }

    // ===== 18. VERSION → String =====
    fun version(): String {
        return System.getProperty("java.version") ?: "unknown"
    }
}
