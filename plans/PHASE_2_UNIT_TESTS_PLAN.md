# PHASE 2: Unit Tests Implementation Plan
## Mock-Based Tests for KeyToolAPI (60-70 tests)

**Status**: Planning Phase - Awaiting User Review
**Estimated Scope**: 60-70 unit tests using mocked ProcessBuilder output
**Duration**: Implementation after approval
**Files Modified**: 1 (KeyToolAPIUnitTests.kt)

---

## 1. OVERVIEW

### Objectives
1. Replace 50 existing mock-based tests with OpenJDK-aligned tests
2. Add missing test cases for edge conditions
3. Validate parsing robustness (exit code 0 + malformed output = error)
4. Test all error message patterns from keytool
5. Maintain fast test execution (< 5 seconds total for unit tests)

### Approach
- Use `runTest` from kotlinx.coroutines.test for suspend functions
- Mock ProcessBuilder with ByteArrayInputStream for output
- Test both success and failure paths
- Validate error messages match OpenJDK patterns
- Parametrize algorithm tests to avoid repetition

### Expected Outcome
- All 16 functions have 4-5 unit tests each
- Comprehensive error coverage (25+ error scenarios)
- Parsing validation for malformed outputs
- Fast, isolated tests with no real I/O

---

## 2. TEST STRUCTURE

### File Organization
```
KeyToolAPIUnitTests.kt
├── Imports & Setup
├── Mock Process Creation
├── Mock Factory Helpers
├── Test Classes by Function
│   ├── ListFunctionTests (5 tests)
│   ├── GenKeyPairFunctionTests (5 tests)
│   ├── GenSecKeyFunctionTests (3 tests)
│   ├── GenCertFunctionTests (4 tests)
│   ├── CertReqFunctionTests (3 tests)
│   ├── ExportCertFunctionTests (4 tests)
│   ├── ImportCertFunctionTests (5 tests)
│   ├── ImportKeystoreFunctionTests (4 tests)
│   ├── ImportPassFunctionTests (2 tests)
│   ├── DeleteFunctionTests (3 tests)
│   ├── ChangeAliasFunctionTests (3 tests)
│   ├── KeyPasswdFunctionTests (3 tests)
│   ├── StorePasswdFunctionTests (3 tests)
│   ├── PrintCertFunctionTests (4 tests)
│   ├── PrintCertReqFunctionTests (3 tests)
│   └── PrintCrlFunctionTests (3 tests)
├── Error Condition Tests (15+ tests)
├── Algorithm Normalization Tests (5 tests)
└── Parsing Edge Case Tests (5 tests)
```

### Test Class Pattern
```kotlin
class ListFunctionTests {
    private lateinit var keystorePath: File

    @BeforeEach
    fun setup() {
        keystorePath = createTempFile("test-", ".jks")
    }

    @AfterEach
    fun cleanup() {
        keystorePath.delete()
        // Restore default process factory
        KeyToolAPI.processFactory = { command ->
            ProcessBuilder(command).start()
        }
    }

    @Test
    fun testListSuccessWithValidKeystore() = runTest {
        // Test implementation
    }

    // ... more tests
}
```

---

## 3. TEST CATEGORIES & COVERAGE

### Category 1: Basic Success Cases (30 tests)

For each of 16 functions, test the success case with typical valid output.

**Example Test**:
```kotlin
@Test
fun testListSuccessWithValidKeystore() = runTest {
    val mockOutput = """
        Keystore type: JKS
        Keystore provider: SUN

        Your keystore contains 2 entries:

        myalias, Jan 22, 2026, PrivateKeyEntry, ...
        castore, Jan 22, 2026, TrustedCertEntry, ...
    """.trimIndent()

    setupMockFactory(mockOutput)

    val result = KeyToolAPI.list(keystorePath, "password")

    assertTrue(result is KeytoolResult.Success)
    val info = (result as KeytoolResult.Success).data
    assertEquals("JKS", info.type)
    assertEquals(2, info.entries.size)
}
```

**Coverage**:
- ✓ list() success
- ✓ genKeyPair() success (various algorithms)
- ✓ genSecKey() success
- ✓ genCert() success
- ✓ certReq() success
- ✓ exportCert() success
- ✓ importCert() success
- ✓ importKeystore() success
- ✓ importPass() success
- ✓ delete() success
- ✓ changeAlias() success
- ✓ keyPasswd() success
- ✓ storePasswd() success
- ✓ printCert() success
- ✓ printCertReq() success
- ✓ printCrl() success

**Files to Reference**: 02_REFERENCE_TEST_PATTERNS.md (Section 1A)

---

### Category 2: Error Condition Tests (25+ tests)

Test various error scenarios matching OpenJDK patterns.

**Sub-categories**:

#### 2.1 Input Validation Errors (5 tests)
```kotlin
@Test
fun testGenKeyPairWithoutKeyalg() = runTest {
    setupMockFactory(
        "",
        "keytool error: option -keyalg must be specified",
        1
    )

    val result = KeyToolAPI.genKeyPair(...)
    assertErrorMatches(result, "keyalg must be specified")
}

// Tests:
// - Missing -keyalg parameter
// - Both -groupname and -keysize specified
// - Invalid keysize value
// - Ambiguous extension option
// - Unknown parameter
```

**Files to Reference**: 04_REFERENCE_ERROR_CONDITIONS.md (Section 1)

#### 2.2 Keystore & File Errors (5 tests)
```kotlin
@Test
fun testKeystoreFileNotFound() = runTest {
    setupMockFactory("", "keytool error: FileNotFoundException", 1)

    val result = KeyToolAPI.list(File("/nonexistent.jks"), "pass")
    assertErrorMatches(result, "FileNotFoundException")
}

// Tests:
// - Keystore file not found
// - Wrong keystore password
// - Unrecognized keystore format
// - Cannot read keystore (permission denied)
// - File already exists (export)
```

**Files to Reference**: 04_REFERENCE_ERROR_CONDITIONS.md (Section 2)

#### 2.3 Certificate Chain Errors (5 tests)
```kotlin
@Test
fun testImportCertFailedChain() = runTest {
    setupMockFactory(
        "",
        "keytool error: Failed to establish chain from reply",
        1
    )

    val result = KeyToolAPI.importCert(...)
    assertErrorMatches(result, "Failed to establish chain")
}

// Tests:
// - Failed to establish chain from reply
// - Public keys don't match
// - Certificate chain too long
// - Invalid certificate format
// - Certificate untrusted (may prompt)
```

**Files to Reference**: 04_REFERENCE_ERROR_CONDITIONS.md (Section 3)

#### 2.4 Alias & Entry Errors (4 tests)
```kotlin
@Test
fun testAliasAlreadyExists() = runTest {
    setupMockFactory("", "keytool error: Alias <test> already exists", 1)

    val result = KeyToolAPI.genKeyPair(..., alias = "test")
    assertErrorMatches(result, "already exists")
}

// Tests:
// - Alias already exists
// - Alias does not exist
// - Entry type mismatch
```

**Files to Reference**: 04_REFERENCE_ERROR_CONDITIONS.md (Section 4)

#### 2.5 Algorithm & Crypto Errors (4 tests)
```kotlin
@Test
fun testDisabledMD5Algorithm() = runTest {
    setupMockFactory("", "keytool error: MD5withRSA is disabled", 1)

    val result = KeyToolAPI.genKeyPair(..., sigAlg = "MD5withRSA")
    assertErrorMatches(result, "disabled")
}

// Tests:
// - Algorithm not available
// - Cannot derive signature algorithm
// - Weak/disabled algorithm
// - RSA key too small (512-bit)
```

**Files to Reference**: 04_REFERENCE_ERROR_CONDITIONS.md (Section 5)

#### 2.6 Parsing & Output Errors (2 tests)
```kotlin
@Test
fun testMalformedKeystoreOutput() = runTest {
    val malformedOutput = "Keystore type: \nInvalid format"
    setupMockFactory(malformedOutput, "", 0)

    val result = KeyToolAPI.list(...)
    assertTrue(result is KeytoolResult.Error)
    assertErrorMatches(result, "parse")
}

// Tests:
// - Malformed output (exit code 0 but unparseable)
// - Missing critical fields in output
```

**Files to Reference**: 04_REFERENCE_ERROR_CONDITIONS.md (Section 8)

---

### Category 3: Warning Condition Tests (5 tests)

Test cases where exit code is 0 but warnings are included.

```kotlin
@Test
fun testDeprecatedAlgorithmWarning() = runTest {
    val mockOutput = """
        Generating keypair...
        Warning: The generated certificate ... SHA1withRSA ...
        is deprecated and will be disabled in a future release
    """.trimIndent()

    setupMockFactory(mockOutput, "", 0)

    val result = KeyToolAPI.genKeyPair(
        ...,
        sigAlg = "SHA1withRSA",  // Deprecated
        keySize = 1024  // Deprecated
    )

    assertTrue(result is KeytoolResult.Success)
    val message = (result as KeytoolResult.Success).message
    assertTrue(message?.contains("Warning") ?: false)
}

// Tests:
// - SHA1withRSA deprecated warning
// - 1024-bit RSA deprecated warning
// - JKS format deprecated warning
// - Untrusted certificate import prompt
// - Weak algorithm warning (visible in list)
```

**Files to Reference**: 04_REFERENCE_ERROR_CONDITIONS.md (Section 6)

---

### Category 4: Algorithm Normalization Tests (5 tests)

Test case-insensitive algorithm name handling (from StandardAlgName.java pattern).

```kotlin
@ParameterizedTest
@ValueSource(strings = ["rsa", "RsA", "RSA", "rSa"])
fun testAlgorithmNameNormalization(inputAlg: String) = runTest {
    val mockOutput = "Generating RSA key pair..."
    setupMockFactory(mockOutput)

    val result = KeyToolAPI.genKeyPair(
        ...,
        algorithm = inputAlg  // Case variation
    )

    assertTrue(result is KeytoolResult.Success)
}

// Tests:
// - RSA case variations
// - Signature algorithm case variations (SHA1withRSA → RGBA1withRSA)
// - EC curve name normalization
// - Algorithm abbreviations
```

**Files to Reference**: 03_REFERENCE_ALGORITHM_COVERAGE.md (Section 6)

---

### Category 5: Parsing Robustness Tests (5 tests)

Test that parsing failures are caught and reported as errors.

```kotlin
@Test
fun testPrintCertParsingCompleteOutput() = runTest {
    val mockOutput = """
        Certificate:
        Data:
          Subject: CN=example.com
          Issuer: CN=CA
          Public Key Algorithm: RSA (2048 bits)
          Signature Algorithm: sha256WithRSAEncryption

        Extensions:
          X509v3 Basic Constraints: critical CA:FALSE
          X509v3 Key Usage: critical Digital Signature

        SHA-1 Fingerprint: AB:CD:EF:...
        SHA-256 Fingerprint: 01:23:45:...
    """.trimIndent()

    setupMockFactory(mockOutput)

    val result = KeyToolAPI.printCert(File("cert.crt"))

    assertTrue(result is KeytoolResult.Success)
    val cert = (result as KeytoolResult.Success).data
    assertEquals("CN=example.com", cert.subject)
    assertEquals("RSA", cert.publicKeyAlgorithm)
    assertEquals("2048", cert.keySize)
    assertEquals("sha256WithRSAEncryption", cert.signatureAlgorithm)
    assertNotNull(cert.sha1Fingerprint)
    assertNotNull(cert.sha256Fingerprint)
}

// Tests:
// - Complete valid certificate output parsing
// - Partial/incomplete output (missing fingerprint)
// - Missing required fields (subject, issuer)
// - Extra whitespace/newlines handling
// - Multiple extension parsing
```

**Files to Reference**: 02_REFERENCE_TEST_PATTERNS.md (Section 1E)

---

## 4. TEST DATA & MOCK PATTERNS

### Mock Output Templates

**Pattern A: Keystore List Output**
```kotlin
fun createMockKeystoreListOutput(entries: Int = 2): String {
    val entriesText = (1..entries).joinToString("\n") { i ->
        "alias$i, Jan 22, 2026, PrivateKeyEntry, ..."
    }

    return """
        Keystore type: JKS
        Keystore provider: SUN

        Your keystore contains $entries entries:

        $entriesText
    """.trimIndent()
}
```

**Pattern B: Certificate Output**
```kotlin
fun createMockCertificateOutput(
    subject: String = "CN=example.com",
    issuer: String = "CN=CA"
): String {
    return """
        Certificate:
        Data:
          Subject: $subject
          Issuer: $issuer
          ...
        SHA-1 Fingerprint: AB:CD:EF:...
        SHA-256 Fingerprint: 01:23:45:...
    """.trimIndent()
}
```

**Pattern C: Error Output**
```kotlin
fun createErrorOutput(message: String, exitCode: Int = 1): Pair<String, String> {
    return "" to "keytool error: $message"
}
```

**Files to Reference**: 02_REFERENCE_TEST_PATTERNS.md (Section 5)

---

## 5. IMPLEMENTATION CHECKLIST

### Before Implementation
- [ ] Review 04_REFERENCE_ERROR_CONDITIONS.md for all error messages
- [ ] Review 02_REFERENCE_TEST_PATTERNS.md for test patterns
- [ ] Review 03_REFERENCE_ALGORITHM_COVERAGE.md for algorithm tests
- [ ] Prepare mock output templates
- [ ] Set up test class structure and imports

### During Implementation
- [ ] Implement Category 1 tests (30 success cases)
- [ ] Implement Category 2 tests (25+ error cases)
- [ ] Implement Category 3 tests (5 warning cases)
- [ ] Implement Category 4 tests (5 normalization cases)
- [ ] Implement Category 5 tests (5 parsing tests)
- [ ] Create helper functions for assertions
- [ ] Run tests and verify all pass
- [ ] Verify test execution time < 5 seconds

### After Implementation
- [ ] Count total unit tests (should be 60-70)
- [ ] Document any modifications to error messages
- [ ] Create summary of test coverage
- [ ] Review against OpenJDK patterns (01_REFERENCE_OPENJDK_FINDINGS.md)
- [ ] Prepare Phase 3 (Integration Tests)

---

## 6. KEY DIFFERENCES FROM CURRENT TESTS

### Current (50 Tests)
```
- Arbitrary test selection
- Limited error coverage
- No parsing validation for malformed output
- No warning/deprecation testing
- No algorithm normalization testing
```

### New (60-70 Tests)
```
✓ Based on OpenJDK test patterns
✓ Comprehensive error coverage (25+ scenarios)
✓ Parsing validation ensures exit 0 + bad format = error
✓ Warning/deprecation tests match keytool behavior
✓ Algorithm normalization tests case-insensitivity
✓ Better organization by function and error type
✓ Parametrized tests for algorithm variants
✓ Faster execution with optimized mocking
```

---

## 7. CROSS-REFERENCES

| Section | Reference Document | Details |
|---|---|---|
| Test Patterns | 02_REFERENCE_TEST_PATTERNS.md | Code templates for all test types |
| Error Messages | 04_REFERENCE_ERROR_CONDITIONS.md | Exact error messages from keytool |
| Algorithm Coverage | 03_REFERENCE_ALGORITHM_COVERAGE.md | All algorithms to normalize test |
| OpenJDK Tests | 01_REFERENCE_OPENJDK_FINDINGS.md | Base patterns and test classes |
| Test Data | This document (Section 4) | Mock output templates |

---

## 8. QUESTIONS FOR USER

1. **Test Organization**: Should tests be:
   - One class per function (16 classes)? ✓ Recommended
   - One class total?
   - Grouped by error type?

2. **Error Message Matching**: Should tests:
   - Match exact error strings? ✓ Recommended (helps validate integration)
   - Match regex patterns?
   - Just check for keywords?

3. **Algorithm Testing**: Should we:
   - Test 15+ algorithm combinations in unit tests? ✓ Recommended (with @ParameterizedTest)
   - Move to integration tests?
   - Keep minimal (just RSA)?

4. **Warning Testing**: Should we:
   - Include warning validation? ✓ Recommended (important for user feedback)
   - Skip warnings and focus on success/error?

5. **Test Execution**: Is < 5 seconds total test time:
   - Acceptable?
   - Should be faster?
   - Can be slower?

---

## 9. SUCCESS CRITERIA

### Phase 2 Complete When:
- [ ] 60-70 unit tests implemented and passing
- [ ] All 16 functions have 3-5 dedicated tests each
- [ ] 25+ error conditions tested and validated
- [ ] Parsing robustness verified (exit 0 + bad format = error)
- [ ] Algorithm normalization tested
- [ ] Warning conditions tested
- [ ] Test execution time < 5 seconds
- [ ] Code coverage > 85% for KeyToolAPI
- [ ] All tests use `runTest` from coroutines.test
- [ ] No real file I/O or process execution in unit tests

---

**Document Status**: Ready for User Review
**Created**: 2026-01-22
**Next Step**: User approval → Implementation of Phase 2

**Once approved**: Phase 3 (Integration Tests) will reference this for completeness
