package com.spotifyroast.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TokenEncryptionConverterTests {

    @Test
    fun encryptsAndDecryptsToken() {
        val converter = TokenEncryptionConverter(TEST_KEY)

        val encrypted = converter.convertToDatabaseColumn("spotify-token")

        assertTrue(encrypted.isNotBlank())
        assertTrue(encrypted != "spotify-token")
        assertEquals("spotify-token", converter.convertToEntityAttribute(encrypted))
    }

    @Test
    fun rejectsMissingEncryptionKey() {
        val exception = assertFailsWith<IllegalArgumentException> {
            TokenEncryptionConverter("")
        }

        assertTrue(exception.message!!.contains("ENCRYPTION_KEY is required"))
    }

    @Test
    fun rejectsNonBase64EncryptionKey() {
        val exception = assertFailsWith<IllegalArgumentException> {
            TokenEncryptionConverter("not-base64!")
        }

        assertTrue(exception.message!!.contains("Base64-encoded"))
    }

    @Test
    fun rejectsWrongLengthEncryptionKey() {
        val exception = assertFailsWith<IllegalArgumentException> {
            TokenEncryptionConverter("c2hvcnQ=")
        }

        assertTrue(exception.message!!.contains("32-byte"))
    }

    private companion object {
        private const val TEST_KEY = "dGVzdC1lbmNyeXB0aW9uLWtleS1mb3ItMzItYnl0ZXM="
    }
}
