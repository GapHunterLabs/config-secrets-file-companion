package dev.gaphunter.configsecretsfilecompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * End-to-end: real PSI + real inspection registration, not a direct
 * unit call into [HardcodedConfigSecretInspection]'s internals -- the
 * scanning/matching logic itself is already covered exhaustively by
 * [dev.gaphunter.configsecretsfilecompanion.detect.SecretDetectorTest]
 * and [dev.gaphunter.configsecretsfilecompanion.detect.ConfigLineScannerTest].
 * This confirms the inspection is actually wired up end to end.
 */
class HardcodedConfigSecretInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(HardcodedConfigSecretInspection::class.java)
    }

    fun `test a real secret in a properties file produces a warning`() {
        myFixture.configureByText("application.properties", "aws.access.key=AKIAIOSFODNN7EXAMPLE\n")
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("hardcoded secret") == true })
    }

    fun `test a real secret in a yml file produces a warning`() {
        myFixture.configureByText("application.yml", "database:\n  password: kX9pL2mQzR7vT4nB8wA1\n")
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("hardcoded secret") == true })
    }

    fun `test a real secret in a env file produces a warning`() {
        myFixture.configureByText(".env", "DB_PASSWORD=kX9pL2mQzR7vT4nB8wA1\n")
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("hardcoded secret") == true })
    }

    fun `test a placeholder value produces no warning`() {
        myFixture.configureByText(".env", "DB_PASSWORD=changeme\n")
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("hardcoded secret") == true })
    }

    fun `test a non-config file is never scanned`() {
        myFixture.configureByText("Config.java", "String key = \"AKIAIOSFODNN7EXAMPLE\";")
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("hardcoded secret") == true })
    }
}
