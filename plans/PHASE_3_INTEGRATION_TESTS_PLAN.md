# PHASE 3: Integration Tests Implementation Plan
## Real Keytool Operations with Actual Keystores (50-70 tests)

**Status**: Planning Phase - Awaiting User Review
**Estimated Scope**: 50-70 integration tests using real keytool binary
**Duration**: Implementation after Phase 2 approval
**Files Modified**: 1 (KeyToolAPIIntegrationTests.kt)

---

## 1. OVERVIEW

### Objectives
1. Validate KeyToolAPI functions work with real keytool binary
2. Test multi-operation workflows (generate → sign → import → list)
3. Verify certificate chain generation and validation
4. Test format conversion (JKS ↔ PKCS12)
5. Validate algorithm-specific operations
6. Test error scenarios in real-world conditions

### Approach
- Use temporary keystores and certificate files
- Execute real keytool operations (no mocking)
- Clean up all temporary files in finally blocks
- Test complete workflows, not isolated operations
- Verify results by listing/exporting keystores
- Use @Timeout for long-running operations

### Expected Outcome
- All 16 functions validated with real keytool
- 10+ multi-operation workflows tested
- Certificate chain scenarios (2-level, 3-level, deduplication)
- Format conversion fully tested
- Algorithm variants tested (RSA, EC, DSA, EdDSA)
- Integration tests can be optional (e.g., `-PintTest` flag)

---

## 2. TEST CATEGORIES

### Category 1: Single Operation Validation (30 tests)

For each of 16 functions, validate it works end-to-end with real keytool.

**Pattern**:
```kotlin
@Test
fun testListWithRealKeystore() = runTest {
    val keystorePath = createTemporaryKeystore(
        alias = "test",
        password = "changeit"
    )

    try {
        val result = KeyToolAPI.list(keystorePath, "changeit")

        assertTrue(result is KeytoolResult.Success)
        val info = (result as KeytoolResult.Success).data
        assertEquals(1, info.entries.size)
        assertEquals("test", info.entries[0].alias)
    } finally {
        keystorePath.delete()
    }
}
```

**Coverage** (30 tests):
- listKeystore()
- genKeyPair() with various algorithms (3 tests: RSA, EC, EdDSA)
- genSecKey() (2 tests: AES, 3DES)
- genCert()
- certReq()
- exportCert()
- importCert()
- importKeystore()
- importPass()
- delete()
- changeAlias()
- keyPasswd()
- storePasswd()
- printCert()
- printCertReq()
- printCrl()

**Files to Reference**: 02_REFERENCE_TEST_PATTERNS.md (Section 2A-2E)

---

### Category 2: Certificate Chain Generation (12 tests)

Test multi-step workflows to create and validate chains.

#### 2.1 Basic 2-Level Chain (2 tests)
```kotlin
@Test
fun testGenerate2LevelChain() = runTest {
    // Step 1: Create CA
    // Step 2: Create user key
    // Step 3: Create CSR
    // Step 4: CA signs CSR
    // Step 5: Import signed cert
    // Verify: Chain length = 2
}

// Tests:
// - Standard 2-level chain
// - 2-level chain with custom extensions
```

**Files to Reference**: 05_REFERENCE_CERTIFICATE_CHAINS.md (Section 1)

#### 2.2 Hierarchical 3-4 Level Chain (3 tests)
```kotlin
@Test
fun testGenerateHierarchical4LevelChain() = runTest {
    // Create: CA → CA1 → CA2 → E1
    // Delete intermediates
    // Import final cert
    // Verify: Chain reconstructed
}

// Tests:
// - 3-level chain (CA → intermediate → entity)
// - 4-level self-issued chain
// - Chain with missing intermediates (auto-recovery)
```

**Files to Reference**: 05_REFERENCE_CERTIFICATE_CHAINS.md (Section 2)

#### 2.3 Certificate Deduplication (2 tests)
```kotlin
@Test
fun testCertificateDeduplicationOnImport() = runTest {
    // Create chain file with duplicates
    // Import deduplicated chain
    // Verify: Duplicates removed, order correct
}

// Tests:
// - Deduplication with 2 certs
// - Deduplication with 3+ certs and complex ordering
```

**Files to Reference**: 05_REFERENCE_CERTIFICATE_CHAINS.md (Section 3)

#### 2.4 AKID Chain Validation (2 tests)
```kotlin
@Test
fun testAKIDMatchesParentSKID() = runTest {
    // Generate parent with explicit SKID
    // Generate child with AKID
    // Verify: AKID matches parent SKID
}

// Tests:
// - AKID auto-generated from parent SKID
// - AKID validation in chain
```

**Files to Reference**: 05_REFERENCE_CERTIFICATE_CHAINS.md (Section 4)

#### 2.5 Empty Subject with SAN (2 tests)
```kotlin
@Test
fun testEmptySubjectRequiresSAN() = runTest {
    // Generate cert with empty subject
    // Without SAN: should fail
    // With SAN: should succeed
}

// Tests:
// - Empty subject without SAN (fail)
// - Empty subject with SAN (success)
```

**Files to Reference**: 05_REFERENCE_CERTIFICATE_CHAINS.md (Section 5)

#### 2.6 Chain Preservation in Format Conversion (1 test)
```kotlin
@Test
fun testChainPreservedInFormatConversion() = runTest {
    // Create 2-level chain in JKS
    // Convert to PKCS12
    // Verify chain length unchanged
}
```

**Files to Reference**: 05_REFERENCE_CERTIFICATE_CHAINS.md (Section 6)

---

### Category 3: Algorithm-Specific Testing (12 tests)

Test operations with all supported algorithms.

#### 3.1 RSA Variants (3 tests)
```kotlin
@ParameterizedTest
@ValueSource(ints = [1024, 2048, 4096])
fun testRSAKeyGeneration(keySize: Int) = runTest {
    // Generate RSA key of specific size
    // Export cert
    // Verify key size in exported certificate
}
```

#### 3.2 EC Curve Variants (3 tests)
```kotlin
@ParameterizedTest
@ValueSource(strings = ["secp256r1", "secp384r1", "secp521r1"])
fun testECKeyGeneration(curveName: String) = runTest {
    // Generate EC key with specific curve
    // Verify curve in certificate
}
```

#### 3.3 DSA (2 tests)
```kotlin
@Test
fun testDSAKeyGeneration() = runTest {
    // Generate 2048-bit DSA key
    // Verify in list output
}

@Test
fun testDSAWithDeprecationWarning() = runTest {
    // 1024-bit DSA shows deprecation warning
}
```

#### 3.4 EdDSA (2 tests)
```kotlin
@ParameterizedTest
@ValueSource(strings = ["Ed25519", "Ed448"])
fun testEdDSAKeyGeneration(algorithm: String) = runTest {
    // Generate EdDSA key
    // Verify deterministic signatures
}
```

#### 3.5 Secret Keys (2 tests)
```kotlin
@ParameterizedTest
@ValueSource(strings = ["AES", "TripleDES"])
fun testSecretKeyGeneration(algorithm: String) = runTest {
    // Generate secret key
    // Verify in PKCS12 keystore
}
```

**Files to Reference**: 03_REFERENCE_ALGORITHM_COVERAGE.md (Sections 1-4)

---

### Category 4: Format Conversion (8 tests)

Test keystore format conversion and compatibility.

#### 4.1 JKS to PKCS12 (2 tests)
```kotlin
@Test
fun testJKSToPKCS12Conversion() = runTest {
    // Create JKS with multiple entries
    // Convert to PKCS12
    // Verify all entries present
    // Verify passwords work
}

@Test
fun testJKSToPKCS12WithSeparatePasswords() = runTest {
    // JKS with separate store/key passwords
    // Convert to PKCS12 (unified password)
    // Verify conversion successful
}
```

#### 4.2 PKCS12 to JKS (2 tests)
```kotlin
@Test
fun testPKCS12ToJKSConversion() = runTest {
    // Create PKCS12
    // Convert to JKS
    // Verify result
}
```

#### 4.3 Keystore Type Detection (2 tests)
```kotlin
@Test
fun testAutomaticKeystoreTypeDetection() = runTest {
    // Without -storetype: tool detects format
    // Verify correct detection
}
```

#### 4.4 Password Handling Across Formats (2 tests)
```kotlin
@Test
fun testPasswordUnificationInPKCS12() = runTest {
    // JKS with separate store/key passwords
    // Loaded as PKCS12: unified password
    // Modify password in PKCS12 context
    // Verify unified behavior
}
```

**Files to Reference**: 05_REFERENCE_CERTIFICATE_CHAINS.md (Section 6), 01_REFERENCE_OPENJDK_FINDINGS.md (Section 8)

---

### Category 5: Multi-Operation Workflows (10 tests)

Test complete realistic workflows combining multiple operations.

#### 5.1 Complete Certificate Lifecycle (4 tests)
```kotlin
@Test
fun testCompleteCertificateLifecycle() = runTest {
    // 1. Generate keypair
    // 2. Create CSR
    // 3. Sign CSR (CA)
    // 4. Import cert
    // 5. Rename alias
    // 6. Export cert
    // 7. Import to different keystore
    // 8. Delete original
    // 9. List final keystore
    // Verify all steps successful
}

// Tests:
// - Basic lifecycle
// - Lifecycle with format conversion
// - Lifecycle with password changes
// - Lifecycle with certificate replacement
```

#### 5.2 Bulk Key Management (3 tests)
```kotlin
@Test
fun testBulkCertificateImport() = runTest {
    // Create multiple certificates
    // Import chain with multiple entries
    // Verify all imported with correct aliases
}

// Tests:
// - Import multiple certs from file
// - Import multiple keystores
// - Bulk password changes
```

#### 5.3 Certificate Renewal (2 tests)
```kotlin
@Test
fun testCertificateRenewal() = runTest {
    // Create certificate (expires in 1 year)
    // Generate new certificate (expires in 3 years)
    // Import new cert (replaces old)
    // Verify chain length and dates
}
```

#### 5.4 Cross-Keystore Operations (1 test)
```kotlin
@Test
fun testCrossKeystoreImportExport() = runTest {
    // Create cert in keystore A
    // Export cert
    // Import to keystore B
    // Delete from keystore A
    // Verify present in B only
}
```

**Files to Reference**: 02_REFERENCE_TEST_PATTERNS.md (Section 2B-2E)

---

### Category 6: Error Handling (8 tests)

Test error conditions in integration context.

#### 6.1 Password-Related Errors (2 tests)
```kotlin
@Test
fun testWrongPasswordHandling() = runTest {
    // Create keystore
    // Try to list with wrong password
    // Verify error returned
}

@Test
fun testPasswordChangeValidation() = runTest {
    // Change password
    // Verify old password doesn't work
    // Verify new password works
}
```

#### 6.2 File Operation Errors (2 tests)
```kotlin
@Test
fun testExportedFileOperations() = runTest {
    // Export certificate
    // Verify file can be deleted (stream closed)
    // Verify file size reasonable
}

@Test
fun testMissingKeystoreFile() = runTest {
    // Try to list nonexistent file
    // Verify error
}
```

#### 6.3 Validation Errors (2 tests)
```kotlin
@Test
fun testInvalidCertificateImport() = runTest {
    // Create corrupted certificate file
    // Try to import
    // Verify error
}

@Test
fun testChainValidationFailure() = runTest {
    // Try to import cert without chain
    // Verify error or prompt handling
}
```

#### 6.4 Algorithm Errors (2 tests)
```kotlin
@Test
fun testDisabledAlgorithmRejection() = runTest {
    // Try to use MD5withRSA
    // Verify error
}

@Test
fun testWeakAlgorithmWarning() = runTest {
    // Use 1024-bit RSA
    // Verify warning in output
    // Operation still succeeds
}
```

**Files to Reference**: 04_REFERENCE_ERROR_CONDITIONS.md

---

## 3. TEST STRUCTURE & HELPERS

### Integration Test Base Class
```kotlin
abstract class KeyToolAPIIntegrationTest {
    protected lateinit var tempDir: File

    @BeforeEach
    open fun setup() {
        tempDir = createTempDirectory("keytool-test-")
    }

    @AfterEach
    open fun cleanup() {
        tempDir.deleteRecursively()
    }

    protected suspend fun createTestKeystore(
        alias: String = "test",
        algorithm: String = "rsa",
        keySize: Int = 2048,
        password: String = "changeit"
    ): File {
        val keystorePath = File(tempDir, "test.jks")
        val result = KeyToolAPI.genKeyPair(
            dn = "CN=$alias",
            alias = alias,
            keySize = keySize,
            sigAlg = "SHA256withRSA",
            algorithm = algorithm,
            keystore = keystorePath,
            storePassword = password
        )
        require(result is KeytoolResult.Success) { "Failed to create test keystore" }
        return keystorePath
    }

    protected suspend fun assertKeystoreContainsAlias(
        keystorePath: File,
        password: String,
        alias: String
    ) {
        val result = KeyToolAPI.list(keystorePath, password).shouldBeSuccess()
        assertTrue(result.entries.any { it.alias == alias },
                   "Keystore should contain alias $alias")
    }
}
```

### Common Test Helpers
```kotlin
fun File.createCertificateChain(
    rootDN: String,
    intermediateDN: String,
    entityDN: String,
    password: String
): Triple<File, File, File> {
    // Create 3-level chain
    // Return (rootCert, intermediateCert, entityCert)
}

fun File.getCertificateChainLength(
    keystorePath: File,
    password: String,
    alias: String
): Int {
    // Return chain length for given alias
}

fun File.verifyCertificateAlgorithm(
    expectedKeyAlg: String,
    expectedSigAlg: String,
    expectedKeySize: String
) {
    // Parse exported cert and verify algorithms
}
```

**Files to Reference**: 02_REFERENCE_TEST_PATTERNS.md (Section 4)

---

## 4. CONFIGURATION & EXECUTION

### Test Execution Strategy

#### Option A: Separate Suite (Recommended)
```kotlin
// In build.gradle.kts
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

// Run integration tests separately:
// ./gradlew integrationTest
```

#### Option B: Optional Conditional
```kotlin
// In test class
@Tag("integration")
class KeyToolAPIIntegrationTests {
    // tests with @Timeout(30) or longer
}

// Run with: ./gradlew test -PincludeIntegration=true
```

#### Option C: Always Run
```kotlin
// Include in regular test suite
// Use @Timeout for long-running tests
```

**User Input Needed**: Which execution strategy preferred?

### Timeout Strategy
```kotlin
class KeyToolAPIIntegrationTests {
    @Test
    @Timeout(10)  // 10 seconds per test
    fun testSingleOperation() = runTest { ... }

    @Test
    @Timeout(30)  // 30 seconds for multi-operation
    fun testCompleteWorkflow() = runTest { ... }
}

// Total estimated time: 10-15 minutes for all 50-70 tests
```

---

## 5. IMPLEMENTATION CHECKLIST

### Prerequisites
- [ ] Phase 2 (Unit Tests) completed and passing
- [ ] Review 05_REFERENCE_CERTIFICATE_CHAINS.md for chain patterns
- [ ] Review 02_REFERENCE_TEST_PATTERNS.md for integration patterns
- [ ] Review 03_REFERENCE_ALGORITHM_COVERAGE.md for algorithm tests
- [ ] Prepare test data generation helpers
- [ ] Decide on test execution strategy

### During Implementation
- [ ] Create KeyToolAPIIntegrationTests.kt (or separate file per category)
- [ ] Implement Category 1 tests (30 single operations)
- [ ] Implement Category 2 tests (12 chain scenarios)
- [ ] Implement Category 3 tests (12 algorithm variants)
- [ ] Implement Category 4 tests (8 format conversions)
- [ ] Implement Category 5 tests (10 workflows)
- [ ] Implement Category 6 tests (8 error scenarios)
- [ ] Create shared helpers and base classes
- [ ] Verify all temporary files are cleaned up
- [ ] Test file cleanup on exceptions (finally blocks)
- [ ] Run full suite and verify timing < 15 minutes

### After Implementation
- [ ] Count total integration tests (should be 50-70)
- [ ] Verify no test pollution (each independent)
- [ ] Document any platform-specific issues
- [ ] Create execution documentation
- [ ] Measure total test time
- [ ] Prepare Phase 4 (Edge Cases)

---

## 6. EXPECTED BENEFITS

### Over Unit Tests
```
✓ Validates with actual keytool binary
✓ Tests complete workflows, not isolated operations
✓ Catches integration issues (file I/O, chain building)
✓ Validates real-world password handling
✓ Tests format conversion with actual keystores
✓ Validates error messages from real keytool
```

### Coverage Improvements
```
- Single operations: 30 tests
- Multi-operation workflows: 10 tests
- Certificate chains: 12 tests
- Algorithm variants: 12 tests
- Format conversions: 8 tests
- Error scenarios: 8 tests
Total: 80 additional test cases beyond unit tests
```

---

## 7. CROSS-REFERENCES

| Section | Reference | Details |
|---|---|---|
| Chain Testing | 05_REFERENCE_CERTIFICATE_CHAINS.md | 8 chain scenarios |
| Test Patterns | 02_REFERENCE_TEST_PATTERNS.md | Integration test patterns |
| Algorithms | 03_REFERENCE_ALGORITHM_COVERAGE.md | All algorithms to test |
| OpenJDK Tests | 01_REFERENCE_OPENJDK_FINDINGS.md | Base test patterns |
| Error Handling | 04_REFERENCE_ERROR_CONDITIONS.md | Error scenarios |

---

## 8. QUESTIONS FOR USER

1. **Execution**: Should integration tests be:
   - Skipped by default (optional `-PintTest`)?  ✓ Recommended
   - Run with regular tests (slower build)?
   - In separate CI pipeline?

2. **Timeout**: Is 15 minutes total for all 50-70 tests:
   - Acceptable?
   - Should be faster?
   - Can be slower?

3. **File Cleanup**: Should temporary files be:
   - In system temp directory? ✓ Recommended
   - In `build/test` directory?
   - Custom location?

4. **Platform Support**: Should tests:
   - Work on Linux only (current)?
   - Support Windows + macOS + Linux?
   - Have conditional tests?

5. **Parallelization**: Should tests:
   - Run sequentially (safer)? ✓ Recommended
   - Run in parallel (faster)?

---

## 9. SUCCESS CRITERIA

### Phase 3 Complete When:
- [ ] 50-70 integration tests implemented and passing
- [ ] All 16 functions validated with real keytool
- [ ] 10+ multi-operation workflows tested
- [ ] 12+ certificate chain scenarios tested
- [ ] Algorithm variants (RSA, EC, DSA, EdDSA) tested
- [ ] Format conversion fully tested
- [ ] Error scenarios tested in real context
- [ ] Total test time < 15 minutes
- [ ] All temporary files properly cleaned up
- [ ] Tests are independent and can run in any order
- [ ] Tests have appropriate timeouts
- [ ] Code coverage > 90% with unit + integration

---

**Document Status**: Ready for User Review
**Created**: 2026-01-22
**Next Step**: User approval → Implementation of Phase 3
**Depends On**: Phase 2 completion

**Phase Sequence**: Phase 1 (Research) ✓ → Phase 2 (Unit Tests) → Phase 3 (Integration Tests) → Phase 4 (Edge Cases) → Phase 5 (Data) → Phase 6 (Verification)
