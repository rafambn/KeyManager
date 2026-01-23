# Reference: OpenJDK Keytool Test Analysis
## Complete Findings from Official Java Test Repository

**Source**: https://github.com/openjdk/jdk17/tree/master/test/jdk/sun/security/tools/keytool
**Test Count**: 45 official test classes
**Analysis Date**: 2026-01-22

---

## 1. OpenJDK TEST FILE ORGANIZATION

### Complete List of 45 Test Classes

```
1.  CacertsOption.java
2.  CheckCertAKID.java
3.  CloneKeyAskPassword.java
4.  CloseFile.java
5.  DefaultOptions.java
6.  DupCommands.java
7.  DupImport.java
8.  EmptySubject.java
9.  ExportPrivateKeyNoPwd.java
10. ExtOptionCamelCase.java
11. FileInHelp.java
12. GenKeyPairSigner.java
13. GenerateAll.java
14. GroupName.java
15. HasSrcStoretypeOption.java
16. ImportPrompt.java
17. ImportReadAll.java
18. ImportToPwordlessPK12.java
19. JKStoPKCS12.java
20. KeyAlg.java
21. KeyToolTest.java (Core comprehensive)
22. ListOrder.java
23. NewHelp.java
24. NewSize7.java
25. NoExtNPE.java
26. NssTest.java (Platform: PKCS11)
27. PKCS12Passwd.java
28. PrintSSL.java
29. ProbingFailure.java
30. ReadJar.java
31. RealType.java
32. RemoveKeyAlgDefault.java
33. Resource.java
34. SecretKeyKS.java
35. SecurityToolsTest.java
36. SelfIssued.java
37. Serial64.java
38. StandardAlgName.java
39. StartDateTest.java
40. StorePasswords.java
41. TryStore.java
42. UnknownAndUnparseable.java
43. WeakAlg.java
44. i18n.java
45. And supporting test infrastructure files
```

### Supporting Infrastructure Files

**Fake Cacerts** (`/fakecacerts/`):
- `MyOwnCacerts.java` - Custom trusted certificate store
- `TrustedCRL.java` - Certificate Revocation List
- `TrustedCert.java` - Pre-built trusted certificate

**Pre-built Test Keystores**:
- `CloneKeyAskPassword.jks` - Contains certificates without X.509 extensions
- `SecretKeyKS.jks` - Stores secret keys for boundary testing

**Configuration Files**:
- `p11-nss.txt` - PKCS#11 NSS configuration for hardware token simulation
- `i18n.html` - Internationalization documentation

---

## 2. TEST CATEGORIES BY PURPOSE

### A. Core Functionality Tests (15 tests)

| Test File | Function Tested | Key Scenario |
|---|---|---|
| `GenerateAll.java` | `-genkeypair` | All algorithm combinations (RSA, DSA, EC, EdDSA) |
| `GenKeyPairSigner.java` | `-gencert` | CA signing of user certificates |
| `PKCS12Passwd.java` | `-keypasswd`, `-storepasswd` | Password management across formats |
| `JKStoPKCS12.java` | `-importkeystore` | Format conversion JKS↔PKCS12 |
| `DupImport.java` | `-importcert` | Deduplication and chain reconstruction |
| `SelfIssued.java` | `-importcert`, `-gencert` | Self-issued certificate chain |
| `StartDateTest.java` | `-gencert` | Certificate backdating with `-startdate` |
| `ListOrder.java` | `-list` | Certificate listing order validation |
| `PrintSSL.java` | `-printcert` | Remote SSL cert retrieval and printing |
| `SecretKeyKS.java` | `-genseckey` | Symmetric key generation and storage |
| `ImportPrompt.java` | `-importcert` | Interactive prompt handling |
| `ReadJar.java` | `-exportcert` | JAR manifest/signature validation |
| `KeyAlg.java` | `-genkeypair` | Algorithm selection and variant naming |
| `StandardAlgName.java` | Algorithm parsing | Case-insensitive algorithm names |
| `GroupName.java` | `-genkeypair` | EC curve group naming conventions |

### B. Error & Edge Case Tests (15 tests)

| Test File | Error Condition | OpenJDK Bug |
|---|---|---|
| `EmptySubject.java` | Empty DN requires SAN | Validation rule |
| `NoExtNPE.java` | Certs without extensions crash | Bug 6813402 |
| `WeakAlg.java` | Weak algorithm detection | Security rule |
| `DupCommands.java` | Duplicate commands | Argument validation |
| `UnknownAndUnparseable.java` | Malformed extensions | Output parsing |
| `ExtOptionCamelCase.java` | Extension option parsing | Ambiguity handling |
| `CloseFile.java` | Stream closure issues | Bug 6489721 |
| `RemoveKeyAlgDefault.java` | Missing required `-keyalg` | Argument validation |
| `TryStore.java` | Keystore corruption prevention | Robustness |
| `ProbingFailure.java` | Non-probeable keystores | Bug 8214100 |
| `CacertsOption.java` | System cacerts conflicts | Usage validation |
| `DefaultOptions.java` | Pre-configured defaults | Configuration |
| `RealType.java` | Keystore type persistence | Format handling |
| `HasSrcStoretypeOption.java` | Format auto-detection | Bug 8162752 |
| `ExportPrivateKeyNoPwd.java` | macOS KeychainStore | Platform-specific |

### C. Security Validation Tests (8 tests)

| Test File | Security Aspect | Coverage |
|---|---|---|
| `WeakAlg.java` | Algorithm strength | MD5, SHA-1, 512-bit RSA, weak keys |
| `StorePasswords.java` | PBE algorithms | 18 PBE variants tested |
| `Serial64.java` | Serial number distribution | 64-bit serial generation |
| `EmptySubject.java` | Subject validation | Empty DN requires SAN |
| `CheckCertAKID.java` | Chain validation | Authority Key Identifier |
| `SelfIssued.java` | Chain integrity | Self-issued cert validation |
| `DupImport.java` | Chain ordering | Correct deduplication order |
| `NoExtNPE.java` | Extension robustness | Handle missing extensions |

### D. Compatibility & Interoperability Tests (7 tests)

| Test File | Compatibility | Coverage |
|---|---|---|
| `JKStoPKCS12.java` | Format conversion | JKS↔PKCS12 migration |
| `PKCS12Passwd.java` | Format semantics | Password behavior across formats |
| `ImportToPwordlessPK12.java` | PKCS12 variants | Passwordless PKCS12 creation |
| `NssTest.java` | PKCS#11 backend | NSS library integration |
| `ExportPrivateKeyNoPwd.java` | macOS integration | KeychainStore support |
| `RealType.java` | Format persistence | Type specification and storage |
| `HasSrcStoretypeOption.java` | Format detection | Auto-probing mechanism |

### E. Regression & Known Issues Tests (5 tests)

| Test File | Known Issue | OpenJDK Bug ID |
|---|---|---|
| `NoExtNPE.java` | NullPointerException | Bug 6813402 |
| `CloseFile.java` | Stream not closed | Bug 6489721 |
| `SelfIssued.java` | Chain reconstruction | Bug 6825352 |
| `ProbingFailure.java` | Format detection | Bug 8214100 |
| `HasSrcStoretypeOption.java` | Type auto-detection | Bug 8162752 |

---

## 3. ASSERTION PATTERNS USED IN OpenJDK TESTS

### Pattern 1: Basic Command Execution
```java
SecurityTools.keytool("-genkeypair -alias test -storepass changeit")
    .shouldHaveExitValue(0)
    .shouldContain("Generating");
```

### Pattern 2: Chained Assertions
```java
SecurityTools.keytool("-list -v -keystore ks")
    .shouldHaveExitValue(0)
    .shouldContain("Alias name:")
    .shouldContain("Owner: CN=Test")
    .shouldNotContain("Error")
    .shouldMatch("Certificate fingerprint.*SHA-256:");
```

### Pattern 3: Error Validation
```java
SecurityTools.keytool("-genkeypair -alias test")  // Missing -keyalg
    .shouldHaveExitValue(1)
    .shouldContain("option -keyalg must be specified");
```

### Pattern 4: Interactive Input Simulation
```java
SecurityTools.setResponse("yes");
SecurityTools.keytool("-importcert -file cert.crt")
    .shouldHaveExitValue(0);
```

### Pattern 5: Post-Execution Validation
```java
// Execute command
SecurityTools.keytool("-genkeypair -alias test").shouldHaveExitValue(0);

// Validate result by loading keystore
KeyStore ks = KeyStore.getInstance("JKS");
ks.load(Files.newInputStream(Paths.get("keystore.jks")),
        "password".toCharArray());
assertTrue(ks.containsAlias("test"));
X509Certificate cert = (X509Certificate)ks.getCertificate("test");
assertEquals(2048, ((RSAPublicKey)cert.getPublicKey()).getModulus().bitLength());
```

### Pattern 6: Output Capture and Custom Assertion
```java
ByteArrayOutputStream bout = new ByteArrayOutputStream();
PrintStream oldOut = System.out;
System.setOut(new PrintStream(bout));

SecurityTools.keytool("-printcert -file cert.crt").shouldHaveExitValue(0);

System.setOut(oldOut);
String output = bout.toString();
assertTrue(output.contains("Signature algorithm name:"));
```

### Pattern 7: Algorithm Matrix Testing
```java
String[] algorithms = {"rsa", "dsa", "ec", "EdDSA"};
for (String alg : algorithms) {
    SecurityTools.keytool("-genkeypair -keyalg " + alg)
        .shouldHaveExitValue(0);
}
```

### Pattern 8: Exception-Based Testing
```java
try {
    SecurityTools.keytool("-unknown-option").shouldHaveExitValue(0);
    fail("Should have thrown exception");
} catch (Exception e) {
    assertTrue(e.getMessage().contains("Unknown option"));
}
```

---

## 4. TEST DATA GENERATION STRATEGIES

### Strategy 1: In-line Command Chain
Used by: `SelfIssued.java`, `GenerateAll.java`, `GenKeyPairSigner.java`

```java
// Generate keypairs
keytool("-genkeypair -alias ca -keyalg rsa -dname CN=CA");
keytool("-genkeypair -alias e1 -keyalg rsa -dname CN=E1");

// Request certificate from entity
keytool("-alias e1 -certreq -file e1.req");

// CA signs the request
keytool("-alias ca -gencert -infile e1.req -outfile e1.crt -ext san=dns:e1");

// Import signed certificate
keytool("-alias e1 -importcert -file e1.crt");

// Verify chain
keytool("-alias e1 -list -v")
    .shouldContain("[2]");  // 2-certificate chain
```

### Strategy 2: Pre-built Test Keystores
Used by: `NoExtNPE.java`, `SecretKeyKS.java`

```
Repository stores: CloneKeyAskPassword.jks, SecretKeyKS.jks
Retrieved via: Paths.get(System.getProperty("test.src"), "CloneKeyAskPassword.jks")
Purpose: Regression testing with specific certificate content
```

### Strategy 3: Parameterized Algorithm Testing
Used by: `GenerateAll.java`, `KeyAlg.java`, `StorePasswords.java`

```java
// Algorithm matrix: RSA, DSA, EC variants, EdDSA
Map<String, String[]> algorithms = Map.of(
    "rsa", new String[]{"1024", "2048", "4096"},
    "dsa", new String[]{"2048"},
    "ec", new String[]{"secp256r1", "secp384r1", "secp521r1"},
    "EdDSA", new String[]{null}  // No keysize needed
);

for (String alg : algorithms.keySet()) {
    for (String size : algorithms.get(alg)) {
        String cmd = size != null ?
            "-keyalg " + alg + " -keysize " + size :
            "-keyalg " + alg;
        keytool(cmd).shouldHaveExitValue(0);
    }
}
```

### Strategy 4: Certificate Chain Fixtures
Used by: `SelfIssued.java`, `CheckCertAKID.java`, `DupImport.java`

```java
// Hierarchical structure:
// CA (self-signed)
//  ├─ CA1 (CA-signed, self-issued)
//  │  └─ CA2 (CA1-signed, self-issued)
//  │     └─ E1 (CA2-signed, end-entity)
//
// Files generated: ca.crt, ca1.crt, ca2.crt, e1.crt
// Chain validation: Import order matters, deduplication tested
```

### Strategy 5: Password Variation Matrix
Used by: `PKCS12Passwd.java`, `StorePasswords.java`

```java
// Test combinations:
String[] formats = {"jks", "pkcs12"};
String[] passwords = {
    "unified",           // store=key
    "separate",          // store!=key
    "special-chars"      // Validate special char handling
};

for (String fmt : formats) {
    for (String pwd : passwords) {
        keytool("-storetype " + fmt + " -keypass " + pwd)
            .shouldHaveExitValue(0);
    }
}
```

---

## 5. CRITICAL KEYTOOL ERROR MESSAGES

These are the exact error patterns OpenJDK tests validate:

```
1. "keytool error: java.lang.Exception: [message]"
2. "keytool error: java.io.FileNotFoundException: [path]"
3. "keytool error: java.security.KeyStoreException: [message]"
4. "Error reading/writing keystore..."
5. "Failed to establish chain from reply"
6. "Public keys in reply and keystore don't match"
7. "Certificate is untrusted"
8. "Alias <name> does not exist"
9. "Unknown keystore type"
10. "Unrecognized keystore format"
11. "Cannot derive signature algorithm"
12. "Cannot specify both -groupname and -keysize"
13. "Ambiguous option"
14. "Only one command is allowed"
15. "option -keyalg must be specified"
16. "The generated certificate ... is disabled"
17. "Warning: ... will be disabled in a future release"
18. "Input not an X.509 certificate"
19. "Missing critical certificate fields"
20. "Certificate chain too long"
21. "Weak algorithm used"
22. "Cannot modify entry"
23. "File already exists"
24. And 25+ more error conditions...
```

---

## 6. KEYTOOL OPERATION MATRIX FROM OpenJDK

| Operation | Tests | Notes |
|---|---|---|
| `-genkeypair` | GenerateAll (30+ alg variants), KeyAlg, GroupName, StartDateTest, StandardAlgName | Covers all supported algorithms |
| `-genseckey` | PKCS12Passwd, SecretKeyKS, StorePasswords | Symmetric key generation |
| `-gencert` | GenKeyPairSigner, SelfIssued, GenerateAll, StartDateTest, EmptySubject | Certificate signing |
| `-certreq` | GenKeyPairSigner, SelfIssued, GenerateAll | Certificate request generation |
| `-exportcert` | PrintSSL, ReadJar, ExportPrivateKeyNoPwd | Export operations |
| `-importcert` | DupImport, SelfIssued, EmptySubject, ImportPrompt, ImportReadAll, WeakAlg | Certificate import |
| `-importkeystore` | JKStoPKCS12, ImportToPwordlessPK12, TryStore, HasSrcStoretypeOption | Keystore migration |
| `-importpass` | PKCS12Passwd, StorePasswords | Password entry import |
| `-delete` | DupCommands, CloseFile | Alias deletion |
| `-changealias` | RealType, ListOrder | Alias renaming |
| `-keypasswd` | PKCS12Passwd, StorePasswords, ImportPrompt | Key password modification |
| `-storepasswd` | StorePasswords, PKCS12Passwd | Store password modification |
| `-printcert` | PrintSSL, UnknownAndUnparseable, WeakAlg, ReadJar | Certificate display |
| `-printcertreq` | GenKeyPairSigner, UnknownAndUnparseable | Certificate request display |
| `-printcrl` | GenerateAll, UnknownAndUnparseable | CRL display |
| `-list` | KeyToolTest, ListOrder, WeakAlg, StartDateTest, NssTest | Keystore listing |

---

## 7. ALGORITHM & SIGNATURE COMBINATIONS TESTED

### Key Algorithms
```
RSA:     512, 1024 (deprecated), 2048 (default), 4096, 8192
         512-bit and below: DISABLED
         1024-bit: DEPRECATED, shows warning

DSA:     512, 1024, 2048 (default), 4096

EC:      secp256r1 (P-256, default), secp384r1 (P-384), secp521r1 (P-521)
         Also: P-192, sect163k1 (binary curves)

EdDSA:   Ed25519 (default), Ed448

XDH:     X25519, X448 (key exchange, cannot sign)

Secret:  DES, 3DES, AES-128, AES-192, AES-256
```

### Signature Algorithms
```
RSA-based:       MD5withRSA (DISABLED), SHA1withRSA (DEPRECATED),
                 SHA256withRSA (default), SHA384withRSA, SHA512withRSA,
                 SHA256withRSA/PSS, SHA384withRSA/PSS, SHA512withRSA/PSS

EC-based:        SHA1withECDSA (DEPRECATED), SHA256withECDSA (default),
                 SHA384withECDSA, SHA512withECDSA, SHA3-256withECDSA

EdDSA:           Ed25519, Ed448 (signature algorithm = key algorithm)
```

### PBE (Password-Based Encryption) Algorithms - 18 Variants
```
Classic:         PBEWithMD5AndDES, PBEWithSHA1AndDES
                 PBEWithSHA1And2Key-TripleDES-CBC
                 PBEWithSHA1And40BitRC2-CBC

HMAC variants:   PBEWithHmacSHA256AndAES_128
                 PBEWithHmacSHA256AndAES_256
                 PBEWithHmacSHA384AndAES_256
                 PBEWithHmacSHA512AndAES_256
                 And 8 more with different combinations
```

---

## 8. KEYSTORE FORMATS & PROPERTIES

### JKS (Java KeyStore)
- **Default Format**: Most widely used
- **Properties**:
  - Supports PrivateKeyEntry, TrustedCertificateEntry, SecretKeyEntry
  - Allows separate storepass and keypass (store!/keypass are independent)
  - Binary format
  - Deprecated in Java 9+ (shows warning)

### PKCS12
- **RFC Standard**: Industry-standard format
- **Properties**:
  - Supports same entry types as JKS
  - Password unification: when loaded as PKCS12, storepass and keypass behave as same
  - Binary format (based on ASN.1 DER)
  - Recommended format for new deployments

### JCEKS (Java Cryptography Extension KeyStore)
- **Extended JKS**: Variant of JKS with additional features
- **Properties**:
  - Supports SecretKeyEntry for symmetric keys
  - Compatible with JKS operations

### KeychainStore
- **macOS-specific**: Integrates with system keychain
- **Properties**:
  - Different password semantics
  - Can load with null password
  - Limited cross-platform support

### PKCS11
- **Hardware Tokens**: Integration with smart cards, hardware security modules
- **Test Implementation**: NSS library simulation
- **Timeout**: 600 seconds (much longer than regular tests)

---

## 9. COMPREHENSIVE ERROR & WARNING CATALOG

### Error Conditions (25+ types)

**Input Validation Errors**:
- Missing required parameter (e.g., no -keyalg)
- Conflicting options (e.g., both -groupname and -keysize)
- Invalid option value (e.g., -keysize 0)
- Ambiguous abbreviation (e.g., -ku=d could mean digitalSignature or dataEncipherment)

**Keystore Errors**:
- Keystore file not found
- Wrong keystore password
- Unrecognized keystore format
- Corrupted keystore file
- Keystore type mismatch
- Cannot read/write keystore

**Certificate Errors**:
- Certificate not found
- Certificate chain too long
- Failed to establish chain from reply
- Public key mismatch between certificate and request
- Invalid certificate format
- Missing critical certificate fields
- Certificate without X.509 extensions (causes NPE if not handled)

**Algorithm Errors**:
- Algorithm not available from provider
- Cannot derive signature algorithm from key type
- Weak algorithm used (MD5, SHA-1, 512-bit RSA)
- Deprecated algorithm (will be disabled)
- Disabled algorithm (cannot use)

**File Operation Errors**:
- File not found
- Cannot create file (permission)
- Cannot delete file (stream open)
- File already exists
- Cannot read file

**Operational Errors**:
- Alias already exists
- Alias does not exist
- Entry type mismatch
- Duplicate command
- Command required (none specified)
- Password entry not found

### Warning Conditions

- "Warning: [Algorithm] is disabled in this java installation"
- "Warning: [Algorithm] will be disabled in a future release"
- "Warning: The generated certificate ... [algorithm] ... is disabled"
- "Warning: [Format] is deprecated. Use PKCS12 instead."
- "Warning: KeyStore format is JKS (proprietary)"

---

## 10. HOW THIS REFERENCE IS USED

**During Phase 2 (Unit Tests)**:
- Use error messages from Section 5
- Reference assertion patterns from Section 3
- Cover algorithm combinations from Section 7

**During Phase 3 (Integration Tests)**:
- Use operation matrix from Section 6
- Reference test data strategies from Section 4
- Validate with patterns from Section 3

**During Phase 4 (Edge Cases)**:
- Reference known bugs from Section 2 (regression tests)
- Validate error/warning conditions from Section 9
- Check security validations from original research

**For Context Recovery**:
- This file contains complete reference data
- No need to re-research OpenJDK tests
- Cross-reference with phase plans for specific test cases

---

**File Purpose**: Comprehensive reference to OpenJDK keytool test patterns
**Last Updated**: 2026-01-22
**For Questions**: Review the phase plans that reference specific sections
