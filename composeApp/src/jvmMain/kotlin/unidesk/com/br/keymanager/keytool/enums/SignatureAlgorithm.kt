package unidesk.com.br.keymanager.keytool.enums

/**
 * Type-safe enumeration of signature algorithms supported by keytool.
 * Includes automatic default selection based on key type and size (JDK 25+).
 *
 * Based on OpenJDK 25 keytool implementation.
 */
enum class SignatureAlgorithm(
    val cliName: String,
    val displayName: String,
    val keyAlgorithmFamily: String,
    val hashAlgorithm: String,
    val deprecated: Boolean = false,
    val disabled: Boolean = false
) {
    // ===== RSA-BASED SIGNATURES =====
    /**
     * MD5withRSA - DISABLED
     * Cryptographically broken hash function
     */
    MD5_WITH_RSA(
        cliName = "MD5withRSA",
        displayName = "MD5 with RSA",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "MD5",
        disabled = true
    ),

    /**
     * SHA1withRSA - DEPRECATED
     * SHA-1 is showing weakness, will be disabled in future Java versions
     */
    SHA1_WITH_RSA(
        cliName = "SHA1withRSA",
        displayName = "SHA-1 with RSA",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA1",
        deprecated = true
    ),

    /**
     * SHA256withRSA - STANDARD (default for RSA < 3072 bits)
     * Recommended for most new RSA keys
     */
    SHA256_WITH_RSA(
        cliName = "SHA256withRSA",
        displayName = "SHA-256 with RSA",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA256"
    ),

    /**
     * SHA384withRSA - STRONG (default for RSA 3072-7680 bits)
     * Recommended for high-security applications
     */
    SHA384_WITH_RSA(
        cliName = "SHA384withRSA",
        displayName = "SHA-384 with RSA",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA384"
    ),

    /**
     * SHA512withRSA - STRONG (default for RSA > 7680 bits)
     * Recommended for very high-security applications
     */
    SHA512_WITH_RSA(
        cliName = "SHA512withRSA",
        displayName = "SHA-512 with RSA",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA512"
    ),

    /**
     * SHA3-256withRSA - MODERN
     * Uses SHA-3 family (Keccak-based)
     */
    SHA3_256_WITH_RSA(
        cliName = "SHA3-256withRSA",
        displayName = "SHA3-256 with RSA",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA3-256"
    ),

    /**
     * SHA3-384withRSA - MODERN
     */
    SHA3_384_WITH_RSA(
        cliName = "SHA3-384withRSA",
        displayName = "SHA3-384 with RSA",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA3-384"
    ),

    /**
     * SHA3-512withRSA - MODERN
     */
    SHA3_512_WITH_RSA(
        cliName = "SHA3-512withRSA",
        displayName = "SHA3-512 with RSA",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA3-512"
    ),

    // ===== RSA-PSS VARIANTS (JDK 17+) =====
    /**
     * SHA256withRSA/PSS - MODERN
     * Uses PKCS#1 v2.1 PSS padding instead of PKCS#1 v1.5
     * More secure than traditional RSA signatures
     */
    SHA256_WITH_RSA_PSS(
        cliName = "SHA256withRSA/PSS",
        displayName = "SHA-256 with RSA/PSS",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA256"
    ),

    /**
     * SHA384withRSA/PSS - MODERN
     */
    SHA384_WITH_RSA_PSS(
        cliName = "SHA384withRSA/PSS",
        displayName = "SHA-384 with RSA/PSS",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA384"
    ),

    /**
     * SHA512withRSA/PSS - MODERN
     */
    SHA512_WITH_RSA_PSS(
        cliName = "SHA512withRSA/PSS",
        displayName = "SHA-512 with RSA/PSS",
        keyAlgorithmFamily = "RSA",
        hashAlgorithm = "SHA512"
    ),

    // ===== ELLIPTIC CURVE SIGNATURES (ECDSA) =====
    /**
     * SHA1withECDSA - DEPRECATED
     */
    SHA1_WITH_ECDSA(
        cliName = "SHA1withECDSA",
        displayName = "SHA-1 with ECDSA",
        keyAlgorithmFamily = "EC",
        hashAlgorithm = "SHA1",
        deprecated = true
    ),

    /**
     * SHA256withECDSA - STANDARD (default for EC < 384 bits)
     * Recommended for most EC keys
     */
    SHA256_WITH_ECDSA(
        cliName = "SHA256withECDSA",
        displayName = "SHA-256 with ECDSA",
        keyAlgorithmFamily = "EC",
        hashAlgorithm = "SHA256"
    ),

    /**
     * SHA384withECDSA - STRONG (default for EC 384-511 bits)
     * Recommended for EC P-384 curves
     */
    SHA384_WITH_ECDSA(
        cliName = "SHA384withECDSA",
        displayName = "SHA-384 with ECDSA",
        keyAlgorithmFamily = "EC",
        hashAlgorithm = "SHA384"
    ),

    /**
     * SHA512withECDSA - STRONG (default for EC >= 512 bits)
     * Recommended for EC P-521 curves
     */
    SHA512_WITH_ECDSA(
        cliName = "SHA512withECDSA",
        displayName = "SHA-512 with ECDSA",
        keyAlgorithmFamily = "EC",
        hashAlgorithm = "SHA512"
    ),

    // ===== DSA SIGNATURES =====
    /**
     * SHA1withDSA - DEPRECATED
     */
    SHA1_WITH_DSA(
        cliName = "SHA1withDSA",
        displayName = "SHA-1 with DSA",
        keyAlgorithmFamily = "DSA",
        hashAlgorithm = "SHA1",
        deprecated = true
    ),

    /**
     * SHA224withDSA - LEGACY
     */
    SHA224_WITH_DSA(
        cliName = "SHA224withDSA",
        displayName = "SHA-224 with DSA",
        keyAlgorithmFamily = "DSA",
        hashAlgorithm = "SHA224"
    ),

    /**
     * SHA256withDSA - STANDARD
     * Default and recommended signature algorithm for DSA keys
     */
    SHA256_WITH_DSA(
        cliName = "SHA256withDSA",
        displayName = "SHA-256 with DSA",
        keyAlgorithmFamily = "DSA",
        hashAlgorithm = "SHA256"
    ),

    // ===== EDWARDS CURVE SIGNATURES (EdDSA) =====
    /**
     * Ed25519 - STANDARD
     * Signature algorithm = key algorithm for EdDSA
     * Deterministic, modern signature scheme
     */
    ED25519(
        cliName = "Ed25519",
        displayName = "Ed25519",
        keyAlgorithmFamily = "EdDSA",
        hashAlgorithm = "Ed25519"
    ),

    /**
     * Ed448 - STANDARD
     * Higher security variant of EdDSA
     */
    ED448(
        cliName = "Ed448",
        displayName = "Ed448",
        keyAlgorithmFamily = "EdDSA",
        hashAlgorithm = "Ed448"
    ),

    // ===== QUANTUM-RESISTANT SIGNATURES (JDK 24+) =====
    /**
     * ML-DSA - MODERN (Quantum-Resistant)
     * Module-Lattice-Based Digital Signature Algorithm
     * Parameter set derived from key algorithm specification
     */
    ML_DSA(
        cliName = "ML-DSA",
        displayName = "ML-DSA",
        keyAlgorithmFamily = "ML-DSA",
        hashAlgorithm = "ML-DSA"
    );

    companion object {
        /**
         * Parse signature algorithm name (case-insensitive) from CLI format
         */
        fun fromCliName(name: String?): SignatureAlgorithm? {
            if (name == null) return null
            return entries.find { it.cliName.equals(name, ignoreCase = true) }
        }

        /**
         * Auto-select appropriate signature algorithm based on key type and size.
         * This implements JDK 25+ default selection logic.
         */
        fun selectDefault(keyAlgorithm: KeyAlgorithm, keySize: Int): SignatureAlgorithm {
            return when (keyAlgorithm) {

                KeyAlgorithm.RSA -> when {
                    keySize < 3072 -> SHA256_WITH_RSA
                    keySize < 7680 -> SHA384_WITH_RSA
                    else -> SHA512_WITH_RSA
                }

                KeyAlgorithm.RSA_PSS -> when {
                    keySize < 3072 -> SHA256_WITH_RSA_PSS
                    keySize < 7680 -> SHA384_WITH_RSA_PSS
                    else -> SHA512_WITH_RSA_PSS
                }

                KeyAlgorithm.EC -> when {
                    keySize < 384 -> SHA256_WITH_ECDSA
                    keySize < 512 -> SHA384_WITH_ECDSA
                    else -> SHA512_WITH_ECDSA
                }

                KeyAlgorithm.DSA -> SHA256_WITH_DSA

                KeyAlgorithm.ED25519 -> ED25519
                KeyAlgorithm.ED448 -> ED448

                KeyAlgorithm.ML_DSA_44, KeyAlgorithm.ML_DSA_65, KeyAlgorithm.ML_DSA_87 -> ML_DSA

                else -> SHA256_WITH_RSA
            }
        }

        /**
         * Get all signature algorithms compatible with a given key algorithm
         */
        fun compatibleWith(keyAlgorithm: KeyAlgorithm): List<SignatureAlgorithm> {
            return entries.filter { it.keyAlgorithmFamily == keyAlgorithm.cliName }
        }

        /**
         * Get all usable (non-disabled) signature algorithms
         */
        fun usableAlgorithms(): List<SignatureAlgorithm> {
            return entries.filter { !it.disabled }
        }

        /**
         * Get all deprecated (but usable) signature algorithms
         */
        fun deprecatedAlgorithms(): List<SignatureAlgorithm> {
            return entries.filter { it.deprecated && !it.disabled }
        }

        /**
         * Get all quantum-resistant signature algorithms
         */
        fun quantumResistantAlgorithms(): List<SignatureAlgorithm> {
            return listOf(ML_DSA)
        }
    }

    /**
     * Check if this signature algorithm is usable (not disabled)
     */
    fun isUsable(): Boolean = !disabled

    /**
     * Check if this signature algorithm shows a deprecation warning
     */
    fun showsDeprecationWarning(): Boolean = deprecated
}
