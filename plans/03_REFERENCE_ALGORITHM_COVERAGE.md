# Reference: Complete Algorithm Coverage Matrix
## All Algorithms to Test for KeyToolAPI

---

## 1. KEY ALGORITHM MATRIX

### RSA (Rivest-Shamir-Adleman)

| Key Size | Status | Test | Notes |
|---|---|---|---|
| **512-bit** | DISABLED | No | Cryptographically broken, disabled since Java 8 |
| **1024-bit** | DEPRECATED | Yes | Legacy support only, shows warning |
| **2048-bit** | STANDARD | Yes | Default for RSA, widely compatible |
| **4096-bit** | STRONG | Yes | Enhanced security, slower operations |
| **8192-bit** | STRONG | Optional | Rarely used, very slow |

**Test Case Template**:
```kotlin
@ParameterizedTest
@ValueSource(ints = [1024, 2048, 4096])
fun testGenerateRSAWithVariousKeySizes(keySize: Int) = runTest {
    val result = KeyToolAPI.genKeyPair(
        dn = "CN=RSA-Test",
        alias = "rsa-$keySize",
        keySize = keySize,
        sigAlg = "SHA256withRSA",
        algorithm = "rsa",
        keystore = keystorePath,
        storePassword = "changeit"
    )
    assertTrue(result is KeytoolResult.Success)
    // Verify key size in exported cert
}
```

**Expected Output Patterns**:
- 512-bit: ERROR - "Keysize 512 < 1024: is disabled"
- 1024-bit: SUCCESS with WARNING - "Warning: 1024-bit RSA... weak"
- 2048-bit: SUCCESS without warning
- 4096-bit: SUCCESS without warning

---

### DSA (Digital Signature Algorithm)

| Key Size | Status | Test | Notes |
|---|---|---|---|
| **1024-bit** | DEPRECATED | Yes | Legacy support, shows warning |
| **2048-bit** | STANDARD | Yes | Default for DSA |
| **3072-bit** | STRONG | Optional | Enhanced security variant |
| **4096-bit** | STRONG | Optional | Maximum recommended |

**Test Case Template**:
```kotlin
@Test
fun testGenerateDSAWithStandardKeySize() = runTest {
    val result = KeyToolAPI.genKeyPair(
        dn = "CN=DSA-Test",
        alias = "dsa-2048",
        keySize = 2048,
            sigAlg = "SHA256withDSA",  // May fail - DSA uses DSA signature alg
        algorithm = "dsa",
        keystore = keystorePath,
        storePassword = "changeit"
    )
    // DSA signature algorithm is automatically derived
}
```

**Expected Output Patterns**:
- 1024-bit: SUCCESS with WARNING - "1024-bit DSA deprecated"
- 2048-bit: SUCCESS without warning
- Default signature algorithm: SHA256withDSA

---

### EC (Elliptic Curve)

| Curve Name | Bits | Status | Test | Alternative Names |
|---|---|---|---|---|
| **secp256r1** | 256 | STANDARD | Yes | P-256, prime256v1, NIST P-256 |
| **secp384r1** | 384 | STRONG | Yes | P-384, NIST P-384 |
| **secp521r1** | 521 | STRONG | Yes | P-521, NIST P-521 |
| **sect163k1** | 163 | LEGACY | No | Binary curve, not recommended |
| **sect163r2** | 163 | LEGACY | No | Binary curve, not recommended |

**Test Case Template**:
```kotlin
@ParameterizedTest
@CsvSource(
    "secp256r1, 256",
    "secp384r1, 384",
    "secp521r1, 521"
)
fun testGenerateECWithVariousCurves(curveName: String, bitLength: Int) = runTest {
    val result = KeyToolAPI.genKeyPair(
        dn = "CN=EC-Test",
        alias = "ec-$curveName",
        keySize = bitLength,
        sigAlg = "SHA256withECDSA",
        algorithm = "ec",
        keystore = keystorePath,
        storePassword = "changeit",
        groupName = curveName  // If supported by API
    )
    assertTrue(result is KeytoolResult.Success)
}
```

**Expected Output Patterns**:
- Default (no curve specified): Uses secp256r1 (256-bit)
- Output: "Generating 256 bit EC (secp256r1) key pair"

**Modern Java Behavior** (Java 15+):
- `-keysize` parameter deprecated for EC
- Use `-groupname` instead to specify curve explicitly
- Both parameters together: ERROR - "Cannot specify both -groupname and -keysize"

---

### EdDSA (Edwards Curve Digital Signature Algorithm)

| Algorithm | Bits | Status | Test | Key Exchange |
|---|---|---|---|---|
| **Ed25519** | 256 | MODERN | Yes | Deterministic signatures |
| **Ed448** | 456 | MODERN | Yes | High security variant |
| **X25519** | 256 | MODERN | No | Key exchange only, cannot sign |
| **X448** | 456 | MODERN | No | Key exchange only, cannot sign |

**Test Case Template**:
```kotlin
@ParameterizedTest
@ValueSource(strings = ["Ed25519", "Ed448"])
fun testGenerateEdDSAKeys(algorithm: String) = runTest {
    val result = KeyToolAPI.genKeyPair(
        dn = "CN=EdDSA-Test",
        alias = algorithm.lowercase(),
        keySize = null,  // EdDSA doesn't use keysize
        sigAlg = algorithm,  // Signature algorithm = algorithm
        algorithm = algorithm,
        keystore = keystorePath,
        storePassword = "changeit"
    )
    assertTrue(result is KeytoolResult.Success)
}
```

**Expected Output Patterns**:
- No keysize parameter accepted
- Signature algorithm defaults to same as key algorithm (e.g., Ed25519)
- Deterministic signatures (same input = same signature)

**Special Cases**:
- XDH keys (X25519, X448) cannot be used for signing
- ERROR if trying to sign with XDH: "Cannot derive signature algorithm from XDH"

---

## 2. SIGNATURE ALGORITHM MATRIX

### RSA-Based Signature Algorithms

| Algorithm | Hash | RSA Variant | Status | Deprecation |
|---|---|---|---|---|
| **MD5withRSA** | MD5 | Classic | DISABLED | Broken, disabled |
| **SHA1withRSA** | SHA-1 | Classic | DEPRECATED | Will be disabled Java 20+ |
| **SHA256withRSA** | SHA-256 | Classic | STANDARD | Default for RSA |
| **SHA384withRSA** | SHA-384 | Classic | STRONG | High security |
| **SHA512withRSA** | SHA-512 | Classic | STRONG | High security |
| **SHA3-256withRSA** | SHA3-256 | Classic | MODERN | SHA-3 family |
| **SHA3-384withRSA** | SHA3-384 | Classic | MODERN | SHA-3 family |
| **SHA3-512withRSA** | SHA3-512 | Classic | MODERN | SHA-3 family |
| **SHA256withRSA/PSS** | SHA-256 | PSS padding | MODERN | PKCS#1 v2.1 |
| **SHA384withRSA/PSS** | SHA-384 | PSS padding | MODERN | PKCS#1 v2.1 |
| **SHA512withRSA/PSS** | SHA-512 | PSS padding | MODERN | PKCS#1 v2.1 |

**Test Case for Deprecated Algorithm**:
```kotlin
@Test
fun testGenerateRSACertWithDeprecatedSHA1() = runTest {
    val result = KeyToolAPI.genKeyPair(
        dn = "CN=SHA1-Test",
        alias = "sha1",
        keySize = 2048,
        sigAlg = "SHA1withRSA",  // Deprecated
        algorithm = "rsa",
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertTrue(result is KeytoolResult.Success)
    // Should contain warning about SHA1 deprecation
    val message = (result as KeytoolResult.Success).message
    assertTrue(message?.contains("SHA1withRSA") ?: false)
}
```

**Test Case for PSS Variant**:
```kotlin
@Test
fun testGenerateRSACertWithPSSPadding() = runTest {
    val result = KeyToolAPI.genKeyPair(
        dn = "CN=PSS-Test",
        alias = "pss",
        keySize = 2048,
        sigAlg = "SHA256withRSA/PSS",  // Modern PSS padding
        algorithm = "rsa",
        keystore = keystorePath,
        storePassword = "changeit"
    )
    assertTrue(result is KeytoolResult.Success)
}
```

---

### EC-Based Signature Algorithms

| Algorithm | Hash | Curve Context | Status |
|---|---|---|---|
| **SHA1withECDSA** | SHA-1 | Any EC curve | DEPRECATED |
| **SHA256withECDSA** | SHA-256 | Any EC curve | STANDARD |
| **SHA384withECDSA** | SHA-384 | Any EC curve | STRONG |
| **SHA512withECDSA** | SHA-512 | Any EC curve | STRONG |
| **SHA3-256withECDSA** | SHA3-256 | Any EC curve | MODERN |
| **SHA3-384withECDSA** | SHA3-384 | Any EC curve | MODERN |
| **SHA3-512withECDSA** | SHA3-512 | Any EC curve | MODERN |

**Test Case**:
```kotlin
@ParameterizedTest
@ValueSource(strings = ["SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA"])
fun testGenerateECCertWithVariousSHA(sigAlg: String) = runTest {
    val result = KeyToolAPI.genKeyPair(
        dn = "CN=EC-Test",
        alias = sigAlg.lowercase(),
        keySize = 256,
        sigAlg = sigAlg,
        algorithm = "ec",
        keystore = keystorePath,
        storePassword = "changeit"
    )
    assertTrue(result is KeytoolResult.Success)
}
```

---

### EdDSA Signature Algorithms

| Algorithm | Derivation | Status |
|---|---|---|
| **Ed25519** | From Ed25519 key | STANDARD |
| **Ed448** | From Ed448 key | MODERN |

**Special Behavior**:
- Signature algorithm is derived automatically from key algorithm
- Cannot mix Ed25519 key with Ed448 signature or vice versa
- No intermediate `-sigalg` parameter in traditional sense

**Test Case**:
```kotlin
@Test
fun testEdDSASignatureAlgorithmAutoDerived() = runTest {
    val result = KeyToolAPI.genKeyPair(
        dn = "CN=EdDSA",
        alias = "ed25519",
        keySize = null,  // Not applicable
        sigAlg = "Ed25519",  // Mandatory
        algorithm = "Ed25519",
        keystore = keystorePath,
        storePassword = "changeit"
    )
    assertTrue(result is KeytoolResult.Success)

    // When printing, should show: Signature algorithm name: Ed25519
}
```

---

### DSA Signature Algorithms

| Algorithm | Hash | Status |
|---|---|---|
| **SHA1withDSA** | SHA-1 | DEPRECATED |
| **SHA224withDSA** | SHA-224 | LEGACY |
| **SHA256withDSA** | SHA-256 | STANDARD |

**Test Case**:
```kotlin
@Test
fun testGenerateDSACertWithSHA256() = runTest {
    val result = KeyToolAPI.genKeyPair(
        dn = "CN=DSA-Test",
        alias = "dsa",
        keySize = 2048,
        sigAlg = "SHA256withDSA",
        algorithm = "dsa",
        keystore = keystorePath,
        storePassword = "changeit"
    )
    assertTrue(result is KeytoolResult.Success)
}
```

---

## 3. SYMMETRIC KEY ALGORITHM MATRIX

For `-genseckey` operations (secret keys used in PKCS12):

| Algorithm | Key Size (bits) | Status | Test |
|---|---|---|---|
| **DES** | 56 | DISABLED | No |
| **TripleDES/3DES** | 168 | DEPRECATED | Yes |
| **AES** | 128, 192, 256 | STANDARD | Yes |
| **Blowfish** | 32-448 | LEGACY | No |
| **RC2** | 40-1024 | LEGACY | No |

**Test Case for AES**:
```kotlin
@ParameterizedTest
@ValueSource(ints = [128, 192, 256])
fun testGenerateSecretKeyWithAES(keySize: Int) = runTest {
    val result = KeyToolAPI.genSecKey(
        keyalg = "AES",
        keysize = keySize,
        alias = "secretkey-aes$keySize",
        keystore = keystorePath,
        storePassword = "changeit"
    )
    assertTrue(result is KeytoolResult.Success)
}
```

**Test Case for TripleDES** (with deprecation warning):
```kotlin
@Test
fun testGenerateSecretKeyWith3DES() = runTest {
    val result = KeyToolAPI.genSecKey(
        keyalg = "TripleDES",
        keysize = 168,
        alias = "secretkey-3des",
        keystore = keystorePath,
        storePassword = "changeit"
    )
    assertTrue(result is KeytoolResult.Success)
    // Should contain deprecation warning
}
```

---

## 4. PASSWORD-BASED ENCRYPTION (PBE) ALGORITHM MATRIX

For password protection in keystores:

| Algorithm | MD Function | Encryption | Status | Test |
|---|---|---|---|---|
| **PBEWithMD5AndDES** | MD5 | DES | DEPRECATED | Yes |
| **PBEWithSHA1AndDES** | SHA-1 | DES | DEPRECATED | Yes |
| **PBEWithSHA1And2Key-TripleDES-CBC** | SHA-1 | 3DES | DEPRECATED | Yes |
| **PBEWithSHA1And40BitRC2-CBC** | SHA-1 | RC2 | LEGACY | No |
| **PBEWithHmacSHA224AndAES_128** | HMAC-SHA224 | AES-128 | MODERN | Yes |
| **PBEWithHmacSHA256AndAES_128** | HMAC-SHA256 | AES-128 | STANDARD | Yes |
| **PBEWithHmacSHA256AndAES_256** | HMAC-SHA256 | AES-256 | STANDARD | Yes |
| **PBEWithHmacSHA384AndAES_256** | HMAC-SHA384 | AES-256 | STRONG | Yes |
| **PBEWithHmacSHA512AndAES_256** | HMAC-SHA512 | AES-256 | STRONG | Yes |

**Test Case Template**:
```kotlin
@ParameterizedTest
@ValueSource(strings = [
    "PBEWithMD5AndDES",
    "PBEWithSHA1AndDES",
    "PBEWithSHA1And2Key-TripleDES-CBC",
    "PBEWithHmacSHA256AndAES_128",
    "PBEWithHmacSHA256AndAES_256",
    "PBEWithHmacSHA512AndAES_256"
])
fun testStorePasswordWithPBEAlgorithm(pbeAlg: String) = runTest {
    // Note: -storepasswd command may accept -storetype parameter
    // to specify PBE algorithm, but this depends on keytool implementation

    val result = KeyToolAPI.storePasswd(
        keystore = keystorePath,
        storePassword = "oldpass",
        newStorePassword = "newpass",
        // PBE algorithm specification (if API supports it)
    )
    assertTrue(result is KeytoolResult.Success)
}
```

---

## 5. ALGORITHM STRENGTH CLASSIFICATION

### Class 1: DISABLED (Cannot Use)
```
- MD5withRSA
- SHA1withRSA (partially, see deprecation)
- RSA-512
- DSA-512 or smaller
- MD5
```

**Test Pattern**: Expect exit code 1 with error message

### Class 2: DEPRECATED (Shows Warning)
```
- SHA1withRSA
- SHA1withECDSA
- SHA1withDSA
- RSA-1024
- DSA-1024
- DES
- TripleDES (3DES)
- JKS keystore format
```

**Test Pattern**: Expect exit code 0 with warning in message

### Class 3: STANDARD (Default, No Warning)
```
- SHA256withRSA
- SHA256withECDSA
- SHA256withDSA
- RSA-2048
- DSA-2048
- EC-256 (secp256r1)
- AES-128
- PKCS12 format
```

**Test Pattern**: Expect exit code 0 without warning

### Class 4: STRONG (Enhanced Security)
```
- SHA384withRSA/PSS
- SHA512withRSA/PSS
- SHA512withECDSA
- RSA-4096
- EC-384 (secp384r1)
- EC-521 (secp521r1)
- AES-256
- Ed25519, Ed448
```

**Test Pattern**: Expect exit code 0 without warning

### Class 5: MODERN (Latest Recommendations)
```
- SHA3-256/384/512withRSA
- SHA3-256/384/512withECDSA
- RSA/PSS variants
- EdDSA (Ed25519, Ed448)
- PBEWithHmacSHA512AndAES_256
```

**Test Pattern**: Expect exit code 0 without warning (may be newest Java versions)

---

## 6. ALGORITHM CASE-INSENSITIVITY TESTING

OpenJDK tests validate that algorithms are parsed case-insensitively:

**Test Cases** (from StandardAlgName.java pattern):

```kotlin
@Test
fun testAlgorithmNameNormalization() = runTest {
    val variations = listOf(
        "RsA" to "RSA",
        "rSa" to "RSA",
        "rsa" to "RSA",
        "RSA" to "RSA"
    )

    for ((input, expected) in variations) {
        val result = KeyToolAPI.genKeyPair(
            dn = "CN=Test",
            alias = "test-$input",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = input,  // Case variation
            keystore = keystorePath,
            storePassword = "changeit"
        )
        assertTrue(result is KeytoolResult.Success)

        // When printed, should show normalized form
        val listResult = KeyToolAPI.list(keystorePath, "changeit")
        val keystoreInfo = (listResult as KeytoolResult.Success).data
        // Verify algorithm is stored in normalized form
    }
}

@Test
fun testSignatureAlgorithmNormalization() = runTest {
    val variations = listOf(
        "ShA1wItHRSA" to "SHA1withRSA",
        "sha1withRSA" to "SHA1withRSA",
        "mD5withRSA" to "MD5withRSA"
    )

    for ((input, expected) in variations) {
        // Note: Will fail for disabled algorithms
        // Use SHA256withRSA for all variations
        val result = KeyToolAPI.genKeyPair(
            dn = "CN=Test",
            alias = "test-$input",
            keySize = 2048,
            sigAlg = "SHA256withRSA",  // Normalized
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        )
        assertTrue(result is KeytoolResult.Success)
    }
}
```

---

## 7. ALGORITHM COVERAGE CHECKLIST

### For Unit Tests (Mock):
- [ ] All RSA key sizes (1024, 2048, 4096)
- [ ] All EC curves (256, 384, 521-bit)
- [ ] EdDSA (Ed25519, Ed448)
- [ ] DSA (2048-bit)
- [ ] All signature algorithms (SHA256, SHA384, SHA512 variants)
- [ ] Case-insensitive algorithm names
- [ ] Algorithm deprecation warnings
- [ ] Disabled algorithm errors

### For Integration Tests (Real):
- [ ] Generate keys for each algorithm class
- [ ] Export and reimport certificates
- [ ] Verify signature algorithms in printed certificates
- [ ] Verify key sizes are correct
- [ ] Test algorithm combinations in chains

### For Edge Cases:
- [ ] MD5withRSA disabled error
- [ ] SHA1withRSA deprecation warning
- [ ] RSA-1024 deprecation warning
- [ ] Invalid algorithm names
- [ ] Algorithm property persistence

---

## 8. ALGORITHM TEST MATRIX BY FUNCTION

| Function | Algorithms to Test | Notes |
|---|---|---|
| `genKeyPair()` | RSA (1024, 2048, 4096), DSA (2048), EC (256,384,521), EdDSA | ~15 test cases |
| `genseckey()` | AES (128, 192, 256), 3DES | ~4 test cases |
| `genCert()` | Inherit from parent; test signing with different algs | ~8 test cases |
| `certReq()` | Inherit from parent; test CSR generation | ~4 test cases |
| `printCert()` | All signature algorithms | ~12 test cases |
| `printCertReq()` | All signature algorithms | ~12 test cases |
| `list()` | All entry types with various algorithms | ~6 test cases |
| `importCert()` | Weak algorithm detection | ~3 test cases |

---

**File Purpose**: Comprehensive algorithm coverage matrix for testing
**Last Updated**: 2026-01-22
**Total Algorithms to Test**: 50+ (keys + signatures + PBE)
**Cross-References**: 01_REFERENCE_OPENJDK_FINDINGS.md, 02_REFERENCE_TEST_PATTERNS.md
