package dev.gaphunter.configsecretsfilecompanion.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecretDetectorTest {

    @Test
    fun `AWS access key is detected regardless of key name`() {
        val finding = SecretDetector.scanValue("AKIAIOSFODNN7EXAMPLE", "some_random_field")
        assertEquals("AWS_ACCESS_KEY", finding?.kind)
    }

    @Test
    fun `GitHub token is detected`() {
        val finding = SecretDetector.scanValue("ghp_" + "a".repeat(36), "GITHUB_TOKEN")
        assertEquals("GITHUB_TOKEN", finding?.kind)
    }

    @Test
    fun `high entropy value with a secret-shaped key is flagged`() {
        val finding = SecretDetector.scanValue("kX9pL2mQzR7vT4nB8wA1", "DB_PASSWORD")
        assertEquals("GENERIC_HIGH_ENTROPY", finding?.kind)
    }

    @Test
    fun `placeholder value is not flagged even with a secret-shaped key`() {
        assertNull(SecretDetector.scanValue("changeme", "DB_PASSWORD"))
        assertNull(SecretDetector.scanValue("\${DB_PASSWORD}", "db.password"))
    }

    @Test
    fun `high entropy value with a non-secret key name is not flagged`() {
        assertNull(SecretDetector.scanValue("kX9pL2mQzR7vT4nB8wA1", "app.instance.id"))
    }

    @Test
    fun `short value is not flagged`() {
        assertNull(SecretDetector.scanValue("abc123", "API_KEY"))
    }
}
