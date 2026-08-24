package dev.gaphunter.configsecretsfilecompanion.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigLineScannerTest {

    @Test
    fun `properties-style AWS key is found with correct offsets`() {
        val text = "aws.access.key=AKIAIOSFODNN7EXAMPLE\n"
        val matches = ConfigLineScanner.scan(text)
        assertEquals(1, matches.size)
        val match = matches[0]
        assertEquals("AWS_ACCESS_KEY", match.finding.kind)
        assertEquals("AKIAIOSFODNN7EXAMPLE", text.substring(match.valueStartOffset, match.valueEndOffset))
    }

    @Test
    fun `env-style line with spaces around equals is found`() {
        val text = "DB_PASSWORD = kX9pL2mQzR7vT4nB8wA1\n"
        val matches = ConfigLineScanner.scan(text)
        assertEquals(1, matches.size)
        assertEquals("kX9pL2mQzR7vT4nB8wA1", text.substring(matches[0].valueStartOffset, matches[0].valueEndOffset))
    }

    @Test
    fun `yaml-style key value is found`() {
        val text = "database:\n  password: kX9pL2mQzR7vT4nB8wA1\n"
        val matches = ConfigLineScanner.scan(text)
        assertEquals(1, matches.size)
        assertEquals("kX9pL2mQzR7vT4nB8wA1", text.substring(matches[0].valueStartOffset, matches[0].valueEndOffset))
    }

    @Test
    fun `comment lines are skipped`() {
        val text = "# DB_PASSWORD=kX9pL2mQzR7vT4nB8wA1\n"
        assertTrue(ConfigLineScanner.scan(text).isEmpty())
    }

    @Test
    fun `placeholder value is not flagged`() {
        val text = "DB_PASSWORD=changeme\n"
        assertTrue(ConfigLineScanner.scan(text).isEmpty())
    }

    @Test
    fun `multiple lines only flag the real secret`() {
        val text = """
            app.name=my-service
            app.debug=true
            aws.access.key=AKIAIOSFODNN7EXAMPLE
        """.trimIndent() + "\n"
        val matches = ConfigLineScanner.scan(text)
        assertEquals(1, matches.size)
        assertEquals("aws.access.key", matches[0].key)
    }
}
