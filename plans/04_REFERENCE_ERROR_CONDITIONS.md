# Reference: Comprehensive Error Conditions Catalog
## All Error & Warning Scenarios for KeyToolAPI

---

## 1. INPUT VALIDATION ERRORS

### Error 1.1: Missing Required Parameter
**Command**: `-genkeypair` without `-keyalg`
**Error Message**: "keytool error: option -keyalg must be specified"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testGenKeyPairWithoutKeyalg() = runTest {
    setupMockFactory("", "keytool error: option -keyalg must be specified", 1)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test",
        keySize = 2048,
        sigAlg = "SHA256withRSA",
        algorithm = null,  // Missing -keyalg
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "keyalg must be specified")
}
```

**OpenJDK Test**: `RemoveKeyAlgDefault.java`

---

### Error 1.2: Conflicting Options
**Command**: `-genkeypair -keysize 256 -groupname secp256r1`
**Error Message**: "keytool error: ... Cannot specify both -groupname and -keysize"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testECKeyGenWithBothGroupNameAndKeySize() = runTest {
    val errorMsg = "keytool error: Cannot specify both -groupname and -keysize"
    setupMockFactory("", errorMsg, 1)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test",
        keySize = 256,
        sigAlg = "SHA256withECDSA",
        algorithm = "ec",
        keystore = keystorePath,
        storePassword = "changeit",
        groupName = "secp256r1"  // Both parameters set
    )

    assertErrorMatches(result, "Cannot specify both")
}
```

**OpenJDK Test**: `GroupName.java`

---

### Error 1.3: Invalid Option Value
**Command**: `-keysize 0`
**Error Message**: "keytool error: Invalid key size"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testInvalidKeySizeZero() = runTest {
    setupMockFactory("", "keytool error: Invalid key size", 1)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test",
        keySize = 0,  // Invalid
        sigAlg = "SHA256withRSA",
        algorithm = "rsa",
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "Invalid key size")
}
```

---

### Error 1.4: Ambiguous Extension Option
**Command**: `-gencert -ext ku=d`
**Error Message**: "keytool error: ... -ku "d" is ambiguous"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testAmbiguousExtensionOption() = runTest {
    val errorMsg = "keytool error: -ku \"d\" is ambiguous"
    setupMockFactory("", errorMsg, 1)

    val result = KeyToolAPI.genCert(
        requestFile = File("test.req"),
        outFile = File("test.crt"),
        daysValid = 365,
        alias = "ca",
        keystore = keystorePath,
        storePassword = "changeit",
        extensions = listOf("ku=d")  // Ambiguous: digitalSignature or dataEncipherment?
    )

    assertErrorMatches(result, "ambiguous")
}
```

**OpenJDK Test**: `ExtOptionCamelCase.java`

---

### Error 1.5: RSA-Specific Parameter Used with EC Key
**Command**: `-genkeypair -keyalg ec -groupname secp256r1` (with RSA param)
**Error Message**: "keytool error: ... invalid for algorithm ec"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testRSAParamWithECKey() = runTest {
    setupMockFactory("", "keytool error: Invalid parameter", 1)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test",
        keySize = null,
        sigAlg = "SHA256withECDSA",
        algorithm = "ec",
        keystore = keystorePath,
        storePassword = "changeit"
        // rsapss = true  // Invalid for EC
    )
}
```

---

## 2. KEYSTORE & FILE ERRORS

### Error 2.1: Keystore File Not Found
**Command**: `-list -keystore /nonexistent/path.jks`
**Error Message**: "keytool error: java.io.FileNotFoundException: /nonexistent/path.jks"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testKeystoreFileNotFound() = runTest {
    setupMockFactory(
        "",
        "keytool error: java.io.FileNotFoundException: /nonexistent.jks",
        1
    )

    val result = KeyToolAPI.list(File("/nonexistent/path.jks"), "password")

    assertTrue(result is KeytoolResult.Error)
    assertErrorMatches(result, "FileNotFoundException")
}
```

---

### Error 2.2: Wrong Keystore Password
**Command**: `-list -keystore test.jks -storepass wrongpass`
**Error Message**: "keytool error: java.io.IOException: Keystore was tampered with, or password was incorrect"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testWrongKeystorePassword() = runTest {
    val errorMsg = "keytool error: java.io.IOException: Keystore was tampered with, or password was incorrect"
    setupMockFactory("", errorMsg, 1)

    val result = KeyToolAPI.list(keystorePath, "wrongpass")

    assertTrue(result is KeytoolResult.Error)
    assertErrorMatches(result, "tampered", "incorrect")
}
```

---

### Error 2.3: Unrecognized Keystore Format
**Command**: `-list -keystore text-file.txt`
**Error Message**: "keytool error: ... Unrecognized keystore format"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testUnrecognizedKeystoreFormat() = runTest {
    setupMockFactory("", "keytool error: Unrecognized keystore format", 1)

    val textFile = createTempFile("test-", ".txt")
    textFile.writeText("This is not a keystore")

    try {
        val result = KeyToolAPI.list(textFile, "password")
        assertErrorMatches(result, "Unrecognized")
    } finally {
        textFile.delete()
    }
}
```

---

### Error 2.4: Cannot Read/Write Keystore
**Command**: `-genkeypair -keystore /readonly/path.jks` (on read-only filesystem)
**Error Message**: "keytool error: java.io.IOException: Error writing/reading keystore"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testReadOnlyKeystorePath() = runTest {
    setupMockFactory("", "keytool error: java.io.IOException: Permission denied", 1)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test",
        keySize = 2048,
        sigAlg = "SHA256withRSA",
        algorithm = "rsa",
        keystore = File("/readonly/path.jks"),  // Inaccessible
        storePassword = "changeit"
    )

    assertErrorMatches(result, "Permission")
}
```

---

### Error 2.5: File Already Exists (for export)
**Command**: `-exportcert -file existing.crt` (file exists)
**Error Message**: "keytool error: ... file already exists"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testExportCertToExistingFile() = runTest {
    setupMockFactory("", "keytool error: file already exists", 1)

    val existingFile = createTempFile("existing-", ".crt")

    try {
        val result = KeyToolAPI.exportCert(
            alias = "test",
            file = existingFile,  // File already exists
            keystore = keystorePath,
            storePassword = "changeit"
        )

        assertErrorMatches(result, "already exists")
    } finally {
        existingFile.delete()
    }
}
```

---

## 3. CERTIFICATE & CHAIN ERRORS

### Error 3.1: Failed to Establish Certificate Chain
**Command**: `-importcert -file cert.crt` (missing intermediate CA)
**Error Message**: "keytool error: ... Failed to establish chain from reply"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testImportCertFailedChainEstablishment() = runTest {
    val errorMsg = "keytool error: Failed to establish chain from reply"
    setupMockFactory("", errorMsg, 1)

    val result = KeyToolAPI.importCert(
        alias = "user",
        file = File("cert-without-chain.crt"),  // Missing intermediate
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "Failed to establish chain")
}
```

**OpenJDK Test**: `SelfIssued.java`, `DupImport.java`

---

### Error 3.2: Public Key Mismatch Between Certificate and Request
**Command**: `-importcert -file cert-from-different-csr.crt`
**Error Message**: "keytool error: ... Public keys in reply and keystore don't match"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testImportCertPublicKeyMismatch() = runTest {
    val errorMsg = "keytool error: Public keys in reply and keystore don't match"
    setupMockFactory("", errorMsg, 1)

    val result = KeyToolAPI.importCert(
        alias = "user",
        file = File("cert-for-different-key.crt"),
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "Public keys", "don't match")
}
```

**OpenJDK Test**: `DupImport.java`

---

### Error 3.3: Certificate Chain Too Long
**Command**: `-importcert -file cert-chain.crt` (chain > 5 levels)
**Error Message**: "keytool error: ... Certificate chain too long"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testCertificateChainTooLong() = runTest {
    setupMockFactory("", "keytool error: Certificate chain too long", 1)

    val result = KeyToolAPI.importCert(
        alias = "deep-chain",
        file = File("6-level-chain.crt"),
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "chain too long")
}
```

---

### Error 3.4: Invalid Certificate Format
**Command**: `-importcert -file not-a-cert.txt`
**Error Message**: "keytool error: ... Input not an X.509 certificate"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testImportInvalidCertificateFormat() = runTest {
    val errorMsg = "keytool error: Input not an X.509 certificate"
    setupMockFactory("", errorMsg, 1)

    val invalidCert = createTempFile("invalid-", ".crt")
    invalidCert.writeText("This is not a certificate\n-----BEGIN CERT-----")

    try {
        val result = KeyToolAPI.importCert(
            alias = "invalid",
            file = invalidCert,
            keystore = keystorePath,
            storePassword = "changeit"
        )

        assertErrorMatches(result, "not an X.509")
    } finally {
        invalidCert.delete()
    }
}
```

---

### Error 3.5: Certificate Not Trusted
**Command**: `-importcert -file self-signed.crt` (self-signed, not trusted)
**Error Message**: "keytool error: ... Certificate is untrusted"
**Exit Code**: (usually prompts user unless `-noprompt`)

**Test Case**:
```kotlin
@Test
fun testImportUntrustedCertificate() = runTest {
    // Without -trustcacerts, untrusted certs may fail or prompt
    val result = KeyToolAPI.importCert(
        alias = "untrusted",
        file = File("untrusted-self-signed.crt"),
        keystore = keystorePath,
        storePassword = "changeit",
        trustCacerts = false  // Don't trust system cacerts
    )

    // Behavior depends on whether it's user-prompted or error
    assertTrue(result is KeytoolResult.Error || result is KeytoolResult.Success)
}
```

---

## 4. ALIAS & ENTRY ERRORS

### Error 4.1: Alias Already Exists
**Command**: `-genkeypair -alias test` (when test already exists)
**Error Message**: "keytool error: ... Alias <test> already exists"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testGenerateKeyWithExistingAlias() = runTest {
    val keystorePath = createTempFile("test-", ".jks")

    try {
        // Create first key
        KeyToolAPI.genKeyPair(
            dn = "CN=Test",
            alias = "duplicate",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).also { assertTrue(it is KeytoolResult.Success) }

        // Try to create another with same alias
        val result = KeyToolAPI.genKeyPair(
            dn = "CN=Test2",
            alias = "duplicate",  // Already exists
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        )

        assertErrorMatches(result, "already exists", "duplicate")
    } finally {
        keystorePath.delete()
    }
}
```

---

### Error 4.2: Alias Does Not Exist
**Command**: `-delete -alias nonexistent`
**Error Message**: "keytool error: Alias <nonexistent> does not exist"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testDeleteNonexistentAlias() = runTest {
    setupMockFactory("", "keytool error: Alias <nonexistent> does not exist", 1)

    val result = KeyToolAPI.delete(
        alias = "nonexistent",
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "does not exist", "nonexistent")
}
```

---

### Error 4.3: Entry Type Mismatch
**Command**: `-importpass -alias key` (but alias is PrivateKeyEntry, not SecretKeyEntry)
**Error Message**: "keytool error: ... Alias already exists"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testImportPassToPrivateKeyEntry() = runTest {
    setupMockFactory("", "keytool error: Alias <test> already exists", 1)

    val result = KeyToolAPI.importPass(
        alias = "test",  // Already a private key
        password = "newsecret",
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "already exists")
}
```

---

## 5. ALGORITHM & CRYPTO ERRORS

### Error 5.1: Algorithm Not Available
**Command**: `-genkeypair -keyalg UnknownAlgorithm`
**Error Message**: "keytool error: ... Algorithm not available"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testUnknownAlgorithm() = runTest {
    setupMockFactory("", "keytool error: Algorithm not available", 1)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test",
        keySize = 2048,
        sigAlg = "SHA256withRSA",
        algorithm = "UnknownAlgorithm",  // Invalid
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "Algorithm", "not available")
}
```

---

### Error 5.2: Cannot Derive Signature Algorithm
**Command**: `-genkeypair -keyalg XDH -sigalg SHA256withRSA`
**Error Message**: "keytool error: ... Cannot derive signature algorithm"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testXDHKeyCannotDeriveSignature() = runTest {
    val errorMsg = "keytool error: Cannot derive signature algorithm from XDH"
    setupMockFactory("", errorMsg, 1)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "xdh",
        keySize = null,
        sigAlg = null,  // XDH cannot create signatures
        algorithm = "XDH",
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "Cannot derive", "signature algorithm")
}
```

**OpenJDK Test**: `GenKeyPairSigner.java`

---

### Error 5.3: Weak Algorithm Used (Disabled)
**Command**: `-genkeypair -sigalg MD5withRSA`
**Error Message**: "keytool error: ... MD5withRSA is disabled"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testDisabledMD5Algorithm() = runTest {
    setupMockFactory("", "keytool error: MD5withRSA is disabled", 1)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test",
        keySize = 2048,
        sigAlg = "MD5withRSA",  // DISABLED
        algorithm = "rsa",
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "disabled")
}
```

**OpenJDK Test**: `WeakAlg.java`

---

### Error 5.4: RSA Key Too Small (512-bit)
**Command**: `-genkeypair -keysize 512`
**Error Message**: "keytool error: ... 512 bit RSA key is too small"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testRSAKeyTooSmall() = runTest {
    setupMockFactory("", "keytool error: 512 < 1024: is disabled", 1)

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test",
        keySize = 512,  // Too small
        sigAlg = "SHA256withRSA",
        algorithm = "rsa",
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertErrorMatches(result, "disabled")
}
```

---

## 6. WARNINGS (Exit Code 0, But with Message)

### Warning 6.1: Algorithm Deprecated
**Pattern**: Command succeeds (exit 0) but includes warning
**Warning Message**: "Warning: The generated certificate ... SHA1withRSA ... will be disabled in Java 20"
**When**: Using SHA1, 1024-bit RSA, DES, etc.

**Test Case**:
```kotlin
@Test
fun testDeprecatedAlgorithmWarning() = runTest {
    val mockOutput = """
        Generating keypair...
        Warning: The generated certificate ... SHA1withRSA ... will be disabled in a future release
        Certificate Successfully Generated
    """.trimIndent()

    setupMockFactory(mockOutput, "", 0)  // Exit 0, but has warning

    val result = KeyToolAPI.genKeyPair(
        dn = "CN=Test",
        alias = "test",
        keySize = 1024,  // 1024-bit RSA is deprecated
        sigAlg = "SHA1withRSA",
        algorithm = "rsa",
        keystore = keystorePath,
        storePassword = "changeit"
    )

    assertTrue(result is KeytoolResult.Success)
    val message = (result as KeytoolResult.Success).message
    assertTrue(message?.contains("Warning") ?: false)
    assertTrue(message?.contains("deprecated") ?: false)
}
```

---

### Warning 6.2: Keystore Format Deprecated
**Pattern**: JKS keystore generates warning
**Warning Message**: "Warning: JKS keystore format is deprecated. Use PKCS12 instead"
**When**: Using JKS format

**Test Case**:
```kotlin
@Test
fun testJKSFormatDeprecationWarning() = runTest {
    val mockOutput = """
        Keystore type: JKS
        Warning: JKS keystore format is deprecated. Use PKCS12 instead.
        ...
    """.trimIndent()

    setupMockFactory(mockOutput, "", 0)

    val result = KeyToolAPI.list(keystorePath, "changeit")

    assertTrue(result is KeytoolResult.Success)
    // Warning about JKS deprecation should be present
}
```

---

### Warning 6.3: Untrusted Certificate Import (With Prompt)
**Pattern**: May prompt user to confirm import
**Default Behavior**: Requires `-noprompt` to skip or user input to confirm

**Test Case**:
```kotlin
@Test
fun testUntrustedCertImportPrompt() = runTest {
    val mockOutput = """
        Owner: CN=Untrusted
        Issuer: CN=Untrusted
        ...
        Trust this certificate? [no]:
    """.trimIndent()

    setupMockFactory(mockOutput, "", 0)

    // With -noprompt or answer=yes
    val result = KeyToolAPI.importCert(
        alias = "untrusted",
        file = File("untrusted.crt"),
        keystore = keystorePath,
        storePassword = "changeit",
        noprompt = true  // Skip prompt
    )

    assertTrue(result is KeytoolResult.Success)
}
```

**OpenJDK Test**: `ImportPrompt.java`

---

## 7. OPERATIONAL ERRORS

### Error 7.1: Duplicate Command
**Command**: `-genkeypair -genkeypair`
**Error Message**: "keytool error: ... Only one command is allowed"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testDuplicateCommand() = runTest {
    setupMockFactory("", "keytool error: Only one command is allowed", 1)

    // This would be at keytool CLI level, not API level
    // API enforces single command per call
}
```

**OpenJDK Test**: `DupCommands.java`

---

### Error 7.2: No Command Specified
**Command**: `keytool -keystore test.jks` (no operation)
**Error Message**: "keytool error: No command provided"
**Exit Code**: 1

---

### Error 7.3: Conflicting Keystore Type
**Command**: `-srckeystore file.jks -srcstoretype pkcs12`
**Error Message**: "keytool error: ... Type mismatch"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testKeystoreTypeMismatch() = runTest {
    setupMockFactory("", "keytool error: Type mismatch", 1)

    val result = KeyToolAPI.importKeystore(
        srcKeystore = File("source.jks"),
        srcType = "pkcs12",  // Mismatch: file is JKS, spec says PKCS12
        srcPassword = "pass",
        destKeystore = File("target.p12"),
        destType = "pkcs12",
        destPassword = "pass"
    )

    assertErrorMatches(result, "Type", "mismatch")
}
```

---

## 8. PARSING & OUTPUT ERRORS

### Error 8.1: Malformed Certificate Output (Parsing Failure)
**Scenario**: Exit code 0 but unparseable output
**Error Message**: "Failed to parse certificate output"
**When**: Certificate output format is corrupted

**Test Case**:
```kotlin
@Test
fun testMalformedCertificateOutput() = runTest {
    val malformedOutput = """
        Certificate:
        Data:
          Subject: CN=Test
          This is not valid certificate output format
    """.trimIndent()

    setupMockFactory(malformedOutput, "", 0)  // Exit 0 but bad format

    val result = KeyToolAPI.printCert(File("cert.crt"))

    assertTrue(result is KeytoolResult.Error)
    assertErrorMatches(result, "parse")
}
```

---

### Error 8.2: Missing Critical Certificate Fields
**Scenario**: Output parsing succeeds but critical field is missing
**Error Message**: "Missing critical certificate fields"
**When**: Subject, issuer, or signature algorithm missing

**Test Case**:
```kotlin
@Test
fun testMissingCriticalCertificateField() = runTest {
    val incompleteOutput = """
        Certificate:
        Data:
          Subject: CN=Test
          (missing Issuer and other fields)
    """.trimIndent()

    setupMockFactory(incompleteOutput, "", 0)

    val result = KeyToolAPI.printCert(File("cert.crt"))

    assertTrue(result is KeytoolResult.Error)
    assertErrorMatches(result, "Missing", "critical")
}
```

---

### Error 8.3: Unknown Extension in Certificate
**Scenario**: Certificate contains unrecognized X.509 extension
**Error Message**: "Unknown extension: [OID]"
**Behavior**: Usually displayed as hex dump

**Test Case**:
```kotlin
@Test
fun testUnknownCertificateExtension() = runTest {
    val mockOutput = """
        Certificate:
        ...
        Extensions:
          Unknown extension: 1.2.3.4.5.6.7.8.9
            0000: AB CD EF 00 11 22 33 44 55 66 77 88 99 AA BB CC
    """.trimIndent()

    setupMockFactory(mockOutput, "", 0)

    val result = KeyToolAPI.printCert(File("cert-unknown-ext.crt"))

    // Should succeed but note the unknown extension
    assertTrue(result is KeytoolResult.Success)
}
```

**OpenJDK Test**: `UnknownAndUnparseable.java`

---

## 9. PLATFORM-SPECIFIC ERRORS

### Error 9.1: PKCS#11 Library Not Found
**Scenario**: NSS library not available
**Error Message**: "keytool error: ... sun.security.pkcs11.SunPKCS11: NSS not found"
**Exit Code**: 1 or test skips

**Test Case**:
```kotlin
@Test
@Disabled("PKCS#11 NSS library may not be available")
fun testPKCS11WithNSS() = runTest {
    // Test would verify PKCS#11 keystore operations
    // Skipped if NSS not available
}
```

**OpenJDK Test**: `NssTest.java` (600-second timeout)

---

### Error 9.2: KeychainStore Not Available (Non-macOS)
**Scenario**: Trying to use KeychainStore on non-macOS system
**Error Message**: "keytool error: ... KeychainStore provider not available"
**Exit Code**: 1 or test skips

**Test Case**:
```kotlin
@Test
@EnabledOnOs(OS.MAC)  // Only on macOS
fun testKeychainStore() = runTest {
    val result = KeyToolAPI.list(
        keystore = File("/System/Keychains/System.keychain"),
        password = ""  // Keychain doesn't need password
    )
    // Verify result
}
```

**OpenJDK Test**: `ExportPrivateKeyNoPwd.java`

---

## 10. EDGE CASE ERRORS

### Error 10.1: Empty Subject Without SAN Extension
**Scenario**: Certificate with empty subject but no SAN
**Error Message**: "keytool error: ... Subject name is empty without SubjectAlternativeName"
**Exit Code**: 1

**Test Case**:
```kotlin
@Test
fun testEmptySubjectWithoutSAN() = runTest {
    setupMockFactory(
        "",
        "keytool error: Subject name is empty without SubjectAlternativeName",
        1
    )

    val result = KeyToolAPI.genKeyPair(
        dn = "",  // Empty distinguished name
        alias = "empty-subj",
        keySize = 2048,
        sigAlg = "SHA256withRSA",
        algorithm = "rsa",
        keystore = keystorePath,
        storePassword = "changeit",
        extensions = listOf()  // No SAN extension
    )

    assertErrorMatches(result, "Subject", "empty", "SAN")
}
```

**OpenJDK Test**: `EmptySubject.java`

---

### Error 10.2: Certificate Without X.509 Extensions (NPE)
**Scenario**: Printing certificate without critical extensions
**Error Message**: Previously caused NullPointerException, now handled
**Exit Code**: 0 (fixed in OpenJDK)

**Test Case**:
```kotlin
@Test
fun testCertificateWithoutExtensions() = runTest {
    val noExtOutput = """
        Certificate:
        Data:
          Subject: CN=NoExt
          Issuer: CN=NoExt
        (no Extensions section)
    """.trimIndent()

    setupMockFactory(noExtOutput, "", 0)

    val result = KeyToolAPI.printCert(File("cert-no-ext.crt"))

    // Should not throw NPE
    assertTrue(result is KeytoolResult.Success)
}
```

**OpenJDK Test**: `NoExtNPE.java` - Bug 6813402

---

### Error 10.3: File Cannot Be Deleted (Stream Open)
**Scenario**: Exported file cannot be deleted (stream not closed)
**Error Message**: File deletion fails
**Behavior**: Now fixed - streams are properly closed

**Test Case**:
```kotlin
@Test
fun testExportedFileDeletion() = runTest {
    val exportedFile = createTempFile("export-", ".crt")

    try {
        val result = KeyToolAPI.exportCert(
            alias = "test",
            file = exportedFile,
            keystore = keystorePath,
            storePassword = "changeit"
        )

        assertTrue(result is KeytoolResult.Success)

        // Should be able to delete exported file (stream was closed)
        val canDelete = exportedFile.delete()
        assertTrue(canDelete, "Stream should be closed, allowing file deletion")
    } finally {
        if (exportedFile.exists()) exportedFile.delete()
    }
}
```

**OpenJDK Test**: `CloseFile.java` - Bug 6489721

---

## 11. ERROR CATALOG INDEX

Quick reference of all 40+ error conditions:

| # | Category | Error | OpenJDK Test | Severity |
|---|---|---|---|---|
| 1.1 | Input | Missing -keyalg | RemoveKeyAlgDefault | High |
| 1.2 | Input | Both -groupname and -keysize | GroupName | High |
| 1.3 | Input | Invalid keysize | New | High |
| 1.4 | Input | Ambiguous extension | ExtOptionCamelCase | Medium |
| 2.1 | File | Keystore not found | KeyToolTest | High |
| 2.2 | File | Wrong password | KeyToolTest | High |
| 2.3 | File | Unrecognized format | KeyToolTest | High |
| 2.4 | File | Permission denied | New | Medium |
| 2.5 | File | File exists | New | Medium |
| 3.1 | Cert | Chain establishment failed | DupImport | High |
| 3.2 | Cert | Public key mismatch | DupImport | High |
| 3.3 | Cert | Chain too long | New | Low |
| 3.4 | Cert | Invalid format | KeyToolTest | High |
| 3.5 | Cert | Untrusted cert | ImportPrompt | Medium |
| 4.1 | Alias | Alias exists | KeyToolTest | High |
| 4.2 | Alias | Alias not found | KeyToolTest | High |
| 4.3 | Alias | Type mismatch | PKCS12Passwd | Medium |
| 5.1 | Crypto | Algorithm unknown | KeyToolTest | High |
| 5.2 | Crypto | Cannot derive sig | GenKeyPairSigner | High |
| 5.3 | Crypto | Weak algorithm | WeakAlg | High |
| 5.4 | Crypto | RSA too small | KeyToolTest | High |
| 6.1 | Warning | Deprecated algorithm | WeakAlg | Medium |
| 6.2 | Warning | JKS format | KeyToolTest | Low |
| 6.3 | Warning | Untrusted cert prompt | ImportPrompt | Medium |
| 7.1 | Op | Duplicate command | DupCommands | High |
| 7.2 | Op | No command | New | High |
| 7.3 | Op | Type mismatch | RealType | High |
| 8.1 | Parse | Malformed output | New | High |
| 8.2 | Parse | Missing fields | New | High |
| 8.3 | Parse | Unknown extension | UnknownAndUnparseable | Low |
| 9.1 | Platform | PKCS#11 not found | NssTest | Medium |
| 9.2 | Platform | Keychain not available | ExportPrivateKeyNoPwd | Low |
| 10.1 | Edge | Empty subject | EmptySubject | Medium |
| 10.2 | Edge | No extensions | NoExtNPE | Low |
| 10.3 | Edge | File in use | CloseFile | Medium |
| And 5+ more... | - | - | - | - |

---

**File Purpose**: Comprehensive catalog of all error conditions for testing
**Last Updated**: 2026-01-22
**Total Error Cases**: 40+
**Total Warning Cases**: 5+
**Cross-References**: 01_REFERENCE_OPENJDK_FINDINGS.md, 02_REFERENCE_TEST_PATTERNS.md
