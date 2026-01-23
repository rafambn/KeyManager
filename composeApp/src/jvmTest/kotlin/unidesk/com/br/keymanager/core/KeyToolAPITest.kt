package unidesk.com.br.keymanager.core

import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.*
import kotlinx.coroutines.test.runTest

class KeyToolAPITest {

    companion object {
        private const val TEST_KEYSTORE_PATH = "/tmp/test.jks"
        private const val TEST_PASSWORD = "testpass123"
        private const val TEST_ALIAS = "mykey"
        private const val TEST_CERT_PATH = "/tmp/cert.pem"
    }

    private val originalFactory = KeyToolAPI.processFactory

    @BeforeTest
    fun setUp() {
        // Reset to original factory before each test
        KeyToolAPI.processFactory = originalFactory
    }

    @AfterTest
    fun tearDown() {
        // Restore original factory after each test
        KeyToolAPI.processFactory = originalFactory
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

    private fun setupMockFactory(stdout: String, stderr: String = "", exitCode: Int = 0) {
        KeyToolAPI.processFactory = { _: List<String> ->
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
        val error = result
        assertEquals(1, error.exitCode)
        assertTrue(error.message.contains("password was incorrect"))
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
        val error = result
        assertTrue(error.message.contains("No such file or directory"))
    }

    @Test
    fun testListParsingError() = runTest {
        setupMockFactory("invalid output that doesn't match expected format", "", 0)
        val result = KeyToolAPI.list(File(TEST_KEYSTORE_PATH), TEST_PASSWORD)

        assertTrue(result is KeytoolResult.Error)
        val error = result
        assertTrue(error.message.contains("Failed to parse"))
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
        val error = result
        assertTrue(error.message.contains("Failed to parse"))
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
        val error = result
        assertTrue(error.message.contains("Failed to parse"))
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
        val error = result
        assertTrue(error.message.contains("Failed to parse"))
    }
}
