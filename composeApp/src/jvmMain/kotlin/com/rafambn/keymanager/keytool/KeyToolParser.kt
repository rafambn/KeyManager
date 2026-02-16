package com.rafambn.keymanager.keytool

import com.rafambn.keymanager.keytool.enums.EntryType
import com.rafambn.keymanager.keytool.model.CertRequestInfo
import com.rafambn.keymanager.keytool.model.CertificateInfo
import com.rafambn.keymanager.keytool.model.CrlInfo
import com.rafambn.keymanager.keytool.model.KeystoreEntry
import com.rafambn.keymanager.keytool.model.KeystoreInfo

internal object KeyToolParser {
    internal fun parseListOutput(output: String, verbose: Boolean = true): KeystoreInfo {
        val lines = output.lines()

        var type = "Unknown"
        var provider = "Unknown"
        var entryCount = 0
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Keystore type:") -> {
                    type = trimmed.substringAfter("Keystore type:").trim()
                }

                trimmed.startsWith("Keystore provider:") -> {
                    provider = trimmed.substringAfter("Keystore provider:").trim()
                }

                trimmed.contains("keystore contains") && trimmed.contains("entr") -> {
                    val match = Regex("(\\d+)\\s+entr").find(trimmed)
                    entryCount = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                }
            }
        }

        val entryBlocks = output.split(Regex("(?=Alias name:)")).filter { it.contains("Alias name:") }
        val entries = entryBlocks.map { parseKeystoreEntry(it, verbose) }

        if (entries.size != entryCount && entryCount > 0) {
            System.err.println("Warning: Expected $entryCount entries but parsed ${entries.size}")
        }

        if ((type.isEmpty() || type == "Unknown") && entries.isEmpty()) {
            throw IllegalArgumentException("Could not determine keystore type from output")
        }

        if (entryCount == 0 && entries.isNotEmpty()) {
            entryCount = entries.size
        }

        entries.forEach { entry ->
            if (entry.alias.isBlank()) {
                throw IllegalArgumentException("Parsed entry with blank alias")
            }
            if (entry.entryType == EntryType.UNKNOWN) {
                throw IllegalArgumentException("Could not determine entry type for alias '${entry.alias}'")
            }
        }
        return KeystoreInfo(type, provider, entryCount, entries)
    }

    internal fun parseKeystoreEntry(block: String, verbose: Boolean = true): KeystoreEntry {
        val lines = block.lines()
        var alias = ""
        var creationDate = ""
        var entryType = EntryType.UNKNOWN
        var chainLength: Int? = null
        var owner: String? = null
        var issuer: String? = null
        var keyAlgorithm: String? = null
        var serialNumber: String? = null
        var validFrom: String? = null
        var validUntil: String? = null
        var fingerprint: String? = null
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Alias name:") -> {
                    alias = trimmed.substringAfter("Alias name:").trim()
                }

                trimmed.startsWith("Creation date:") -> {
                    creationDate = trimmed.substringAfter("Creation date:").trim()
                }

                trimmed.startsWith("Entry type:") -> {
                    val typeStr = trimmed.substringAfter("Entry type:").trim()
                    entryType = when {
                        typeStr.contains("PrivateKey", ignoreCase = true) -> EntryType.PRIVATE_KEY
                        typeStr.contains("trustedCert", ignoreCase = true) -> EntryType.TRUSTED_CERT
                        typeStr.contains("SecretKey", ignoreCase = true) -> EntryType.SECRET_KEY
                        else -> EntryType.UNKNOWN
                    }
                }

                trimmed.startsWith("Certificate chain length:") -> {
                    chainLength = trimmed.substringAfter("Certificate chain length:").trim().toIntOrNull()
                }

                verbose && trimmed.startsWith("Owner:") -> {
                    owner = trimmed.substringAfter("Owner:").trim()
                }

                verbose && trimmed.startsWith("Issuer:") -> {
                    issuer = trimmed.substringAfter("Issuer:").trim()
                }

                verbose && trimmed.startsWith("Serial number:") -> {
                    serialNumber = trimmed.substringAfter("Serial number:").trim()
                }

                verbose && trimmed.startsWith("Valid from:") -> {

                    val parts = trimmed.substringAfter("Valid from:").split("until:")
                    validFrom = parts.getOrNull(0)?.trim()
                    validUntil = parts.getOrNull(1)?.trim()
                }

                verbose && trimmed.contains("Fingerprint:") -> {
                    fingerprint = trimmed.substringAfter(":").trim()
                }

                verbose && trimmed.startsWith("Subject Public Key Algorithm:") -> {
                    val algStr = trimmed.substringAfter("Subject Public Key Algorithm:").trim()

                    keyAlgorithm = when {
                        algStr.contains("RSA", ignoreCase = true) -> "RSA"
                        algStr.contains("EC", ignoreCase = true) -> "EC"
                        algStr.contains("DSA", ignoreCase = true) -> "DSA"
                        else -> algStr.substringBefore(" ").trim().ifBlank { null }
                    }
                }
            }
        }
        return KeystoreEntry(
            alias = alias,
            creationDate = creationDate,
            entryType = entryType,
            certificateChainLength = chainLength,
            owner = owner,
            issuer = issuer,
            algorithm = keyAlgorithm,
            serialNumber = serialNumber,
            validFrom = validFrom,
            validUntil = validUntil,
            fingerprint = fingerprint
        )
    }

    internal fun parseCertificateOutput(output: String): CertificateInfo {
        val lines = output.lines()
        var owner = ""
        var issuer = ""
        var serialNumber = ""
        var validFrom = ""
        var validUntil = ""
        var algorithm = ""
        val fingerprints = mutableMapOf<String, String>()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Owner:") -> {
                    owner = trimmed.substringAfter("Owner:").trim()
                }

                trimmed.startsWith("Issuer:") -> {
                    issuer = trimmed.substringAfter("Issuer:").trim()
                }

                trimmed.startsWith("Serial number:") -> {
                    serialNumber = trimmed.substringAfter("Serial number:").trim()
                }

                trimmed.startsWith("Valid from:") -> {
                    val parts = trimmed.substringAfter("Valid from:").split("until:")
                    validFrom = parts.getOrNull(0)?.trim() ?: ""
                    validUntil = parts.getOrNull(1)?.trim() ?: ""
                }

                trimmed.startsWith("Signature algorithm name:") -> {
                    algorithm = trimmed.substringAfter("Signature algorithm name:").trim()
                }

                trimmed.contains("SHA256:") || trimmed.contains("SHA-256:") -> {
                    fingerprints["SHA-256"] = trimmed.substringAfter(":").trim()
                }

                trimmed.contains("SHA512:") || trimmed.contains("SHA-512:") -> {
                    fingerprints["SHA-512"] = trimmed.substringAfter(":").trim()
                }

                trimmed.contains("SHA1:") || trimmed.contains("SHA-1:") -> {
                    fingerprints["SHA-1"] = trimmed.substringAfter(":").trim()
                }

                trimmed.contains("MD5:") -> {
                    fingerprints["MD5"] = trimmed.substringAfter(":").trim()
                }
            }
        }

        if (owner.isEmpty() || issuer.isEmpty() || serialNumber.isEmpty()) {
            throw IllegalArgumentException("Missing critical certificate fields: owner=$owner, issuer=$issuer, serialNumber=$serialNumber")
        }
        return CertificateInfo(owner, issuer, serialNumber, validFrom, validUntil, algorithm, fingerprints)
    }

    internal fun parseCertReqOutput(output: String): CertRequestInfo {
        val lines = output.lines()
        var subject = ""
        var algorithm = ""
        val extensions = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Subject:") -> {
                    subject = trimmed.substringAfter("Subject:").trim()
                }

                trimmed.startsWith("Signature algorithm:") || trimmed.startsWith("Signature Algorithm:") -> {
                    algorithm = trimmed.substringAfter(":").trim()
                }

                trimmed.startsWith("Extension:") || trimmed.contains("OID:") -> {
                    extensions.add(trimmed)
                }
            }
        }

        if (subject.isEmpty() || algorithm.isEmpty()) {
            throw IllegalArgumentException("Missing critical certificate request fields: subject=$subject, algorithm=$algorithm")
        }
        return CertRequestInfo(subject, algorithm, extensions)
    }

    internal fun parseCrlOutput(output: String): CrlInfo {
        val lines = output.lines()
        var issuer = ""
        var thisUpdate = ""
        var nextUpdate: String? = null
        val revokedCerts = mutableListOf<String>()
        var inRevokedSection = false
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Issuer:") -> {
                    issuer = trimmed.substringAfter("Issuer:").trim()
                }

                trimmed.startsWith("This Update:") || trimmed.startsWith("ThisUpdate:") -> {
                    thisUpdate = trimmed.substringAfter(":").trim()
                }

                trimmed.startsWith("Next Update:") || trimmed.startsWith("NextUpdate:") -> {
                    nextUpdate = trimmed.substringAfter(":").trim()
                }

                trimmed.contains("Revoked Certificates:") -> {
                    inRevokedSection = true
                }

                inRevokedSection && trimmed.startsWith("Serial Number:") -> {
                    revokedCerts.add(trimmed.substringAfter("Serial Number:").trim())
                }
            }
        }

        if (issuer.isEmpty() || thisUpdate.isEmpty()) {
            throw IllegalArgumentException("Missing critical CRL fields: issuer=$issuer, thisUpdate=$thisUpdate")
        }
        return CrlInfo(issuer, thisUpdate, nextUpdate, revokedCerts)
    }
}
