package unidesk.com.br.keymanager.core.domain

/**
 * Type-safe enumeration of keystore formats supported by keytool.
 * Specifies the binary format and compatibility characteristics of each keystore type.
 *
 * Based on OpenJDK 25 keytool implementation.
 */
enum class KeystoreFormat(
    val cliName: String,
    val displayName: String,
    val fileExtension: String?,
    val isBinary: Boolean,
    val defaultFormat: Boolean = false,
    val deprecated: Boolean = false,
    val platformSpecific: Boolean = false
) {
    // ===== STANDARD FORMATS =====
    /**
     * PKCS12 - DEFAULT (RFC 7292 standard)
     * Recommended format for all new keystores
     * Default in JDK 9+
     */
    PKCS12(
        cliName = "pkcs12",
        displayName = "PKCS12",
        fileExtension = ".p12",
        isBinary = true,
        defaultFormat = true
    ),

    /**
     * JKS (Java KeyStore) - DEPRECATED
     * Legacy format, proprietary to Java
     * Shows deprecation warning, use PKCS12 instead
     */
    JKS(
        cliName = "jks",
        displayName = "JKS",
        fileExtension = ".jks",
        isBinary = true,
        deprecated = true
    ),

    /**
     * JCEKS (Java Cryptography Extension KeyStore) - DEPRECATED
     * Extended version of JKS with additional features
     * Shows deprecation warning, use PKCS12 instead
     */
    JCEKS(
        cliName = "jceks",
        displayName = "JCEKS",
        fileExtension = ".jceks",
        isBinary = true,
        deprecated = true
    ),

    // ===== HARDWARE & SPECIAL =====
    /**
     * PKCS11 - Hardware Token Integration
     * Integrates with PKCS#11 devices (smart cards, HSMs)
     * Configuration via Java security properties
     */
    PKCS11(
        cliName = "pkcs11",
        displayName = "PKCS11",
        fileExtension = null,  
        isBinary = false
    ),

    /**
     * Windows-MY - Windows Platform Integration
     * Native Windows certificate store
     * Platform-specific: Windows only
     */
    WINDOWS_MY(
        cliName = "windows-my",
        displayName = "Windows-MY",
        fileExtension = null,
        isBinary = false,
        platformSpecific = true
    );

    companion object {
        /**
         * Parse keystore format name (case-insensitive) from CLI format
         */
        fun fromCliName(name: String?): KeystoreFormat? {
            if (name == null) return null
            return values().find { it.cliName.equals(name, ignoreCase = true) }
        }

        /**
         * Get the default keystore format (PKCS12 in JDK 9+)
         */
        fun default(): KeystoreFormat = PKCS12

        /**
         * Get all file-based formats (have file extension)
         */
        fun fileBasedFormats(): List<KeystoreFormat> {
            return values().filter { it.fileExtension != null }
        }

        /**
         * Get all deprecated formats (JKS, JCEKS)
         */
        fun deprecatedFormats(): List<KeystoreFormat> {
            return values().filter { it.deprecated }
        }

        /**
         * Get all non-platform-specific formats (portable across systems)
         */
        fun portableFormats(): List<KeystoreFormat> {
            return values().filter { !it.platformSpecific }
        }

        /**
         * Get appropriate extension for a given format
         */
        fun extensionFor(format: KeystoreFormat): String {
            return format.fileExtension ?: when (format) {
                PKCS11 -> ".p11"
                WINDOWS_MY -> ".win"
                else -> ".keystore"
            }
        }
    }

    fun isDeprecated(): Boolean = deprecated

    /**
     * Check if this format is the default (PKCS12)
     */
    fun isDefault(): Boolean = defaultFormat

    fun isFileBased(): Boolean = fileExtension != null

    /**
     * Check if this format is platform-specific
     */
    fun isPlatformSpecific(): Boolean = platformSpecific

    /**
     * Get recommended replacement format if this format is deprecated
     */
    fun recommendedReplacement(): KeystoreFormat? {
        return if (deprecated) PKCS12 else null
    }
}
