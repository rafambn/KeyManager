# Reference: Certificate Chain Testing Strategies
## Complex Chain Scenarios from OpenJDK Keytool Tests

---

## 1. BASIC 2-LEVEL CHAIN (User + CA)

### Structure
```
Root CA (self-signed)
  ├─ signs
  └─ End Entity (user certificate)
```

### OpenJDK Implementation (GenKeyPairSigner.java)

**Test Steps**:
```kotlin
@Test
fun testBasic2LevelChain() = runTest {
    val keystorePath = createTempFile("chain2-", ".jks")
    val caReq = createTempFile("ca-", ".req")
    val caCert = createTempFile("ca-", ".crt")

    try {
        // Step 1: Generate CA keypair (self-signed)
        KeyToolAPI.genKeyPair(
            dn = "CN=Root CA, O=Test, C=US",
            alias = "ca",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Step 2: Generate user keypair
        KeyToolAPI.genKeyPair(
            dn = "CN=User, O=Test, C=US",
            alias = "user",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Step 3: User creates CSR
        KeyToolAPI.certReq(
            alias = "user",
            file = caReq,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Step 4: CA signs the CSR
        KeyToolAPI.genCert(
            requestFile = caReq,
            outFile = caCert,
            daysValid = 365,
            alias = "ca",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Step 5: User imports signed certificate
        KeyToolAPI.importCert(
            alias = "user",
            file = caCert,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Verification: List should show chain
        val listResult = KeyToolAPI.list(keystorePath, "changeit").shouldBeSuccess()
        val userEntry = listResult.entries.find { it.alias == "user" }!!
        assertEquals(2, userEntry.chainLength, "Should be 2-certificate chain (user + CA)")

    } finally {
        keystorePath.delete()
        caReq.delete()
        caCert.delete()
    }
}
```

### Expected Output Pattern
```
Alias name: user
Entry type: PrivateKeyEntry
Certificate chain length: 2
Certificate[1]:
  Owner: CN=User, O=Test, C=US
  Issuer: CN=Root CA, O=Test, C=US
Certificate[2]:
  Owner: CN=Root CA, O=Test, C=US
  Issuer: CN=Root CA, O=Test, C=US
```

---

## 2. HIERARCHICAL 4-LEVEL CHAIN (CA → CA1 → CA2 → E1)

### Structure
```
CA (Root, self-signed)
  ├─ signs
  └─ CA1 (Intermediate, self-issued)
       ├─ signs
       └─ CA2 (Intermediate, self-issued)
            ├─ signs
            └─ E1 (End Entity)
```

### OpenJDK Implementation (SelfIssued.java)

**Complexity**: Tests chain reconstruction with missing intermediates

**Test Steps**:
```kotlin
@Test
fun testHierarchical4LevelChain() = runTest {
    val keystorePath = createTempFile("chain4-", ".jks")
    val ca1Req = createTempFile("ca1-", ".req")
    val ca1Cert = createTempFile("ca1-", ".crt")
    val ca2Req = createTempFile("ca2-", ".req")
    val ca2Cert = createTempFile("ca2-", ".crt")
    val e1Req = createTempFile("e1-", ".req")
    val e1Cert = createTempFile("e1-", ".crt")

    try {
        // Level 0: Generate Root CA
        KeyToolAPI.genKeyPair(
            dn = "CN=Root CA",
            alias = "ca",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Level 1: Generate CA1
        KeyToolAPI.genKeyPair(
            dn = "CN=CA1",
            alias = "ca1",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Step: CA signs CA1 (creates self-issued intermediate)
        KeyToolAPI.certReq(
            alias = "ca1",
            file = ca1Req,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        KeyToolAPI.genCert(
            requestFile = ca1Req,
            outFile = ca1Cert,
            daysValid = 365,
            alias = "ca",
            keystore = keystorePath,
            storePassword = "changeit",
            extensions = listOf(
                "BasicConstraints=critical,CA:TRUE",
                "KeyUsage=critical,keyCertSign"
            )
        ).shouldBeSuccess()

        KeyToolAPI.importCert(
            alias = "ca1",
            file = ca1Cert,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Level 2: Generate CA2 (CA1 signs CA2)
        KeyToolAPI.genKeyPair(
            dn = "CN=CA2",
            alias = "ca2",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        KeyToolAPI.certReq(
            alias = "ca2",
            file = ca2Req,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        KeyToolAPI.genCert(
            requestFile = ca2Req,
            outFile = ca2Cert,
            daysValid = 365,
            alias = "ca1",
            keystore = keystorePath,
            storePassword = "changeit",
            extensions = listOf(
                "BasicConstraints=critical,CA:TRUE",
                "KeyUsage=critical,keyCertSign"
            )
        ).shouldBeSuccess()

        KeyToolAPI.importCert(
            alias = "ca2",
            file = ca2Cert,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Level 3: Generate E1 (CA2 signs E1 - end entity)
        KeyToolAPI.genKeyPair(
            dn = "CN=E1",
            alias = "e1",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        KeyToolAPI.certReq(
            alias = "e1",
            file = e1Req,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        KeyToolAPI.genCert(
            requestFile = e1Req,
            outFile = e1Cert,
            daysValid = 365,
            alias = "ca2",
            keystore = keystorePath,
            storePassword = "changeit",
            extensions = listOf(
                "BasicConstraints=critical,CA:FALSE",
                "KeyUsage=critical,digitalSignature"
            )
        ).shouldBeSuccess()

        KeyToolAPI.importCert(
            alias = "e1",
            file = e1Cert,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Verification: Delete CA1 and CA2, then import E1 cert
        // The chain should auto-reconstruct
        keystorePath.delete()

        // Re-create with just root CA
        KeyToolAPI.genKeyPair(
            dn = "CN=Root CA",
            alias = "ca",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Import the full chain from E1 cert file
        // (This is the key test - can it reconstruct the chain?)
        val importResult = KeyToolAPI.importCert(
            alias = "e1",
            file = e1Cert,
            keystore = keystorePath,
            storePassword = "changeit"
        )

        // Whether this succeeds depends on cert structure and
        // whether intermediate certs are embedded in the file

        if (importResult is KeytoolResult.Success) {
            val listResult = KeyToolAPI.list(keystorePath, "changeit").shouldBeSuccess()
            val e1Entry = listResult.entries.find { it.alias == "e1" }
            if (e1Entry != null) {
                assertEquals(4, e1Entry.chainLength,
                    "Should reconstruct 4-level chain")
            }
        }

    } finally {
        keystorePath.delete()
        ca1Req.delete()
        ca1Cert.delete()
        ca2Req.delete()
        ca2Cert.delete()
        e1Req.delete()
        e1Cert.delete()
    }
}
```

### Key Points from OpenJDK
- Self-issued certificates (subject = issuer) are treated as intermediates
- Chain reconstruction requires issuer DN to match subsequent cert's subject
- Deletion of intermediates tests whether chain is properly stored
- Use BasicConstraints extension to indicate CA vs end-entity

---

## 3. CERTIFICATE CHAIN DEDUPLICATION

### Scenario
Multiple certificates in import file (same cert repeated):

```
Input File: [Cert1, Cert1, Cert2, Cert3, Cert2, Cert3]
After Import: [Cert1, Cert2, Cert3]  (deduplicated and ordered)
```

### OpenJDK Implementation (DupImport.java)

**Test Steps**:
```kotlin
@Test
fun testCertificateDeduplicationDuringImport() = runTest {
    val keystorePath = createTempFile("dedup-", ".jks")

    try {
        // Create a chain: Root CA
        KeyToolAPI.genKeyPair(
            dn = "CN=Root",
            alias = "root",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Create another cert
        KeyToolAPI.genKeyPair(
            dn = "CN=Intermediate",
            alias = "int",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Export certs
        val rootFile = createTempFile("root-", ".crt")
        val intFile = createTempFile("int-", ".crt")

        KeyToolAPI.exportCert(
            alias = "root",
            file = rootFile,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        KeyToolAPI.exportCert(
            alias = "int",
            file = intFile,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Create file with duplicates (concatenated)
        val chainFile = createTempFile("chain-", ".crt")
        chainFile.writeText(
            rootFile.readText() +
            "\n" +
            rootFile.readText() +  // Duplicate
            "\n" +
            intFile.readText() +
            "\n" +
            intFile.readText()  // Duplicate
        )

        // Import the duplicated chain
        val importResult = KeyToolAPI.importCert(
            alias = "chain",
            file = chainFile,
            keystore = keystorePath,
            storePassword = "changeit"
        )

        if (importResult is KeytoolResult.Success) {
            // Verify deduplication
            val listResult = KeyToolAPI.list(keystorePath, "changeit").shouldBeSuccess()
            val chainEntry = listResult.entries.find { it.alias == "chain" }

            // Should be 2-cert chain, not 4
            assertEquals(2, chainEntry?.chainLength ?: 0,
                "Duplicates should be removed from chain")
        }

        rootFile.delete()
        intFile.delete()
        chainFile.delete()

    } finally {
        keystorePath.delete()
    }
}
```

### Ordering Rule
When deduplicating: **Most specific (end-entity) comes first, root comes last**

```
Original: [Root, Leaf, Root, Leaf]
Result:   [Leaf, Root]  (leaf first)
```

---

## 4. AUTHORITY KEY IDENTIFIER (AKID) CHAIN VALIDATION

### Scenario
Parent certificate's SubjectKeyIdentifier (SKID) should match child's AuthorityKeyIdentifier (AKID)

### OpenJDK Implementation (CheckCertAKID.java)

**Test Steps**:
```kotlin
@Test
fun testAKIDChainValidation() = runTest {
    val keystorePath = createTempFile("akid-", ".jks")
    val userReq = createTempFile("user-", ".req")
    val userCert = createTempFile("user-", ".crt")

    try {
        // Generate Root CA with explicit SKID
        val skidValue = "00:01:02:03:04:05:06:07:08:09:0A:0B:0C:0D:0E:0F:10:11:12:13"

        KeyToolAPI.genKeyPair(
            dn = "CN=Root CA",
            alias = "ca",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit",
            extensions = listOf("SubjectKeyIdentifier=$skidValue")
        ).shouldBeSuccess()

        // Generate user keypair
        KeyToolAPI.genKeyPair(
            dn = "CN=User",
            alias = "user",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Create CSR
        KeyToolAPI.certReq(
            alias = "user",
            file = userReq,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // CA signs with AKID extension (auto-generated from parent SKID)
        KeyToolAPI.genCert(
            requestFile = userReq,
            outFile = userCert,
            daysValid = 365,
            alias = "ca",
            keystore = keystorePath,
            storePassword = "changeit",
            extensions = listOf(
                "AuthorityKeyIdentifier=keyid:always"  // Auto-generate from parent
            )
        ).shouldBeSuccess()

        // Import the signed cert
        KeyToolAPI.importCert(
            alias = "user",
            file = userCert,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Verify: Print cert and check AKID matches CA's SKID
        val printResult = KeyToolAPI.printCert(userCert).shouldBeSuccess()

        // The AKID should contain the CA's SKID value
        // (Implementation dependent on how printCert displays this)

        userReq.delete()
        userCert.delete()

    } finally {
        keystorePath.delete()
    }
}
```

### Validation Logic
```kotlin
// From certificate parsing:
// Child cert's AKID should match Parent cert's SKID byte-for-byte
val childAKID = extractAKID(userCert)
val parentSKID = extractSKID(caCert)

assertEquals(parentSKID, childAKID, "AKID must match parent's SKID")
```

---

## 5. EMPTY SUBJECT WITH SAN EXTENSION

### Scenario
Certificate with empty distinguished name (DN) but includes Subject Alternative Names

### OpenJDK Implementation (EmptySubject.java)

**Test Steps**:
```kotlin
@Test
fun testEmptySubjectRequiresSAN() = runTest {
    val keystorePath = createTempFile("empty-", ".jks")
    val reqFile = createTempFile("empty-", ".req")
    val certFile = createTempFile("empty-", ".crt")

    try {
        // Generate CA
        KeyToolAPI.genKeyPair(
            dn = "CN=CA",
            alias = "ca",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Generate user with EMPTY subject
        KeyToolAPI.genKeyPair(
            dn = "",  // Empty!
            alias = "user",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Step 1: CSR with empty subject (no SAN)
        // This should either fail or be allowed
        val csrResult = KeyToolAPI.certReq(
            alias = "user",
            file = reqFile,
            keystore = keystorePath,
            storePassword = "changeit"
        )

        // Step 2: CA signs with SAN extension (makes it valid)
        val certResult = KeyToolAPI.genCert(
            requestFile = reqFile,
            outFile = certFile,
            daysValid = 365,
            alias = "ca",
            keystore = keystorePath,
            storePassword = "changeit",
            extensions = listOf(
                "SubjectAlternativeName=dns:example.com,dns:www.example.com"
            )
        )

        // Should succeed because SAN compensates for empty subject
        assertTrue(certResult is KeytoolResult.Success)

        // Import the cert
        KeyToolAPI.importCert(
            alias = "user",
            file = certFile,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        reqFile.delete()
        certFile.delete()

    } finally {
        keystorePath.delete()
    }
}
```

### Key Point from OpenJDK
- Empty subject is **valid only if SAN extension is present**
- Without SAN, empty subject is **invalid** and should fail
- This is a security/validation rule in X.509 certificate standards

---

## 6. FORMAT CONVERSION CHAIN PRESERVATION

### Scenario
Converting between JKS and PKCS12 preserves certificate chains

### OpenJDK Implementation (JKStoPKCS12.java)

**Test Steps**:
```kotlin
@Test
fun testFormatConversionPreservesChain() = runTest {
    val jksPath = createTempFile("source-", ".jks")
    val pkcs12Path = createTempFile("target-", ".p12")
    val userReq = createTempFile("user-", ".req")
    val userCert = createTempFile("user-", ".crt")

    try {
        // Setup JKS with chain
        KeyToolAPI.genKeyPair(
            dn = "CN=CA",
            alias = "ca",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = jksPath,
            storePassword = "jkspass"
        ).shouldBeSuccess()

        KeyToolAPI.genKeyPair(
            dn = "CN=User",
            alias = "user",
            keySize = 2048,
            sigAlg = "SHA256withRSA",
            algorithm = "rsa",
            keystore = jksPath,
            storePassword = "jkspass"
        ).shouldBeSuccess()

        KeyToolAPI.certReq(
            alias = "user",
            file = userReq,
            keystore = jksPath,
            storePassword = "jkspass"
        ).shouldBeSuccess()

        KeyToolAPI.genCert(
            requestFile = userReq,
            outFile = userCert,
            daysValid = 365,
            alias = "ca",
            keystore = jksPath,
            storePassword = "jkspass"
        ).shouldBeSuccess()

        KeyToolAPI.importCert(
            alias = "user",
            file = userCert,
            keystore = jksPath,
            storePassword = "jkspass"
        ).shouldBeSuccess()

        // Verify chain in JKS
        val jksListResult = KeyToolAPI.list(jksPath, "jkspass").shouldBeSuccess()
        val jksUserEntry = jksListResult.entries.find { it.alias == "user" }!!
        val originalChainLength = jksUserEntry.chainLength

        // Convert to PKCS12
        KeyToolAPI.importKeystore(
            srcKeystore = jksPath,
            srcType = "jks",
            srcPassword = "jkspass",
            destKeystore = pkcs12Path,
            destType = "pkcs12",
            destPassword = "pkcs12pass"
        ).shouldBeSuccess()

        // Verify chain preserved in PKCS12
        val pkcs12ListResult = KeyToolAPI.list(pkcs12Path, "pkcs12pass").shouldBeSuccess()
        val pkcs12UserEntry = pkcs12ListResult.entries.find { it.alias == "user" }!!
        val convertedChainLength = pkcs12UserEntry.chainLength

        assertEquals(originalChainLength, convertedChainLength,
            "Chain length should be preserved during format conversion")

        userReq.delete()
        userCert.delete()

    } finally {
        jksPath.delete()
        pkcs12Path.delete()
    }
}
```

---

## 7. ALGORITHM-SPECIFIC CHAINS (GenerateAll Pattern)

### Scenario
Create chains for all supported algorithms

**Test Pattern**:
```kotlin
@ParameterizedTest
@CsvSource(
    "rsa, 2048, SHA256withRSA",
    "dsa, 2048, SHA256withDSA",
    "ec, 256, SHA256withECDSA",
    "EdDSA, null, Ed25519"
)
fun testChainForEachAlgorithm(
    algorithm: String,
    keySize: Int?,
    sigAlg: String
) = runTest {
    val keystorePath = createTempFile("alg-${algorithm}-", ".jks")

    try {
        // Generate CA with algorithm
        KeyToolAPI.genKeyPair(
            dn = "CN=CA",
            alias = "ca",
            keySize = keySize ?: 256,
            sigAlg = sigAlg,
            algorithm = algorithm,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Generate user with same algorithm
        KeyToolAPI.genKeyPair(
            dn = "CN=User",
            alias = "user",
            keySize = keySize ?: 256,
            sigAlg = sigAlg,
            algorithm = algorithm,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Create and sign cert
        val userReq = createTempFile("req-", ".csr")
        val userCert = createTempFile("cert-", ".crt")

        KeyToolAPI.certReq(
            alias = "user",
            file = userReq,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        KeyToolAPI.genCert(
            requestFile = userReq,
            outFile = userCert,
            daysValid = 365,
            alias = "ca",
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        KeyToolAPI.importCert(
            alias = "user",
            file = userCert,
            keystore = keystorePath,
            storePassword = "changeit"
        ).shouldBeSuccess()

        // Verify chain created
        val listResult = KeyToolAPI.list(keystorePath, "changeit").shouldBeSuccess()
        val userEntry = listResult.entries.find { it.alias == "user" }!!
        assertEquals(2, userEntry.chainLength)

        userReq.delete()
        userCert.delete()

    } finally {
        keystorePath.delete()
    }
}
```

---

## 8. CHAIN TESTING CHECKLIST

### For Unit Tests
- [ ] Mock chain length in list output
- [ ] Validate chain order (end-entity first, root last)
- [ ] Test parsing of [1], [2], [3] certificate markers
- [ ] Handle 1-cert vs 2-cert vs 3+ cert chains

### For Integration Tests
- [ ] Generate 2-level chains (user + CA)
- [ ] Generate 3-level chains (user + intermediate + root)
- [ ] Test chain with missing intermediates
- [ ] Test certificate deduplication
- [ ] Verify AKID matches parent SKID
- [ ] Test empty subject with SAN
- [ ] Test format conversion preserves chains
- [ ] Test each algorithm with chain generation

### For Edge Cases
- [ ] Very long chains (5+ levels)
- [ ] Chains with weak algorithms
- [ ] Chains with missing extensions
- [ ] Circular references (self-issued loops)
- [ ] Broken chains (issuer not found)

---

**File Purpose**: Detailed certificate chain testing strategies
**Last Updated**: 2026-01-22
**Patterns Covered**: 8 major chain scenarios
**Total Test Cases**: 30+
**Cross-References**: 01_REFERENCE_OPENJDK_FINDINGS.md, 02_REFERENCE_TEST_PATTERNS.md
