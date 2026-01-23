# Type Safety Redesign for KeyToolAPI
## Enum-Based API Changes Required (Priority: HIGH)

**Status**: Planning Phase - User Feedback Integration
**Motivation**: Current string-based API lacks type safety; need enums for algorithm names
**Impact**: Requires KeyToolAPI.kt API changes before comprehensive integration testing
**Based On**: JDK 25 keytool implementation analysis + user requirements

---

## 1. CURRENT STATE (String-Based)

### Current API Issues
```kotlin
// CURRENT - Unsafe, error-prone
fun genKeyPair(
    dn: String,
    alias: String,
    keySize: Int = 2048,
    sigAlg: String? = null,      // ← Accepts any string, no validation
    algorithm: String = "RSA",    // ← Easy to typo: "RsA", "rsa", "RSsA"
    keystore: File,
    storePassword: String
)

// Problem: User can pass invalid values
KeyToolAPI.genKeyPair(
    ...,
    algorithm = "INVALID",        // Compiles! Fails at runtime!
    sigAlg = "SHAWithRSA"         // Typo accepted, fails later
)
```

### Issues with String-Based API
1. **No compile-time validation** - typos only caught at runtime
2. **Type ambiguity** - "SHA1withRSA" vs "SHA1WithRSA" vs "sha1withRSA"
3. **Invalid combinations** - signing with XDH key (impossible) compiles
4. **IDE autocomplete fails** - can't suggest valid values
5. **Documentation burden** - must document all valid strings
6. **Hard to reason about** - which algorithms work with which operations?

---

## 2. TYPE-SAFE ENUMS REQUIRED

### Enum 2.1: KeyAlgorithm

```kotlin
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
    // RSA Family
    RSA(
        displayName = "RSA",
        cliName = "rsa",
        supportedKeySizes = 512..16384,
        defaultKeySize = 3072,  // JDK 25 default
        supportsSignature = true,
        supportsKeyAgreement = false
    ),
    RSA_PSS(
        displayName = "RSA-PSS",
        cliName = "rsassa-pss",
        supportedKeySizes = 512..16384,
        defaultKeySize = 3072,
        supportsSignature = true,
        supportsKeyAgreement = false
    ),

    // DSA
    DSA(
        displayName = "DSA",
        cliName = "dsa",
        supportedKeySizes = 512..3072,
        defaultKeySize = 2048,
        supportsSignature = true,
        supportsKeyAgreement = false,
        deprecated = true  // Legacy, shows warning
    ),

    // Elliptic Curve
    EC(
        displayName = "EC",
        cliName = "ec",
        supportedKeySizes = 160..571,  // Depends on curve
        defaultKeySize = 384,  // JDK 25 default, secp384r1
        supportsSignature = true,
        supportsKeyAgreement = false
    ),

    // Edwards Curve (EdDSA)
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

    // Diffie-Hellman Family (Key Agreement)
    DH(
        displayName = "DH",
        cliName = "dh",
        supportedKeySizes = 512..8192,
        defaultKeySize = 3072,  // JDK 25 default
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

    // Quantum-Resistant (JDK 24+)
    ML_DSA_44(
        displayName = "ML-DSA (44)",
        cliName = "ML-DSA",
        supportedKeySizes = -1..-1,  // Parameter-set based, not bits
        defaultKeySize = -1,
        supportsSignature = true,
        supportsKeyAgreement = false,
        isQuantumResistant = true
    ),
    ML_DSA_65(
        displayName = "ML-DSA (65)",
        cliName = "ML-DSA",
        supportedKeySizes = -1..-1,
        defaultKeySize = -1,
        supportsSignature = true,
        supportsKeyAgreement = false,
        isQuantumResistant = true
    ),
    ML_DSA_87(
        displayName = "ML-DSA (87)",
        cliName = "ML-DSA",
        supportedKeySizes = -1..-1,
        defaultKeySize = -1,
        supportsSignature = true,
        supportsKeyAgreement = false,
        isQuantumResistant = true
    ),

    // Symmetric Keys (for -genseckey)
    AES(
        displayName = "AES",
        cliName = "AES",
        supportedKeySizes = 128..256 step 8,
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
        deprecated = true
    );

    companion object {
        fun fromCliName(name: String): KeyAlgorithm? {
            return values().find { it.cliName.equals(name, ignoreCase = true) }
        }
    }
}
```

### Enum 2.2: SignatureAlgorithm

```kotlin
enum class SignatureAlgorithm(
    val cliName: String,
    val keyAlgorithmFamily: String,  // "RSA", "EC", "DSA", etc.
    val hashAlgorithm: String,       // "SHA256", "SHA384", etc.
    val deprecated: Boolean = false,
    val disabled: Boolean = false
) {
    // RSA-based
    MD5_WITH_RSA("MD5withRSA", "RSA", "MD5", disabled = true),
    SHA1_WITH_RSA("SHA1withRSA", "RSA", "SHA1", deprecated = true),
    SHA256_WITH_RSA("SHA256withRSA", "RSA", "SHA256"),
    SHA384_WITH_RSA("SHA384withRSA", "RSA", "SHA384"),
    SHA512_WITH_RSA("SHA512withRSA", "RSA", "SHA512"),
    SHA3_256_WITH_RSA("SHA3-256withRSA", "RSA", "SHA3-256"),
    SHA3_384_WITH_RSA("SHA3-384withRSA", "RSA", "SHA3-384"),
    SHA3_512_WITH_RSA("SHA3-512withRSA", "RSA", "SHA3-512"),

    // RSA-PSS variants (JDK 17+)
    SHA256_WITH_RSA_PSS("SHA256withRSA/PSS", "RSA", "SHA256"),
    SHA384_WITH_RSA_PSS("SHA384withRSA/PSS", "RSA", "SHA384"),
    SHA512_WITH_RSA_PSS("SHA512withRSA/PSS", "RSA", "SHA512"),

    // EC-based
    SHA1_WITH_ECDSA("SHA1withECDSA", "EC", "SHA1", deprecated = true),
    SHA256_WITH_ECDSA("SHA256withECDSA", "EC", "SHA256"),
    SHA384_WITH_ECDSA("SHA384withECDSA", "EC", "SHA384"),
    SHA512_WITH_ECDSA("SHA512withECDSA", "EC", "SHA512"),

    // DSA-based
    SHA1_WITH_DSA("SHA1withDSA", "DSA", "SHA1", deprecated = true),
    SHA224_WITH_DSA("SHA224withDSA", "DSA", "SHA224"),
    SHA256_WITH_DSA("SHA256withDSA", "DSA", "SHA256"),

    // EdDSA (algorithm = signature algorithm)
    ED25519("Ed25519", "EdDSA", "Ed25519"),
    ED448("Ed448", "EdDSA", "Ed448"),

    // ML-DSA (JDK 24+)
    ML_DSA("ML-DSA", "ML-DSA", "ML-DSA");

    companion object {
        fun fromCliName(name: String): SignatureAlgorithm? {
            return values().find { it.cliName.equals(name, ignoreCase = true) }
        }

        // Auto-select based on key type and size
        fun selectDefault(keyAlgorithm: KeyAlgorithm, keySize: Int): SignatureAlgorithm {
            return when {
                keyAlgorithm == KeyAlgorithm.RSA -> when {
                    keySize < 3072 -> SHA256_WITH_RSA
                    keySize < 7680 -> SHA384_WITH_RSA
                    else -> SHA512_WITH_RSA
                }
                keyAlgorithm == KeyAlgorithm.EC -> when {
                    keySize < 384 -> SHA256_WITH_ECDSA
                    keySize < 512 -> SHA384_WITH_ECDSA
                    else -> SHA512_WITH_ECDSA
                }
                keyAlgorithm == KeyAlgorithm.DSA -> SHA256_WITH_DSA
                keyAlgorithm == KeyAlgorithm.ED25519 -> ED25519
                keyAlgorithm == KeyAlgorithm.ED448 -> ED448
                else -> SHA256_WITH_RSA  // Default fallback
            }
        }
    }
}
```

### Enum 2.3: ECCurve (For EC Keys)

```kotlin
enum class ECCurve(
    val cliName: String,
    val bitLength: Int,
    val stdName: String,
    val aliases: List<String> = emptyList()
) {
    // NIST Curves
    P256("secp256r1", 256, "NIST P-256", listOf("prime256v1", "P-256")),
    P384("secp384r1", 384, "NIST P-384", listOf("P-384")),
    P521("secp521r1", 521, "NIST P-521", listOf("P-521")),

    // Binary Curves (Legacy)
    SECT163K1("sect163k1", 163, "Binary", deprecated = true),
    SECT163R2("sect163r2", 163, "Binary", deprecated = true);

    companion object {
        fun fromCliName(name: String): ECCurve? {
            return values().find {
                it.cliName.equals(name, ignoreCase = true) ||
                it.aliases.any { alias -> alias.equals(name, ignoreCase = true) }
            }
        }

        fun defaultCurve(): ECCurve = P384  // JDK 25 default
    }
}
```

### Enum 2.4: KeystoreFormat

```kotlin
enum class KeystoreFormat(
    val cliName: String,
    val fileExtension: String,
    val isBinary: Boolean,
    val deprecated: Boolean = false,
    val default: Boolean = false
) {
    JKS("jks", ".jks", true, deprecated = true),
    JCEKS("jceks", ".jceks", true, deprecated = true),
    PKCS12("pkcs12", ".p12", true, default = true),
    PKCS11("pkcs11", ".p11", false),
    WINDOWS_MY("windows-my", null, false);

    companion object {
        fun fromCliName(name: String): KeystoreFormat? {
            return values().find { it.cliName.equals(name, ignoreCase = true) }
        }

        fun defaultFormat(): KeystoreFormat = PKCS12
    }
}
```

---

## 3. UPDATED API WITH TYPE SAFETY

### genKeyPair (Type-Safe Version)

```kotlin
suspend fun genKeyPair(
    keystore: File,
    storePassword: String,
    alias: String,
    keyPassword: String,
    dname: String,
    validity: Int = 90,
    keyAlgorithm: KeyAlgorithm = KeyAlgorithm.RSA,
    keySize: Int? = null,  // null = use default for algorithm
    signatureAlgorithm: SignatureAlgorithm? = null,  // null = auto-select
    ecCurve: ECCurve? = null,  // Required if keyAlgorithm == EC
    extensions: List<String> = emptyList()
): KeytoolResult<Unit> {
    // Validation at compile time + runtime
    require(keyAlgorithm.supportsSignature) {
        "Algorithm $keyAlgorithm does not support signing"
    }

    val actualKeySize = keySize ?: keyAlgorithm.defaultKeySize
    require(actualKeySize in keyAlgorithm.supportedKeySizes) {
        "Key size $actualKeySize not supported for $keyAlgorithm"
    }

    val actualSigAlg = signatureAlgorithm ?:
        SignatureAlgorithm.selectDefault(keyAlgorithm, actualKeySize)

    // Build CLI args...
}
```

### genCert (Type-Safe Version)

```kotlin
suspend fun genCert(
    requestFile: File,
    outFile: File,
    daysValid: Int = 365,
    alias: String,
    keystore: File,
    storePassword: String,
    keyPassword: String? = null,
    extensions: List<String> = emptyList(),
    signatureAlgorithm: SignatureAlgorithm? = null
): KeytoolResult<Unit> {
    // Validation happens automatically via enum
}
```

---

## 4. IMPLEMENTATION STRATEGY

### Phase A: Create Enums (High Priority)
1. Create `KeyAlgorithm.kt` enum
2. Create `SignatureAlgorithm.kt` enum
3. Create `ECCurve.kt` enum
4. Create `KeystoreFormat.kt` enum
5. Create validation utility class

### Phase B: Update KeyToolAPI.kt
1. Change function signatures to use enums
2. Add validation based on enum properties
3. Convert enums to CLI strings before calling keytool
4. Update all 16 functions

### Phase C: Update Tests
1. Integration tests use type-safe enums
2. No string literals for algorithm names
3. Compile-time validation of test parameters
4. IDE autocomplete shows valid options

---

## 5. BENEFITS

| Benefit | Impact |
|---------|--------|
| **Compile-time validation** | Catch typos before runtime |
| **IDE autocomplete** | "SHA" → suggests all SHA algorithms |
| **API clarity** | Easy to see valid options |
| **Self-documenting** | Enum properties explain constraints |
| **Refactoring safety** | Rename algorithm = update everywhere |
| **Prevent invalid combinations** | Can't use ED25519 for key agreement |
| **Future-proof** | Easy to add ML-DSA/ML-KEM support |

---

## 6. TIMELINE IMPACT

### Current Plan
- Phase 2: Unit Tests (1 day)
- Phase 3: Integration Tests (2 days)
- **Total: 3 days**

### With Type Safety First
- **Enum Creation: 1 day** ← NEW, but crucial
- Update KeyToolAPI.kt: 1 day
- Phase 2: Unit Tests (0.5 days - faster with enums)
- Phase 3: Integration Tests (2 days - cleaner tests)
- **Total: 4.5 days** (but much higher quality)

### User Decision Needed
1. **Option A** (Recommended): Do enum changes first, then integration tests
   - Higher quality, more maintainable
   - Better IDE support during development
   - Easier to validate correctness

2. **Option B**: Focus integration tests first, enums later
   - Faster initial validation
   - Refactoring needed when adding enums
   - Tests use string literals

---

**Recommendation**: **Option A** - Type safety first, then integration tests
**Reason**: Enums make integration tests much more reliable and self-documenting

---

**Document Status**: Planning Phase
**Created**: 2026-01-22
**Next**: User approval + feedback on enum design
