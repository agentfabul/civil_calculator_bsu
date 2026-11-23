package com.example.domain.calc

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor

class ConcreteRebarCalcModule : CalcModule {
    override val id = CalcModuleId.CONCRETE_REBAR
    override val category = CalcCategory.CONCRETE_REBAR
    override val name = "Concrete & Rebar Quantity Estimate"
    override val shortDescription = "Preliminary wet/dry concrete volume, nominal batch materials (cement bags, sand, aggregate), and rebar mass."
    override val nature = CalculationNature.PRELIMINARY_ESTIMATE

    override val supportedStandards = listOf(
        DesignStandardSpec(
            id = "NOMINAL_VOL_EST",
            name = "Preliminary Nominal Volume Method",
            edition = "Field Batching Guide",
            methodSummary = "Wet geometric volume × dry bulking factor (1.54) proportioned by nominal 1:n:m volume ratio; rebar unit mass = db²/162.2 kg/m.",
            family = StandardFamily.ACI_ASCE
        )
    )

    override val scopeAndLimitations = listOf(
        "PRELIMINARY QUANTITY ESTIMATE ONLY — NOT a structural concrete mix design per ACI 211 or EN 206.",
        "Ready-mix concrete should be ordered by specified compressive strength, slump, exposure class, and wet volume (+waste).",
        "Nominal 1:n:m batch ratios ignore aggregate moisture content, specific gravity variations, and air entrainment.",
        "Rebar estimate calculates straight bars in one direction across the span; orthogonal distribution steel, hooks, and waste require separate takeoff."
    )

    override val engineeringNotes = listOf(
        "Dry volume conversion factor (default 1.54) accounts for mortar filling voids between dry coarse aggregates.",
        "Standard cement bag weights: 50 kg (SI) or 94 lb (1 US bag ≈ 1 ft³)."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("length", "Member Length (L)", "m", "ft", "Span or member length", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("width", "Member Width (W)", "m", "ft", "Cross-section or slab width", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("thickness", "Member Thickness (D)", "m", "ft", "Depth or slab thickness", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("count", "Member Count (N)", "nos", "nos", "Number of identical members", minInclusive = 1.0, allowZero = false),
        InputFieldSpec("sand_part", "Mix Sand Ratio (n in 1:n:m)", "part", "part", "Fine aggregate volume parts (e.g. 2.0 for 1:2:4)", minInclusive = 0.5, allowZero = false),
        InputFieldSpec("agg_part", "Mix Aggregate Ratio (m in 1:n:m)", "part", "part", "Coarse aggregate volume parts (e.g. 4.0 for 1:2:4)", minInclusive = 1.0, allowZero = false),
        InputFieldSpec("bar_dia", "Rebar Diameter (db)", "mm", "in", "Nominal bar diameter (e.g. 16 mm or 0.625 in)", minInclusive = 6.0.let { if (unitSystem == UnitSystem.SI) 6.0 else 0.25 }, allowZero = false),
        InputFieldSpec("bar_spacing", "Bar Spacing c/c (s)", "mm", "in", "Center-to-center bar spacing across width", minInclusive = 1.0, allowZero = false)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("waste_pct", "Concrete & Rebar Waste Allowance", "5.0", "%", "Placement and cutting waste (0–25%)."),
        AssumptionSpec("dry_factor", "Concrete Dry Volume Factor", "1.54", "factor", "Ratio of dry loose ingredients to compacted wet concrete (1.50–1.60)."),
        AssumptionSpec("cover", "End Concrete Cover", if (unitSystem == UnitSystem.SI) "40" else "1.5", if (unitSystem == UnitSystem.SI) "mm" else "in", "Clear end cover deducted from bar length and spacing distribution."),
        AssumptionSpec("lap_pct", "Lap / Hook Allowance", "10.0", "%", "Additional steel length allowance for splices & hooks (0–40%).")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("length" to "6.0", "width" to "4.0", "thickness" to "0.20", "count" to "1", "sand_part" to "1.5", "agg_part" to "3.0", "bar_dia" to "12", "bar_spacing" to "200")
        } else {
            mapOf("length" to "20.0", "width" to "12.0", "thickness" to "0.67", "count" to "1", "sand_part" to "1.5", "agg_part" to "3.0", "bar_dia" to "0.50", "bar_spacing" to "8.0")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_slab_mat_si",
            title = "6m x 4m x 200mm Slab (1:1.5:3 Mix, T12 @ 200)",
            description = "Preliminary concrete ingredients and primary rebar takeoff for a 4.80 m³ slab.",
            unitSystem = UnitSystem.SI,
            standardId = "NOMINAL_VOL_EST",
            inputs = mapOf("length" to "6.0", "width" to "4.0", "thickness" to "0.20", "count" to "1", "sand_part" to "1.5", "agg_part" to "3.0", "bar_dia" to "12", "bar_spacing" to "200"),
            expectedNote = "Wet=4.80 m³ (+5%=5.04 m³), ~44 bags (50 kg), 31 bars"
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

        val l = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val w = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val d = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val n = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val sandPart = CalcFmt.parseDoubleInput(rawInputs, specs[4], unitSystem, issues)
        val aggPart = CalcFmt.parseDoubleInput(rawInputs, specs[5], unitSystem, issues)
        val db = CalcFmt.parseDoubleInput(rawInputs, specs[6], unitSystem, issues)
        val spacing = CalcFmt.parseDoubleInput(rawInputs, specs[7], unitSystem, issues)

        val wastePct = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 0.0, 25.0, issues)
        val dryFactor = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[1], 1.40, 1.65, issues)
        val cover = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[2], 0.0, if (unitSystem == UnitSystem.SI) 150.0 else 6.0, issues)
        val lapPct = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[3], 0.0, 40.0, issues)

        if (issues.any { it.severity == IssueSeverity.ERROR } || l == null || w == null || d == null || n == null || sandPart == null || aggPart == null || db == null || spacing == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("waste_pct" to wastePct.toString(), "dry_factor" to dryFactor.toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        // Internal SI: m, mm, kg
        val lM = if (unitSystem == UnitSystem.SI) l else l * 0.3048
        val wM = if (unitSystem == UnitSystem.SI) w else w * 0.3048
        val dM = if (unitSystem == UnitSystem.SI) d else d * 0.3048
        val dbMm = if (unitSystem == UnitSystem.SI) db else db * 25.4
        val spacingMm = if (unitSystem == UnitSystem.SI) spacing else spacing * 25.4
        val coverMm = if (unitSystem == UnitSystem.SI) cover else cover * 25.4

        val kernelRes = EngineeringKernel.computeConcreteAndRebarSi(
            lM, wM, dM, n.toInt().coerceAtLeast(1),
            sandPart, aggPart, dbMm, spacingMm,
            wastePct, dryFactor, coverMm, lapPct
        )
        val wetWithWasteM3 = kernelRes.wetWithWasteM3
        val dryVolM3 = kernelRes.dryVolM3
        val totalParts = 1.0 + sandPart + aggPart
        val cementMassKg = kernelRes.cementMassKg
        val bags50Kg = kernelRes.bags50Kg
        val bags94Lb = kernelRes.bags94Lb
        val sandVolM3 = kernelRes.sandVolM3
        val aggVolM3 = kernelRes.aggVolM3
        val totalBars = kernelRes.totalBars
        val totalBarLenM = kernelRes.totalBarLenM
        val unitWeightKgPerM = kernelRes.unitWeightKgPerM
        val totalSteelKg = kernelRes.totalSteelKg

        val volUnit = if (unitSystem == UnitSystem.SI) "m³" else "yd³"
        val massUnit = if (unitSystem == UnitSystem.SI) "kg" else "lb"
        val lenUnit = if (unitSystem == UnitSystem.SI) "m" else "ft"

        val wetDisp = if (unitSystem == UnitSystem.SI) wetWithWasteM3 else wetWithWasteM3 / 0.764554858
        val sandDisp = if (unitSystem == UnitSystem.SI) sandVolM3 else sandVolM3 / 0.764554858
        val aggDisp = if (unitSystem == UnitSystem.SI) aggVolM3 else aggVolM3 / 0.764554858
        val bagsDisp = if (unitSystem == UnitSystem.SI) bags50Kg else bags94Lb
        val bagLabel = if (unitSystem == UnitSystem.SI) "bags (50 kg)" else "bags (94 lb)"
        val steelDisp = if (unitSystem == UnitSystem.SI) totalSteelKg else totalSteelKg * 2.20462262
        val steelLenDisp = if (unitSystem == UnitSystem.SI) totalBarLenM else totalBarLenM / 0.3048

        val steps = listOf(
            CalcStep(1, "Wet Concrete Volume (+Waste)", "V_wet = L × W × D × N × (1 + waste%)", listOf("waste = ${CalcFmt.num(wastePct, 1)}%"), "V_wet = ${CalcFmt.num(wetWithWasteM3, 3)} m³", "${CalcFmt.num(wetDisp, 2)} $volUnit"),
            CalcStep(2, "Dry Volume & Mix Constituents (1:${CalcFmt.smart(sandPart)}:${CalcFmt.smart(aggPart)})", "V_dry = V_wet × $dryFactor ; Cement = (V_dry/Σparts) × 1440 kg/m³", listOf("Σparts = ${CalcFmt.num(totalParts, 2)}"), "V_dry = ${CalcFmt.num(dryVolM3, 3)} m³ ; Cement = ${CalcFmt.num(cementMassKg, 1)} kg", "${CalcFmt.num(bagsDisp, 0)} $bagLabel"),
            CalcStep(3, "Rebar Count & Steel Mass", "N_bars = (⌊(W − 2c)/s⌋ + 1)·N ; w = 7850·(π·db²/4)", listOf("Unit wt = ${CalcFmt.num(unitWeightKgPerM, 3)} kg/m", "Total bars = $totalBars"), "Length = ${CalcFmt.num(steelLenDisp, 2)} $lenUnit", "${CalcFmt.num(steelDisp, 2)} $massUnit")
        )

        return CalcResult(
            moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
            status = CalcStatus.ESTIMATE,
            governingCheck = "Preliminary Estimate: ${CalcFmt.num(wetDisp, 2)} $volUnit concrete | ${CalcFmt.num(steelDisp, 1)} $massUnit steel",
            utilizationRatio = null,
            summaryMetrics = listOf(
                OutputMetric("Wet Concrete (+Waste)", CalcFmt.num(wetDisp, 2), volUnit, wetDisp, highlight = true),
                OutputMetric("Cement Required", CalcFmt.num(bagsDisp, 0), bagLabel, bagsDisp, highlight = true),
                OutputMetric("Sand Volume", CalcFmt.num(sandDisp, 2), volUnit, sandDisp),
                OutputMetric("Coarse Aggregate", CalcFmt.num(aggDisp, 2), volUnit, aggDisp),
                OutputMetric("Rebar Count (1-Way)", totalBars.toString(), "bars", totalBars.toDouble()),
                OutputMetric("Rebar Steel Mass", CalcFmt.num(steelDisp, 1), massUnit, steelDisp, highlight = true)
            ),
            steps = steps, issues = issues,
            resolvedAssumptions = mapOf(
                "waste_pct" to CalcFmt.num(wastePct, 1),
                "dry_factor" to CalcFmt.num(dryFactor, 2),
                "cover" to CalcFmt.smart(cover),
                "lap_pct" to CalcFmt.num(lapPct, 1)
            ),
            scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
        )
    }
}
