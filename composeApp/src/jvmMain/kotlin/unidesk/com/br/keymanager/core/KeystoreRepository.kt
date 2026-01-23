package unidesk.com.br.keymanager.core

import java.io.File

class KeystoreRepository {

    private var currentFile: File? = null
    private var currentStorePassword: String? = null

    // In-memory cache of aliases with their key passwords (if known)
    private val keyPasswordCache = mutableMapOf<String, String>()

    suspend fun loadKeystore(file: File, password: String) {
        // Use KeyToolAPI.list() to validate the keystore and password
        when (val result = KeyToolAPI.list(file, password)) {
            is KeytoolResult.Success -> {
                currentFile = file
                currentStorePassword = password
                keyPasswordCache.clear()
            }
            is KeytoolResult.Error -> {
                throw IllegalArgumentException("Failed to load keystore: ${result.message}")
            }
        }
    }

    suspend fun getKeys(): List<KeyInfo> {
        val file = currentFile ?: return emptyList()
        val password = currentStorePassword ?: return emptyList()

        return when (val result = KeyToolAPI.list(file, password, verbose = true)) {
            is KeytoolResult.Success -> {
                result.data.entries.sortedBy { it.alias }.map { entry ->
                    val type = when (entry.entryType) {
                        EntryType.PRIVATE_KEY -> "type_key"
                        EntryType.TRUSTED_CERT -> "type_certificate"
                        EntryType.SECRET_KEY -> "type_secret"
                        EntryType.UNKNOWN -> "type_unknown"
                    }

                    val algorithm = entry.algorithm ?: "Unknown"

                    KeyInfo(
                        alias = entry.alias,
                        algorithm = algorithm,
                        type = type,
                        details = entry.owner ?: ""
                    )
                }
            }
            is KeytoolResult.Error -> {
                System.err.println("Error loading keys: ${result.message}")
                emptyList()
            }
        }
    }

    suspend fun getAliases(): List<String> {
        val file = currentFile ?: return emptyList()
        val password = currentStorePassword ?: return emptyList()

        return when (val result = KeyToolAPI.list(file, password, verbose = false)) {
            is KeytoolResult.Success -> result.data.entries.map { it.alias }.sorted()
            is KeytoolResult.Error -> {
                System.err.println("Error listing aliases: ${result.message}")
                emptyList()
            }
        }
    }

    suspend fun deleteAlias(alias: String) {
        val file = currentFile ?: return
        val password = currentStorePassword ?: return

        when (val result = KeyToolAPI.delete(file, password, alias)) {
            is KeytoolResult.Success -> {
                keyPasswordCache.remove(alias)
            }
            is KeytoolResult.Error -> {
                throw IllegalStateException("Failed to delete alias: ${result.message}")
            }
        }
    }

    suspend fun moveAlias(alias: String, targetFile: File, targetPassword: String) {
        val sourceFile = currentFile ?: throw IllegalStateException("Source keystore not loaded")
        val sourcePassword = currentStorePassword ?: throw IllegalStateException("Source password not set")

        // Get key password from cache, fall back to store password
        val keyPassword = keyPasswordCache[alias] ?: sourcePassword

        // Import the alias to the target keystore
        when (val importResult = KeyToolAPI.importKeystore(
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
                when (val deleteResult = KeyToolAPI.delete(sourceFile, sourcePassword, alias)) {
                    is KeytoolResult.Success -> {
                        keyPasswordCache.remove(alias)
                    }
                    is KeytoolResult.Error -> {
                        throw IllegalStateException("Alias copied but failed to delete from source: ${deleteResult.message}")
                    }
                }
            }
            is KeytoolResult.Error -> {
                throw IllegalStateException("Failed to move alias: ${importResult.message}")
            }
        }
    }

    suspend fun renameAlias(oldAlias: String, newAlias: String, keyPassword: String? = null) {
        val file = currentFile ?: return
        val password = currentStorePassword ?: return

        // Store key password if provided
        if (keyPassword != null) {
            keyPasswordCache[oldAlias] = keyPassword
        }

        // Use cached key password or store password
        val effectiveKeyPassword = keyPasswordCache[oldAlias] ?: password

        when (val result = KeyToolAPI.changeAlias(
            keystore = file,
            storepass = password,
            alias = oldAlias,
            destalias = newAlias,
            keypass = effectiveKeyPassword
        )) {
            is KeytoolResult.Success -> {
                // Update cache: move password from old alias to new alias
                keyPasswordCache[oldAlias]?.let { cachedPassword ->
                    keyPasswordCache.remove(oldAlias)
                    keyPasswordCache[newAlias] = cachedPassword
                }
            }
            is KeytoolResult.Error -> {
                throw IllegalStateException("Failed to rename alias: ${result.message}")
            }
        }
    }

    suspend fun addCertificate(alias: String, dn: String, validityDays: Int, keyPassword: String? = null) {
        val file = currentFile ?: return
        val password = currentStorePassword ?: return

        // Use provided key password or default to store password
        val effectiveKeyPassword = keyPassword ?: password

        when (val result = KeyToolAPI.genKeyPair(
            keystore = file,
            storepass = password,
            alias = alias,
            keypass = effectiveKeyPassword,
            dname = dn,
            validity = validityDays
        )) {
            is KeytoolResult.Success -> {
                // Cache the key password if different from store password
                if (keyPassword != null && keyPassword != password) {
                    keyPasswordCache[alias] = keyPassword
                }
            }
            is KeytoolResult.Error -> {
                throw IllegalStateException("Failed to add certificate: ${result.message}")
            }
        }
    }

    // Store key password for an alias (user provided it for an operation)
    fun setKeyPassword(alias: String, keyPassword: String) {
        keyPasswordCache[alias] = keyPassword
    }

    fun getKeyPassword(alias: String): String? = keyPasswordCache[alias]

    fun isLoaded(): Boolean = currentFile != null

    fun getCurrentPassword(): CharArray? = currentStorePassword?.toCharArray()
}
