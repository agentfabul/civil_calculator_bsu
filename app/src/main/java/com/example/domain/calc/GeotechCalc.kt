package com.example.domain.calc

import kotlin.math.abs
import kotlin.math.sqrt

class GeotechFootingCalcModule : CalcModule {
    override val id = CalcModuleId.GEOTECH_FOOTING
    override val category = CalcCategory.GEOTECH
    override val name = "Spread Footing: Bearing & Sizing"
    override val shortDescription = "Gross/net bearing pressure under axial load + moment (with kern check) and preliminary square footing sizing."
    override val nature = CalculationNature.PRELIMINARY_ESTIMATE

    override val supportedStandards = listOf(
        DesignStandardSpec(
            id = "ALLOWABLE_STRESS_GEOTECH",
            name = "Allowable Bearing Stress Method (ASD)",
            edition = "Preliminary Geotechnical Sizing",
            methodSummary = "Rigid footing linear contact pressure q = P_total/A × (1 ± 6e/L) for e ≤ L/6; no-tension triangular contact q_max = 2P_total/(3B(L/2 − e)) for L/6 < e < L/2.",
            family = StandardFamily.ACI_ASCE
        )
    )

    override val scopeAndLimitations = listOf(
        "PRELIMINARY GEOTECHNICAL SIZING ONLY — must be verified against a site-specific geotechnical report and settlement analysis.",
        "Assumes a rigid concentric spread footing on uniform soil with uniaxial bending along length L.",
        "Concrete structural design (one-way beam shear, two-way punching shear, flexural reinforcement) is NOT performed here.",
        "Groundwater buoyancy uplift is not automatically deducted unless accounted for in net/gross allowable bearing capacity."
    )

    override val engineeringNotes = listOf(
        "Enter service (unfactored) axial load P and moment M when comparing against allowable bearing capacity q_allow.",
        "Keep eccentricity e ≤ L/6 to maintain full bearing contact without footing uplift."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("p_service", "Service Axial Load (P)", "kN", "kip", "Unfactored column vertical load", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("m_service", "Service Moment (M)", "kN·m", "kip·ft", "Unfactored moment about footing width axis", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("footing_b", "Footing Width (B)", "m", "ft", "Dimension perpendicular to moment bending", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("footing_l", "Footing Length (L)", "m", "ft", "Dimension parallel to moment eccentricity", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("depth_df", "Embedment Depth (Df)", "m", "ft", "Finished grade to footing bottom", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("thickness_tf", "Footing Thickness (tf)", "m", "ft", "Concrete pad thickness (≤ Df)", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("q_allow", "Allowable Gross Bearing (qa)", "kPa", "ksf", "Geotechnical allowable bearing pressure", minInclusive = 0.0, allowZero = false)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("gamma_conc", "Concrete Unit Weight (γc)", if (unitSystem == UnitSystem.SI) "24.0" else "0.150", if (unitSystem == UnitSystem.SI) "kN/m³" else "kcf", "Reinforced concrete density."),
        AssumptionSpec("gamma_soil", "Backfill Soil Unit Weight (γs)", if (unitSystem == UnitSystem.SI) "18.0" else "0.115", if (unitSystem == UnitSystem.SI) "kN/m³" else "kcf", "Moist compacted soil density above footing.")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("p_service" to "650", "m_service" to "65", "footing_b" to "2.2", "footing_l" to "2.5", "depth_df" to "1.5", "thickness_tf" to "0.50", "q_allow" to "200")
        } else {
            mapOf("p_service" to "145", "m_service" to "48", "footing_b" to "7.5", "footing_l" to "8.5", "depth_df" to "5.0", "thickness_tf" to "1.75", "q_allow" to "4.2")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_footing_si",
            title = "2.2m × 2.5m Eccentric Spread Footing",
            description = "Service P=650 kN, M=65 kN·m, Df=1.5 m, tf=0.50 m, qa=200 kPa.",
            unitSystem = UnitSystem.SI,
            standardId = "ALLOWABLE_STRESS_GEOTECH",
            inputs = mapOf("p_service" to "650", "m_service" to "65", "footing_b" to "2.2", "footing_l" to "2.5", "depth_df" to "1.5", "thickness_tf" to "0.50", "q_allow" to "200"),
            expectedNote = "q_max ≈ 167.0 kPa ≤ 200 kPa (Util ≈ 0.835 PASS)"
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

        val p = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val m = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val b = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val l = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val df = CalcFmt.parseDoubleInput(rawInputs, specs[4], unitSystem, issues)
        val tf = CalcFmt.parseDoubleInput(rawInputs, specs[5], unitSystem, issues)
        val qa = CalcFmt.parseDoubleInput(rawInputs, specs[6], unitSystem, issues)

        val gammaC = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], if (unitSystem == UnitSystem.SI) 18.0 else 0.11, if (unitSystem == UnitSystem.SI) 28.0 else 0.18, issues)
        val gammaS = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[1], if (unitSystem == UnitSystem.SI) 10.0 else 0.06, if (unitSystem == UnitSystem.SI) 24.0 else 0.15, issues)

        if (df != null && tf != null && tf > df) {
            issues.add(ValidationIssue(IssueSeverity.WARNING, "thickness_tf", "Footing Projects Above Grade", "Thickness tf exceeds embedment depth Df; overburden soil weight taken as zero."))
        }

        if (issues.any { it.severity == IssueSeverity.ERROR } || p == null || m == null || b == null || l == null || df == null || tf == null || qa == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("gamma_conc" to gammaC.toString(), "gamma_soil" to gammaS.toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        val lenUnit = if (unitSystem == UnitSystem.SI) "m" else "ft"
        val forceUnit = if (unitSystem == UnitSystem.SI) "kN" else "kip"
        val stressUnit = if (unitSystem == UnitSystem.SI) "kPa" else "ksf"

        val fp = EngineeringKernel.computeFootingPressure(
            p, m, b, l, df, tf, gammaC, gammaS, qa
        )
        val area = fp.area
        val selfWeight = fp.selfWeight
        val pTotal = fp.pTotal
        val ecc = fp.eccentricity
        val kern = fp.kernLimit
        val qMax = fp.qMax
        val qMin = fp.qMin
        val contactNote: String

        if (fp.overturning) {
            contactNote = "Eccentricity e >= L/2 (Overturning instability)"
            issues.add(ValidationIssue(IssueSeverity.ERROR, "m_service", "Overturning Failure (e ≥ L/2)", "Resultant falls outside footing base (e=${CalcFmt.num(ecc, 3)} $lenUnit ≥ L/2=${CalcFmt.num(l / 2.0, 3)} $lenUnit)."))
        } else if (fp.outsideKern) {
            contactNote = "No-tension triangular contact (e > L/6)"
            issues.add(
                ValidationIssue(
                    IssueSeverity.WARNING,
                    "m_service",
                    "Kern Limit Exceeded (e > L/6)",
                    "Eccentricity e = ${CalcFmt.num(ecc, 3)} $lenUnit > L/6 = ${CalcFmt.num(kern, 3)} $lenUnit. Partial uplift occurs; triangular contact used."
                )
            )
        } else {
            contactNote = "Full compression contact (e ≤ L/6)"
        }

        val util = if (qMax.isFinite()) qMax / qa else 999.0
        if (util > 1.0 && qMax.isFinite()) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.ERROR,
                    "q_allow",
                    "Allowable Bearing Exceeded",
                    "q_max = ${CalcFmt.num(qMax, 2)} $stressUnit exceeds qa = ${CalcFmt.num(qa, 2)} $stressUnit."
                )
            )
        }

        // Preliminary square footing size for concentric P_total (iterative/estimate)
        val bReqSquare = fp.bReqSquare

        val status = when {
            issues.any { it.severity == IssueSeverity.ERROR } || util > 1.0 -> CalcStatus.FAIL
            issues.any { it.severity == IssueSeverity.WARNING } -> CalcStatus.WARNING
            else -> CalcStatus.PASS
        }

        val steps = listOf(
            CalcStep(1, "Total Vertical Load Including Footing & Overburden", "P_total = P + B·L·(tf·γc + max(0, Df − tf)·γs)", listOf("Area = ${CalcFmt.num(area, 2)} $lenUnit²", "Self-wt = ${CalcFmt.num(selfWeight, 2)} $forceUnit"), "P_total = ${CalcFmt.num(p, 2)} + ${CalcFmt.num(selfWeight, 2)}", "${CalcFmt.num(pTotal, 2)} $forceUnit"),
            CalcStep(2, "Eccentricity & Kern Check", "e = M / P_total  vs.  L/6", listOf("L/6 = ${CalcFmt.num(kern, 3)} $lenUnit"), "e = ${CalcFmt.smart(m)} / ${CalcFmt.num(pTotal, 2)} = ${CalcFmt.num(ecc, 3)} $lenUnit", contactNote),
            CalcStep(3, "Gross Bearing Pressure & Utilization", if (ecc <= kern) "q_max/min = (P_total/A)·(1 ± 6e/L)" else "q_max = 2·P_total / [3·B·(L/2 − e)]", listOf("qa = ${CalcFmt.smart(qa)} $stressUnit"), "q_max = ${CalcFmt.num(qMax, 2)} $stressUnit ; q_min = ${CalcFmt.num(qMin, 2)} $stressUnit", "Utilization = ${CalcFmt.num(util, 3)}", CalcFmt.rawNote(qMax, stressUnit))
        )

        return CalcResult(
            moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
            status = status, governingCheck = "Bearing q_max / qa = ${CalcFmt.num(util, 3)}", utilizationRatio = util,
            summaryMetrics = listOf(
                OutputMetric("Max Bearing (q_max)", CalcFmt.num(qMax, 2), stressUnit, qMax, highlight = true),
                OutputMetric("Min Bearing (q_min)", CalcFmt.num(qMin, 2), stressUnit, qMin),
                OutputMetric("Bearing Utilization", CalcFmt.num(util, 3), "ratio", util, highlight = true),
                OutputMetric("Eccentricity (e)", CalcFmt.num(ecc, 3), lenUnit, ecc),
                OutputMetric("Prelim. Square Size B_req", CalcFmt.num(bReqSquare, 2), lenUnit, bReqSquare)
            ),
            steps = steps, issues = issues,
            resolvedAssumptions = mapOf("gamma_conc" to CalcFmt.num(gammaC, 2), "gamma_soil" to CalcFmt.num(gammaS, 2)),
            scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
        )
    }
}
