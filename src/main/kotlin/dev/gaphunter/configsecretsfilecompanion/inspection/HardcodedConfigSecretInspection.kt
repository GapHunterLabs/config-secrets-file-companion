package dev.gaphunter.configsecretsfilecompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.gaphunter.configsecretsfilecompanion.detect.ConfigLineScanner
import dev.gaphunter.configsecretsfilecompanion.review.ReviewPrompt

/**
 * Flags a suspected real secret (AWS key, GitHub/Slack token, JWT, PEM
 * private key, or a high-entropy value assigned to a secret-shaped key
 * name) hardcoded directly in a config file -- `.properties`, `.env`
 * (and its variants), or a `.yml`/`.yaml` file. Complements
 * `api-security-companion`, which only scans Java/Kotlin *source*
 * files -- config files are a real, common, and separately-owned place
 * secrets get committed by accident (a real value pasted into
 * `application.yml` "just for local testing").
 *
 * Runs via [checkFile] (whole-file text scan) rather than a PSI
 * visitor, same reasoning as `env-var-missing-companion`'s
 * `MissingEnvVarInspection`: detection is plain-text line scanning, not
 * a PSI walk of a specific grammar -- see `build.gradle.kts` for why no
 * YAML PSI dependency is taken.
 */
class HardcodedConfigSecretInspection : LocalInspectionTool() {

    companion object {
        /** Files larger than this are skipped -- avoids pathological cost on generated/huge files. */
        const val MAX_FILE_LENGTH = 500_000

        private val CONFIG_FILE_NAME = Regex(
            """^(\.env(\..+)?|[^.]+\.(properties|ya?ml))$""",
            RegexOption.IGNORE_CASE,
        )
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val virtualFile = file.virtualFile ?: return null
        if (!CONFIG_FILE_NAME.matches(virtualFile.name)) return null

        val text = file.text
        if (text.length > MAX_FILE_LENGTH) return null

        val matches = ConfigLineScanner.scan(text)
        if (matches.isEmpty()) return null

        val problems = mutableListOf<ProblemDescriptor>()
        for (match in matches) {
            val anchor = leafElementAt(file, match.valueStartOffset) ?: continue
            val anchorStart = anchor.textRange.startOffset
            val relativeRange = TextRange(
                (match.valueStartOffset - anchorStart).coerceAtLeast(0),
                (match.valueEndOffset - anchorStart).coerceAtMost(anchor.textLength),
            )
            if (relativeRange.startOffset >= relativeRange.endOffset) continue

            problems += manager.createProblemDescriptor(
                anchor,
                relativeRange,
                "Possible hardcoded secret: ${match.finding.description}",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                isOnTheFly,
            )

            val lineNumber = file.viewProvider.document?.getLineNumber(match.valueStartOffset) ?: -1
            ReviewPrompt.recordHit(file.project, "${virtualFile.path}:$lineNumber")
        }

        return if (problems.isEmpty()) null else problems.toTypedArray()
    }

    /** Descends to a real leaf PSI element -- ProblemDescriptor must never anchor on a composite node. */
    private fun leafElementAt(file: PsiFile, startOffset: Int): PsiElement? {
        if (startOffset < 0 || startOffset >= file.textLength) return null
        var element = file.findElementAt(startOffset) ?: return file
        while (element.firstChild != null) {
            element = element.firstChild
        }
        return element
    }
}
