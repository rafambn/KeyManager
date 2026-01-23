package unidesk.com.br.keymanager.core.domain

/**
 * Type-safe enumeration of elliptic curves supported by keytool.
 * Used with KeyAlgorithm.EC for specifying curve parameters.
 *
 * Based on OpenJDK 25 keytool implementation.
 */
enum class ECCurve(
    val cliName: String,
    val displayName: String,
    val bitLength: Int,
    val standardName: String,
    val aliases: List<String> = emptyList(),
    val deprecated: Boolean = false
) {
    // ===== NIST CURVES (RECOMMENDED) =====
    /**
     * P-256 (secp256r1) - DEFAULT for EC keys before JDK 17
     * NIST standard curve
     * Also known as: prime256v1, NIST P-256
     */
    P256(
        cliName = "secp256r1",
        displayName = "P-256 (secp256r1)",
        bitLength = 256,
        standardName = "NIST P-256",
        aliases = listOf("prime256v1", "P-256")
    ),

    /**
     * P-384 (secp384r1) - DEFAULT for EC keys in JDK 17+
     * NIST standard curve
     * Recommended for balanced security/performance
     */
    P384(
        cliName = "secp384r1",
        displayName = "P-384 (secp384r1)",
        bitLength = 384,
        standardName = "NIST P-384",
        aliases = listOf("P-384")
    ),

    /**
     * P-521 (secp521r1)
     * NIST standard curve (note: 521 bits, not 512)
     * Recommended for high-security applications
     */
    P521(
        cliName = "secp521r1",
        displayName = "P-521 (secp521r1)",
        bitLength = 521,
        standardName = "NIST P-521",
        aliases = listOf("P-521")
    ),

    // ===== BINARY CURVES (LEGACY - NOT RECOMMENDED) =====
    /**
     * sect163k1 - DEPRECATED
     * Binary curve, 163 bits
     * Legacy support only
     */
    SECT163K1(
        cliName = "sect163k1",
        displayName = "sect163k1",
        bitLength = 163,
        standardName = "Binary Curve",
        deprecated = true
    ),

    /**
     * sect163r2 - DEPRECATED
     * Binary curve, 163 bits
     * Legacy support only
     */
    SECT163R2(
        cliName = "sect163r2",
        displayName = "sect163r2",
        bitLength = 163,
        standardName = "Binary Curve",
        deprecated = true
    );

    companion object {
        /**
         * Parse curve name (case-insensitive) from CLI format.
         * Matches against cliName and all aliases.
         */
        fun fromCliName(name: String?): ECCurve? {
            if (name == null) return null
            val lowerName = name.lowercase()
            return values().find {
                it.cliName.lowercase() == lowerName ||
                it.aliases.any { alias -> alias.lowercase() == lowerName }
            }
        }

        /**
         * Get the default EC curve (JDK 25+)
         */
        fun default(): ECCurve = P384

        /**
         * Get all recommended NIST curves (not deprecated)
         */
        fun recommendedCurves(): List<ECCurve> {
            return values().filter { !it.deprecated }
        }

        /**
         * Get all deprecated curves
         */
        fun deprecatedCurves(): List<ECCurve> {
            return values().filter { it.deprecated }
        }
    }

    /**
     * Check if this curve is deprecated
     */
    fun isDeprecated(): Boolean = deprecated

    /**
     * Check if this curve is recommended (NIST standard, not binary)
     */
    fun isRecommended(): Boolean = !deprecated && standardName.contains("NIST")
}
