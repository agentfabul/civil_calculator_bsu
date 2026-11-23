package com.example.domain.calc

import kotlin.math.pow
import kotlin.math.sqrt

class HydrologyRationalCalcModule : CalcModule {
    override val id = CalcModuleId.HYDROLOGY_RATIONAL
    override val category = CalcCategory.HYDROLOGY
    override val name = "Rational Method Peak Runoff (Q)"
    override val shortDescription = "Peak stormwater runoff rate Q = C·i·A (with SI 360 conversion) for small urban and site catchments."
    override val nature = CalculationNature.PRELIMINARY_ESTIMATE

    override val supportedStandards = listOf(
        DesignStandardSpec(
            id = "RATIONAL_METHOD",
            name = "Rational Method (ASCE / FHWA HEC-22)",
            edition = "Standard Hydrologic Practice",
            methodSummary = "Q = (C · i · A) / 360 in SI (m³/s for mm/hr & ha); Q = C · i · A in US Customary (cfs for in/hr & acres).",
            family = StandardFamily.ACI_ASCE
        )
    )

    override val scopeAndLimitations = listOf(
        "Valid ONLY for small drainage catchments (typically A ≤ 80 ha / 200 acres) with uniform rainfall and no significant storage routing.",
        "Assumes rainfall duration equals or exceeds catchment time of concentration Tc.",
        "Does not compute hydrograph volume or detention pond routing."
    )

    override val engineeringNotes = listOf(
        "Typical runoff coefficient C: 0.70–0.95 paved asphalt/roofs, 0.30–0.50 suburban residential, 0.10–0.25 parks/grass.",
        "Verify local IDF (Intensity-Duration-Frequency) curves for the selected return period (e.g., 10-yr or 100-yr storm)."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("c_coeff", "Runoff Coefficient (C)", "dim.", "dim.", "Imperviousness factor (0.05 to 1.00)", minInclusive = 0.01, maxInclusive = 1.0, allowZero = false),
        InputFieldSpec("intensity", "Rainfall Intensity (i)", "mm/hr", "in/hr", "Design storm intensity at Tc", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("area", "Catchment Area (A)", "ha", "acres", "Contributing watershed area", minInclusive = 0.0, allowZero = false)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("cf_adj", "Frequency Adjustment Factor (Cf)", "1.0", "factor", "Optional ASCE/HEC-22 multiplier for extreme events (1.0 for ≤10-yr, 1.1 for 25-yr, 1.25 for 100-yr; capped so C·Cf ≤ 1.0).")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("c_coeff" to "0.65", "intensity" to "85.0", "area" to "4.5")
        } else {
            mapOf("c_coeff" to "0.65", "intensity" to "3.35", "area" to "11.1")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_rational_si",
            title = "4.5 ha Commercial Parking Lot (SI)",
            description = "C=0.65, i=85 mm/hr, A=4.5 ha, Cf=1.0.",
            unitSystem = UnitSystem.SI,
            standardId = "RATIONAL_METHOD",
            inputs = mapOf("c_coeff" to "0.65", "intensity" to "85.0", "area" to "4.5"),
            expectedNote = "Q = (0.65 × 85 × 4.5) / 360 = 0.6906 m³/s (690.6 L/s)"
        )
    )

    override fun evaluate(
        unitSystem: UnitSystem,
        standardId: String,
        rawInputs: Map<String, String>,
        rawAssumptions: Map<String, String>
    ): CalcResult {
        val issues = mutableListOf<ValidationIssue>()
        val std = supportedStandards.first()
        val specs = inputSpecs(unitSystem, std.id)
        val aSpecs = assumptionSpecs(unitSystem, std.id)

        val c = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val i = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val a = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val cf = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 1.0, 1.30, issues)

        if (c != null && c > 1.0) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "c_coeff", "Runoff Coefficient > 1.0", "Runoff coefficient C cannot exceed 1.00."))
        }
        val areaLimit = if (unitSystem == UnitSystem.SI) 80.0 else 200.0
        if (a != null && a > areaLimit) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.WARNING,
                    "area",
                    "Catchment Exceeds Rational Method Limit",
                    "Catchment area exceeds typical ${CalcFmt.num(areaLimit, 0)} ${specs[2].unitFor(unitSystem)} limit for Rational Method; use SCS/TR-55 hydrograph routing instead."
                )
            )
        }

        if (issues.any { it.severity == IssueSeverity.ERROR } || c == null || i == null || a == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("cf_adj" to cf.toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        val cEff = (c * cf).coerceAtMost(1.0)
        val qPrimary = EngineeringKernel.rationalPeakRunoff(cEff, i, a, unitSystem == UnitSystem.SI)
        // Note: Standard US engineering treats 1 acre*in/hr = 1.008 cfs (~1.0 commonly). Let's note exact 1.008 vs 1.0!
        val qUnit = if (unitSystem == UnitSystem.SI) "m³/s" else "cfs"
        val qAlt = if (unitSystem == UnitSystem.SI) qPrimary * 1000.0 else qPrimary * 448.8311688
        val altUnit = if (unitSystem == UnitSystem.SI) "L/s" else "gpm"

        val steps = listOf(
            CalcStep(
                stepNumber = 1,
                title = "Effective Runoff Coefficient",
                equation = "C_eff = min(1.0, C × Cf)",
                variables = listOf("C = ${CalcFmt.num(c, 3)}", "Cf = ${CalcFmt.num(cf, 2)}"),
                substituted = "C_eff = min(1.0, ${CalcFmt.num(c, 3)} × ${CalcFmt.num(cf, 2)})",
                resultFormatted = CalcFmt.num(cEff, 3)
            ),
            CalcStep(
                stepNumber = 2,
                title = "Peak Runoff Discharge",
                equation = if (unitSystem == UnitSystem.SI) "Q = (C_eff × i × A) / 360" else "Q = 1.0083 × C_eff × i × A",
                variables = listOf("i = ${CalcFmt.smart(i)} ${specs[1].unitFor(unitSystem)}", "A = ${CalcFmt.smart(a)} ${specs[2].unitFor(unitSystem)}"),
                substituted = if (unitSystem == UnitSystem.SI) "(${CalcFmt.num(cEff, 3)} × ${CalcFmt.smart(i)} × ${CalcFmt.smart(a)}) / 360" else "1.0083 × ${CalcFmt.num(cEff, 3)} × ${CalcFmt.smart(i)} × ${CalcFmt.smart(a)}",
                resultFormatted = "${CalcFmt.num(qPrimary, 4)} $qUnit (${CalcFmt.num(qAlt, 1)} $altUnit)",
                rawPrecisionNote = CalcFmt.rawNote(qPrimary, qUnit)
            )
        )

        val status = if (issues.any { it.severity == IssueSeverity.WARNING }) CalcStatus.WARNING else CalcStatus.ESTIMATE

        return CalcResult(
            moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
            status = status, governingCheck = "Peak Q = ${CalcFmt.num(qPrimary, 3)} $qUnit", utilizationRatio = null,
            summaryMetrics = listOf(
                OutputMetric("Peak Discharge (Q)", CalcFmt.num(qPrimary, 4), qUnit, qPrimary, highlight = true),
                OutputMetric("Equivalent Flow", CalcFmt.num(qAlt, 1), altUnit, qAlt)
            ),
            steps = steps, issues = issues,
            resolvedAssumptions = mapOf("cf_adj" to CalcFmt.num(cf, 2)),
            scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
        )
    }
}

class HydrologyManningCalcModule : CalcModule {
    override val id = CalcModuleId.HYDROLOGY_MANNING
    override val category = CalcCategory.HYDROLOGY
    override val name = "Manning's Open-Channel Flow"
    override val shortDescription = "Steady uniform discharge Q, velocity V, hydraulic radius Rh, and Froude regime for trapezoidal/rectangular channels."
    override val nature = CalculationNature.CODE_BASED_CHECK

    override val supportedStandards = listOf(
        DesignStandardSpec(
            id = "MANNING_OPEN_CHANNEL",
            name = "Manning's Equation (FHWA HDS-4 / Chow)",
            edition = "Steady Uniform Flow",
            methodSummary = "Q = (k / n) · A · Rh^(2/3) · S^(1/2), where k = 1.0 (SI) or 1.486 (US Customary).",
            family = StandardFamily.ACI_ASCE
        )
    )

    override val scopeAndLimitations = listOf(
        "Assumes steady, uniform open-channel flow (friction slope equals bed slope).",
        "Set side slope z = 0 for vertical-walled rectangular channels.",
        "Does not evaluate backwater profiles (GVF), hydraulic jumps, or culvert inlet control."
    )

    override val engineeringNotes = listOf(
        "Typical Manning n: 0.013 finished concrete, 0.024 corrugated metal, 0.030 clean earth channel, 0.045 riprap.",
        "Check Froude number Fr: Fr > 1 indicates supercritical flow (erosion / jump risk)."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("b_width", "Channel Bottom Width (b)", "m", "ft", "Base width of channel", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("y_depth", "Flow Depth (y)", "m", "ft", "Uniform water depth", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("z_slope", "Side Slope (z : 1 H:V)", "H:V", "H:V", "0 for rectangular; 2.0 for 2H:1V", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("bed_slope", "Longitudinal Bed Slope (S)", "m/m", "ft/ft", "Decimal slope (e.g., 0.005 = 0.5%)", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("manning_n", "Manning Roughness (n)", "dim.", "dim.", "Surface roughness coefficient (0.009–0.150)", minInclusive = 0.008, maxInclusive = 0.20, allowZero = false)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("max_velocity", "Permissible Max Velocity Check", if (unitSystem == UnitSystem.SI) "3.5" else "11.5", if (unitSystem == UnitSystem.SI) "m/s" else "ft/s", "Advisory channel lining erosion limit.")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("b_width" to "1.50", "y_depth" to "0.80", "z_slope" to "2.0", "bed_slope" to "0.004", "manning_n" to "0.015")
        } else {
            mapOf("b_width" to "5.0", "y_depth" to "2.5", "z_slope" to "2.0", "bed_slope" to "0.004", "manning_n" to "0.015")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_manning_si",
            title = "Concrete Trapezoidal Ditch (SI)",
            description = "b=1.5 m, y=0.80 m, z=2.0, S=0.004, n=0.015.",
            unitSystem = UnitSystem.SI,
            standardId = "MANNING_OPEN_CHANNEL",
            inputs = mapOf("b_width" to "1.50", "y_depth" to "0.80", "z_slope" to "2.0", "bed_slope" to "0.004", "manning_n" to "0.015"),
            expectedNote = "A = 2.48 m², P = 5.078 m, Rh = 0.488 m, Q ≈ 6.47 m³/s"
        )
    )

    override fun evaluate(
        unitSystem: UnitSystem,
        standardId: String,
        rawInputs: Map<String, String>,
        rawAssumptions: Map<String, String>
    ): CalcResult {
        val issues = mutableListOf<ValidationIssue>()
        val std = supportedStandards.first()
        val specs = inputSpecs(unitSystem, std.id)
        val aSpecs = assumptionSpecs(unitSystem, std.id)

        val b = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val y = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val z = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val s = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val n = CalcFmt.parseDoubleInput(rawInputs, specs[4], unitSystem, issues)
        val vMax = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 0.3, 50.0, issues)

        if (b != null && z != null && b == 0.0 && z == 0.0) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "b_width", "Zero Channel Width", "Bottom width b and side slope z cannot both be zero."))
        }

        if (issues.any { it.severity == IssueSeverity.ERROR } || b == null || y == null || z == null || s == null || n == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("max_velocity" to vMax.toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        val lenUnit = if (unitSystem == UnitSystem.SI) "m" else "ft"
        val velUnit = if (unitSystem == UnitSystem.SI) "m/s" else "ft/s"
        val qUnit = if (unitSystem == UnitSystem.SI) "m³/s" else "cfs"
        val k = if (unitSystem == UnitSystem.SI) 1.0 else 1.48591867 // exact 3.280839895^(1/3)

        val mRes = EngineeringKernel.computeManningTrapezoidal(
            b, y, z, s, n, unitSystem == UnitSystem.SI
        )
        val area = mRes.area
        val wetPerim = mRes.wettedPerimeter
        val rh = mRes.hydraulicRadius
        val topWidth = mRes.topWidth
        val hydDepth = mRes.hydraulicDepth
        val velocity = mRes.velocity
        val discharge = mRes.discharge
        val froude = mRes.froudeNumber

        if (froude >= 1.0) {
            issues.add(ValidationIssue(IssueSeverity.WARNING, "bed_slope", "Supercritical Flow (Fr ≥ 1.0)", "Fr = ${CalcFmt.num(froude, 2)} indicates supercritical flow; evaluate hydraulic jump and scour protection."))
        }
        val velUtil = velocity / vMax
        if (velUtil > 1.0) {
            issues.add(ValidationIssue(IssueSeverity.WARNING, null, "Velocity Exceeds Lining Limit", "V = ${CalcFmt.num(velocity, 2)} $velUnit > ${CalcFmt.num(vMax, 2)} $velUnit permissible velocity."))
        }

        val status = when {
            issues.any { it.severity == IssueSeverity.WARNING } -> CalcStatus.WARNING
            else -> CalcStatus.PASS
        }

        val steps = listOf(
            CalcStep(1, "Trapezoidal Section Geometry", "A = (b + z·y)·y ; P = b + 2y·√(1 + z²) ; Rh = A / P", listOf("b=${CalcFmt.smart(b)}", "y=${CalcFmt.smart(y)}", "z=${CalcFmt.smart(z)}"), "A = ${CalcFmt.num(area, 3)} $lenUnit² ; P = ${CalcFmt.num(wetPerim, 3)} $lenUnit", "Rh = ${CalcFmt.num(rh, 4)} $lenUnit"),
            CalcStep(2, "Manning Velocity & Discharge", "V = (${CalcFmt.num(k, 3)} / n) · Rh^(2/3) · S^(1/2) ; Q = A·V", listOf("n=${CalcFmt.smart(n)}", "S=${CalcFmt.smart(s)}"), "V = ${CalcFmt.num(velocity, 3)} $velUnit", "Q = ${CalcFmt.num(discharge, 3)} $qUnit", CalcFmt.rawNote(discharge, qUnit)),
            CalcStep(3, "Froude Regime Check", "Fr = V / √(g · Dh), where Dh = A / T", listOf("T = ${CalcFmt.num(topWidth, 2)} $lenUnit", "Dh = ${CalcFmt.num(hydDepth, 3)} $lenUnit"), "Fr = ${CalcFmt.num(froude, 3)}", if (froude < 1.0) "Subcritical (Fr < 1)" else "Supercritical (Fr ≥ 1)")
        )

        return CalcResult(
            moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
            status = status, governingCheck = "Q = ${CalcFmt.num(discharge, 3)} $qUnit | V/Vmax = ${CalcFmt.num(velUtil, 2)}", utilizationRatio = velUtil,
            summaryMetrics = listOf(
                OutputMetric("Discharge (Q)", CalcFmt.num(discharge, 3), qUnit, discharge, highlight = true),
                OutputMetric("Flow Velocity (V)", CalcFmt.num(velocity, 2), velUnit, velocity, highlight = true),
                OutputMetric("Hydraulic Radius (Rh)", CalcFmt.num(rh, 3), lenUnit, rh),
                OutputMetric("Froude Number (Fr)", CalcFmt.num(froude, 2), if (froude < 1.0) "Subcritical" else "Supercritical", froude)
            ),
            steps = steps, issues = issues,
            resolvedAssumptions = mapOf("max_velocity" to CalcFmt.num(vMax, 2)),
            scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
        )
    }
}
