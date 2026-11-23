package com.example.domain.calc

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.sqrt

class SurveyingLevelingCalcModule : CalcModule {
    override val id = CalcModuleId.SURVEYING_LEVELING
    override val category = CalcCategory.SURVEYING
    override val name = "Differential Leveling & Slope Grade"
    override val shortDescription = "Height of Instrument (HI), Reduced Level (RL), arithmetic closure check, and slope/grade between stations."
    override val nature = CalculationNature.EXACT_GEOMETRY_CONVERSION

    override val supportedStandards = listOf(
        DesignStandardSpec(
            id = "PLANE_SURVEY_LEVEL",
            name = "Plane Differential Leveling (HI Method)",
            edition = "Standard Field Reduction",
            methodSummary = "HI = RL_BM + ΣBS_step ; RL = HI − FS ; Arithmetic check: ΣBS − ΣFS = RL_end − RL_start.",
            family = StandardFamily.ACI_ASCE
        )
    )

    override val scopeAndLimitations = listOf(
        "Plane surveying reduction without earth curvature or atmospheric refraction corrections.",
        "Accepts comma-separated Backsight (BS) and Foresight (FS) turning point series of equal count.",
        "Misclosure tolerance is evaluated against C·√K (mm or ft, where K is total loop distance in km or miles)."
    )

    override val engineeringNotes = listOf(
        "Always check ΣBS − ΣFS = RL_last − RL_first before accepting field notes.",
        "Balance BS and FS sight distances in the field to minimize collimation error."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("bm_rl", "Starting Benchmark RL", "m", "ft", "Known elevation of starting station", minInclusive = -1000.0, allowZero = true),
        InputFieldSpec("bs_series", "Backsights (BS, comma-separated)", "m", "ft", "e.g., 1.425, 1.810", valueType = InputValueType.TEXT_SERIES),
        InputFieldSpec("fs_series", "Foresights (FS, comma-separated)", "m", "ft", "Same count as BS, e.g., 1.120, 0.945", valueType = InputValueType.TEXT_SERIES),
        InputFieldSpec("horiz_dist", "Horizontal Distance (Dh)", "m", "ft", "Horizontal distance between start and end for slope/grade", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("known_close_rl", "Known Closing RL (Optional)", "m", "ft", "Leave blank if open traverse; enter closing BM RL to check misclosure", required = false, allowZero = true)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("closure_const", "Allowable Misclosure Coeff (C·√K)", if (unitSystem == UnitSystem.SI) "12.0" else "0.05", if (unitSystem == UnitSystem.SI) "mm/√km" else "ft/√mi", "Third-order leveling typical: 12 mm·√K (SI) or 0.05 ft·√mi (US).")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("bm_rl" to "100.000", "bs_series" to "1.425, 1.810", "fs_series" to "1.120, 0.945", "horiz_dist" to "120.0", "known_close_rl" to "")
        } else {
            mapOf("bm_rl" to "328.00", "bs_series" to "4.68, 5.94", "fs_series" to "3.67, 3.10", "horiz_dist" to "400.0", "known_close_rl" to "")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_level_si",
            title = "Two-Setup Benchmark Transfer (SI)",
            description = "Start BM = 100.000 m, BS=[1.425, 1.810], FS=[1.120, 0.945], Dh=120 m.",
            unitSystem = UnitSystem.SI,
            standardId = "PLANE_SURVEY_LEVEL",
            inputs = mapOf("bm_rl" to "100.000", "bs_series" to "1.425, 1.810", "fs_series" to "1.120, 0.945", "horiz_dist" to "120.0", "known_close_rl" to "101.175"),
            expectedNote = "End RL = 101.170 m, ΔH = +1.170 m, Grade = +0.975%, Misclosure = -5 mm"
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

        val bm = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val dh = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val closeRl = CalcFmt.parseDoubleInput(rawInputs, specs[4], unitSystem, issues)
        val cTol = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], if (unitSystem == UnitSystem.SI) 1.0 else 0.001, 100.0, issues)

        val bsList = parseNumberSeries(rawInputs["bs_series"], "bs_series", "Backsights", issues)
        val fsList = parseNumberSeries(rawInputs["fs_series"], "fs_series", "Foresights", issues)

        if (bsList != null && fsList != null && bsList.size != fsList.size) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "fs_series", "BS/FS Count Mismatch", "Number of Backsights (${bsList.size}) must equal Foresights (${fsList.size})."))
        }

        if (issues.any { it.severity == IssueSeverity.ERROR } || bm == null || dh == null || bsList == null || fsList == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("closure_const" to cTol.toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        val lenUnit = if (unitSystem == UnitSystem.SI) "m" else "ft"
        val startRl: Double = bm
        var currentRl: Double = startRl
        val hiList = mutableListOf<Double>()
        for (idx in bsList.indices) {
            val hi = currentRl + bsList[idx]
            hiList.add(hi)
            currentRl = hi - fsList[idx]
        }

        val sumBs = bsList.sum()
        val sumFs = fsList.sum()
        val deltaH = sumBs - sumFs
        val endRl = currentRl
        val gradePct = (deltaH / dh) * 100.0
        val slopeAngleDeg = Math.toDegrees(atan(deltaH / dh))
        val slopeDist = hypot(dh, deltaH)

        var misclosureCheck = "Open traverse (no closing BM provided)"
        var util: Double? = null
        if (closeRl != null) {
            val misclosure = endRl - closeRl
            val kDist = if (unitSystem == UnitSystem.SI) (dh / 1000.0).coerceAtLeast(0.01) else (dh / 5280.0).coerceAtLeast(0.01)
            val allow = if (unitSystem == UnitSystem.SI) cTol * sqrt(kDist) / 1000.0 else cTol * sqrt(kDist)
            util = abs(misclosure) / allow
            val mDisp = if (unitSystem == UnitSystem.SI) "${CalcFmt.num(misclosure * 1000.0, 1)} mm" else "${CalcFmt.num(misclosure, 3)} ft"
            val aDisp = if (unitSystem == UnitSystem.SI) "${CalcFmt.num(allow * 1000.0, 1)} mm" else "${CalcFmt.num(allow, 3)} ft"
            misclosureCheck = "Misclosure = $mDisp vs Allow = ±$aDisp"
            if (util > 1.0) {
                issues.add(ValidationIssue(IssueSeverity.ERROR, "known_close_rl", "Leveling Misclosure Exceeds Tolerance", "$misclosureCheck (Ratio ${CalcFmt.num(util, 2)})."))
            }
        }

        val status = when {
            issues.any { it.severity == IssueSeverity.ERROR } -> CalcStatus.FAIL
            closeRl != null -> CalcStatus.PASS
            else -> CalcStatus.PASS
        }

        val steps = listOf(
            CalcStep(1, "Instrument Heights & Turning Points", "HI_i = RL_(i-1) + BS_i ; RL_i = HI_i − FS_i", listOf("Setups = ${bsList.size}", "HI values = ${hiList.joinToString { CalcFmt.num(it, 3) }}"), "ΣBS = ${CalcFmt.num(sumBs, 3)} $lenUnit ; ΣFS = ${CalcFmt.num(sumFs, 3)} $lenUnit", "RL_end = ${CalcFmt.num(endRl, 3)} $lenUnit"),
            CalcStep(2, "Arithmetic Check & Elevation Change", "ΣBS − ΣFS = RL_end − RL_start", listOf("RL_start = ${CalcFmt.num(bm, 3)} $lenUnit"), "${CalcFmt.num(sumBs, 3)} − ${CalcFmt.num(sumFs, 3)} = ${CalcFmt.num(endRl - bm, 3)}", "ΔH = ${CalcFmt.num(deltaH, 3)} $lenUnit ($misclosureCheck)"),
            CalcStep(3, "Grade & Slope Distance", "Grade% = (ΔH / Dh) × 100 ; S_dist = √(Dh² + ΔH²)", listOf("Dh = ${CalcFmt.smart(dh)} $lenUnit"), "Grade = (${CalcFmt.num(deltaH, 3)} / ${CalcFmt.smart(dh)}) × 100", "${CalcFmt.num(gradePct, 3)}% (${CalcFmt.num(slopeAngleDeg, 2)}°)")
        )

        return CalcResult(
            moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
            status = status, governingCheck = "End RL = ${CalcFmt.num(endRl, 3)} $lenUnit | $misclosureCheck", utilizationRatio = util,
            summaryMetrics = listOf(
                OutputMetric("Final Elevation (RL)", CalcFmt.num(endRl, 3), lenUnit, endRl, highlight = true),
                OutputMetric("Elevation Change (ΔH)", CalcFmt.num(deltaH, 3), lenUnit, deltaH),
                OutputMetric("Average Grade", CalcFmt.num(gradePct, 3), "%", gradePct, highlight = true),
                OutputMetric("Slope Distance", CalcFmt.num(slopeDist, 3), lenUnit, slopeDist)
            ),
            steps = steps, issues = issues,
            resolvedAssumptions = mapOf("closure_const" to CalcFmt.smart(cTol)),
            scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
        )
    }

    private fun parseNumberSeries(
        raw: String?,
        fieldKey: String,
        label: String,
        issues: MutableList<ValidationIssue>
    ): List<Double>? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, fieldKey, "Missing $label", "Enter comma-separated numeric readings for $label."))
            return null
        }
        val parts = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, fieldKey, "Empty $label", "Provide at least one reading."))
            return null
        }
        val nums = mutableListOf<Double>()
        for (p in parts) {
            val v = p.toDoubleOrNull()
            if (v == null || v.isNaN() || v.isInfinite()) {
                issues.add(ValidationIssue(IssueSeverity.ERROR, fieldKey, "Invalid Reading in $label", "\"$p\" is not a valid number."))
                return null
            }
            nums.add(v)
        }
        return nums
    }
}

class SurveyingAreaVolumeCalcModule : CalcModule {
    override val id = CalcModuleId.SURVEYING_AREA_VOLUME
    override val category = CalcCategory.SURVEYING
    override val name = "Coordinate Area & Earthwork Volume"
    override val shortDescription = "Exact Gauss Shoelace polygon area from (E, N) vertices and Average End Area cut/fill volume."
    override val nature = CalculationNature.EXACT_GEOMETRY_CONVERSION

    override val supportedStandards = listOf(
        DesignStandardSpec(
            id = "SHOELACE_AEA",
            name = "Shoelace Area & Average End Area Volume",
            edition = "Plane Surveying / Route Earthwork",
            methodSummary = "A = ½ |Σ(x_i·y_(i+1) − x_(i+1)·y_i)| ; V = ((A1 + A2) / 2) × L.",
            family = StandardFamily.ACI_ASCE
        )
    )

    override val scopeAndLimitations = listOf(
        "Coordinate area assumes a simple non-self-intersecting plane polygon with ≥ 3 vertices.",
        "Average End Area overestimates volume when A1 and A2 differ sharply (use prismoidal correction if A_max/A_min > 2)."
    )

    override val engineeringNotes = listOf(
        "Enter polygon vertices in order (clockwise or counter-clockwise) as 'E1,N1; E2,N2; ...'."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("coords", "Polygon Vertices (E,N; ...)", "m", "ft", "Semicolon-separated pairs, e.g. 0,0; 50,0; 50,30; 0,30", valueType = InputValueType.TEXT_SERIES),
        InputFieldSpec("area_1", "Cross-Section Area A1", "m²", "ft²", "Cut or fill area at Station 1", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("area_2", "Cross-Section Area A2", "m²", "ft²", "Cut or fill area at Station 2", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("station_len", "Station Spacing (L)", "m", "ft", "Horizontal distance between A1 and A2", minInclusive = 0.0, allowZero = false)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("shrink_factor", "Compaction / Shrinkage Factor", "1.00", "factor", "Multiplier applied to earthwork volume (e.g., 0.90 for 10% compaction shrinkage).")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("coords" to "0,0; 50,0; 60,40; 10,40", "area_1" to "18.5", "area_2" to "24.0", "station_len" to "25.0")
        } else {
            mapOf("coords" to "0,0; 150,0; 180,120; 30,120", "area_1" to "200", "area_2" to "260", "station_len" to "100")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_survey_poly_si",
            title = "Parallelogram Parcel & 25 m Roadway Cut",
            description = "Vertices (0,0)-(50,0)-(60,40)-(10,40), A1=18.5 m², A2=24.0 m², L=25 m.",
            unitSystem = UnitSystem.SI,
            standardId = "SHOELACE_AEA",
            inputs = mapOf("coords" to "0,0; 50,0; 60,40; 10,40", "area_1" to "18.5", "area_2" to "24.0", "station_len" to "25.0"),
            expectedNote = "Polygon Area = 2000.0 m² (0.200 ha), Volume = 531.25 m³"
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

        val a1 = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val a2 = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val len = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val factor = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 0.5, 1.5, issues)
        val pts = parseVertices(rawInputs["coords"], issues)

        if (issues.any { it.severity == IssueSeverity.ERROR } || a1 == null || a2 == null || len == null || pts == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("shrink_factor" to factor.toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        val xList = pts.map { it.first }
        val yList = pts.map { it.second }
        val sum = EngineeringKernel.shoelaceDoubleSignedArea(xList, yList)
        val perimeter = EngineeringKernel.polygonPerimeter(xList, yList)
        val polyArea = 0.5 * abs(sum)
        val volAdj = EngineeringKernel.averageEndAreaVolume(a1, a2, len, factor)

        val minA = minOf(a1, a2)
        val maxA = maxOf(a1, a2)
        if (minA > 0.0 && maxA / minA > 2.0) {
            issues.add(ValidationIssue(IssueSeverity.WARNING, "area_2", "Large Section Transition (A_max/A_min > 2)", "Average End Area may overestimate volume; apply prismoidal formula for sharper accuracy."))
        }

        val lenUnit = if (unitSystem == UnitSystem.SI) "m" else "ft"
        val areaUnit = if (unitSystem == UnitSystem.SI) "m²" else "ft²"
        val altAreaUnit = if (unitSystem == UnitSystem.SI) "ha" else "acres"
        val altArea = if (unitSystem == UnitSystem.SI) polyArea / 10000.0 else polyArea / 43560.0
        val volUnit = if (unitSystem == UnitSystem.SI) "m³" else "yd³"
        val volDisp = if (unitSystem == UnitSystem.SI) volAdj else volAdj / 27.0

        val steps = listOf(
            CalcStep(1, "Shoelace Polygon Area (${pts.size} vertices)", "A = ½ |Σ(Ei·N(i+1) − E(i+1)·Ni)|", listOf("Vertices = ${pts.size}", "Perimeter = ${CalcFmt.num(perimeter, 2)} $lenUnit"), "2·A = ${CalcFmt.num(abs(sum), 2)}", "${CalcFmt.num(polyArea, 2)} $areaUnit (${CalcFmt.num(altArea, 4)} $altAreaUnit)"),
            CalcStep(2, "Average End Area Earthwork Volume", "V = ((A1 + A2) / 2) × L × factor", listOf("A1=${CalcFmt.smart(a1)}", "A2=${CalcFmt.smart(a2)}", "L=${CalcFmt.smart(len)}", "factor=${CalcFmt.num(factor, 2)}"), "V = ((${CalcFmt.smart(a1)} + ${CalcFmt.smart(a2)}) / 2) × ${CalcFmt.smart(len)} × ${CalcFmt.num(factor, 2)}", "${CalcFmt.num(volDisp, 2)} $volUnit", CalcFmt.rawNote(volDisp, volUnit))
        )

        val status = if (issues.any { it.severity == IssueSeverity.WARNING }) CalcStatus.WARNING else CalcStatus.PASS

        return CalcResult(
            moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
            status = status, governingCheck = "Area = ${CalcFmt.num(polyArea, 2)} $areaUnit | Vol = ${CalcFmt.num(volDisp, 2)} $volUnit", utilizationRatio = null,
            summaryMetrics = listOf(
                OutputMetric("Polygon Area", CalcFmt.num(polyArea, 2), areaUnit, polyArea, highlight = true),
                OutputMetric("Area ($altAreaUnit)", CalcFmt.num(altArea, 4), altAreaUnit, altArea),
                OutputMetric("Perimeter", CalcFmt.num(perimeter, 2), lenUnit, perimeter),
                OutputMetric("Earthwork Volume", CalcFmt.num(volDisp, 2), volUnit, volDisp, highlight = true)
            ),
            steps = steps, issues = issues,
            resolvedAssumptions = mapOf("shrink_factor" to CalcFmt.num(factor, 2)),
            scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
        )
    }

    private fun parseVertices(raw: String?, issues: MutableList<ValidationIssue>): List<Pair<Double, Double>>? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "coords", "Missing Coordinates", "Enter semicolon-separated E,N pairs."))
            return null
        }
        val pairs = text.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        if (pairs.size < 3) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "coords", "Insufficient Vertices", "At least 3 vertices are required for a closed polygon."))
            return null
        }
        val pts = mutableListOf<Pair<Double, Double>>()
        for (pair in pairs) {
            val xy = pair.split(",").map { it.trim() }
            if (xy.size != 2) {
                issues.add(ValidationIssue(IssueSeverity.ERROR, "coords", "Invalid Vertex Syntax", "Use 'E,N' format for \"$pair\"."))
                return null
            }
            val x = xy[0].toDoubleOrNull()
            val y = xy[1].toDoubleOrNull()
            if (x == null || y == null || x.isNaN() || y.isNaN() || x.isInfinite() || y.isInfinite()) {
                issues.add(ValidationIssue(IssueSeverity.ERROR, "coords", "Non-Numeric Coordinate", "Invalid numbers in \"$pair\"."))
                return null
            }
            pts.add(x to y)
        }
        return pts
    }
}
