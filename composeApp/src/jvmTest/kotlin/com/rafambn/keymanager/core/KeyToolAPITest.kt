package com.rafambn.keymanager.core

import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import com.rafambn.keymanager.keytool.KeyToolAPI
import com.rafambn.keymanager.keytool.KeyToolExecutor
import com.rafambn.keymanager.keytool.KeytoolResult
import com.rafambn.keymanager.keytool.enums.ECCurve
import com.rafambn.keymanager.keytool.enums.EntryType
import com.rafambn.keymanager.keytool.enums.KeyAlgorithm
import com.rafambn.keymanager.keytool.enums.SignatureAlgorithm

class KeyToolAPITest {

    companion object {
        private const val TEST_KEYSTORE_PATH = "/tmp/test.jks"
        private const val TEST_PASSWORD = "testpass123"
        private const val TEST_ALIAS = "mykey"
        private const val TEST_CERT_PATH = "/tmp/cert.pem"
    }

    private val originalFactory = KeyToolExecutor.processFactory

    @BeforeTest
    fun setUp() {
        // Reset to original factory before each test
        KeyToolExecutor.processFactory = originalFactory
    }

    @AfterTest
    fun tearDown() {
        // Restore original factory after each test
        KeyToolExecutor.processFactory = originalFactory
    }

    // ===== Helper: Create Mock Process =====

    private fun createMockProcess(stdout: String, stderr: String = "", exitCode: Int = 0): Process {
        return object : Process() {
            override fun getOutputStream() = object : java.io.OutputStream() {
                override fun write(b: Int) {}
            }

            override fun getInputStream() = ByteArrayInputStream(stdout.toByteArray())

            override fun getErrorStream() = ByteArrayInputStream(stderr.toByteArray())

            override fun waitFor(): Int = exitCode

            override fun waitFor(timeout: Long, unit: java.util.concurrent.TimeUnit): Boolean = true

            override fun exitValue(): Int = exitCode

            override fun destroy() {}

            override fun destroyForcibly(): Process = this

            override fun isAlive(): Boolean = false
        }
    }

    private var capturedCommand: List<String>? = null

    private fun setupMockFactory(stdout: String = "", stderr: String = "", exitCode: Int = 0) {
        KeyToolExecutor.processFactory = { command: List<String> ->
            capturedCommand = command
            createMockProcess(stdout, stderr, exitCode)
        }
    }

    // ===== Test Data =====

    private val validKeystoreListOutput = """
        Keystore type: PKCS12
        Keystore provider: SUN

        Your keystore contains 2 entries

        Alias name: mykey
        Creation date: Jan 22, 2026
        Entry type: PrivateKeyEntry
        Certificate chain length: 1
        Certificate[1]:
        Owner: CN=Test Key, O=Test Org, C=US
        Issuer: CN=Test Key, O=Test Org, C=US
        Serial number: 1234567890abcdef
        Valid from: Mon Jan 22 10:00:00 BRT 2026 until: Mon Jan 22 10:00:00 BRT 2027
        Certificate Signature Strength: 2048-bit
        Signature algorithm name: SHA256withRSA
        Subject Public Key Algorithm: 2048-bit RSA key
        Subject Public Key Size: 2048 bits
        Subject Public Key Exponent: 65537
        Public Key SHA-1 Fingerprint: AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12
        Public Key SHA-256 Fingerprint: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD

        Alias name: anothercert
        Creation date: Jan 20, 2026
        Entry type: trustedCertEntry
        Owner: CN=Another Cert, O=Another Org, C=US
        Issuer: CN=Another Cert, O=Another Org, C=US
        Serial number: fedcba0987654321
        Valid from: Mon Jan 20 10:00:00 BRT 2026 until: Mon Jan 20 10:00:00 BRT 2027
        Certificate Signature Strength: 2048-bit
        Signature algorithm name: SHA256withRSA
        Subject Public Key Algorithm: 2048-bit RSA key
        Subject Public Key Size: 2048 bits
        Subject Public Key Exponent: 65537
        Public Key SHA-256 Fingerprint: AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78
    """.trimIndent()

    private val validCertificateOutput = """
        Owner: CN=Test Certificate, O=Test Organization, C=US
        Issuer: CN=Test Certificate, O=Test Organization, C=US
        Serial number: abc123def456
        Valid from: Mon Jan 22 10:00:00 BRT 2026 until: Tue Jan 22 10:00:00 BRT 2027
        Certificate Signature Strength: 2048-bit
        Signature algorithm name: SHA256withRSA
        Version: 3

        Extensions:

        #1: ObjectIdentifier: 2.5.29.14 Criticality=false
        SubjectKeyIdentifier [
        KeyIdentifier [
        0000: 12 34 56 78 90 AB CD EF   12 34 56 78 90 AB CD EF
        0010: 12 34 56 78
        ]
        ]

        SHA-1: AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12
        SHA-256: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD
        MD5: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD
    """.trimIndent()

    private val validCertReqOutput = """
        Certification Request:
        	Data:
        		Version: 0 (0x0)
        		Subject: CN=Test CSR, O=Test Org, C=US
        		Public Key Algorithm: rsaEncryption
        			Public-Key: (2048 bit)
        			Modulus:
        				00:ab:cd:ef:12:34:56:78:90:ab:cd:ef:12:34:56
        		Attributes:
        			Requested Extensions:
        				X509v3 Subject Alternative Name:
        					DNS:example.com, DNS:www.example.com
        	Signature Algorithm: sha256WithRSAEncryption
        	Signature Value:
        		12:34:56:78:90:ab:cd:ef:12:34:56:78:90:ab:cd:ef

        Subject: CN=Test CSR, O=Test Org, C=US
        Signature algorithm: sha256WithRSAEncryption
        OID: 2.5.4.3
        OID: 2.5.4.10
        OID: 2.5.4.6
    """.trimIndent()

    private val validCrlOutput = """
        Certificate Revocation List (CRL)
        Version: 1 (0x0)
        Issuer: CN=Test CA, O=Test Organization, C=US
        This Update: Jan 22 10:00:00 2026 GMT
        Next Update: Feb 22 10:00:00 2026 GMT

        Revoked Certificates:
        Serial Number: 0x123456789abcdef0
            Revocation Date: Jan 15 10:00:00 2026 GMT
        Serial Number: 0x0fedcba987654321
            Revocation Date: Jan 10 10:00:00 2026 GMT
    """.trimIndent()

    // ===== 1. LIST Function Tests =====

    @Test
    fun testListSuccess() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD)

        assertTrue(result is KeytoolResult.Success)
        val keystoreInfo = (result).data
        assertEquals("PKCS12", keystoreInfo.type)
        assertEquals("SUN", keystoreInfo.provider)
        assertEquals(2, keystoreInfo.entryCount)
        assertEquals(2, keystoreInfo.entries.size)

        // Check first entry
        assertEquals("mykey", keystoreInfo.entries[0].alias)
        assertEquals(EntryType.PRIVATE_KEY, keystoreInfo.entries[0].entryType)
        assertEquals("CN=Test Key, O=Test Org, C=US", keystoreInfo.entries[0].owner)
        assertEquals(1, keystoreInfo.entries[0].certificateChainLength)
        assertEquals("RSA", keystoreInfo.entries[0].algorithm)
    }

    @Test
    fun testListWrongPassword() = runTest {
        setupMockFactory(
            "",
            "keytool error: java.lang.Exception: Keystore was tampered with, or password was incorrect",
            1
        )
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), "wrongpass")

        assertTrue(result is KeytoolResult.Error)
        assertEquals(1, result.exitCode)
        assertTrue(result.message.contains("password was incorrect"))
    }

    @Test
    fun testListKeystoreNotFound() = runTest {
        setupMockFactory(
            "",
            "keytool error: java.io.FileNotFoundException: /tmp/test.jks (No such file or directory)",
            1
        )
        val result = KeyToolAPI.list(File("/nonexistent.jks"), TEST_PASSWORD)

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("No such file or directory"))
    }

    @Test
    fun testListParsingError() = runTest {
        setupMockFactory("invalid output that doesn't match expected format", "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD)

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Failed to parse"))
    }

    @Test
    fun testListVerboseFalse() = runTest {
        val simpleOutput = "Keystore type: PKCS12\nYour keystore contains 1 entries"
        setupMockFactory(simpleOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, verbose = false)

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testListMultipleAliases() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD)

        assertTrue(result is KeytoolResult.Success)
        val keystoreInfo = (result).data
        assertEquals(2, keystoreInfo.entries.size)

        // Check second entry
        assertEquals("anothercert", keystoreInfo.entries[1].alias)
        assertEquals(EntryType.TRUSTED_CERT, keystoreInfo.entries[1].entryType)
    }

    // ===== Non-Verbose List Tests =====

    @Test
    fun testListNonVerboseSuccess() = runTest {
        val nonVerboseOutput = """
            Keystore type: PKCS12
            Keystore provider: SUN

            Your keystore contains 2 entries

            Alias name: mykey
            Creation date: Jan 22, 2026
            Entry type: PrivateKeyEntry
            Certificate chain length: 1

            Alias name: anothercert
            Creation date: Jan 20, 2026
            Entry type: trustedCertEntry
        """.trimIndent()

        setupMockFactory(nonVerboseOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, verbose = false)

        assertTrue(result is KeytoolResult.Success)
        val keystoreInfo = result.data

        // Verify basic fields are populated
        assertEquals("PKCS12", keystoreInfo.type)
        assertEquals(2, keystoreInfo.entryCount)
        assertEquals("mykey", keystoreInfo.entries[0].alias)
        assertEquals(EntryType.PRIVATE_KEY, keystoreInfo.entries[0].entryType)
        assertEquals("Jan 22, 2026", keystoreInfo.entries[0].creationDate)
        assertEquals(1, keystoreInfo.entries[0].certificateChainLength)

        // Verify verbose-only fields are null
        assertNull(keystoreInfo.entries[0].owner)
        assertNull(keystoreInfo.entries[0].issuer)
        assertNull(keystoreInfo.entries[0].algorithm)
        assertNull(keystoreInfo.entries[0].fingerprint)
    }

    @Test
    fun testListNonVerboseEmptyKeystore() = runTest {
        val emptyKeystoreOutput = """
            Keystore type: PKCS12
            Keystore provider: SUN

            Your keystore contains 0 entries
        """.trimIndent()

        setupMockFactory(emptyKeystoreOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, verbose = false)

        assertTrue(result is KeytoolResult.Success)
        val keystoreInfo = result.data
        assertEquals(0, keystoreInfo.entryCount)
        assertEquals(0, keystoreInfo.entries.size)
    }

    @Test
    fun testListNonVerboseSingleEntry() = runTest {
        val singleEntryOutput = """
            Keystore type: JKS
            Keystore provider: SUN

            Your keystore contains 1 entries

            Alias name: singlekey
            Creation date: Feb 01, 2026
            Entry type: PrivateKeyEntry
            Certificate chain length: 2
        """.trimIndent()

        setupMockFactory(singleEntryOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, verbose = false)

        assertTrue(result is KeytoolResult.Success)
        val keystoreInfo = result.data
        assertEquals(1, keystoreInfo.entries.size)
        assertEquals("singlekey", keystoreInfo.entries[0].alias)
        assertEquals(2, keystoreInfo.entries[0].certificateChainLength)
    }

    @Test
    fun testListVerboseHasAllFields() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, verbose = true)

        assertTrue(result is KeytoolResult.Success)
        val keystoreInfo = result.data

        // Verify verbose-only fields are populated
        assertNotNull(keystoreInfo.entries[0].owner)
        assertNotNull(keystoreInfo.entries[0].issuer)
        assertNotNull(keystoreInfo.entries[0].algorithm)
        assertNotNull(keystoreInfo.entries[0].fingerprint)
        assertEquals("RSA", keystoreInfo.entries[0].algorithm)
        assertTrue(keystoreInfo.entries[0].fingerprint?.contains(":") ?: false)
    }

    @Test
    fun testListNonVerboseMultipleEntriesMixedTypes() = runTest {
        val mixedTypesOutput = """
            Keystore type: PKCS12
            Keystore provider: SUN

            Your keystore contains 3 entries

            Alias name: privatekey
            Creation date: Jan 15, 2026
            Entry type: PrivateKeyEntry
            Certificate chain length: 1

            Alias name: trustedcert
            Creation date: Jan 16, 2026
            Entry type: trustedCertEntry

            Alias name: secretkey
            Creation date: Jan 17, 2026
            Entry type: SecretKeyEntry
        """.trimIndent()

        setupMockFactory(mixedTypesOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, verbose = false)

        assertTrue(result is KeytoolResult.Success)
        val keystoreInfo = result.data
        assertEquals(3, keystoreInfo.entries.size)
        assertEquals(EntryType.PRIVATE_KEY, keystoreInfo.entries[0].entryType)
        assertEquals(EntryType.TRUSTED_CERT, keystoreInfo.entries[1].entryType)
        assertEquals(EntryType.SECRET_KEY, keystoreInfo.entries[2].entryType)
    }

    @Test
    fun testListNonVerboseParsingError() = runTest {
        val malformedOutput = """
            Keystore type: PKCS12
            Your keystore contains invalid entries
            Alias name: test
            Entry type: InvalidEntryType
        """.trimIndent()

        setupMockFactory(malformedOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, verbose = false)

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Failed to parse"))
    }

    // ===== 2. GENKEYPAIR Function Tests =====

    @Test
    fun testGenKeyPairSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testGenKeyPairInvalidDname() = runTest {
        setupMockFactory("", "keytool error: java.lang.Exception: DName components must be separated by \",\"", 1)
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "InvalidDname", 365
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue((result).message.contains("DName"))
    }

    @Test
    fun testGenKeyPairAliasAlreadyExists() = runTest {
        setupMockFactory(
            "",
            "keytool error: java.lang.Exception: Key pair not generated, alias <mykey> already exists",
            1
        )
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365
        )

        assertTrue(result is KeytoolResult.Error)
    }

    @Test
    fun testGenKeyPairWithCustomSigAlg() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, signatureAlgorithm = SignatureAlgorithm.SHA512_WITH_RSA
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testGenKeyPairWithCustomKeysize() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keySize = 4096
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testGenKeyPairInvalidKeySizeTooSmall() = runTest {
        setupMockFactory("", "keytool error: 512 < 1024: is disabled", 1)
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keySize = 512
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("disabled"))
    }

    @Test
    fun testGenKeyPairInvalidKeySizeTooBig() = runTest {
        setupMockFactory("", "keytool error: Invalid key size: 20000", 1)
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keySize = 20000
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("not valid for"))
    }

    @Test
    fun testGenSecKeyAES128() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.genSecKey(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            KeyAlgorithm.AES, 128
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testGenSecKeyAES192() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.genSecKey(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            KeyAlgorithm.AES, 192
        )

        assertTrue(result is KeytoolResult.Success)
    }

    // ===== 3. GENSECKEY Function Tests =====

    @Test
    fun testGenSecKeySuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.genSecKey(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123", KeyAlgorithm.AES, 256
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testGenSecKeyInvalidAlgorithm() = runTest {
        setupMockFactory("", "keytool error: java.lang.Exception: -keyalg must be specified", 1)
        val result = KeyToolAPI.genSecKey(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123", KeyAlgorithm.RSA, 256
        )

        assertTrue(result is KeytoolResult.Error)
    }

    // ===== 4. GENCERT Function Tests =====

    @Test
    fun testGenCertSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.genCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), File("/tmp/cert.pem")
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testGenCertWithoutKeypass() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.genCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, null,
            File("/tmp/req.csr"), File("/tmp/cert.pem")
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testGenCertInvalidInputFile() = runTest {
        setupMockFactory("", "keytool error: java.io.FileNotFoundException: /tmp/nonexistent.csr", 1)
        val result = KeyToolAPI.genCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, null,
            File("/tmp/nonexistent.csr"), File("/tmp/cert.pem")
        )

        assertTrue(result is KeytoolResult.Error)
    }

    // ===== 5. CERTREQ Function Tests =====

    @Test
    fun testCertReqSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.certReq(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123", File("/tmp/req.csr")
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testCertReqWithoutKeypass() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.certReq(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, null, File("/tmp/req.csr")
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testCertReqWithSigAlg() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.certReq(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), SignatureAlgorithm.SHA512_WITH_RSA
        )

        assertTrue(result is KeytoolResult.Success)
    }

    // ===== 6. EXPORTCERT Function Tests =====

    @Test
    fun testExportCertSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.exportCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, File("/tmp/exported.cert")
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testExportCertWithRfc() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.exportCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, File("/tmp/exported.cert"), rfc = true
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testExportCertAliasNotFound() = runTest {
        setupMockFactory("", "keytool error: java.lang.Exception: Alias <nonexistent> does not exist", 1)
        val result = KeyToolAPI.exportCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, "nonexistent", File("/tmp/exported.cert")
        )

        assertTrue(result is KeytoolResult.Error)
    }

    // ===== 7. IMPORTCERT Function Tests =====

    @Test
    fun testImportCertSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.importCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, File("/tmp/cert.pem")
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testImportCertWithKeypass() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.importCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, File("/tmp/cert.pem"),
            keypass = "keypass123"
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testImportCertWithTrustCacerts() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.importCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, File("/tmp/cert.pem"),
            trustcacerts = true
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testImportCertFileNotFound() = runTest {
        setupMockFactory("", "keytool error: java.io.FileNotFoundException: /tmp/nonexistent.pem", 1)
        val result = KeyToolAPI.importCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, File("/tmp/nonexistent.pem")
        )

        assertTrue(result is KeytoolResult.Error)
    }

    // ===== 8. IMPORTKEYSTORE Function Tests =====

    @Test
    fun testImportKeystoreSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, TEST_ALIAS, "sourcekey",
            File("/tmp/dest.jks"), "destpass"
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testImportKeystoreWithoutAlias() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, null, null,
            File("/tmp/dest.jks"), "destpass"
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testImportKeystoreWithAllOptions() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, TEST_ALIAS, "sourcekey",
            File("/tmp/dest.jks"), "destpass",
            destAlias = "newalias", destKeypass = "newkeypass"
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testImportKeystoreSourceNotFound() = runTest {
        setupMockFactory("", "keytool error: java.io.FileNotFoundException: /tmp/source.jks", 1)
        val result = KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, null, null,
            File("/tmp/dest.jks"), "destpass"
        )

        assertTrue(result is KeytoolResult.Error)
    }

    // ===== 9. IMPORTPASS Function Tests =====

    @Test
    fun testImportPassSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.importPass(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "mypassword"
        )

        assertTrue(result is KeytoolResult.Success)
    }

    // ===== 10. DELETE Function Tests =====

    @Test
    fun testDeleteSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.delete(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS)

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testDeleteAliasNotFound() = runTest {
        setupMockFactory("", "keytool error: java.lang.Exception: Alias <nonexistent> does not exist", 1)
        val result = KeyToolAPI.delete(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, "nonexistent")

        assertTrue(result is KeytoolResult.Error)
    }

    // ===== 11. CHANGEALIAS Function Tests =====

    @Test
    fun testChangeAliasSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.changeAlias(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "newalias", "keypass"
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testChangeAliasWithoutKeypass() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.changeAlias(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "newalias", null
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testChangeAliasDuplicateTarget() = runTest {
        setupMockFactory("", "keytool error: java.lang.Exception: alias <newalias> already exists", 1)
        val result = KeyToolAPI.changeAlias(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "newalias", "keypass"
        )

        assertTrue(result is KeytoolResult.Error)
    }

    // ===== 12. KEYPASSWD Function Tests =====

    @Test
    fun testKeyPasswdSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.keyPasswd(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "oldpass", "newpass"
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testKeyPasswdWrongPassword() = runTest {
        setupMockFactory("", "keytool error: java.lang.UnrecoverableKeyException: Cannot recover key", 1)
        val result = KeyToolAPI.keyPasswd(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "wrongpass", "newpass"
        )

        assertTrue(result is KeytoolResult.Error)
    }

    // ===== 13. STOREPASSWD Function Tests =====

    @Test
    fun testStorePasswdSuccess() = runTest {
        setupMockFactory("", "", 0)
        val result = KeyToolAPI.storePasswd(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, "newstorepass"
        )

        assertTrue(result is KeytoolResult.Success)
    }

    @Test
    fun testStorePasswdWrongPassword() = runTest {
        setupMockFactory(
            "",
            "keytool error: java.lang.Exception: Keystore was tampered with, or password was incorrect",
            1
        )
        val result = KeyToolAPI.storePasswd(
            File(TEST_KEYSTORE_PATH), "wrongpass", "newpass"
        )

        assertTrue(result is KeytoolResult.Error)
    }

    @Test
    fun testGenKeyPairErrorInvalidKeySizeForAlgorithm() = runTest {
        setupMockFactory("", "keytool error: Invalid key size for EC: 129", 1)
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keyAlgorithm = KeyAlgorithm.EC, keySize = 129
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("not valid for"))
    }

    @Test
    fun testListErrorKeystoreCorrupted() = runTest {
        setupMockFactory("", "keytool error: java.security.UnrecoverableKeyException: failed to decrypt safe contents entry", 1)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD)

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("decrypt") || result.message.contains("Unrecoverable"))
    }

    @Test
    fun testImportCertErrorInvalidCertificateFormat() = runTest {
        setupMockFactory("", "keytool error: java.lang.Exception: Input not an X.509 certificate", 1)
        val result = KeyToolAPI.importCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, File("/tmp/invalid.crt")
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("not an X.509"))
    }

    @Test
    fun testImportCertErrorChainEstablishmentFailed() = runTest {
        setupMockFactory("", "keytool error: java.lang.Exception: Failed to establish chain from reply", 1)
        val result = KeyToolAPI.importCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, File("/tmp/cert-no-chain.crt")
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Failed to establish chain"))
    }

    @Test
    fun testImportCertErrorPublicKeyMismatch() = runTest {
        setupMockFactory("", "keytool error: java.lang.Exception: Public keys in reply and keystore don't match", 1)
        val result = KeyToolAPI.importCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, File("/tmp/wrong-key-cert.crt")
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Public keys") && result.message.contains("don't match"))
    }

    @Test
    fun testGenKeyPairErrorWeakAlgorithmDisabled() = runTest {
        setupMockFactory("", "keytool error: java.security.NoSuchAlgorithmException: MD5withRSA is disabled", 1)
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, signatureAlgorithm = SignatureAlgorithm.MD5_WITH_RSA
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("disabled"))
    }

    @Test
    fun testGenKeyPairEcCurveTakesPrecedenceOverKeySize() = runTest {
        setupMockFactory()
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keyAlgorithm = KeyAlgorithm.EC, keySize = 256, ecCurve = ECCurve.P256
        )

        assertTrue(result is KeytoolResult.Success)
        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-groupname"))
        assertTrue(cmd.contains("secp256r1"))
        assertFalse(cmd.contains("-keysize"))
    }

    @Test
    fun testListErrorUnrecognizedKeystoreFormat() = runTest {
        setupMockFactory("", "keytool error: java.io.IOException: Unrecognized keystore format", 1)
        val result = KeyToolAPI.list(File("/tmp/textfile.txt"), TEST_PASSWORD)

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Unrecognized"))
    }

    @Test
    fun testImportKeystoreErrorFormatMismatch() = runTest {
        setupMockFactory("", "keytool error: java.io.IOException: Keystore type mismatch", 1)
        val result = KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, TEST_ALIAS, "keypass",
            File("/tmp/dest.p12"), "destpass"
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("type") && result.message.contains("mismatch"))
    }

    @Test
    fun testImportPassErrorAliasTypeMismatch() = runTest {
        setupMockFactory("", "keytool error: java.lang.Exception: Alias <test> already exists", 1)
        val result = KeyToolAPI.importPass(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "mypassword"
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("already exists"))
    }

    // ===== 14. PRINTCERT Function Tests =====

    @Test
    fun testPrintCertSuccess() = runTest {
        setupMockFactory(validCertificateOutput, "", 0)
        val result = KeyToolAPI.printCert(File(TEST_CERT_PATH))

        assertTrue(result is KeytoolResult.Success)
        val certInfo = (result).data
        assertEquals("CN=Test Certificate, O=Test Organization, C=US", certInfo.owner)
        assertEquals("CN=Test Certificate, O=Test Organization, C=US", certInfo.issuer)
        assertEquals("abc123def456", certInfo.serialNumber)
        assertTrue(certInfo.fingerprints.containsKey("SHA-256"))
        assertTrue(certInfo.fingerprints.containsKey("SHA-1"))
        assertTrue(certInfo.fingerprints.containsKey("MD5"))
    }

    @Test
    fun testPrintCertInvalidFile() = runTest {
        setupMockFactory("", "keytool error: java.io.FileNotFoundException: /tmp/cert.pem", 1)
        val result = KeyToolAPI.printCert(File("/tmp/nonexistent.cert"))

        assertTrue(result is KeytoolResult.Error)
    }

    @Test
    fun testPrintCertParsingError() = runTest {
        setupMockFactory("invalid format", "", 0)
        val result = KeyToolAPI.printCert(File(TEST_CERT_PATH))

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Failed to parse"))
    }

    @Test
    fun testPrintCertParsingWithSpecialCharsInDN() = runTest {
        val specialDnOutput = """
            Owner: CN=Test\, Inc., O=Org\=Value, C=US
            Issuer: CN=CA\, Root, O=Authority, C=US
            Serial number: abc123
            Valid from: Mon Jan 22 10:00:00 BRT 2026 until: Tue Jan 22 10:00:00 BRT 2027
            Certificate Signature Strength: 2048-bit
            Signature algorithm name: SHA256withRSA
            SHA-1: AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12
            SHA-256: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD
        """.trimIndent()

        setupMockFactory(specialDnOutput, "", 0)
        val result = KeyToolAPI.printCert(File(TEST_CERT_PATH))

        assertTrue(result is KeytoolResult.Success)
        val certInfo = result.data
        assertTrue(certInfo.owner.contains("Test") && certInfo.owner.contains("Inc"))
    }

    @Test
    fun testPrintCertParsingMultipleFingerprints() = runTest {
        val multiHashOutput = """
            Owner: CN=Test
            Issuer: CN=Test
            Serial number: 123
            Valid from: Mon Jan 22 10:00:00 BRT 2026 until: Tue Jan 22 10:00:00 BRT 2027
            Certificate Signature Strength: 2048-bit
            Signature algorithm name: SHA256withRSA
            SHA-1: AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12
            SHA-256: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD
            SHA-512: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99
            MD5: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD
        """.trimIndent()

        setupMockFactory(multiHashOutput, "", 0)
        val result = KeyToolAPI.printCert(File(TEST_CERT_PATH))

        assertTrue(result is KeytoolResult.Success)
        val certInfo = result.data
        assertTrue(certInfo.fingerprints.containsKey("SHA-1"))
        assertTrue(certInfo.fingerprints.containsKey("SHA-256"))
        assertTrue(certInfo.fingerprints.containsKey("SHA-512"))
        assertTrue(certInfo.fingerprints.containsKey("MD5"))
    }

    @Test
    fun testPrintCertReqParsingWithoutExtensions() = runTest {
        val minimalCsrOutput = """
            Subject: CN=Minimal CSR, O=Test Org, C=US
            Signature algorithm: sha256WithRSAEncryption
            Public Key Algorithm: rsaEncryption
        """.trimIndent()

        setupMockFactory(minimalCsrOutput, "", 0)
        val result = KeyToolAPI.printCertReq(File("/tmp/req.csr"))

        assertTrue(result is KeytoolResult.Success)
        val csrInfo = result.data
        assertEquals("CN=Minimal CSR, O=Test Org, C=US", csrInfo.subject)
        assertTrue(csrInfo.extensions.isEmpty())
    }

    @Test
    fun testListParsingWithUnicodeInDN() = runTest {
        val unicodeOutput = """
            Keystore type: PKCS12
            Keystore provider: SUN

            Your keystore contains 1 entries

            Alias name: testkey
            Creation date: Jan 22, 2026
            Entry type: PrivateKeyEntry
            Certificate chain length: 1
            Certificate[1]:
            Owner: CN=测试用户, O=组织, C=CN
            Issuer: CN=测试用户, O=组织, C=CN
            Serial number: 123abc
            Valid from: Mon Jan 22 10:00:00 BRT 2026 until: Mon Jan 22 10:00:00 BRT 2027
            Certificate Signature Strength: 2048-bit
            Signature algorithm name: SHA256withRSA
            Subject Public Key Algorithm: 2048-bit RSA key
            Public Key SHA-256 Fingerprint: 12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD
        """.trimIndent()

        setupMockFactory(unicodeOutput, "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD)

        assertTrue(result is KeytoolResult.Success)
        val keystoreInfo = result.data
        assertEquals(1, keystoreInfo.entries.size)
        assertTrue(keystoreInfo.entries[0].owner?.contains("测试") ?: false)
    }

    // ===== 15. PRINTCERTREQ Function Tests =====

    @Test
    fun testPrintCertReqSuccess() = runTest {
        setupMockFactory(validCertReqOutput, "", 0)
        val result = KeyToolAPI.printCertReq(File("/tmp/req.csr"))

        assertTrue(result is KeytoolResult.Success)
        val csrInfo = (result).data
        assertEquals("CN=Test CSR, O=Test Org, C=US", csrInfo.subject)
        assertTrue(csrInfo.algorithm.contains("sha256"))
        assertTrue(csrInfo.extensions.isNotEmpty())
    }

    @Test
    fun testPrintCertReqInvalidFile() = runTest {
        setupMockFactory("", "keytool error: java.io.FileNotFoundException: /tmp/req.csr", 1)
        val result = KeyToolAPI.printCertReq(File("/tmp/nonexistent.csr"))

        assertTrue(result is KeytoolResult.Error)
    }

    @Test
    fun testPrintCertReqParsingError() = runTest {
        setupMockFactory("invalid format", "", 0)
        val result = KeyToolAPI.printCertReq(File("/tmp/req.csr"))

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Failed to parse"))
    }

    // ===== 16. PRINTCRL Function Tests =====

    @Test
    fun testPrintCrlSuccess() = runTest {
        setupMockFactory(validCrlOutput, "", 0)
        val result = KeyToolAPI.printCrl(File("/tmp/crl.pem"))

        assertTrue(result is KeytoolResult.Success)
        val crlInfo = (result).data
        assertEquals("CN=Test CA, O=Test Organization, C=US", crlInfo.issuer)
        assertTrue(crlInfo.thisUpdate.contains("Jan"))
        assertTrue(crlInfo.nextUpdate?.contains("Feb") ?: false)
        assertEquals(2, crlInfo.revokedCertificates.size)
    }

    @Test
    fun testPrintCrlEmptyRevocation() = runTest {
        val crlWithoutRevoked = """
            Certificate Revocation List (CRL)
            Issuer: CN=Test CA, O=Test Organization, C=US
            This Update: Jan 22 10:00:00 2026 GMT
            Next Update: Feb 22 10:00:00 2026 GMT
        """.trimIndent()
        setupMockFactory(crlWithoutRevoked, "", 0)
        val result = KeyToolAPI.printCrl(File("/tmp/crl.pem"))

        assertTrue(result is KeytoolResult.Success)
        val crlInfo = (result).data
        assertEquals(0, crlInfo.revokedCertificates.size)
    }

    @Test
    fun testPrintCrlInvalidFile() = runTest {
        setupMockFactory("", "keytool error: java.io.FileNotFoundException: /tmp/crl.pem", 1)
        val result = KeyToolAPI.printCrl(File("/tmp/nonexistent.pem"))

        assertTrue(result is KeytoolResult.Error)
    }

    @Test
    fun testPrintCrlParsingError() = runTest {
        setupMockFactory("invalid format", "", 0)
        val result = KeyToolAPI.printCrl(File("/tmp/crl.pem"))

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Failed to parse"))
    }

    // ===== genKeyPair CLI arg generation tests =====

    @Test
    fun testGenKeyPairWithEcCurveAddsGroupname() = runTest {
        setupMockFactory()
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keyAlgorithm = KeyAlgorithm.EC, ecCurve = ECCurve.P256
        )

        assertTrue(result is KeytoolResult.Success)
        val cmd = capturedCommand!!
        val groupIdx = cmd.indexOf("-groupname")
        assertTrue(groupIdx >= 0)
        assertEquals("secp256r1", cmd[groupIdx + 1])
    }

    @Test
    fun testGenKeyPairWithEcCurveDoesNotAddKeysize() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keyAlgorithm = KeyAlgorithm.EC, ecCurve = ECCurve.P384
        )

        val cmd = capturedCommand!!
        assertFalse(cmd.contains("-keysize"))
    }

    @Test
    fun testGenKeyPairEdDSANoKeysize() = runTest {
        setupMockFactory()
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keyAlgorithm = KeyAlgorithm.ED25519
        )

        assertTrue(result is KeytoolResult.Success)
        val cmd = capturedCommand!!
        assertFalse(cmd.contains("-keysize"))
        assertTrue(cmd.contains("Ed25519"))
    }

    @Test
    fun testGenKeyPairMlDsaNoKeysize() = runTest {
        setupMockFactory()
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keyAlgorithm = KeyAlgorithm.ML_DSA_44
        )

        assertTrue(result is KeytoolResult.Success)
        val cmd = capturedCommand!!
        assertFalse(cmd.contains("-keysize"))
    }

    @Test
    fun testGenKeyPairRsaAddsKeysize() = runTest {
        setupMockFactory()
        val result = KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keyAlgorithm = KeyAlgorithm.RSA, keySize = 4096
        )

        assertTrue(result is KeytoolResult.Success)
        val cmd = capturedCommand!!
        val keysizeIdx = cmd.indexOf("-keysize")
        assertTrue(keysizeIdx >= 0)
        assertEquals("4096", cmd[keysizeIdx + 1])
        assertFalse(cmd.contains("-groupname"))
    }

    @Test
    fun testGenKeyPairEcCurveP521DefaultsSha512() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, keyAlgorithm = KeyAlgorithm.EC, ecCurve = ECCurve.P521
        )

        val cmd = capturedCommand!!
        val sigalgIdx = cmd.indexOf("-sigalg")
        assertTrue(sigalgIdx >= 0)
        assertEquals("SHA512withECDSA", cmd[sigalgIdx + 1])
    }

    @Test
    fun testListWithAlias() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, alias = "mykey")

        val cmd = capturedCommand!!
        val aliasIdx = cmd.indexOf("-alias")
        assertTrue(aliasIdx >= 0, "-alias flag should be present")
        assertEquals("mykey", cmd[aliasIdx + 1])
    }

    @Test
    fun testListWithStoretype() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, storetype = "JKS")

        val cmd = capturedCommand!!
        val storetypeIdx = cmd.indexOf("-storetype")
        assertTrue(storetypeIdx >= 0, "-storetype flag should be present")
        assertEquals("JKS", cmd[storetypeIdx + 1])
    }

    @Test
    fun testListWithAliasAndStoretype() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, alias = "mykey", storetype = "PKCS12")

        val cmd = capturedCommand!!
        val aliasIdx = cmd.indexOf("-alias")
        assertTrue(aliasIdx >= 0, "-alias flag should be present")
        assertEquals("mykey", cmd[aliasIdx + 1])

        val storetypeIdx = cmd.indexOf("-storetype")
        assertTrue(storetypeIdx >= 0, "-storetype flag should be present")
        assertEquals("PKCS12", cmd[storetypeIdx + 1])
    }

    @Test
    fun testListWithoutAliasOrStoretype() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD)

        val cmd = capturedCommand!!
        assertFalse(cmd.contains("-alias"), "-alias flag should not be present by default")
        assertFalse(cmd.contains("-storetype"), "-storetype flag should not be present by default")
    }

    // ===== NEW PARAMETER TESTS =====

    // --- list() new params ---

    @Test
    fun testListWithRfcFlag() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, rfc = true)

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-rfc"), "-rfc flag should be present")
        assertFalse(cmd.contains("-v"), "-v flag should not be present when rfc=true")
    }

    @Test
    fun testListWithRfcOverridesVerbose() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, verbose = true, rfc = true)

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-rfc"), "-rfc should be present")
        assertFalse(cmd.contains("-v"), "-v should not be present when rfc=true even with verbose=true")
    }

    @Test
    fun testListWithCacerts() = runTest {
        setupMockFactory(validKeystoreListOutput, "", 0)
        KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD, cacerts = true)

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-cacerts"), "-cacerts flag should be present")
        assertFalse(cmd.contains("-keystore"), "-keystore should not be present when cacerts=true")
    }

    // --- genKeyPair() new params ---

    @Test
    fun testGenKeyPairWithSingleExt() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, ext = listOf("san=dns:example.com")
        )

        val cmd = capturedCommand!!
        val extIdx = cmd.indexOf("-ext")
        assertTrue(extIdx >= 0, "-ext flag should be present")
        assertEquals("san=dns:example.com", cmd[extIdx + 1])
    }

    @Test
    fun testGenKeyPairWithMultipleExt() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365,
            ext = listOf("san=dns:a.com", "bc=ca:true")
        )

        val cmd = capturedCommand!!
        val extIndices = cmd.mapIndexedNotNull { i, v -> if (v == "-ext") i else null }
        assertEquals(2, extIndices.size, "Should have two -ext flags")
        assertEquals("san=dns:a.com", cmd[extIndices[0] + 1])
        assertEquals("bc=ca:true", cmd[extIndices[1] + 1])
    }

    @Test
    fun testGenKeyPairWithStartdate() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, startdate = "2025/06/01"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-startdate")
        assertTrue(idx >= 0, "-startdate flag should be present")
        assertEquals("2025/06/01", cmd[idx + 1])
    }

    @Test
    fun testGenKeyPairWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, storetype = "JKS"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0, "-storetype flag should be present")
        assertEquals("JKS", cmd[idx + 1])
    }

    @Test
    fun testGenKeyPairWithSigner() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, signer = "myca"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-signer")
        assertTrue(idx >= 0, "-signer flag should be present")
        assertEquals("myca", cmd[idx + 1])
    }

    @Test
    fun testGenKeyPairWithSignerKeypass() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365, signerKeypass = "capass"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-signerkeypass")
        assertTrue(idx >= 0, "-signerkeypass flag should be present")
        assertEquals("capass", cmd[idx + 1])
    }

    @Test
    fun testGenKeyPairWithAllNewParams() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365,
            ext = listOf("san=dns:example.com"),
            startdate = "2025/06/01",
            storetype = "JKS",
            signer = "myca",
            signerKeypass = "capass"
        )

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-ext"))
        assertTrue(cmd.contains("-startdate"))
        assertTrue(cmd.contains("-storetype"))
        assertTrue(cmd.contains("-signer"))
        assertTrue(cmd.contains("-signerkeypass"))
    }

    // --- genSecKey() new params ---

    @Test
    fun testGenSecKeyWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.genSecKey(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            KeyAlgorithm.AES, 256, storetype = "PKCS12"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0, "-storetype flag should be present")
        assertEquals("PKCS12", cmd[idx + 1])
    }

    // --- genCert() new params ---

    @Test
    fun testGenCertWithExt() = runTest {
        setupMockFactory()
        KeyToolAPI.genCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), File("/tmp/cert.pem"),
            ext = listOf("ku=digitalSignature")
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-ext")
        assertTrue(idx >= 0)
        assertEquals("ku=digitalSignature", cmd[idx + 1])
    }

    @Test
    fun testGenCertWithRfc() = runTest {
        setupMockFactory()
        KeyToolAPI.genCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), File("/tmp/cert.pem"), rfc = true
        )

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-rfc"))
    }

    @Test
    fun testGenCertWithDname() = runTest {
        setupMockFactory()
        KeyToolAPI.genCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), File("/tmp/cert.pem"), dname = "CN=Override"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-dname")
        assertTrue(idx >= 0)
        assertEquals("CN=Override", cmd[idx + 1])
    }

    @Test
    fun testGenCertWithStartdate() = runTest {
        setupMockFactory()
        KeyToolAPI.genCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), File("/tmp/cert.pem"), startdate = "+30d"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-startdate")
        assertTrue(idx >= 0)
        assertEquals("+30d", cmd[idx + 1])
    }

    @Test
    fun testGenCertWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.genCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), File("/tmp/cert.pem"), storetype = "JKS"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0)
        assertEquals("JKS", cmd[idx + 1])
    }

    // --- certReq() new params ---

    @Test
    fun testCertReqWithExt() = runTest {
        setupMockFactory()
        KeyToolAPI.certReq(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), ext = listOf("san=dns:a.com")
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-ext")
        assertTrue(idx >= 0)
        assertEquals("san=dns:a.com", cmd[idx + 1])
    }

    @Test
    fun testCertReqWithDname() = runTest {
        setupMockFactory()
        KeyToolAPI.certReq(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), dname = "CN=Override"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-dname")
        assertTrue(idx >= 0)
        assertEquals("CN=Override", cmd[idx + 1])
    }

    @Test
    fun testCertReqWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.certReq(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            File("/tmp/req.csr"), storetype = "PKCS12"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0)
        assertEquals("PKCS12", cmd[idx + 1])
    }

    // --- exportCert() new params ---

    @Test
    fun testExportCertWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.exportCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS,
            File("/tmp/cert.pem"), storetype = "JKS"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0)
        assertEquals("JKS", cmd[idx + 1])
    }

    @Test
    fun testExportCertWithCacerts() = runTest {
        setupMockFactory()
        KeyToolAPI.exportCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS,
            File("/tmp/cert.pem"), cacerts = true
        )

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-cacerts"))
        assertFalse(cmd.contains("-keystore"))
    }

    // --- importCert() new params ---

    @Test
    fun testImportCertWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.importCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS,
            File("/tmp/cert.pem"), storetype = "JKS"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0)
        assertEquals("JKS", cmd[idx + 1])
    }

    @Test
    fun testImportCertWithCacerts() = runTest {
        setupMockFactory()
        KeyToolAPI.importCert(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS,
            File("/tmp/cert.pem"), cacerts = true
        )

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-cacerts"))
        assertFalse(cmd.contains("-keystore"))
    }

    // --- importKeystore() new params ---

    @Test
    fun testImportKeystoreWithSrcStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, null, null,
            File("/tmp/dest.jks"), "destpass",
            srcStoretype = "JKS"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-srcstoretype")
        assertTrue(idx >= 0)
        assertEquals("JKS", cmd[idx + 1])
    }

    @Test
    fun testImportKeystoreWithDestStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, null, null,
            File("/tmp/dest.jks"), "destpass",
            destStoretype = "PKCS12"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-deststoretype")
        assertTrue(idx >= 0)
        assertEquals("PKCS12", cmd[idx + 1])
    }

    @Test
    fun testImportKeystoreWithSrcProvidername() = runTest {
        setupMockFactory()
        KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, null, null,
            File("/tmp/dest.jks"), "destpass",
            srcProvidername = "SUN"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-srcprovidername")
        assertTrue(idx >= 0)
        assertEquals("SUN", cmd[idx + 1])
    }

    @Test
    fun testImportKeystoreWithDestProvidername() = runTest {
        setupMockFactory()
        KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, null, null,
            File("/tmp/dest.jks"), "destpass",
            destProvidername = "SunJSSE"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-destprovidername")
        assertTrue(idx >= 0)
        assertEquals("SunJSSE", cmd[idx + 1])
    }

    @Test
    fun testImportKeystoreWithAllNewParams() = runTest {
        setupMockFactory()
        KeyToolAPI.importKeystore(
            File("/tmp/source.jks"), TEST_PASSWORD, null, null,
            File("/tmp/dest.jks"), "destpass",
            srcStoretype = "JKS",
            destStoretype = "PKCS12",
            srcProvidername = "SUN",
            destProvidername = "SunJSSE"
        )

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-srcstoretype"))
        assertTrue(cmd.contains("-deststoretype"))
        assertTrue(cmd.contains("-srcprovidername"))
        assertTrue(cmd.contains("-destprovidername"))
    }

    // --- importPass() new params ---

    @Test
    fun testImportPassWithKeyAlgorithm() = runTest {
        setupMockFactory()
        KeyToolAPI.importPass(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "mypassword",
            keyAlgorithm = KeyAlgorithm.AES
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-keyalg")
        assertTrue(idx >= 0)
        assertEquals("AES", cmd[idx + 1])
    }

    @Test
    fun testImportPassWithKeySize() = runTest {
        setupMockFactory()
        KeyToolAPI.importPass(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "mypassword",
            keySize = 256
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-keysize")
        assertTrue(idx >= 0)
        assertEquals("256", cmd[idx + 1])
    }

    @Test
    fun testImportPassWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.importPass(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "mypassword",
            storetype = "PKCS12"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0)
        assertEquals("PKCS12", cmd[idx + 1])
    }

    // --- delete() new params ---

    @Test
    fun testDeleteWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.delete(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS,
            storetype = "JKS"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0)
        assertEquals("JKS", cmd[idx + 1])
    }

    @Test
    fun testDeleteWithCacerts() = runTest {
        setupMockFactory()
        KeyToolAPI.delete(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS,
            cacerts = true
        )

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-cacerts"))
        assertFalse(cmd.contains("-keystore"))
    }

    // --- changeAlias() new params ---

    @Test
    fun testChangeAliasWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.changeAlias(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "newalias", "keypass",
            storetype = "JKS"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0)
        assertEquals("JKS", cmd[idx + 1])
    }

    @Test
    fun testChangeAliasWithCacerts() = runTest {
        setupMockFactory()
        KeyToolAPI.changeAlias(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "newalias", "keypass",
            cacerts = true
        )

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-cacerts"))
        assertFalse(cmd.contains("-keystore"))
    }

    // --- keyPasswd() new params ---

    @Test
    fun testKeyPasswdWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.keyPasswd(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "oldpass", "newpass",
            storetype = "PKCS12"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0)
        assertEquals("PKCS12", cmd[idx + 1])
    }

    // --- storePasswd() new params ---

    @Test
    fun testStorePasswdWithStoretype() = runTest {
        setupMockFactory()
        KeyToolAPI.storePasswd(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, "newpass",
            storetype = "JKS"
        )

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-storetype")
        assertTrue(idx >= 0)
        assertEquals("JKS", cmd[idx + 1])
    }

    @Test
    fun testStorePasswdWithCacerts() = runTest {
        setupMockFactory()
        KeyToolAPI.storePasswd(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, "newpass",
            cacerts = true
        )

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-cacerts"))
        assertFalse(cmd.contains("-keystore"))
    }

    // --- printCert() new params ---

    @Test
    fun testPrintCertWithRfc() = runTest {
        setupMockFactory(validCertificateOutput)
        KeyToolAPI.printCert(file = File(TEST_CERT_PATH), rfc = true)

        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-rfc"))
        assertFalse(cmd.contains("-v"))
    }

    @Test
    fun testPrintCertWithSslserver() = runTest {
        setupMockFactory(validCertificateOutput)
        KeyToolAPI.printCert(sslserver = "example.com")

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-sslserver")
        assertTrue(idx >= 0)
        assertEquals("example.com", cmd[idx + 1])
        assertFalse(cmd.contains("-file"))
    }

    @Test
    fun testPrintCertWithJarfile() = runTest {
        setupMockFactory(validCertificateOutput)
        KeyToolAPI.printCert(jarfile = File("/tmp/app.jar"))

        val cmd = capturedCommand!!
        val idx = cmd.indexOf("-jarfile")
        assertTrue(idx >= 0)
        assertEquals("/tmp/app.jar", cmd[idx + 1])
        assertFalse(cmd.contains("-file"))
    }

    @Test
    fun testPrintCertMutualExclusionError() = runTest {
        setupMockFactory(validCertificateOutput)
        val result = KeyToolAPI.printCert(
            file = File(TEST_CERT_PATH),
            sslserver = "example.com"
        )

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Exactly one"))
    }

    @Test
    fun testPrintCertNoSourceError() = runTest {
        setupMockFactory(validCertificateOutput)
        val result = KeyToolAPI.printCert()

        assertTrue(result is KeytoolResult.Error)
        assertTrue(result.message.contains("Exactly one"))
    }

    // --- showInfoTls() ---

    @Test
    fun testShowInfoTlsSuccess() = runTest {
        val tlsOutput = """
            Enabled Protocols
            -----------------
            TLSv1.3
            TLSv1.2

            Enabled Cipher Suites
            ---------------------
            TLS_AES_256_GCM_SHA384
            TLS_AES_128_GCM_SHA256
        """.trimIndent()

        setupMockFactory(tlsOutput)
        val result = KeyToolAPI.showInfoTls()

        assertTrue(result is KeytoolResult.Success)
        val cmd = capturedCommand!!
        assertTrue(cmd.contains("-showinfo"))
        assertTrue(cmd.contains("-tls"))
        assertTrue(result.data.enabledProtocols.contains("TLSv1.3"))
        assertTrue(result.data.enabledCipherSuites.contains("TLS_AES_256_GCM_SHA384"))
    }

    // --- version() ---

    @Test
    fun testVersionReturnsNonEmpty() {
        val version = KeyToolAPI.version()
        assertTrue(version.isNotEmpty())
        assertNotEquals("unknown", version)
    }

    @Test
    fun testGenKeyPairExplicitSigAlgPassedThrough() = runTest {
        setupMockFactory()
        KeyToolAPI.genKeyPair(
            File(TEST_KEYSTORE_PATH), TEST_PASSWORD, TEST_ALIAS, "keypass123",
            "CN=Test, O=TestOrg, C=US", 365,
            keyAlgorithm = KeyAlgorithm.RSA, signatureAlgorithm = SignatureAlgorithm.SHA512_WITH_RSA
        )

        val cmd = capturedCommand!!
        val sigalgIdx = cmd.indexOf("-sigalg")
        assertTrue(sigalgIdx >= 0)
        assertEquals("SHA512withRSA", cmd[sigalgIdx + 1])
    }
}
