package unidesk.com.br.keymanager.core

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.security.SecureRandom
import java.util.Date

object CertificateGenerator {

    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        return kpg.generateKeyPair()
    }

    fun generateSelfSignedCertificate(
        keyPair: KeyPair,
        dn: String,
        validityDays: Int
    ): X509Certificate {
        val now = System.currentTimeMillis()
        val startDate = Date(now)
        val endDate = Date(now + validityDays * 24 * 60 * 60 * 1000L)

        val serialNumber = BigInteger(64, SecureRandom())
        val issuerName = X500Name(dn)
        val subjectName = X500Name(dn)

        val certBuilder = JcaX509v3CertificateBuilder(
            issuerName,
            serialNumber,
            startDate,
            endDate,
            subjectName,
            keyPair.public
        )

        val contentSigner = JcaContentSignerBuilder("SHA256WithRSA")
            .build(keyPair.private)

        val certHolder = certBuilder.build(contentSigner)

        return org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
            .getCertificate(certHolder)
    }
}
