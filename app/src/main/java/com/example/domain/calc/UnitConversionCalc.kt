package com.example.domain.calc

import kotlin.math.abs

data class UnitDef(
    val symbol: String,
    val label: String,
    val category: String,
    val toBaseFactor: Double // converts 1 unit -> base SI unit (m, m2, m3, N, Pa, m3/s, kN/m3)
)

object UnitCatalog {
    val categories = listOf(
        "Length",
        "Area",
        "Volume",
        "Force",
        "Stress / Pressure",
        "Flow Rate",
        "Unit Weight"
    )

    val units: List<UnitDef> = listOf(
        // Length (base: m)
        UnitDef("m", "Meter (m)", "Length", 1.0),
        UnitDef("mm", "Millimeter (mm)", "Length", 0.001),
        UnitDef("cm", "Centimeter (cm)", "Length", 0.01),
        UnitDef("km", "Kilometer (km)", "Length", 1000.0),
        UnitDef("ft", "Foot (ft)", "Length", 0.3048),
        UnitDef("in", "Inch (in)", "Length", 0.0254),
        UnitDef("yd", "Yard (yd)", "Length", 0.9144),

        // Area (base: m2)
        UnitDef("m2", "Square Meter (m²)", "Area", 1.0),
        UnitDef("mm2", "Square Millimeter (mm²)", "Area", 1e-6),
        UnitDef("ha", "Hectare (ha)", "Area", 10000.0),
        UnitDef("ft2", "Square Foot (ft²)", "Area", 0.09290304),
        UnitDef("in2", "Square Inch (in²)", "Area", 0.00064516),
        UnitDef("acre", "Acre (ac)", "Area", 4046.8564224),

        // Volume (base: m3)
        UnitDef("m3", "Cubic Meter (m³)", "Volume", 1.0),
        UnitDef("L", "Liter (L)", "Volume", 0.001),
        UnitDef("ft3", "Cubic Foot (ft³)", "Volume", 0.028316846592),
        UnitDef("yd3", "Cubic Yard (yd³)", "Volume", 0.764554857984),
        UnitDef("gal_US", "US Gallon (gal)", "Volume", 0.003785411784),

        // Force (base: N)
        UnitDef("N", "Newton (N)", "Force", 1.0),
        UnitDef("kN", "Kilonewton (kN)", "Force", 1000.0),
        UnitDef("MN", "Meganewton (MN)", "Force", 1e6),
        UnitDef("lbf", "Pound-force (lbf)", "Force", 4.4482216152605),
        UnitDef("kip", "Kilopound (kip)", "Force", 4448.2216152605),

        // Stress / Pressure (base: Pa)
        UnitDef("Pa", "Pascal (Pa)", "Stress / Pressure", 1.0),
        UnitDef("kPa", "Kilopascal (kPa)", "Stress / Pressure", 1000.0),
        UnitDef("MPa", "Megapascal (MPa)", "Stress / Pressure", 1e6),
        UnitDef("psi", "psi (lbf/in²)", "Stress / Pressure", 6894.757293168361),
        UnitDef("ksi", "ksi (kip/in²)", "Stress / Pressure", 6894757.293168361),
        UnitDef("ksf", "ksf (kip/ft²)", "Stress / Pressure", 47880.25898033584),

        // Flow Rate (base: m3/s)
        UnitDef("m3/s", "m³/s (cumecs)", "Flow Rate", 1.0),
        UnitDef("L/s", "L/s", "Flow Rate", 0.001),
        UnitDef("cfs", "ft³/s (cfs)", "Flow Rate", 0.028316846592),
        UnitDef("gpm", "US gal/min (gpm)", "Flow Rate", 0.003785411784 / 60.0),

        // Unit Weight (base: kN/m3)
        UnitDef("kN/m3", "kN/m³", "Unit Weight", 1.0),
        UnitDef("pcf", "lb/ft³ (pcf)", "Unit Weight", 0.1570874638462462)
    )

    val allUnitSymbols: List<String> = units.map { it.symbol }

    fun find(symbol: String): UnitDef? = units.firstOrNull { it.symbol == symbol }
}

class UnitConversionCalcModule : CalcModule {
    override val id = CalcModuleId.UNIT_CONVERSION
    override val category = CalcCategory.UNITS_TAKEOFF
    override val name = "Engineering Unit Converter"
    override val shortDescription = "Exact NIST/SI conversions for length, area, volume, force, stress, flow, and unit weight."
    override val nature = CalculationNature.EXACT_GEOMETRY_CONVERSION

    override val supportedStandards = listOf(
        DesignStandardSpec(
            id = "NIST_SP811",
            name = "NIST SP 811 / ISO 80000",
            edition = "SI Exact Factors",
            methodSummary = "Exact international foot (0.3048 m), standard gravity (9.80665 m/s²), and SI derived units.",
            family = StandardFamily.ACI_ASCE
        )
    )

    override val scopeAndLimitations = listOf(
        "Covers linear proportional unit conversions across structural, geotechnical, and hydraulic quantities.",
        "Standard gravity g = 9.80665 m/s² is used to relate mass-force conversions (lbf/kip to N/kN).",
        "Temperature offset scales and empirical hardness conversions are outside this module's scope."
    )

    override val engineeringNotes = listOf(
        "Verify stress vs. force units carefully (e.g., ksf vs. ksi vs. kPa) when transferring loads between drawings.",
        "Internal computation retains full IEEE-754 double precision; displayed values are rounded consistently."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec(
            key = "category",
            label = "Quantity Category",
            siUnit = "-",
            usUnit = "-",
            helperText = "Physical dimension group",
            valueType = InputValueType.CHOICE,
            choices = UnitCatalog.categories
        ),
        InputFieldSpec(
            key = "input_value",
            label = "Input Value",
            siUnit = "from unit",
            usUnit = "from unit",
            helperText = "Numeric magnitude to convert"
        ),
        InputFieldSpec(
            key = "from_unit",
            label = "From Unit",
            siUnit = "-",
            usUnit = "-",
            helperText = "Source unit symbol",
            valueType = InputValueType.CHOICE,
            choices = UnitCatalog.allUnitSymbols
        ),
        InputFieldSpec(
            key = "to_unit",
            label = "To Unit",
            siUnit = "-",
            usUnit = "-",
            helperText = "Target unit symbol",
            valueType = InputValueType.CHOICE,
            choices = UnitCatalog.allUnitSymbols
        )
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec(
            key = "display_decimals",
            label = "Display Decimal Places",
            defaultValue = "4",
            unit = "digits",
            description = "Number of decimal places for formatted output (0 to 8). Internal double precision is preserved."
        )
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("category" to "Stress / Pressure", "input_value" to "28.0", "from_unit" to "MPa", "to_unit" to "psi")
        } else {
            mapOf("category" to "Stress / Pressure", "input_value" to "4000.0", "from_unit" to "psi", "to_unit" to "MPa")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_fc_mpa_psi",
            title = "Concrete Strength 28 MPa to psi",
            description = "Standard 28 MPa concrete cylinder strength converted to US customary psi.",
            unitSystem = UnitSystem.SI,
            standardId = "NIST_SP811",
            inputs = mapOf("category" to "Stress / Pressure", "input_value" to "28.0", "from_unit" to "MPa", "to_unit" to "psi"),
            expectedNote = "28 MPa = 4061.06 psi"
        ),
        ExampleCase(
            id = "ex_flow_cfs_m3s",
            title = "Storm Runoff 120 cfs to m³/s",
            description = "Convert drainage discharge from cubic feet per second to cumecs.",
            unitSystem = UnitSystem.US,
            standardId = "NIST_SP811",
            inputs = mapOf("category" to "Flow Rate", "input_value" to "120.0", "from_unit" to "cfs", "to_unit" to "m3/s"),
            expectedNote = "120 cfs = 3.3980 m³/s"
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
        val specs = inputSpecs(unitSystem, standardId)
        val aSpecs = assumptionSpecs(unitSystem, standardId)

        val decimals = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 0.0, 8.0, issues).toInt().coerceIn(0, 8)
        val cat = rawInputs["category"]?.trim().orEmpty()
        val fromSym = rawInputs["from_unit"]?.trim().orEmpty()
        val toSym = rawInputs["to_unit"]?.trim().orEmpty()
        val value = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)

        val fromDef = UnitCatalog.find(fromSym)
        val toDef = UnitCatalog.find(toSym)

        if (fromDef == null) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "from_unit", "Unknown Source Unit", "\"$fromSym\" is not a supported unit."))
        }
        if (toDef == null) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "to_unit", "Unknown Target Unit", "\"$toSym\" is not a supported unit."))
        }
        if (fromDef != null && toDef != null && fromDef.category != toDef.category) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.ERROR,
                    "to_unit",
                    "Incompatible Dimensions",
                    "Cannot convert ${fromDef.label} (${fromDef.category}) to ${toDef.label} (${toDef.category}). Select units from the same physical category."
                )
            )
        } else if (fromDef != null && cat.isNotEmpty() && fromDef.category != cat) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.INFO,
                    "category",
                    "Category Sync Notice",
                    "Units belong to ${fromDef.category}; evaluation used ${fromDef.category} exact factors."
                )
            )
        }

        if (issues.any { it.severity == IssueSeverity.ERROR } || value == null || fromDef == null || toDef == null) {
            return CalcResult(
                moduleId = id,
                moduleName = name,
                nature = nature,
                unitSystem = unitSystem,
                standard = std,
                status = CalcStatus.FAIL,
                governingCheck = "Input validation failed",
                utilizationRatio = null,
                summaryMetrics = emptyList(),
                steps = emptyList(),
                issues = issues,
                resolvedAssumptions = mapOf("display_decimals" to decimals.toString()),
                scopeAndLimitations = scopeAndLimitations,
                engineeringNotes = engineeringNotes
            )
        }

        val factor = fromDef.toBaseFactor / toDef.toBaseFactor
        val baseValue = value * fromDef.toBaseFactor
        val converted = EngineeringKernel.convertViaBase(value, fromDef.toBaseFactor, toDef.toBaseFactor)

        val steps = listOf(
            CalcStep(
                stepNumber = 1,
                title = "Convert Input to SI Base Unit",
                equation = "x_base = x_in × f_toBase",
                variables = listOf("x_in = input magnitude (${fromDef.symbol})", "f_toBase = exact factor to SI base"),
                substituted = "x_base = ${CalcFmt.smart(value)} × ${String.format(java.util.Locale.US, "%.8g", fromDef.toBaseFactor)}",
                resultFormatted = "${String.format(java.util.Locale.US, "%.8g", baseValue)} (SI base)"
            ),
            CalcStep(
                stepNumber = 2,
                title = "Convert SI Base Unit to Target Unit",
                equation = "x_out = x_base / f_target = x_in × (f_toBase / f_target)",
                variables = listOf("f_target = exact factor of ${toDef.symbol}", "net multiplier = ${String.format(java.util.Locale.US, "%.8g", factor)}"),
                substituted = "x_out = ${CalcFmt.smart(value)} × ${String.format(java.util.Locale.US, "%.8g", factor)}",
                resultFormatted = "${CalcFmt.num(converted, decimals)} ${toDef.symbol}",
                rawPrecisionNote = CalcFmt.rawNote(converted, toDef.symbol)
            )
        )

        return CalcResult(
            moduleId = id,
            moduleName = name,
            nature = nature,
            unitSystem = unitSystem,
            standard = std,
            status = CalcStatus.PASS,
            governingCheck = "1 ${fromDef.symbol} = ${String.format(java.util.Locale.US, "%.6g", factor)} ${toDef.symbol}",
            utilizationRatio = null,
            summaryMetrics = listOf(
                OutputMetric("Converted Value", CalcFmt.num(converted, decimals), toDef.symbol, converted, highlight = true),
                OutputMetric("Conversion Factor", String.format(java.util.Locale.US, "%.6g", factor), "${toDef.symbol}/${fromDef.symbol}", factor)
            ),
            steps = steps,
            issues = issues,
            resolvedAssumptions = mapOf("display_decimals" to decimals.toString()),
            scopeAndLimitations = scopeAndLimitations,
            engineeringNotes = engineeringNotes
        )
    }
}

class QuantityTakeoffCalcModule : CalcModule {
    override val id = CalcModuleId.QUANTITY_TAKEOFF
    override val category = CalcCategory.UNITS_TAKEOFF
    override val name = "Earthwork & Geometric Quantity Takeoff"
    override val shortDescription = "Trapezoidal trench, footing, or slab prism volume and area takeoff with editable waste & swell."
    override val nature = CalculationNature.EXACT_GEOMETRY_CONVERSION

    override val supportedStandards = listOf(
        DesignStandardSpec(
            id = "CSI_TAKEOFF",
            name = "Standard Geometric Prismoidal / Trapezoidal Takeoff",
            edition = "Field Quantity Practice",
            methodSummary = "Exact cross-sectional area A = (b_bottom + b_top)/2 × d multiplied by length and count, adjusted by waste & swell.",
            family = StandardFamily.ACI_ASCE
        )
    )

    override val scopeAndLimitations = listOf(
        "Applies to uniform rectangular or trapezoidal prismatic trenches, channels, walls, and slabs.",
        "For rectangular members (slabs, strip footings), set Top Width equal to Bottom Width.",
        "Does not model curved alignments, step transitions, or rock-bulking testing."
    )

    override val engineeringNotes = listOf(
        "Typical concrete placement waste ranges from 3% to 8%; excavation haul swell typically ranges from 15% to 30% depending on soil type.",
        "Always confirm trench side-slope stability against local OSHA/safety shoring requirements."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("b_bottom", "Bottom Width (b1)", "m", "ft", "Base width of trench or slab", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("b_top", "Top Width (b2)", "m", "ft", "Top width (equals b1 for rectangular)", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("depth", "Depth / Thickness (d)", "m", "ft", "Vertical depth or slab thickness", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("length", "Length (L)", "m", "ft", "Run length per element", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("count", "Element Count (N)", "nos", "nos", "Number of identical elements", minInclusive = 1.0, allowZero = false)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("waste_pct", "Material Waste / Overbreak", "5.0", "%", "Explicit allowance for over-excavation or placement loss (0–50%)."),
        AssumptionSpec("swell_pct", "Soil Swell / Bulking Factor", "0.0", "%", "0% for bank/in-situ volume; 15–30% for loose haul volume (0–60%).")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("b_bottom" to "1.20", "b_top" to "2.40", "depth" to "1.50", "length" to "45.0", "count" to "1")
        } else {
            mapOf("b_bottom" to "4.0", "b_top" to "8.0", "depth" to "5.0", "length" to "150.0", "count" to "1")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_trench_si",
            title = "Utility Trench Trapezoidal Excavation",
            description = "1.2 m base, 2.4 m top, 1.5 m deep, 45 m long utility trench with 5% waste and 20% swell.",
            unitSystem = UnitSystem.SI,
            standardId = "CSI_TAKEOFF",
            inputs = mapOf("b_bottom" to "1.20", "b_top" to "2.40", "depth" to "1.50", "length" to "45.0", "count" to "1"),
            assumptions = mapOf("waste_pct" to "5.0", "swell_pct" to "20.0"),
            expectedNote = "Net = 121.50 m³, Loose Gross = 153.09 m³"
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
        val specs = inputSpecs(unitSystem, standardId)
        val aSpecs = assumptionSpecs(unitSystem, standardId)

        val b1 = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val b2 = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val d = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val len = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val n = CalcFmt.parseDoubleInput(rawInputs, specs[4], unitSystem, issues)

        val wastePct = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 0.0, 50.0, issues)
        val swellPct = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[1], 0.0, 60.0, issues)

        if (b1 != null && b2 != null && b2 < b1) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.WARNING,
                    "b_top",
                    "Undercut Geometry",
                    "Top width (${CalcFmt.smart(b2)}) is narrower than bottom width (${CalcFmt.smart(b1)}). Confirm undercut geometry."
                )
            )
        }

        if (issues.any { it.severity == IssueSeverity.ERROR } || b1 == null || b2 == null || d == null || len == null || n == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("waste_pct" to CalcFmt.num(wastePct, 1), "swell_pct" to CalcFmt.num(swellPct, 1)),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        val lenUnit = if (unitSystem == UnitSystem.SI) "m" else "ft"
        val areaUnit = if (unitSystem == UnitSystem.SI) "m²" else "ft²"
        val volUnit = if (unitSystem == UnitSystem.SI) "m³" else "ft³"

        val areaCs = EngineeringKernel.trapezoidalArea(b1, b2, d)
        val netVol = areaCs * len * n
        val grossVol = EngineeringKernel.adjustedGrossVolume(netVol, wastePct, swellPct)
        val topArea = b2 * len * n
        val yd3OrLiters = if (unitSystem == UnitSystem.SI) grossVol * 1000.0 else grossVol / 27.0
        val altVolUnit = if (unitSystem == UnitSystem.SI) "L" else "yd³"

        val steps = listOf(
            CalcStep(
                stepNumber = 1,
                title = "Trapezoidal Cross-Section Area",
                equation = "A_cs = ((b1 + b2) / 2) × d",
                variables = listOf("b1 = bottom width ($lenUnit)", "b2 = top width ($lenUnit)", "d = depth ($lenUnit)"),
                substituted = "A_cs = ((${CalcFmt.smart(b1)} + ${CalcFmt.smart(b2)}) / 2) × ${CalcFmt.smart(d)}",
                resultFormatted = "${CalcFmt.num(areaCs, 3)} $areaUnit"
            ),
            CalcStep(
                stepNumber = 2,
                title = "Net Geometric Volume",
                equation = "V_net = A_cs × L × N",
                variables = listOf("L = length ($lenUnit)", "N = count"),
                substituted = "V_net = ${CalcFmt.num(areaCs, 4)} × ${CalcFmt.smart(len)} × ${CalcFmt.num(n, 0)}",
                resultFormatted = "${CalcFmt.num(netVol, 2)} $volUnit",
                rawPrecisionNote = CalcFmt.rawNote(netVol, volUnit)
            ),
            CalcStep(
                stepNumber = 3,
                title = "Gross Adjusted Volume (Waste & Swell)",
                equation = "V_gross = V_net × (1 + waste/100) × (1 + swell/100)",
                variables = listOf("waste = ${CalcFmt.num(wastePct, 1)}%", "swell = ${CalcFmt.num(swellPct, 1)}%"),
                substituted = "V_gross = ${CalcFmt.num(netVol, 3)} × ${CalcFmt.num(1.0 + wastePct / 100.0, 3)} × ${CalcFmt.num(1.0 + swellPct / 100.0, 3)}",
                resultFormatted = "${CalcFmt.num(grossVol, 2)} $volUnit (${CalcFmt.num(yd3OrLiters, 2)} $altVolUnit)"
            )
        )

        return CalcResult(
            moduleId = id,
            moduleName = name,
            nature = nature,
            unitSystem = unitSystem,
            standard = std,
            status = CalcStatus.ESTIMATE,
            governingCheck = "Net = ${CalcFmt.num(netVol, 2)} $volUnit | Gross = ${CalcFmt.num(grossVol, 2)} $volUnit",
            utilizationRatio = null,
            summaryMetrics = listOf(
                OutputMetric("Gross Volume", CalcFmt.num(grossVol, 2), volUnit, grossVol, highlight = true),
                OutputMetric(if (unitSystem == UnitSystem.SI) "Gross Volume (Liters)" else "Gross Volume (cu yd)", CalcFmt.num(yd3OrLiters, 2), altVolUnit, yd3OrLiters),
                OutputMetric("Net Geometric Volume", CalcFmt.num(netVol, 2), volUnit, netVol),
                OutputMetric("Top Plan Area", CalcFmt.num(topArea, 2), areaUnit, topArea)
            ),
            steps = steps,
            issues = issues,
            resolvedAssumptions = mapOf("waste_pct" to CalcFmt.num(wastePct, 1), "swell_pct" to CalcFmt.num(swellPct, 1)),
            scopeAndLimitations = scopeAndLimitations,
            engineeringNotes = engineeringNotes
        )
    }
}
