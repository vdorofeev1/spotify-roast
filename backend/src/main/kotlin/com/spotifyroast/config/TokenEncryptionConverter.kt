package com.spotifyroast.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JPA AttributeConverter that transparently encrypts/decrypts Spotify tokens
 * using AES-256-GCM before they touch the database.
 *
 * Storage format: Base64( IV(12 bytes) || Ciphertext || GCM Auth Tag(16 bytes) )
 *
 * Key must be a Base64-encoded 32-byte (256-bit) value supplied via ENCRYPTION_KEY env var.
 * Generate with: openssl rand -base64 32
 */
@Component
@Converter
class TokenEncryptionConverter(
    @Value("\${encryption.key:}") keyBase64: String
) : AttributeConverter<String, String> {

    private val secretKey: SecretKeySpec
    private val random = SecureRandom()

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
    }

    init {
        val normalizedKey = keyBase64.trim()
        require(normalizedKey.isNotBlank()) {
            "ENCRYPTION_KEY is required because Spotify tokens are stored encrypted. " +
                "Add ENCRYPTION_KEY=<value> to .env or export it before startup. " +
                "Generate one with: openssl rand -base64 32"
        }

        val keyBytes = try {
            Base64.getDecoder().decode(normalizedKey)
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException(
                "ENCRYPTION_KEY must be a Base64-encoded 256-bit (32-byte) value. " +
                    "Generate one with: openssl rand -base64 32",
                ex,
            )
        }
        require(keyBytes.size == 32) {
            "ENCRYPTION_KEY must be a Base64-encoded 256-bit (32-byte) value. " +
                "Generate with: openssl rand -base64 32"
        }
        secretKey = SecretKeySpec(keyBytes, "AES")
    }

    override fun convertToDatabaseColumn(attribute: String): String {
        val iv = ByteArray(IV_LENGTH_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))
        // Prepend IV so each ciphertext is self-contained; IV is not secret
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    override fun convertToEntityAttribute(dbData: String): String {
        val combined = Base64.getDecoder().decode(dbData)
        require(combined.size > IV_LENGTH_BYTES) { "Stored token is too short to be a valid encrypted value" }
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
