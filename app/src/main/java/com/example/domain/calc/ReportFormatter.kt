package com.example.domain.calc

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportFormatter {

    fun formatCalculationReport(
        projectName: String,
        projectLocation: String,
        calculationTitle: String,
        revisionNumber: Int,
        engineerName: String,
        module: CalcModule,
        result: CalcResult,
        rawInputs: Map<String, String>,
        notes: String,
        timestampMs: Long = System.currentTimeMillis()
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm z", Locale.US).format(Date(timestampMs))
        val sb = StringBuilder()
        sb.appendLine("======================================================================")
        sb.appendLine("CIVILSPEC ENGINEERING CALCULATION SHEET (DECISION SUPPORT)")
        sb.appendLine("======================================================================")
        sb.appendLine("Project:          $projectName")
        if (projectLocation.isNotBlank()) sb.appendLine("Location:         $projectLocation")
        sb.appendLine("Calculation:      $calculationTitle (Rev $revisionNumber)")
        sb.appendLine("Module:           ${module.name} [${module.id.id}]")
        sb.appendLine("Calculation Type: ${result.nature.label}")
        sb.appendLine("Prepared By:      ${engineerName.ifBlank { "Registered User" }}")
        sb.appendLine("Date / Time:      $dateStr")
        sb.appendLine("Unit System:      ${result.unitSystem.label}")
        result.standard?.let {
            sb.appendLine("Design Standard:  ${it.name} (${it.edition})")
            sb.appendLine("Method:           ${it.methodSummary}")
        }
        sb.appendLine("Software Version: CivilSpec $SOFTWARE_VERSION")
        sb.appendLine()
        sb.appendLine("----------------------------------------------------------------------")
        sb.appendLine("1. RESULT SUMMARY & GOVERNING CHECK")
        sb.appendLine("----------------------------------------------------------------------")
        sb.appendLine("Status:           ${result.status.label}")
        sb.appendLine("Governing Check:  ${result.governingCheck}")
        result.utilizationRatio?.let {
            sb.appendLine("Utilization:      ${CalcFmt.num(it, 3)} (${CalcFmt.num(it * 100.0, 1)}%)")
        }
        result.summaryMetrics.forEach { metric ->
            sb.appendLine("  • ${metric.label}: ${metric.formattedValue} ${metric.unit}".trim())
        }
        sb.appendLine()
        sb.appendLine("----------------------------------------------------------------------")
        sb.appendLine("2. INPUTS")
        sb.appendLine("----------------------------------------------------------------------")
        module.inputSpecs(result.unitSystem, result.standard?.id.orEmpty()).forEach { spec ->
            val v = rawInputs[spec.key].orEmpty()
            val u = spec.unitFor(result.unitSystem)
            sb.appendLine("  • ${spec.label}: $v ${if (u == "-") "" else u}".trim())
        }
        sb.appendLine()
        sb.appendLine("----------------------------------------------------------------------")
        sb.appendLine("3. EXPLICIT ASSUMPTIONS")
        sb.appendLine("----------------------------------------------------------------------")
        val aSpecs = module.assumptionSpecs(result.unitSystem, result.standard?.id.orEmpty())
        if (aSpecs.isEmpty()) {
            sb.appendLine("  (None)")
        } else {
            aSpecs.forEach { spec ->
                val v = result.resolvedAssumptions[spec.key] ?: spec.defaultValue
                sb.appendLine("  • ${spec.label}: $v ${spec.unit} — ${spec.description}")
            }
        }
        sb.appendLine()
        sb.appendLine("----------------------------------------------------------------------")
        sb.appendLine("4. STEP-BY-STEP EQUATIONS & SUBSTITUTED VALUES")
        sb.appendLine("----------------------------------------------------------------------")
        result.steps.forEach { step ->
            sb.appendLine("Step ${step.stepNumber}: ${step.title}")
            sb.appendLine("  Formula:     ${step.equation}")
            if (step.variables.isNotEmpty()) {
                sb.appendLine("  Variables:   ${step.variables.joinToString("; ")}")
            }
            sb.appendLine("  Substituted: ${step.substituted}")
            sb.appendLine("  Result:      ${step.resultFormatted}")
            step.rawPrecisionNote?.let { sb.appendLine("  Precision:   $it") }
            sb.appendLine()
        }
        if (result.issues.isNotEmpty()) {
            sb.appendLine("----------------------------------------------------------------------")
            sb.appendLine("5. WARNINGS & VALIDATION FLAGS")
            sb.appendLine("----------------------------------------------------------------------")
            result.issues.forEach { issue ->
                sb.appendLine("  [${issue.severity.name}] ${issue.title}: ${issue.message}")
            }
            sb.appendLine()
        }
        if (notes.isNotBlank()) {
            sb.appendLine("----------------------------------------------------------------------")
            sb.appendLine("6. ENGINEER NOTES")
            sb.appendLine("----------------------------------------------------------------------")
            sb.appendLine(notes)
            sb.appendLine()
        }
        sb.appendLine("----------------------------------------------------------------------")
        sb.appendLine("7. SCOPE, LIMITATIONS & ENGINEERING SAFEGUARD")
        sb.appendLine("----------------------------------------------------------------------")
        result.scopeAndLimitations.forEach { sb.appendLine("  • $it") }
        sb.appendLine()
        sb.appendLine("SAFEGUARD NOTICE: This output is a decision-support calculation only.")
        sb.appendLine("A passing check does NOT constitute proof that a complete design is safe.")
        sb.appendLine("Independent review and seal by a licensed Professional Engineer (PE/SE)")
        sb.appendLine("is required before construction, fabrication, or permitting use.")
        sb.appendLine("======================================================================")
        return sb.toString()
    }
}
