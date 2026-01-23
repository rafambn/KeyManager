package unidesk.com.br.keymanager.core

/**
 * Type-safe enumeration of key algorithms supported by keytool.
 * Each algorithm defines its valid key sizes, default size, and capabilities.
 *
 * Based on OpenJDK 25 keytool implementation.
 */
enum class KeyAlgorithm(
    val displayName: String,
    val cliName: String,
    val supportedKeySizes: IntRange,
    val defaultKeySize: Int,
    val supportsSignature: Boolean,
    val supportsKeyAgreement: Boolean,
    val isQuantumResistant: Boolean = false,
    val deprecated: Boolean = false,
    val disabled: Boolean = false
) {
    // ===== RSA FAMILY =====
    RSA(
        displayName = "RSA",
        cliName = "rsa",
        supportedKeySizes = 512..16384,
        defaultKeySize = 3072,  // JDK 25+ default
        supportsSignature = true,
        supportsKeyAgreement = false,
        deprecated = false  // No deprecation warning for RSA itself
    ),

    RSA_PSS(
        displayName = "RSA-PSS",
        cliName = "rsassa-pss",
        supportedKeySizes = 512..16384,
        defaultKeySize = 3072,
        supportsSignature = true,
        supportsKeyAgreement = false
    ),

    // ===== DSA (DIGITAL SIGNATURE ALGORITHM) =====
    DSA(
        displayName = "DSA",
        cliName = "dsa",
        supportedKeySizes = 512..3072,
        defaultKeySize = 2048,
        supportsSignature = true,
        supportsKeyAgreement = false,
        deprecated = true  // Legacy algorithm, shows deprecation warning
    ),

    // ===== EC (ELLIPTIC CURVE) =====
    EC(
        displayName = "EC",
        cliName = "ec",
        supportedKeySizes = 160..571,  // Depends on curve, see ECCurve
        defaultKeySize = 384,  // JDK 25+ default (secp384r1)
        supportsSignature = true,
        supportsKeyAgreement = false
    ),

    // ===== EDWARDS CURVE DIGITAL SIGNATURE ALGORITHM (EdDSA) =====
    ED25519(
        displayName = "EdDSA (Ed25519)",
        cliName = "Ed25519",
        supportedKeySizes = 256..256,
        defaultKeySize = 256,
        supportsSignature = true,
        supportsKeyAgreement = false
    ),

    ED448(
        displayName = "EdDSA (Ed448)",
        cliName = "Ed448",
        supportedKeySizes = 456..456,
        defaultKeySize = 456,
        supportsSignature = true,
        supportsKeyAgreement = false
    ),

    // ===== DIFFIE-HELLMAN FAMILY (KEY AGREEMENT) =====
    DH(
        displayName = "DH",
        cliName = "dh",
        supportedKeySizes = 512..8192,
        defaultKeySize = 3072,  // JDK 25+ default
        supportsSignature = false,
        supportsKeyAgreement = true
    ),

    X25519(
        displayName = "XDH (X25519)",
        cliName = "X25519",
        supportedKeySizes = 256..256,
        defaultKeySize = 256,
        supportsSignature = false,
        supportsKeyAgreement = true
    ),

    X448(
        displayName = "XDH (X448)",
        cliName = "X448",
        supportedKeySizes = 448..448,
        defaultKeySize = 448,
        supportsSignature = false,
        supportsKeyAgreement = true
    ),

    // ===== QUANTUM-RESISTANT (JDK 24+) =====
    /**
     * ML-DSA (Module-Lattice-Based Digital Signature Algorithm)
     * Parameter set 44 - recommended for most applications
     */
    ML_DSA_44(
        displayName = "ML-DSA (44)",
        cliName = "ML-DSA",
        supportedKeySizes = -1..-1,  // Parameter-set based, not bits
        defaultKeySize = -1,
        supportsSignature = true,
        supportsKeyAgreement = false,
        isQuantumResistant = true
    ),

    /**
     * ML-DSA parameter set 65 - higher security margin
     */
    ML_DSA_65(
        displayName = "ML-DSA (65)",
        cliName = "ML-DSA",
        supportedKeySizes = -1..-1,
        defaultKeySize = -1,
        supportsSignature = true,
        supportsKeyAgreement = false,
        isQuantumResistant = true
    ),

    /**
     * ML-DSA parameter set 87 - highest security
     */
    ML_DSA_87(
        displayName = "ML-DSA (87)",
        cliName = "ML-DSA",
        supportedKeySizes = -1..-1,
        defaultKeySize = -1,
        supportsSignature = true,
        supportsKeyAgreement = false,
        isQuantumResistant = true
    ),

    /**
     * ML-KEM (Module-Lattice-Based Key-Encapsulation Mechanism)
     * Note: ML-KEM is NOT a signature algorithm - use with external signer
     */
    ML_KEM_512(
        displayName = "ML-KEM (512)",
        cliName = "ML-KEM",
        supportedKeySizes = -1..-1,
        defaultKeySize = -1,
        supportsSignature = false,
        supportsKeyAgreement = true,
        isQuantumResistant = true
    ),

    ML_KEM_768(
        displayName = "ML-KEM (768)",
        cliName = "ML-KEM",
        supportedKeySizes = -1..-1,
        defaultKeySize = -1,
        supportsSignature = false,
        supportsKeyAgreement = true,
        isQuantumResistant = true
    ),

    ML_KEM_1024(
        displayName = "ML-KEM (1024)",
        cliName = "ML-KEM",
        supportedKeySizes = -1..-1,
        defaultKeySize = -1,
        supportsSignature = false,
        supportsKeyAgreement = true,
        isQuantumResistant = true
    ),

    // ===== SYMMETRIC KEYS (for -genseckey) =====
    AES(
        displayName = "AES",
        cliName = "AES",
        supportedKeySizes = 128..256,
        defaultKeySize = 256,
        supportsSignature = false,
        supportsKeyAgreement = false
    ),

    TRIPLE_DES(
        displayName = "TripleDES",
        cliName = "TripleDES",
        supportedKeySizes = 168..168,
        defaultKeySize = 168,
        supportsSignature = false,
        supportsKeyAgreement = false,
        deprecated = true  // Legacy, shows deprecation warning
    );

    companion object {
        /**
         * Parse algorithm name (case-insensitive) from CLI format.
         * Matches against cliName for all variants.
         */
        fun fromCliName(name: String?): KeyAlgorithm? {
            if (name == null) return null
            return values().find { it.cliName.equals(name, ignoreCase = true) }
        }

        /**
         * Get all algorithms that support signatures
         */
        fun signatureAlgorithms(): List<KeyAlgorithm> {
            return values().filter { it.supportsSignature }
        }

        /**
         * Get all algorithms that support key agreement
         */
        fun keyAgreementAlgorithms(): List<KeyAlgorithm> {
            return values().filter { it.supportsKeyAgreement }
        }

        /**
         * Get all quantum-resistant algorithms
         */
        fun quantumResistantAlgorithms(): List<KeyAlgorithm> {
            return values().filter { it.isQuantumResistant }
        }
    }

    /**
     * Check if a given key size is valid for this algorithm
     */
    fun isValidKeySize(size: Int): Boolean {
        // For parameter-set based algorithms (ML-DSA, ML-KEM), keysize -1 means N/A
        if (defaultKeySize == -1) return size == -1
        return size in supportedKeySizes
    }

    /**
     * Check if this algorithm is usable (not disabled)
     */
    fun isUsable(): Boolean = !disabled

    /**
     * Check if this algorithm shows a deprecation warning
     */
    fun showsDeprecationWarning(): Boolean = deprecated
}
