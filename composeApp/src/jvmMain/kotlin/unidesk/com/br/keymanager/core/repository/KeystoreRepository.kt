package unidesk.com.br.keymanager.core.repository

import java.io.File
import unidesk.com.br.keymanager.core.api.KeyToolAPI
import unidesk.com.br.keymanager.core.model.KeyInfo
import unidesk.com.br.keymanager.core.model.KeystoreInfo
import unidesk.com.br.keymanager.core.domain.EntryType
import unidesk.com.br.keymanager.core.KeytoolResult

/**
 * Stateless repository for keystore operations.
 * All operations take file and password as parameters.
 */
class KeystoreRepository {

    suspend fun validateKeystore(file: File, password: String): KeytoolResult<KeystoreInfo> {
        return KeyToolAPI.list(file, password)
    }

    suspend fun getKeystoreInfo(file: File, password: String): KeytoolResult<KeystoreInfo> {
        return KeyToolAPI.list(file, password, verbose = true)
    }

    suspend fun getKeys(file: File, password: String): List<KeyInfo> {
        return when (val result = KeyToolAPI.list(file, password, verbose = true)) {
            is KeytoolResult.Success -> {
                result.data.entries.sortedBy { it.alias }.map { entry ->
                    KeyInfo(
                        alias = entry.alias,
                        algorithm = entry.algorithm ?: "Unknown",
                        entryType = entry.entryType,
                        details = entry.owner ?: "",
                        creationDate = entry.creationDate,
                        validFrom = entry.validFrom,
                        validUntil = entry.validUntil,
                        fingerprint = entry.fingerprint,
                        owner = entry.owner,
                        issuer = entry.issuer,
                        certificateChainLength = entry.certificateChainLength,
                        serialNumber = entry.serialNumber
                    )
                }
            }
            is KeytoolResult.Error -> {
                System.err.println("Error loading keys: ${result.message}")
                emptyList()
            }
        }
    }

    suspend fun getAliases(file: File, password: String): List<String> {
        return when (val result = KeyToolAPI.list(file, password, verbose = false)) {
            is KeytoolResult.Success -> result.data.entries.map { it.alias }.sorted()
            is KeytoolResult.Error -> {
                System.err.println("Error listing aliases: ${result.message}")
                emptyList()
            }
        }
    }

    suspend fun deleteAlias(file: File, password: String, alias: String): KeytoolResult<Unit> {
        return KeyToolAPI.delete(file, password, alias)
    }

    suspend fun moveAlias(
        sourceFile: File,
        sourcePassword: String,
        alias: String,
        keyPassword: String,
        targetFile: File,
        targetPassword: String
    ): KeytoolResult<Unit> {
        // Import the alias to the target keystore
        return when (val importResult = KeyToolAPI.importKeystore(
            srcKeystore = sourceFile,
            srcStorepass = sourcePassword,
            srcAlias = alias,
            srcKeypass = keyPassword,
            destKeystore = targetFile,
            destStorepass = targetPassword,
            destKeypass = targetPassword
        )) {
            is KeytoolResult.Success -> {
                // Successfully imported, now delete from source
                KeyToolAPI.delete(sourceFile, sourcePassword, alias)
            }
            is KeytoolResult.Error -> {
                importResult
            }
        }
    }

    suspend fun renameAlias(
        file: File,
        storePassword: String,
        oldAlias: String,
        newAlias: String,
        keyPassword: String
    ): KeytoolResult<Unit> {
        return KeyToolAPI.changeAlias(
            keystore = file,
            storepass = storePassword,
            alias = oldAlias,
            destalias = newAlias,
            keypass = keyPassword
        )
    }

    suspend fun createKeyPair(
        file: File,
        storePassword: String,
        alias: String,
        dn: String,
        validityDays: Int,
        keyPassword: String
    ): KeytoolResult<Unit> {
        return KeyToolAPI.genKeyPair(
            keystore = file,
            storepass = storePassword,
            alias = alias,
            keypass = keyPassword,
            dname = dn,
            validity = validityDays
        )
    }

    suspend fun exportCertificate(
        file: File,
        storePassword: String,
        alias: String,
        outputFile: File,
        rfc: Boolean = true
    ): KeytoolResult<Unit> {
        return KeyToolAPI.exportCert(
            keystore = file,
            storepass = storePassword,
            alias = alias,
            file = outputFile,
            rfc = rfc
        )
    }

    suspend fun changeStorePassword(
        file: File,
        oldPassword: String,
        newPassword: String
    ): KeytoolResult<Unit> {
        return KeyToolAPI.storePasswd(
            keystore = file,
            storepass = oldPassword,
            newStorepass = newPassword
        )
    }

    suspend fun changeKeyPassword(
        file: File,
        storePassword: String,
        alias: String,
        oldKeyPassword: String,
        newKeyPassword: String
    ): KeytoolResult<Unit> {
        return KeyToolAPI.keyPasswd(
            keystore = file,
            storepass = storePassword,
            alias = alias,
            keypass = oldKeyPassword,
            newKeypass = newKeyPassword
        )
    }
}
