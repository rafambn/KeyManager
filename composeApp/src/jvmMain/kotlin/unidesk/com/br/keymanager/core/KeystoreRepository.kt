package unidesk.com.br.keymanager.core

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.cert.Certificate

class KeystoreRepository {

    private var keyStore: KeyStore? = null
    private var currentFile: File? = null
    private var currentPassword: CharArray? = null

    fun loadKeystore(file: File, password: String) {
        val ks = KeyStore.getInstance("PKCS12") // Works for .p12 and usually .jks if standard
        // Try PKCS12 first, if it fails maybe fallback or assume user knows what they are doing.
        // Spec says: KeyStore.getInstance("PKCS12") // Works for .jks and .p12
        
        val inputStream = if (file.exists()) FileInputStream(file) else null
        ks.load(inputStream, password.toCharArray())
        inputStream?.close()

        keyStore = ks
        currentFile = file
        currentPassword = password.toCharArray()
    }

    fun getKeys(): List<KeyInfo> {
        val ks = keyStore ?: return emptyList()
        return ks.aliases().toList().map { alias ->
            val isKey = ks.isKeyEntry(alias)
            val isCert = ks.isCertificateEntry(alias)
            val type = when {
                isKey -> "type_key"
                isCert -> "type_certificate"
                else -> "type_unknown"
            }

            val cert = ks.getCertificate(alias)
            val algorithm = cert?.publicKey?.algorithm ?: "Unknown"
            val details = (cert as? java.security.cert.X509Certificate)?.subjectDN?.name ?: ""

            KeyInfo(alias, algorithm, type, details)
        }
    }

    fun getAliases(): List<String> {
        val ks = keyStore ?: return emptyList()
        return ks.aliases().toList()
    }

    fun deleteAlias(alias: String) {
        val ks = keyStore ?: return
        ks.deleteEntry(alias)
        saveKeystore()
    }

    fun moveAlias(alias: String, targetFile: File, targetPassword: String) {
        val sourceKs = keyStore ?: throw IllegalStateException("Source keystore not loaded")
        val sourcePassword = currentPassword ?: throw IllegalStateException("Source password not set")

        if (!sourceKs.containsAlias(alias)) throw IllegalArgumentException("Alias not found")

        // Load Target
        val targetKs = KeyStore.getInstance("PKCS12")
        if (targetFile.exists()) {
            FileInputStream(targetFile).use { fis ->
                targetKs.load(fis, targetPassword.toCharArray())
            }
        } else {
            targetKs.load(null, targetPassword.toCharArray())
        }

        // Get Entry from Source
        val prot = KeyStore.PasswordProtection(sourcePassword)
        val entry = sourceKs.getEntry(alias, prot)

        // Save to Target
        targetKs.setEntry(alias, entry, KeyStore.PasswordProtection(targetPassword.toCharArray()))
        FileOutputStream(targetFile).use { fos ->
            targetKs.store(fos, targetPassword.toCharArray())
        }

        // Delete from Source
        sourceKs.deleteEntry(alias)
        saveKeystore()
    }

    fun renameAlias(oldAlias: String, newAlias: String) {
        val ks = keyStore ?: return
        val password = currentPassword ?: return

        if (!ks.containsAlias(oldAlias)) return
        if (ks.containsAlias(newAlias)) throw IllegalArgumentException("Alias '$newAlias' already exists.")

        val protection = KeyStore.PasswordProtection(password)
        val entry = ks.getEntry(oldAlias, protection)
        
        ks.setEntry(newAlias, entry, protection)
        ks.deleteEntry(oldAlias)
        saveKeystore()
    }

    fun addCertificate(alias: String, dn: String, validityDays: Int) {
        val ks = keyStore ?: return
        val password = currentPassword ?: return

        if (ks.containsAlias(alias)) throw IllegalArgumentException("Alias '$alias' already exists.")

        val keyPair = CertificateGenerator.generateKeyPair()
        val cert = CertificateGenerator.generateSelfSignedCertificate(keyPair, dn, validityDays)

        val chain = arrayOf<Certificate>(cert)
        ks.setKeyEntry(alias, keyPair.private, password, chain)
        saveKeystore()
    }

    private fun saveKeystore() {
        val ks = keyStore ?: return
        val file = currentFile ?: return
        val password = currentPassword ?: return

        FileOutputStream(file).use { os ->
            ks.store(os, password)
        }
    }
    
    fun isLoaded(): Boolean = keyStore != null
}
