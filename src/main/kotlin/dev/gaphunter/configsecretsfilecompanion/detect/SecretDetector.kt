package dev.gaphunter.configsecretsfilecompanion.detect

data class SecretFinding(val kind: String, val description: String)

/**
 * Pure text-based secret detection for config-file values -- same
 * detection rules (known credential-format signatures +
 * variable-name/Shannon-entropy heuristic) as `api-security-companion`'s
 * own `SecretDetector`, which only ever runs on Java/Kotlin *source*
 * files. This is a separate, standalone copy scoped to config-file
 * *values* (`.properties`/`.env`/simple YAML `key: value` lines) --
 * deliberately not shared code, so each plugin stays independently
 * installable without pulling in the other's dependencies.
 */
object SecretDetector {

    private val AWS_ACCESS_KEY = Regex("""\b(AKIA|ASIA)[0-9A-Z]{16}\b""")
    private val GITHUB_TOKEN = Regex("""\bgh[pousr]_[A-Za-z0-9]{36,}\b""")
    private val SLACK_TOKEN = Regex("""\bxox[baprs]-[0-9A-Za-z-]{10,}\b""")
    private val JWT = Regex("""\beyJ[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}\b""")
    private val PRIVATE_KEY_HEADER = Regex("""-----BEGIN ((RSA|EC|OPENSSH|DSA|ENCRYPTED) )?PRIVATE KEY-----""")

    private val SECRET_LIKE_NAME = Regex(
        """(api[_.-]?key|secret|token|password|passwd|pwd|access[_.-]?key|private[_.-]?key)""",
        RegexOption.IGNORE_CASE,
    )

    private val OBVIOUS_PLACEHOLDER = Regex(
        """^(|changeme|change[_-]?me|todo|fixme|xxx+|your[_-].*|placeholder|example|test|dummy|<.*>|\$\{.*}|\$\(.*\))$""",
        RegexOption.IGNORE_CASE,
    )

    private const val MIN_ENTROPY_LENGTH = 12
    private const val MIN_ENTROPY_BITS_PER_CHAR = 3.3

    fun scanValue(value: String, keyHint: String? = null): SecretFinding? {
        AWS_ACCESS_KEY.find(value)?.let {
            return SecretFinding("AWS_ACCESS_KEY", "Looks like an AWS access key ID (${it.value.take(8)}...)")
        }
        GITHUB_TOKEN.find(value)?.let {
            return SecretFinding("GITHUB_TOKEN", "Looks like a GitHub personal access token")
        }
        SLACK_TOKEN.find(value)?.let {
            return SecretFinding("SLACK_TOKEN", "Looks like a Slack API token")
        }
        JWT.find(value)?.let {
            return SecretFinding("JWT", "Looks like a JWT (base64url header.payload.signature)")
        }
        if (PRIVATE_KEY_HEADER.containsMatchIn(value)) {
            return SecretFinding("PRIVATE_KEY", "Contains a PEM private key block")
        }
        if (keyHint != null && SECRET_LIKE_NAME.containsMatchIn(keyHint) && looksLikeARealSecret(value)) {
            return SecretFinding(
                "GENERIC_HIGH_ENTROPY",
                "Assigned to '$keyHint' and looks like a real credential, not a placeholder",
            )
        }
        return null
    }

    private fun looksLikeARealSecret(value: String): Boolean {
        if (value.length < MIN_ENTROPY_LENGTH) return false
        if (OBVIOUS_PLACEHOLDER.matches(value.trim())) return false
        if (value.toSet().size <= 2) return false
        return shannonEntropyBitsPerChar(value) >= MIN_ENTROPY_BITS_PER_CHAR
    }

    fun shannonEntropyBitsPerChar(value: String): Double {
        if (value.isEmpty()) return 0.0
        val counts = value.groupingBy { it }.eachCount()
        val length = value.length.toDouble()
        return -counts.values.sumOf { count ->
            val p = count / length
            p * (Math.log(p) / Math.log(2.0))
        }
    }
}
