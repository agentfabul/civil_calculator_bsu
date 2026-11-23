package com.example.domain.calc

import java.util.Locale
import kotlin.math.abs

const val SOFTWARE_VERSION = "1.0.0-mvp"

enum class UnitSystem(val code: String, val label: String) {
    SI("SI", "SI (Metric)"),
    US("US", "US Customary");

    companion object {
        fun fromCode(raw: String?): UnitSystem =
            entries.firstOrNull { it.code == raw } ?: SI
    }
}

enum class StandardFamily(val code: String, val label: String) {
    ACI_ASCE("ACI_ASCE", "ACI 318-19 / ASCE 7-22"),
    EUROCODE("EUROCODE", "Eurocode (EN 1990 / EN 1992-1-1)");

    companion object {
        fun fromCode(raw: String?): StandardFamily =
            entries.firstOrNull { it.code == raw } ?: ACI_ASCE
    }
}

enum class CalcCategory(val label: String, val shortLabel: String) {
    UNITS_TAKEOFF("1. Units & Takeoff", "Units"),
    STRUCTURAL("2. Structural", "Structural"),
    CONCRETE_REBAR("3. Concrete & Rebar", "Concrete"),
    GEOTECH("4. Soil & Foundations", "Geotech"),
    HYDROLOGY("5. Hydrology & Drainage", "Hydrology"),
    SURVEYING("6. Surveying & Geometry", "Surveying")
}

enum class CalcModuleId(val id: String) {
    UNIT_CONVERSION("UNIT_CONVERSION"),
    QUANTITY_TAKEOFF("QUANTITY_TAKEOFF"),
    STRUCTURAL_BEAM("STRUCTURAL_BEAM"),
    STRUCTURAL_COLUMN("STRUCTURAL_COLUMN"),
    STRUCTURAL_SLAB("STRUCTURAL_SLAB"),
    STRUCTURAL_LOAD_COMBO("STRUCTURAL_LOAD_COMBO"),
    CONCRETE_REBAR("CONCRETE_REBAR"),
    GEOTECH_FOOTING("GEOTECH_FOOTING"),
    HYDROLOGY_RATIONAL("HYDROLOGY_RATIONAL"),
    HYDROLOGY_MANNING("HYDROLOGY_MANNING"),
    SURVEYING_LEVELING("SURVEYING_LEVELING"),
    SURVEYING_AREA_VOLUME("SURVEYING_AREA_VOLUME");

    companion object {
        fun fromId(raw: String?): CalcModuleId? = entries.firstOrNull { it.id == raw }
    }
}

enum class CalcStatus(val code: String, val label: String) {
    PASS("PASS", "PASS — Within Limits"),
    WARNING("WARNING", "CHECK WARNINGS"),
    FAIL("FAIL", "FAIL — Exceeds Limit"),
    ESTIMATE("ESTIMATE", "PRELIMINARY ESTIMATE");

    companion object {
        fun fromCode(raw: String?): CalcStatus =
            entries.firstOrNull { it.code == raw } ?: ESTIMATE
    }
}

enum class IssueSeverity {
    ERROR,
    WARNING,
    INFO
}

enum class CalculationNature(val label: String) {
    CODE_BASED_CHECK("Code-Based Design Check"),
    PRELIMINARY_ESTIMATE("Preliminary Engineering Estimate"),
    EXACT_GEOMETRY_CONVERSION("Exact Geometry / Unit Conversion")
}

data class DesignStandardSpec(
    val id: String,
    val name: String,
    val edition: String,
    val methodSummary: String,
    val family: StandardFamily
)

data class ValidationIssue(
    val severity: IssueSeverity,
    val fieldKey: String?,
    val title: String,
    val message: String
)

enum class InputValueType {
    NUMBER,
    CHOICE,
    TEXT_SERIES
}

data class InputFieldSpec(
    val key: String,
    val label: String,
    val siUnit: String,
    val usUnit: String,
    val helperText: String,
    val valueType: InputValueType = InputValueType.NUMBER,
    val choices: List<String> = emptyList(),
    val minInclusive: Double? = null,
    val maxInclusive: Double? = null,
    val allowZero: Boolean = false,
    val required: Boolean = true
) {
    fun unitFor(unitSystem: UnitSystem): String =
        if (unitSystem == UnitSystem.SI) siUnit else usUnit
}

data class AssumptionSpec(
    val key: String,
    val label: String,
    val defaultValue: String,
    val unit: String,
    val description: String,
    val editable: Boolean = true
)

data class CalcStep(
    val stepNumber: Int,
    val title: String,
    val equation: String,
    val variables: List<String>,
    val substituted: String,
    val resultFormatted: String,
    val rawPrecisionNote: String? = null
)

data class OutputMetric(
    val label: String,
    val formattedValue: String,
    val unit: String,
    val rawValue: Double? = null,
    val highlight: Boolean = false
)

data class ExampleCase(
    val id: String,
    val title: String,
    val description: String,
    val unitSystem: UnitSystem,
    val standardId: String,
    val inputs: Map<String, String>,
    val assumptions: Map<String, String> = emptyMap(),
    val expectedNote: String
)

data class CalcResult(
    val moduleId: CalcModuleId,
    val moduleName: String,
    val nature: CalculationNature,
    val unitSystem: UnitSystem,
    val standard: DesignStandardSpec?,
    val status: CalcStatus,
    val governingCheck: String,
    val utilizationRatio: Double?,
    val summaryMetrics: List<OutputMetric>,
    val steps: List<CalcStep>,
    val issues: List<ValidationIssue>,
    val resolvedAssumptions: Map<String, String>,
    val scopeAndLimitations: List<String>,
    val engineeringNotes: List<String>
) {
    val isValid: Boolean
        get() = issues.none { it.severity == IssueSeverity.ERROR }

    val primarySummaryText: String
        get() {
            val firstMetric = summaryMetrics.firstOrNull()
            val util = utilizationRatio?.let { " | Util: ${CalcFmt.num(it, 3)}" } ?: ""
            return if (firstMetric != null) {
                "${firstMetric.label}: ${firstMetric.formattedValue} ${firstMetric.unit}$util".trim()
            } else {
                governingCheck
            }
        }
}

interface CalcModule {
    val id: CalcModuleId
    val category: CalcCategory
    val name: String
    val shortDescription: String
    val nature: CalculationNature
    val supportedStandards: List<DesignStandardSpec>
    val scopeAndLimitations: List<String>
    val engineeringNotes: List<String>

    fun inputSpecs(unitSystem: UnitSystem, standardId: String): List<InputFieldSpec>
    fun assumptionSpecs(unitSystem: UnitSystem, standardId: String): List<AssumptionSpec>
    fun defaultInputs(unitSystem: UnitSystem, standardId: String): Map<String, String>
    fun examples(): List<ExampleCase>

    fun evaluate(
        unitSystem: UnitSystem,
        standardId: String,
        rawInputs: Map<String, String>,
        rawAssumptions: Map<String, String>
    ): CalcResult
}

object CalcFmt {
    fun num(value: Double, decimals: Int = 2): String =
        EngineeringKernel.formatNumber(value, decimals)

    fun smart(value: Double): String =
        EngineeringKernel.formatSmart(value)

    fun rawNote(value: Double, unit: String): String =
        "Internal double precision: ${String.format(Locale.US, "%.8g", value)} $unit"

    fun parseDoubleInput(
        rawInputs: Map<String, String>,
        spec: InputFieldSpec,
        unitSystem: UnitSystem,
        issues: MutableList<ValidationIssue>
    ): Double? {
        val raw = rawInputs[spec.key]?.trim().orEmpty()
        if (raw.isEmpty()) {
            if (spec.required) {
                issues.add(
                    ValidationIssue(
                        IssueSeverity.ERROR,
                        spec.key,
                        "Missing Input: ${spec.label}",
                        "Please enter a value for ${spec.label} (${spec.unitFor(unitSystem)}). CivilSpec never guesses missing engineering inputs."
                    )
                )
            }
            return null
        }
        val parsed = raw.toDoubleOrNull()
        if (parsed == null || parsed.isNaN() || parsed.isInfinite()) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.ERROR,
                    spec.key,
                    "Invalid Number: ${spec.label}",
                    "\"$raw\" is not a valid finite numeric value."
                )
            )
            return null
        }
        val min = spec.minInclusive
        if (min != null) {
            if (parsed < min || (!spec.allowZero && min == 0.0 && parsed == 0.0)) {
                val boundDesc = if (!spec.allowZero && min == 0.0) "> 0" else ">= ${smart(min)}"
                issues.add(
                    ValidationIssue(
                        IssueSeverity.ERROR,
                        spec.key,
                        "Out of Range: ${spec.label}",
                        "${spec.label} must be $boundDesc ${spec.unitFor(unitSystem)} (entered ${smart(parsed)})."
                    )
                )
                return null
            }
        }
        val max = spec.maxInclusive
        if (max != null && parsed > max) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.WARNING,
                    spec.key,
                    "High Magnitude Check: ${spec.label}",
                    "${spec.label} (${smart(parsed)} ${spec.unitFor(unitSystem)}) exceeds typical bound (${smart(max)}). Verify units and decimal point."
                )
            )
        }
        return parsed
    }

    fun parseAssumptionDouble(
        rawAssumptions: Map<String, String>,
        spec: AssumptionSpec,
        minInclusive: Double,
        maxInclusive: Double,
        issues: MutableList<ValidationIssue>
    ): Double {
        val raw = rawAssumptions[spec.key]?.trim()
        if (raw.isNullOrEmpty()) {
            return spec.defaultValue.toDouble()
        }
        val parsed = raw.toDoubleOrNull()
        if (parsed == null || parsed.isNaN() || parsed.isInfinite()) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.ERROR,
                    spec.key,
                    "Invalid Assumption: ${spec.label}",
                    "Assumption \"${spec.label}\" must be a finite number."
                )
            )
            return spec.defaultValue.toDouble()
        }
        if (parsed < minInclusive || parsed > maxInclusive) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.ERROR,
                    spec.key,
                    "Assumption Out of Bounds: ${spec.label}",
                    "${spec.label} must be between ${smart(minInclusive)} and ${smart(maxInclusive)} ${spec.unit}."
                )
            )
        }
        return parsed
    }
}
