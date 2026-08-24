package dev.gaphunter.configsecretsfilecompanion.detect

/** One `key = value` / `key: value` config line with a suspected secret value. */
data class ConfigSecretMatch(
    val key: String,
    val finding: SecretFinding,
    val valueStartOffset: Int,
    val valueEndOffset: Int,
)

/**
 * Plain-text line scanner for config-file formats this plugin covers:
 * `.properties`/`.env` (`key=value` or `key = value`) and simple YAML
 * (`key: value`, top-level or nested -- indentation doesn't affect
 * matching). Comments (`#` at line start, after trimming) are skipped.
 *
 * **v0.1 scope, stated honestly:** single-line `key: value`/`key=value`
 * pairs only -- YAML block scalars (`|`, `>`), multi-line values, and
 * quoted values containing an escaped delimiter aren't specially
 * handled (a quoted value is scanned including its quotes, which the
 * entropy heuristic tolerates fine in practice).
 */
object ConfigLineScanner {

    private val PROPERTIES_OR_ENV_LINE = Regex("""^([A-Za-z_][A-Za-z0-9_.-]*)\s*=\s*(.*)$""")
    private val YAML_LINE = Regex("""^(\s*)([A-Za-z_][A-Za-z0-9_.-]*)\s*:\s+(.+)$""")

    fun scan(text: String): List<ConfigSecretMatch> {
        val results = mutableListOf<ConfigSecretMatch>()
        var offset = 0
        for (rawLine in text.lineSequence()) {
            val lineStart = offset
            offset += rawLine.length + 1 // +1 for the newline consumed by lineSequence

            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val match = propertiesOrEnvMatch(rawLine, lineStart) ?: yamlMatch(rawLine, lineStart)
            if (match != null) results += match
        }
        return results
    }

    private fun propertiesOrEnvMatch(rawLine: String, lineStart: Int): ConfigSecretMatch? {
        val trimmedLine = rawLine.trim()
        val match = PROPERTIES_OR_ENV_LINE.find(trimmedLine) ?: return null
        val key = match.groupValues[1]
        val value = match.groupValues[2].trim()
        if (value.isEmpty()) return null
        val finding = SecretDetector.scanValue(value, key) ?: return null

        val leadingWs = rawLine.length - rawLine.trimStart().length
        val valueOffsetInTrimmed = trimmedLine.indexOf(value, key.length)
        if (valueOffsetInTrimmed < 0) return null
        val valueStart = lineStart + leadingWs + valueOffsetInTrimmed
        return ConfigSecretMatch(key, finding, valueStart, valueStart + value.length)
    }

    private fun yamlMatch(rawLine: String, lineStart: Int): ConfigSecretMatch? {
        val match = YAML_LINE.find(rawLine) ?: return null
        val key = match.groupValues[2]
        val value = match.groupValues[3].trim()
        if (value.isEmpty() || value == "|" || value == ">") return null
        val finding = SecretDetector.scanValue(value, key) ?: return null

        val valueOffsetInLine = rawLine.indexOf(value, match.groups[3]!!.range.first)
        if (valueOffsetInLine < 0) return null
        val valueStart = lineStart + valueOffsetInLine
        return ConfigSecretMatch(key, finding, valueStart, valueStart + value.length)
    }
}
