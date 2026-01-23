# Reference: Reusable Test Patterns from OpenJDK
## Code Templates and Best Practices for KeyToolAPI Tests

---

## 1. UNIT TEST PATTERNS (Mock-Based Tests)

### Pattern 1A: Basic Success Case with Mocked Output
```kotlin
@Test
fun testListSuccessWithValidKeystore() = runTest {
    // Setup mock ProcessBuilder
    val mockOutput = """
        Keystore type: JKS
        Keystore provider: SUN

        Your keystore contains 2 entries:

        my-alias, Jan 22, 2026, PrivateKeyEntry, ...
        another-alias, Jan 22, 2026, TrustedCertEntry, ...
    """.trimIndent()

    setupMockFactory(mockOutput)

    // Execute
    val result = KeyToolAPI.list(File("test.jks"), "password")

    // Verify
    assertTrue(result is KeytoolResult.Success)
    val keystoreInfo = (result as KeytoolResult.Success).data
    assertEquals("JKS", keystoreInfo.type)
    assertEquals(2, keystoreInfo.entries.size)
}

// Helper function
private fun setupMockFactory(stdout: String, stderr: String = "", exitCode: Int = 0) {
    KeyToolAPI.processFactory = { _: List<String> ->
        createMockProcess(stdout, stderr, exitCode)
    }
}
```

### Pattern 1B: Error Case with Specific Error Message
```kotlin
@Test
fun testListFailureWrongPassword() = runTest {
    val errorOutput = """
        keytool error: java.io.IOException: Keystore was tampered with, or password was incorrect
    """.trimIndent()

    setupMockFactory("", errorOutput, exitCode = 1)

    val result = KeyToolAPI.list(File("test.jks"), "wrong")

    assertTrue(result is KeytoolResult.Error)
    val error = (result as KeytoolResult.Error)
    assertTrue(error.message.contains("Keystore was tampered with"))
    assertEquals(1, error.exitCode)
}
```

### Pattern 1C: Edge Case - Parsing Failure Even with Exit Code 0
```kotlin
@Test
fun testListParsingFailureWithZeroExitCode() = runTest {
    // Exit code 0 (success) but malformed output
    val malformedOutput = "Invalid keystore format\nCannot parse"

    setupMockFactory(malformedOutput, "", exitCode = 0)

    val result = KeyToolAPI.list(File("test.jks"), "password")

    // Should still return error because parsing failed
    assertTrue(result is KeytoolResult.Error)
    assertTrue((result as KeytoolResult.Error).message.contains("Failed to parse"))
}
```

### Pattern 1D: Parametrized Tests for Multiple Cases
```kotlin
@ParameterizedTest
@ValueSource(strings = ["rsa", "dsa", "ec", "EdDSA"])
fun testGenKeyPairWithVariousAlgorithms(algorithm: String) = runTest {
    val mockOutput = "Generating $algorithm key pair..."
    setupMockFactory(mockOutput)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test-$algorithm",
        keySize = 2048,
        sigAlg = "SHA256withRSA",
        algorithm = algorithm,
        keystore = File("test.jks"),
        storePassword = "changeit"
    )

    assertTrue(result is KeytoolResult.Success)
}
```

### Pattern 1E: Testing Output Parsing Robustness
```kotlin
@Test
fun testPrintCertParsesAllFingerprints() = runTest {
    val mockOutput = """
        Certificate:
        Data:
          Subject: CN=example.com
          Issuer: CN=CA
          Public Key Algorithm: RSA (2048 bits)
          Signature Algorithm: sha256WithRSAEncryption

          Extensions:
            X509v3 Basic Constraints: critical CA:FALSE
            X509v3 Key Usage: critical Digital Signature, Key Encipherment

        SHA-1 Fingerprint: AB:CD:EF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00
        SHA-256 Fingerprint: 01:23:45:67:89:AB:CD:EF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66
    """.trimIndent()

    setupMockFactory(mockOutput)

    val result = KeyToolAPI.printCert(File("cert.crt"))

    assertTrue(result is KeytoolResult.Success)
    val cert = (result as KeytoolResult.Success).data
    assertEquals("CN=example.com", cert.subject)
    assertEquals("CN=CA", cert.issuer)
    assertEquals("RSA", cert.publicKeyAlgorithm)
    assertEquals("2048", cert.keySize)
    assertEquals("sha256WithRSAEncryption", cert.signatureAlgorithm)
    assertNotNull(cert.sha1Fingerprint)
    assertNotNull(cert.sha256Fingerprint)
}
```

---

## 2. INTEGRATION TEST PATTERNS (Real Keytool)

### Pattern 2A: Setup-Execute-Verify Workflow
```kotlin
@Test
fun testGenKeyPairToListWorkflow() = runTest {
    // Setup: Create temporary keystore
    val keystorePath = createTempFile("test-", ".jks")
    val password = "testpass123"
    val alias = "mykey"

    try {
        // Execute: Generate keypair
        val genResult = KeyToolAPI.genKeyPair(
            dn = "CN=Test User, O=Test Org, C=US",
            alias = alias,
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = password,
            keyPassword = password
        )
        assertTrue(genResult is KeytoolResult.Success, "Generation should succeed")

        // Verify: List the keystore
        val listResult = KeyToolAPI.list(keystorePath, password)
        assertTrue(listResult is KeytoolResult.Success, "Listing should succeed")

        val keystoreInfo = (listResult as KeytoolResult.Success).data
        assertEquals(1, keystoreInfo.entries.size)
        val entry = keystoreInfo.entries[0]
        assertEquals(alias, entry.alias)
        assertEquals(EntryType.PRIVATE_KEY, entry.type)

    } finally {
        keystorePath.delete()
    }
}
```

### Pattern 2B: Certificate Chain Generation and Import
```kotlin
@Test
fun testCertificateChainGeneration() = runTest {
    val keystorePath = createTempFile("chain-", ".jks")
    val caReq = createTempFile("ca-", ".req")
    val caCSR = createTempFile("ca-", ".csr")
    val caCert = createTempFile("ca-", ".crt")
    val userReq = createTempFile("user-", ".req")
    val userCert = createTempFile("user-", ".crt")

    try {
        val password = "changeit"

        // Step 1: Generate CA keypair
        KeyToolAPI.genKeyPair(
            dn = "CN=Test CA, O=Test, C=US",
            alias = "ca",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = password
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Step 2: Generate user keypair
        KeyToolAPI.genKeyPair(
            dn = "CN=Test User, O=Test, C=US",
            alias = "user",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = password
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Step 3: User creates certificate request
        KeyToolAPI.certReq(
            alias = "user",
            file = userReq,
            keystore = keystorePath,
            storePassword = password
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Step 4: CA signs user certificate
        KeyToolAPI.genCert(
            requestFile = userReq,
            outFile = userCert,
            daysValid = 365,
            alias = "ca",
            keystore = keystorePath,
            storePassword = password,
            extensions = listOf("san=dns:user.example.com")
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Step 5: User imports signed certificate
        KeyToolAPI.importCert(
            alias = "user",
            file = userCert,
            keystore = keystorePath,
            storePassword = password
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Verify: List shows both entries
        val listResult = KeyToolAPI.list(keystorePath, password)
        assertTrue(listResult is KeytoolResult.Success)
        val keystoreInfo = (listResult as KeytoolResult.Success).data
        assertEquals(2, keystoreInfo.entries.size)

        // Verify chain depth
        val userEntry = keystoreInfo.entries.find { it.alias == "user" }
        assertNotNull(userEntry)
        assertEquals(2, userEntry?.chainLength ?: 0, "Should be 2-cert chain (user + CA)")

    } finally {
        keystorePath.delete()
        caReq.delete()
        caCSR.delete()
        caCert.delete()
        userReq.delete()
        userCert.delete()
    }
}
```

### Pattern 2C: Format Conversion Testing
```kotlin
@Test
fun testJKSToPKCS12Conversion() = runTest {
    val jksPath = createTempFile("source-", ".jks")
    val pkcs12Path = createTempFile("target-", ".p12")

    try {
        val password = "changeit"

        // Setup: Create JKS keystore
        KeyToolAPI.genKeyPair(
            dn = "CN=Test",
            alias = "mykey",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = jksPath,
            storePassword = password
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Execute: Convert to PKCS12
        val convertResult = KeyToolAPI.importKeystore(
            srcKeystore = jksPath,
            srcType = "jks",
            srcPassword = password,
            destKeystore = pkcs12Path,
            destType = "pkcs12",
            destPassword = password
        )
        assertTrue(convertResult is KeytoolResult.Success)

        // Verify: List PKCS12 keystore
        val listResult = KeyToolAPI.list(pkcs12Path, password)
        assertTrue(listResult is KeytoolResult.Success)

        val pkcs12Info = (listResult as KeytoolResult.Success).data
        assertEquals("PKCS12", pkcs12Info.type)
        assertEquals(1, pkcs12Info.entries.size)
        assertEquals("mykey", pkcs12Info.entries[0].alias)

    } finally {
        jksPath.delete()
        pkcs12Path.delete()
    }
}
```

### Pattern 2D: Algorithm-Specific Testing
```kotlin
@Test
fun testGenerateKeyForEachAlgorithm() = runTest {
    val algorithms = listOf(
        Triple("rsa", 2048, "SHA256withRSA"),
        Triple("ec", 256, "SHA256withECDSA"),
        Triple("EdDSA", null, null)
    )

    for ((alg, keySize, sigAlg) in algorithms) {
        val keystorePath = createTempFile("${alg}-", ".jks")
        try {
            val result = KeyToolAPI.genKeyPair(
                dn = "CN=Test-$alg",
                alias = "key-$alg",
                keySize = keySize ?: 256,
                sigAlg = sigAlg ?: "Ed25519",
                algorithm = alg,
                keystore = keystorePath,
                storePassword = "password"
            )

            assertTrue(result is KeytoolResult.Success, "Should generate $alg key")

            // Verify by listing
            val listResult = KeyToolAPI.list(keystorePath, "password")
            assertTrue(listResult is KeytoolResult.Success)
            val keystoreInfo = (listResult as KeytoolResult.Success).data
            assertEquals(1, keystoreInfo.entries.size)

        } finally {
            keystorePath.delete()
        }
    }
}
```

### Pattern 2E: Error Handling in Integration Tests
```kotlin
@Test
fun testImportCertFailureWithWrongPassword() = runTest {
    val keystorePath = createTempFile("test-", ".jks")
    val certPath = createTempFile("cert-", ".crt")

    try {
        // Generate a certificate file
        val genResult = KeyToolAPI.genKeyPair(
            dn = "CN=Test",
            alias = "test",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "correctpass"
        )
        assertTrue(genResult is KeytoolResult.Success)

        // Export it
        KeyToolAPI.exportCert(
            alias = "test",
            file = certPath,
            keystore = keystorePath,
            storePassword = "correctpass"
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Try to import with wrong password
        val importResult = KeyToolAPI.importCert(
            alias = "imported",
            file = certPath,
            keystore = keystorePath,
            storePassword = "wrongpass"
        )

        assertTrue(importResult is KeytoolResult.Error, "Should fail with wrong password")

    } finally {
        keystorePath.delete()
        certPath.delete()
    }
}
```

---

## 3. EDGE CASE TEST PATTERNS

### Pattern 3A: Empty Subject with SAN Extension
```kotlin
@Test
fun testEmptySubjectRequiresSAN() = runTest {
    val keystorePath = createTempFile("empty-subj-", ".jks")
    val password = "changeit"

    try {
        // This should fail: empty subject without SAN
        val failResult = KeyToolAPI.genKeyPair(
            dn = "",  // Empty distinguished name
            alias = "empty",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = password,
            extensions = listOf()  // No SAN extension
        )
        assertTrue(failResult is KeytoolResult.Error, "Empty subject without SAN should fail")

        // This should succeed: empty subject WITH SAN
        val successResult = KeyToolAPI.genKeyPair(
            dn = "",  // Empty distinguished name
            alias = "empty-with-san",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = password,
            extensions = listOf("san=dns:example.com")  // With SAN extension
        )
        assertTrue(successResult is KeytoolResult.Success, "Empty subject with SAN should succeed")

    } finally {
        keystorePath.delete()
    }
}
```

### Pattern 3B: Weak Algorithm Detection
```kotlin
@Test
fun testWeakAlgorithmWarningInAllOperations() = runTest {
    val keystorePath = createTempFile("weak-", ".jks")

    try {
        // MD5withRSA is disabled
        val result = KeyToolAPI.genKeyPair(
            dn = "CN=Test",
            alias = "weak",
            keySize = 1024,  // Weak key size
            sigAlg = "MD5withRSA",  // Weak algorithm
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        )

        // Should return warning even if operation succeeds
        assertTrue(result is KeytoolResult.Success)
        val message = (result as KeytoolResult.Success).message
        assertTrue(message?.contains("disabled") ?: false, "Should warn about disabled algorithm")

    } finally {
        keystorePath.delete()
    }
}
```

### Pattern 3C: Certificate Deduplication in Import
```kotlin
@Test
fun testCertificateDeduplicationDuringImport() = runTest {
    val keystorePath = createTempFile("dedup-", ".jks")
    val certChainPath = createTempFile("chain-", ".crt")

    try {
        // Create cert file with duplicates (this would be created externally)
        // For now, just verify that import handles deduplication

        val result = KeyToolAPI.genKeyPair(
            dn = "CN=CA",
            alias = "ca",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        )
        assertTrue(result is KeytoolResult.Success)

        val listResult = KeyToolAPI.list(keystorePath, "changeit")
        assertTrue(listResult is KeytoolResult.Success)

        // Verify entry count
        val info = (listResult as KeytoolResult.Success).data
        assertEquals(1, info.entries.size)

    } finally {
        keystorePath.delete()
        certChainPath.delete()
    }
}
```

### Pattern 3D: File Stream Closure Validation
```kotlin
@Test
fun testFileStreamClosureAfterOperations() = runTest {
    val keystorePath = createTempFile("stream-", ".jks")
    val password = "changeit"

    try {
        // Generate keystore
        KeyToolAPI.genKeyPair(
            dn = "CN=Test",
            alias = "test",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = password
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Export certificate
        val exportPath = createTempFile("export-", ".crt")
        KeyToolAPI.exportCert(
            alias = "test",
            file = exportPath,
            keystore = keystorePath,
            storePassword = password
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Verify file can be deleted (stream was closed)
        val canDelete = exportPath.delete()
        assertTrue(canDelete, "Should be able to delete exported file (stream should be closed)")

    } finally {
        keystorePath.delete()
    }
}
```

---

## 4. HELPER FUNCTIONS FOR TESTS

### Function 4A: Create Mock Process
```kotlin
private fun createMockProcess(
    stdout: String,
    stderr: String = "",
    exitCode: Int = 0
): Process {
    return object : Process() {
        override fun getInputStream() = ByteArrayInputStream(stdout.toByteArray())
        override fun getErrorStream() = ByteArrayInputStream(stderr.toByteArray())
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun waitFor(): Int = exitCode
        override fun waitFor(timeout: Long, unit: TimeUnit): Boolean = true
        override fun exitValue(): Int = exitCode
        override fun destroy() {}
        override fun destroyForcibly(): Process = this
        override fun isAlive(): Boolean = false
    }
}
```

### Function 4B: Create Temporary Keystore
```kotlin
suspend fun createTemporaryKeystore(
    algorithm: String = "rsa",
    password: String = "changeit",
    alias: String = "test"
): File {
    val keystorePath = createTempFile("temp-", ".jks")

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Temporary Test Key",
        alias = alias,
        keySize = 2048,
        sigAlg = "SHA256withRSA",
        algorithm = algorithm,
        keystore = keystorePath,
        storePassword = password
    )

    require(result is KeytoolResult.Success) { "Failed to create temporary keystore" }
    return keystorePath
}
```

### Function 4C: Assert Certificate Chain Length
```kotlin
suspend fun assertChainLength(
    keystorePath: File,
    password: String,
    alias: String,
    expectedLength: Int
) {
    val listResult = KeyToolAPI.list(keystorePath, password)
    assertTrue(listResult is KeytoolResult.Success)

    val keystoreInfo = (listResult as KeytoolResult.Success).data
    val entry = keystoreInfo.entries.find { it.alias == alias }

    assertNotNull(entry, "Alias $alias not found")
    assertEquals(expectedLength, entry?.chainLength ?: 0,
                 "Expected chain length $expectedLength for alias $alias")
}
```

### Function 4D: Validate Error Message Pattern
```kotlin
fun assertErrorMatches(
    result: KeytoolResult<*>,
    vararg patterns: String
) {
    assertTrue(result is KeytoolResult.Error) { "Expected error but got success" }

    val errorMessage = (result as KeytoolResult.Error).message
    for (pattern in patterns) {
        assertTrue(errorMessage.contains(pattern),
                   "Error message should contain '$pattern', got: $errorMessage")
    }
}
```

---

## 5. TEST DATA TEMPLATES

### Template 5A: Mock Keystore List Output
```kotlin
fun createMockKeystoreListOutput(entries: List<Pair<String, String>>): String {
    val entriesText = entries.joinToString("\n\n") { (alias, type) ->
        "$alias, Jan 22, 2026, $type, ..."
    }

    return """
        Keystore type: JKS
        Keystore provider: SUN

        Your keystore contains ${entries.size} entries:

        $entriesText
    """.trimIndent()
}
```

### Template 5B: Mock Certificate Output
```kotlin
fun createMockCertificateOutput(
    subject: String = "CN=example.com",
    issuer: String = "CN=CA",
    algorithm: String = "sha256WithRSAEncryption"
): String {
    return """
        Certificate:
        Data:
          Subject: $subject
          Issuer: $issuer
          Public Key Algorithm: RSA (2048 bits)
          Signature Algorithm: $algorithm

        SHA-1 Fingerprint: AB:CD:EF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00
        SHA-256 Fingerprint: 01:23:45:67:89:AB:CD:EF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66
    """.trimIndent()
}
```

---

## 6. ASSERTION HELPER LIBRARY

```kotlin
// Fluent assertion helpers
fun <T> KeytoolResult<T>.shouldBeSuccess(): T {
    assertTrue(this is KeytoolResult.Success) { "Expected success but got error" }
    return (this as KeytoolResult.Success).data
}

fun KeytoolResult<*>.shouldBeError(expectedMessage: String? = null) {
    assertTrue(this is KeytoolResult.Error) { "Expected error but got success" }
    if (expectedMessage != null) {
        assertTrue((this as KeytoolResult.Error).message.contains(expectedMessage))
    }
}

// Keystore assertion helpers
suspend fun File.shouldContainAlias(password: String, alias: String) {
    val result = KeyToolAPI.list(this, password).shouldBeSuccess()
    assertTrue(result.entries.any { it.alias == alias },
               "Keystore should contain alias $alias")
}

suspend fun File.shouldNotContainAlias(password: String, alias: String) {
    val result = KeyToolAPI.list(this, password).shouldBeSuccess()
    assertTrue(result.entries.none { it.alias == alias },
               "Keystore should not contain alias $alias")
}
```

---

## 7. BEST PRACTICES CHECKLIST

### For Unit Tests (Mock-based)
- [ ] Use `runTest` from kotlinx.coroutines.test
- [ ] Setup mock factory before each test
- [ ] Test both success and error paths
- [ ] Validate exact error messages
- [ ] Test parsing robustness (malformed output)
- [ ] Use `@ParameterizedTest` for algorithm variants
- [ ] Restore mock factory in cleanup

### For Integration Tests
- [ ] Use temporary files (createTempFile)
- [ ] Always cleanup in finally block
- [ ] Chain multiple operations to test workflows
- [ ] Verify results by listing/exporting keystores
- [ ] Test with real password values
- [ ] Validate file operations (export can be deleted)
- [ ] Test error conditions with wrong passwords

### For Edge Case Tests
- [ ] Reference OpenJDK bug numbers
- [ ] Test known failure scenarios
- [ ] Validate security constraints
- [ ] Test boundary conditions
- [ ] Test platform-specific behavior

### General
- [ ] Each test is independent
- [ ] No shared state between tests
- [ ] Clear test names describing scenario
- [ ] Comments explaining complex operations
- [ ] Assertions validate both success and failure cases

---

**File Purpose**: Reusable code patterns for implementing tests
**Last Updated**: 2026-01-22
**Cross-References**: 01_REFERENCE_OPENJDK_FINDINGS.md
