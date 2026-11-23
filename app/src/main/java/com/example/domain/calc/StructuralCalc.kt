package com.example.domain.calc

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private val ACI_318_STD = DesignStandardSpec(
    id = "ACI_318_19",
    name = "ACI 318-19 / ASCE 7-22",
    edition = "2019 / 2022 Edition",
    methodSummary = "LRFD Strength Design: α1 = 0.85, β1 = 0.65–0.85, φ_flex = 0.90 (tension-controlled), φ_shear = 0.75, φ_comp = 0.65 (tied).",
    family = StandardFamily.ACI_ASCE
)

private val EUROCODE_2_STD = DesignStandardSpec(
    id = "EUROCODE_2",
    name = "Eurocode 2 (EN 1992-1-1)",
    edition = "2004 + A1:2014",
    methodSummary = "Ultimate Limit State: γ_c = 1.50, γ_s = 1.15, α_cc = 0.85, rectangular stress block (λ = 0.80, η = 1.0 for f_ck ≤ 50 MPa).",
    family = StandardFamily.EUROCODE
)

class StructuralBeamCalcModule : CalcModule {
    override val id = CalcModuleId.STRUCTURAL_BEAM
    override val category = CalcCategory.STRUCTURAL
    override val name = "RC Beam: Flexure, Shear & Deflection"
    override val shortDescription = "Simply supported singly reinforced rectangular beam demand (Mu, Vu, Δ) and code capacity check."
    override val nature = CalculationNature.CODE_BASED_CHECK

    override val supportedStandards = listOf(ACI_318_STD, EUROCODE_2_STD)

    override val scopeAndLimitations = listOf(
        "Covers simply supported, singly reinforced rectangular concrete beams under UDL + midspan point load.",
        "Continuous spans, T-beams, doubly reinforced sections, torsion, and development length are excluded.",
        "Shear check evaluates concrete contribution φVc (or simplified EC2 VRd,c); stirrups must be designed separately if Vu > φVc.",
        "Long-term creep/shrinkage deflection multiplier is not included; deflection uses editable I_eff / I_g ratio."
    )

    override val engineeringNotes = listOf(
        "Ensure factored loads (wu, Pu) already reflect governing load combinations before running section checks.",
        "Service deflection check uses unfactored service UDL ws with concrete modulus Ec and effective cracked stiffness."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("span", "Clear Span (L)", "m", "ft", "Center-to-center support span", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("w_u", "Factored Uniform Load (wu)", "kN/m", "kip/ft", "Factored distributed load", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("p_u", "Factored Midspan Point Load (Pu)", "kN", "kip", "Optional concentrated load at L/2", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("w_s", "Service Uniform Load (ws)", "kN/m", "kip/ft", "Unfactored load for deflection check", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("b", "Beam Width (b)", "mm", "in", "Web width", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("d", "Effective Depth (d)", "mm", "in", "Top compression fiber to tension rebar centroid", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("fc", "Concrete Strength (f'c / fck)", "MPa", "ksi", "Cylinder compressive strength", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("fy", "Steel Yield Strength (fy / fyk)", "MPa", "ksi", "Reinforcing bar yield strength", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("as_area", "Tension Steel Area (As)", "mm²", "in²", "Total area of bottom tension bars", minInclusive = 0.0, allowZero = false)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("ieff_ratio", "Effective Stiffness Ratio (Ieff / Ig)", "0.35", "ratio", "Cracked section stiffness ratio per ACI 318-19 Table 6.6.3.1.1(a) (0.20–1.00)."),
        AssumptionSpec("defl_limit_denom", "Deflection Limit Denominator (L / K)", "360", "ratio", "Allowable deflection denominator K (e.g., 240, 360, or 480)."),
        AssumptionSpec("h_over_d", "Total Depth / Effective Depth (h / d)", "1.12", "ratio", "Ratio used to estimate gross Ig = b*h^3/12 from effective depth d (1.05–1.35).")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf(
                "span" to "6.0", "w_u" to "28.0", "p_u" to "0.0", "w_s" to "19.5",
                "b" to "300", "d" to "450", "fc" to "28", "fy" to "420", "as_area" to "1472"
            )
        } else {
            mapOf(
                "span" to "20.0", "w_u" to "2.0", "p_u" to "0.0", "w_s" to "1.4",
                "b" to "12", "d" to "18", "fc" to "4.0", "fy" to "60", "as_area" to "2.37"
            )
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_beam_aci_si",
            title = "6.0 m Office Girder (ACI 318-19, SI)",
            description = "300x500 mm beam (d=450 mm), 3-T25 tension bars (As=1472 mm²), fc'=28 MPa, fy=420 MPa, wu=28 kN/m.",
            unitSystem = UnitSystem.SI,
            standardId = "ACI_318_19",
            inputs = mapOf(
                "span" to "6.0", "w_u" to "28.0", "p_u" to "0.0", "w_s" to "19.5",
                "b" to "300", "d" to "450", "fc" to "28", "fy" to "420", "as_area" to "1472"
            ),
            expectedNote = "Mu = 126.0 kN·m, φMn ≈ 218.2 kN·m (Util ≈ 0.577 PASS)"
        ),
        ExampleCase(
            id = "ex_beam_ec2_si",
            title = "6.0 m Office Girder (Eurocode 2, SI)",
            description = "Same geometry evaluated under EN 1992-1-1 partial factors (γc=1.5, γs=1.15).",
            unitSystem = UnitSystem.SI,
            standardId = "EUROCODE_2",
            inputs = mapOf(
                "span" to "6.0", "w_u" to "28.0", "p_u" to "0.0", "w_s" to "19.5",
                "b" to "300", "d" to "450", "fc" to "28", "fy" to "500", "as_area" to "1472"
            ),
            expectedNote = "Mu = 126.0 kN·m, MRd ≈ 251 kN·m"
        )
    )

    override fun evaluate(
        unitSystem: UnitSystem,
        standardId: String,
        rawInputs: Map<String, String>,
        rawAssumptions: Map<String, String>
    ): CalcResult {
        val issues = mutableListOf<ValidationIssue>()
        val std = supportedStandards.firstOrNull { it.id == standardId } ?: ACI_318_STD
        val specs = inputSpecs(unitSystem, std.id)
        val aSpecs = assumptionSpecs(unitSystem, std.id)

        val L = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val wu = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val pu = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val ws = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val b = CalcFmt.parseDoubleInput(rawInputs, specs[4], unitSystem, issues)
        val d = CalcFmt.parseDoubleInput(rawInputs, specs[5], unitSystem, issues)
        val fc = CalcFmt.parseDoubleInput(rawInputs, specs[6], unitSystem, issues)
        val fy = CalcFmt.parseDoubleInput(rawInputs, specs[7], unitSystem, issues)
        val asArea = CalcFmt.parseDoubleInput(rawInputs, specs[8], unitSystem, issues)

        val iEffRatio = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 0.15, 1.0, issues)
        val deflDenom = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[1], 120.0, 1000.0, issues)
        val hOverD = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[2], 1.02, 1.50, issues)

        if (wu != null && pu != null && wu == 0.0 && pu == 0.0) {
            issues.add(ValidationIssue(IssueSeverity.WARNING, "w_u", "Zero Factored Load", "Both wu and Pu are 0; demand utilization will be 0."))
        }

        if (issues.any { it.severity == IssueSeverity.ERROR } || L == null || wu == null || pu == null || ws == null || b == null || d == null || fc == null || fy == null || asArea == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("ieff_ratio" to iEffRatio.toString(), "defl_limit_denom" to deflDenom.toString(), "h_over_d" to hOverD.toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        // Convert to internal SI consistent base: L(m), wu(kN/m), pu(kN), ws(kN/m), b(mm), d(mm), fc(MPa), fy(MPa), As(mm2)
        val lM = if (unitSystem == UnitSystem.SI) L else L * 0.3048
        val wuKnM = if (unitSystem == UnitSystem.SI) wu else wu * 14.5939029372
        val puKn = if (unitSystem == UnitSystem.SI) pu else pu * 4.44822161526
        val wsKnM = if (unitSystem == UnitSystem.SI) ws else ws * 14.5939029372
        val bMm = if (unitSystem == UnitSystem.SI) b else b * 25.4
        val dMm = if (unitSystem == UnitSystem.SI) d else d * 25.4
        val fcMpa = if (unitSystem == UnitSystem.SI) fc else fc * 6.89475729317
        val fyMpa = if (unitSystem == UnitSystem.SI) fy else fy * 6.89475729317
        val asMm2 = if (unitSystem == UnitSystem.SI) asArea else asArea * 645.16

        if (fcMpa > 50.0 && std.id == "EUROCODE_2") {
            issues.add(ValidationIssue(IssueSeverity.WARNING, "fc", "High-Strength Concrete (fck > 50 MPa)", "EN 1992-1-1 λ=0.80, η=1.0 applies strictly up to C50/60; high-strength reductions are not applied here."))
        }

        // Demands in kN*m and kN
        val muKnM = (wuKnM * lM * lM) / 8.0 + (puKn * lM) / 4.0
        val vuKn = (wuKnM * lM) / 2.0 + puKn / 2.0

        val phiFlex: Double
        val phiMnKnM: Double
        val phiVcKn: Double
        val aOrXLabel: String
        val aOrXValMm: Double
        val rho: Double

        if (std.id == "ACI_318_19") {
            val res = EngineeringKernel.evaluateBeamAci318(bMm, dMm, fcMpa, fyMpa, asMm2)
            val aMm = res.aMm
            val cMm = res.cMm
            val epsT = res.epsT
            phiFlex = res.phiFlex
            rho = res.rho
            if (epsT < 0.004) {
                issues.add(
                    ValidationIssue(
                        IssueSeverity.ERROR,
                        "as_area",
                        "Over-Reinforced / Non-Ductile Section (ACI 318-19 §9.3.3.1)",
                        "Net tensile strain εt = ${CalcFmt.num(epsT, 5)} < 0.004. Reduce tension steel As or increase beam depth."
                    )
                )
            } else if (epsT < 0.005) {
                issues.add(
                    ValidationIssue(
                        IssueSeverity.WARNING,
                        "as_area",
                        "Transition Zone Steel Strain",
                        "Net tensile strain εt = ${CalcFmt.num(epsT, 5)} is between 0.004 and 0.005; φ reduced to ${CalcFmt.num(phiFlex, 3)}."
                    )
                )
            }
            val rhoMin = res.rhoMin
            if (rho < rhoMin) {
                issues.add(
                    ValidationIssue(
                        IssueSeverity.WARNING,
                        "as_area",
                        "Below Minimum Flexural Steel (ACI 318-19 §9.6.1.2)",
                        "ρ = ${CalcFmt.num(rho * 100, 3)}% < ρ_min = ${CalcFmt.num(rhoMin * 100, 3)}%."
                    )
                )
            }
            phiMnKnM = res.phiMnKnM
            phiVcKn = res.phiVcKn
            aOrXLabel = "Stress block depth a = ${CalcFmt.num(aMm, 2)} mm (c = ${CalcFmt.num(cMm, 2)} mm, εt = ${CalcFmt.num(epsT, 5)})"
            aOrXValMm = aMm
        } else {
            val res = EngineeringKernel.evaluateBeamEurocode2(bMm, dMm, fcMpa, fyMpa, asMm2)
            phiFlex = 1.0 // built into gamma_c, gamma_s
            val xMm = res.xMm
            val zMm = res.zMm
            rho = res.rho
            if (xMm / dMm > 0.45) {
                issues.add(
                    ValidationIssue(
                        IssueSeverity.WARNING,
                        "as_area",
                        "Neutral Axis Depth x/d > 0.45 (EC2 Ductility)",
                        "x/d = ${CalcFmt.num(xMm / dMm, 3)} exceeds 0.45; section ductility is limited without compression steel."
                    )
                )
            }
            val rhoMinEc = res.rhoMin
            if (rho < rhoMinEc) {
                issues.add(
                    ValidationIssue(
                        IssueSeverity.WARNING,
                        "as_area",
                        "Below Minimum Steel (EN 1992-1-1 §9.2.1.1)",
                        "ρ = ${CalcFmt.num(rho * 100, 3)}% < ρ_min = ${CalcFmt.num(rhoMinEc * 100, 3)}%."
                    )
                )
            }
            phiMnKnM = res.mRdKnM
            phiVcKn = res.vRdCKn
            aOrXLabel = "Neutral axis x = ${CalcFmt.num(xMm, 2)} mm (z = ${CalcFmt.num(zMm, 2)} mm)"
            aOrXValMm = xMm
        }

        if (aOrXValMm >= dMm) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "d", "Invalid Compression Zone", "Compression block depth exceeds effective depth d."))
        }

        // Deflection (elastic unfactored service UDL)
        val ecMpa = 4700.0 * sqrt(fcMpa)
        val hEstMm = dMm * hOverD
        val igMm4 = (bMm * hEstMm.pow(3)) / 12.0
        val iEffMm4 = iEffRatio * igMm4
        // wsKnM is kN/m = N/mm; lM*1000 is mm; ecMpa is N/mm2
        val lMm = lM * 1000.0
        val deltaMm = (5.0 * wsKnM * lMm.pow(4)) / (384.0 * ecMpa * iEffMm4)
        val deltaAllowMm = lMm / deflDenom

        val utilFlex = if (phiMnKnM > 0.0) muKnM / phiMnKnM else Double.POSITIVE_INFINITY
        val utilShearConc = if (phiVcKn > 0.0) vuKn / phiVcKn else Double.POSITIVE_INFINITY
        val utilDefl = if (deltaAllowMm > 0.0) deltaMm / deltaAllowMm else 0.0

        if (utilShearConc > 1.0) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.WARNING,
                    null,
                    "Shear Stirrups Required (Vu > φVc)",
                    "Factored shear exceeds unreinforced concrete shear capacity (Vu/φVc = ${CalcFmt.num(utilShearConc, 2)}). Provide minimum/designed transverse stirrups."
                )
            )
        }
        if (utilDefl > 1.0) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.WARNING,
                    "w_s",
                    "Service Deflection Exceeds L/${deflDenom.toInt()}",
                    "Estimated service deflection (${CalcFmt.num(deltaMm, 2)} mm) exceeds limit (${CalcFmt.num(deltaAllowMm, 2)} mm)."
                )
            )
        }

        val governingRatio = max(utilFlex, utilDefl)
        val govName = when {
            utilFlex >= utilDefl -> "Flexure (${if (std.id == "ACI_318_19") "Mu / φMn" else "MEd / MRd"}) = ${CalcFmt.num(utilFlex, 3)}"
            else -> "Deflection (Δ / Δallow) = ${CalcFmt.num(utilDefl, 3)}"
        }

        val status = when {
            issues.any { it.severity == IssueSeverity.ERROR } || utilFlex > 1.0 -> CalcStatus.FAIL
            issues.any { it.severity == IssueSeverity.WARNING } -> CalcStatus.WARNING
            else -> CalcStatus.PASS
        }

        // Convert outputs back to selected UnitSystem
        val mUnit = if (unitSystem == UnitSystem.SI) "kN·m" else "kip·ft"
        val vUnit = if (unitSystem == UnitSystem.SI) "kN" else "kip"
        val dUnit = if (unitSystem == UnitSystem.SI) "mm" else "in"

        val muDisp = if (unitSystem == UnitSystem.SI) muKnM else muKnM / 1.3558179483
        val phiMnDisp = if (unitSystem == UnitSystem.SI) phiMnKnM else phiMnKnM / 1.3558179483
        val vuDisp = if (unitSystem == UnitSystem.SI) vuKn else vuKn / 4.44822161526
        val phiVcDisp = if (unitSystem == UnitSystem.SI) phiVcKn else phiVcKn / 4.44822161526
        val deltaDisp = if (unitSystem == UnitSystem.SI) deltaMm else deltaMm / 25.4
        val deltaAllowDisp = if (unitSystem == UnitSystem.SI) deltaAllowMm else deltaAllowMm / 25.4

        val steps = listOf(
            CalcStep(
                stepNumber = 1,
                title = "Maximum Factored Bending Moment & Shear",
                equation = "Mu = wu·L²/8 + Pu·L/4 ;  Vu = wu·L/2 + Pu/2",
                variables = listOf("L = ${CalcFmt.smart(L)}", "wu = ${CalcFmt.smart(wu)}", "Pu = ${CalcFmt.smart(pu)}"),
                substituted = "Mu = ${CalcFmt.num(muDisp, 2)} $mUnit ; Vu = ${CalcFmt.num(vuDisp, 2)} $vUnit",
                resultFormatted = "Mu = ${CalcFmt.num(muDisp, 2)} $mUnit | Vu = ${CalcFmt.num(vuDisp, 2)} $vUnit",
                rawPrecisionNote = CalcFmt.rawNote(muDisp, mUnit)
            ),
            CalcStep(
                stepNumber = 2,
                title = "Flexural Capacity (${std.name})",
                equation = if (std.id == "ACI_318_19") "a = As·fy / (0.85·f'c·b) ; φMn = φ·As·fy·(d − a/2)" else "x = As·fyd / (0.8·fcd·b) ; MRd = As·fyd·(d − 0.4x)",
                variables = listOf(aOrXLabel, "ρ = ${CalcFmt.num(rho * 100, 3)}%"),
                substituted = "Capacity = ${CalcFmt.num(phiMnDisp, 2)} $mUnit ; Demand = ${CalcFmt.num(muDisp, 2)} $mUnit",
                resultFormatted = "Utilization = ${CalcFmt.num(utilFlex, 3)} (${if (utilFlex <= 1.0) "OK" else "NG"})"
            ),
            CalcStep(
                stepNumber = 3,
                title = "Concrete Shear Capacity Check (Unreinforced Web)",
                equation = if (std.id == "ACI_318_19") "φVc = 0.75 × 0.17·√f'c · b · d" else "VRd,c = [CRd,c · k · (100·ρl·fck)^(1/3)] · b · d",
                variables = listOf("Vu = ${CalcFmt.num(vuDisp, 2)} $vUnit", "φVc = ${CalcFmt.num(phiVcDisp, 2)} $vUnit"),
                substituted = "Vu / φVc = ${CalcFmt.num(vuDisp, 2)} / ${CalcFmt.num(phiVcDisp, 2)}",
                resultFormatted = "Ratio = ${CalcFmt.num(utilShearConc, 3)} ${if (utilShearConc <= 1.0) "(Concrete alone sufficient)" else "(Stirrups required)"}"
            ),
            CalcStep(
                stepNumber = 4,
                title = "Elastic Service Deflection Check",
                equation = "Δ = 5·ws·L⁴ / (384·Ec·Ieff)  vs.  Δ_allow = L / ${deflDenom.toInt()}",
                variables = listOf("Ec = ${CalcFmt.num(ecMpa, 0)} MPa", "Ieff = ${CalcFmt.num(iEffRatio, 2)}·Ig"),
                substituted = "Δ = ${CalcFmt.num(deltaDisp, 3)} $dUnit vs ${CalcFmt.num(deltaAllowDisp, 3)} $dUnit",
                resultFormatted = "Δ / Δ_allow = ${CalcFmt.num(utilDefl, 3)}"
            )
        )

        return CalcResult(
            moduleId = id,
            moduleName = name,
            nature = nature,
            unitSystem = unitSystem,
            standard = std,
            status = status,
            governingCheck = govName,
            utilizationRatio = utilFlex,
            summaryMetrics = listOf(
                OutputMetric("Factored Moment (Mu)", CalcFmt.num(muDisp, 2), mUnit, muDisp),
                OutputMetric("Design Moment Cap. (φMn / MRd)", CalcFmt.num(phiMnDisp, 2), mUnit, phiMnDisp, highlight = true),
                OutputMetric("Flexural Utilization", CalcFmt.num(utilFlex, 3), "ratio", utilFlex, highlight = true),
                OutputMetric("Factored Shear (Vu)", CalcFmt.num(vuDisp, 2), vUnit, vuDisp),
                OutputMetric("Concrete Shear Cap. (φVc)", CalcFmt.num(phiVcDisp, 2), vUnit, phiVcDisp),
                OutputMetric("Service Deflection (Δ)", CalcFmt.num(deltaDisp, 3), dUnit, deltaDisp)
            ),
            steps = steps,
            issues = issues,
            resolvedAssumptions = mapOf(
                "ieff_ratio" to CalcFmt.num(iEffRatio, 2),
                "defl_limit_denom" to deflDenom.toInt().toString(),
                "h_over_d" to CalcFmt.num(hOverD, 2)
            ),
            scopeAndLimitations = scopeAndLimitations,
            engineeringNotes = engineeringNotes
        )
    }
}

class StructuralColumnCalcModule : CalcModule {
    override val id = CalcModuleId.STRUCTURAL_COLUMN
    override val category = CalcCategory.STRUCTURAL
    override val name = "Short RC Column: Axial Capacity"
    override val shortDescription = "Concentric axial load capacity of a short, braced, tied rectangular RC column."
    override val nature = CalculationNature.CODE_BASED_CHECK

    override val supportedStandards = listOf(ACI_318_STD, EUROCODE_2_STD)

    override val scopeAndLimitations = listOf(
        "Valid ONLY for short, braced, concentrically loaded tied columns (slenderness kl/r not evaluated).",
        "Biaxial bending / P-M interaction is excluded; significant moments invalidate pure axial assumptions.",
        "Spiral columns (ACI α=0.85, φ=0.75) are not modeled; conservative tied-column factors apply."
    )

    override val engineeringNotes = listOf(
        "Verify transverse tie spacing and minimum eccentricity requirements in final detailing.",
        "Longitudinal steel ratio ρg must remain within 1%–8% (ACI 318-19) or 0.2%–4% (EC2)."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("p_u", "Factored Axial Load (Pu / NEd)", "kN", "kip", "Factored concentric compression", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("b", "Column Width (b)", "mm", "in", "Short side dimension", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("h", "Column Depth (h)", "mm", "in", "Orthogonal side dimension", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("fc", "Concrete Strength (f'c / fck)", "MPa", "ksi", "Cylinder compressive strength", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("fy", "Steel Yield Strength (fy / fyk)", "MPa", "ksi", "Longitudinal bar yield", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("ast", "Longitudinal Steel Area (Ast)", "mm²", "in²", "Total area of vertical bars", minInclusive = 0.0, allowZero = false)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("alpha_factor", "Accidental Eccentricity Factor", if (standardId == "EUROCODE_2") "0.85" else "0.80", "factor", "ACI 318 tied column 0.80 factor; EC2 α_cc = 0.85.")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("p_u" to "1650", "b" to "350", "h" to "350", "fc" to "30", "fy" to "420", "ast" to "2454")
        } else {
            mapOf("p_u" to "370", "b" to "14", "h" to "14", "fc" to "4.0", "fy" to "60", "ast" to "3.16")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_col_aci_si",
            title = "350x350 Tied Column (ACI 318-19)",
            description = "8-T20 bars (Ast=2454 mm²), fc'=30 MPa, fy=420 MPa, Pu=1650 kN.",
            unitSystem = UnitSystem.SI,
            standardId = "ACI_318_19",
            inputs = mapOf("p_u" to "1650", "b" to "350", "h" to "350", "fc" to "30", "fy" to "420", "ast" to "2454"),
            expectedNote = "Ag=122,500 mm², ρg=2.00%, φPn ≈ 2131 kN (Util ≈ 0.774 PASS)"
        )
    )

    override fun evaluate(
        unitSystem: UnitSystem,
        standardId: String,
        rawInputs: Map<String, String>,
        rawAssumptions: Map<String, String>
    ): CalcResult {
        val issues = mutableListOf<ValidationIssue>()
        val std = supportedStandards.firstOrNull { it.id == standardId } ?: ACI_318_STD
        val specs = inputSpecs(unitSystem, std.id)
        val aSpecs = assumptionSpecs(unitSystem, std.id)

        val pu = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val b = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val h = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val fc = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val fy = CalcFmt.parseDoubleInput(rawInputs, specs[4], unitSystem, issues)
        val ast = CalcFmt.parseDoubleInput(rawInputs, specs[5], unitSystem, issues)
        val alpha = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 0.50, 1.00, issues)

        if (issues.any { it.severity == IssueSeverity.ERROR } || pu == null || b == null || h == null || fc == null || fy == null || ast == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("alpha_factor" to alpha.toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        val puKn = if (unitSystem == UnitSystem.SI) pu else pu * 4.44822161526
        val bMm = if (unitSystem == UnitSystem.SI) b else b * 25.4
        val hMm = if (unitSystem == UnitSystem.SI) h else h * 25.4
        val fcMpa = if (unitSystem == UnitSystem.SI) fc else fc * 6.89475729317
        val fyMpa = if (unitSystem == UnitSystem.SI) fy else fy * 6.89475729317
        val astMm2 = if (unitSystem == UnitSystem.SI) ast else ast * 645.16

        val agMm2 = bMm * hMm
        if (astMm2 >= agMm2) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "ast", "Steel Area Exceeds Gross Area", "Ast must be smaller than Ag."))
        }
        val rhoG = astMm2 / agMm2
        val (minRho, maxRho) = if (std.id == "ACI_318_19") 0.01 to 0.08 else 0.002 to 0.04
        if (rhoG < minRho || rhoG > maxRho) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.WARNING,
                    "ast",
                    "Steel Ratio Outside Code Bounds",
                    "ρg = ${CalcFmt.num(rhoG * 100, 2)}% is outside ${CalcFmt.num(minRho * 100, 1)}%–${CalcFmt.num(maxRho * 100, 1)}% (${std.name})."
                )
            )
        }

        val capKn = if (std.id == "ACI_318_19") {
            0.65 * alpha * (0.85 * fcMpa * (agMm2 - astMm2) + fyMpa * astMm2) / 1000.0
        } else {
            val fcd = alpha * fcMpa / 1.50
            val fyd = fyMpa / 1.15
            (fcd * (agMm2 - astMm2) + fyd * astMm2) / 1000.0
        }

        val util = if (capKn > 0.0) puKn / capKn else Double.POSITIVE_INFINITY
        val pUnit = if (unitSystem == UnitSystem.SI) "kN" else "kip"
        val capDisp = if (unitSystem == UnitSystem.SI) capKn else capKn / 4.44822161526

        val status = when {
            issues.any { it.severity == IssueSeverity.ERROR } || util > 1.0 -> CalcStatus.FAIL
            issues.any { it.severity == IssueSeverity.WARNING } -> CalcStatus.WARNING
            else -> CalcStatus.PASS
        }

        val steps = listOf(
            CalcStep(
                stepNumber = 1,
                title = "Gross Area & Steel Ratio",
                equation = "Ag = b × h ;  ρg = Ast / Ag",
                variables = listOf("b = ${CalcFmt.smart(b)}", "h = ${CalcFmt.smart(h)}", "Ast = ${CalcFmt.smart(ast)}"),
                substituted = "Ag = ${CalcFmt.num(agMm2, 0)} mm² ; ρg = ${CalcFmt.num(rhoG * 100, 2)}%",
                resultFormatted = "ρg = ${CalcFmt.num(rhoG * 100, 2)}%"
            ),
            CalcStep(
                stepNumber = 2,
                title = "Design Axial Capacity (${std.name})",
                equation = if (std.id == "ACI_318_19") "φPn = 0.65 × α × [0.85·f'c·(Ag − Ast) + fy·Ast]" else "NRd = fcd·(Ag − Ast) + fyd·Ast",
                variables = listOf("α = ${CalcFmt.num(alpha, 2)}", "Pu = ${CalcFmt.num(pu, 2)} $pUnit"),
                substituted = "Design Capacity = ${CalcFmt.num(capDisp, 2)} $pUnit",
                resultFormatted = "Utilization = ${CalcFmt.num(util, 3)}",
                rawPrecisionNote = CalcFmt.rawNote(capDisp, pUnit)
            )
        )

        return CalcResult(
            moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
            status = status,
            governingCheck = "Axial Load Pu / Cap = ${CalcFmt.num(util, 3)}",
            utilizationRatio = util,
            summaryMetrics = listOf(
                OutputMetric("Factored Load (Pu)", CalcFmt.num(pu, 2), pUnit, pu),
                OutputMetric("Design Axial Capacity", CalcFmt.num(capDisp, 2), pUnit, capDisp, highlight = true),
                OutputMetric("Axial Utilization", CalcFmt.num(util, 3), "ratio", util, highlight = true),
                OutputMetric("Steel Ratio (ρg)", CalcFmt.num(rhoG * 100, 2), "%", rhoG * 100)
            ),
            steps = steps, issues = issues,
            resolvedAssumptions = mapOf("alpha_factor" to CalcFmt.num(alpha, 2)),
            scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
        )
    }
}

class StructuralSlabCalcModule : CalcModule {
    override val id = CalcModuleId.STRUCTURAL_SLAB
    override val category = CalcCategory.STRUCTURAL
    override val name = "One-Way RC Slab Strip Check"
    override val shortDescription = "Unit-width one-way solid slab flexural capacity, minimum thickness, and shrinkage steel check."
    override val nature = CalculationNature.CODE_BASED_CHECK

    override val supportedStandards = listOf(ACI_318_STD, EUROCODE_2_STD)

    override val scopeAndLimitations = listOf(
        "Evaluates a 1 m (SI) or 1 ft (US) unit strip of a simply supported one-way solid slab.",
        "Two-way slabs, flat plate punching shear, and continuous negative moments require dedicated analysis."
    )

    override val engineeringNotes = listOf(
        "Check minimum cover (typically 20 mm / 0.75 in for interior slabs) when computing d = h − cover − db/2."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("span", "Clear Span (L)", "m", "ft", "One-way span between supports", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("h", "Slab Thickness (h)", "mm", "in", "Total slab depth", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("d", "Effective Depth (d)", "mm", "in", "Depth to main tension bar center", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("q_u", "Factored Area Load (qu)", "kPa", "ksf", "Total factored load including self-weight", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("fc", "Concrete Strength (f'c / fck)", "MPa", "ksi", "Cylinder compressive strength", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("fy", "Steel Yield Strength (fy / fyk)", "MPa", "ksi", "Main bar yield strength", minInclusive = 0.0, allowZero = false),
        InputFieldSpec("as_strip", "Main Steel Area / Unit Width", "mm²/m", "in²/ft", "Area of primary steel per meter or foot", minInclusive = 0.0, allowZero = false)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("min_thick_denom", "Min. Thickness Span Factor (L / K)", "20", "ratio", "ACI 318 Table 7.3.1.1: 20 simply supported, 24 one-end cont., 28 both-ends cont.")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("span" to "3.6", "h" to "180", "d" to "150", "q_u" to "12.5", "fc" to "28", "fy" to "420", "as_strip" to "565")
        } else {
            mapOf("span" to "12.0", "h" to "7.0", "d" to "5.75", "q_u" to "0.26", "fc" to "4.0", "fy" to "60", "as_strip" to "0.26")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_slab_si",
            title = "3.6 m One-Way Floor Slab (ACI 318-19)",
            description = "180 mm slab, d=150 mm, qu=12.5 kPa, T12 @ 200 mm c/c (565 mm²/m).",
            unitSystem = UnitSystem.SI,
            standardId = "ACI_318_19",
            inputs = mapOf("span" to "3.6", "h" to "180", "d" to "150", "q_u" to "12.5", "fc" to "28", "fy" to "420", "as_strip" to "565"),
            expectedNote = "Mu = 20.25 kN·m/m, φMn ≈ 30.8 kN·m/m (Util ≈ 0.658 PASS)"
        )
    )

    override fun evaluate(
        unitSystem: UnitSystem,
        standardId: String,
        rawInputs: Map<String, String>,
        rawAssumptions: Map<String, String>
    ): CalcResult {
        val issues = mutableListOf<ValidationIssue>()
        val std = supportedStandards.firstOrNull { it.id == standardId } ?: ACI_318_STD
        val specs = inputSpecs(unitSystem, std.id)
        val aSpecs = assumptionSpecs(unitSystem, std.id)

        val L = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val h = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val d = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val qu = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val fc = CalcFmt.parseDoubleInput(rawInputs, specs[4], unitSystem, issues)
        val fy = CalcFmt.parseDoubleInput(rawInputs, specs[5], unitSystem, issues)
        val asStrip = CalcFmt.parseDoubleInput(rawInputs, specs[6], unitSystem, issues)
        val kThick = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 10.0, 36.0, issues)

        if (h != null && d != null && d >= h) {
            issues.add(ValidationIssue(IssueSeverity.ERROR, "d", "Invalid Effective Depth", "Effective depth d must be strictly less than slab thickness h."))
        }

        if (issues.any { it.severity == IssueSeverity.ERROR } || L == null || h == null || d == null || qu == null || fc == null || fy == null || asStrip == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("min_thick_denom" to kThick.toInt().toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        val lM = if (unitSystem == UnitSystem.SI) L else L * 0.3048
        val hMm = if (unitSystem == UnitSystem.SI) h else h * 25.4
        val dMm = if (unitSystem == UnitSystem.SI) d else d * 25.4
        val quKpa = if (unitSystem == UnitSystem.SI) qu else qu * 47.88025898
        val fcMpa = if (unitSystem == UnitSystem.SI) fc else fc * 6.89475729317
        val fyMpa = if (unitSystem == UnitSystem.SI) fy else fy * 6.89475729317
        // Convert As per unit strip to mm2 per meter (1000 mm)
        val asMm2PerM = if (unitSystem == UnitSystem.SI) asStrip else asStrip * (645.16 / 0.3048)

        val muKnMPerM = (quKpa * lM * lM) / 8.0
        val hMinMm = (lM * 1000.0) / kThick
        if (hMm < hMinMm) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.WARNING,
                    "h",
                    "Below Minimum Thickness Guideline (L/${kThick.toInt()})",
                    "Provided h = ${CalcFmt.num(hMm, 0)} mm < L/${kThick.toInt()} = ${CalcFmt.num(hMinMm, 0)} mm. Deflection calculation is required."
                )
            )
        }

        val rhoGross = asMm2PerM / (1000.0 * hMm)
        val rhoMin = if (std.id == "ACI_318_19") 0.0018 else 0.0013
        if (rhoGross < rhoMin) {
            issues.add(
                ValidationIssue(
                    IssueSeverity.WARNING,
                    "as_strip",
                    "Below Shrinkage/Min Steel Ratio",
                    "Gross steel ratio ${CalcFmt.num(rhoGross * 100, 3)}% < ${CalcFmt.num(rhoMin * 100, 2)}%."
                )
            )
        }

        val capKnMPerM = if (std.id == "ACI_318_19") {
            val aMm = (asMm2PerM * fyMpa) / (0.85 * fcMpa * 1000.0)
            0.90 * (asMm2PerM * fyMpa * (dMm - aMm / 2.0)) / 1e6
        } else {
            val fcd = 0.85 * fcMpa / 1.50
            val fyd = fyMpa / 1.15
            val xMm = (asMm2PerM * fyd) / (0.8 * fcd * 1000.0)
            (asMm2PerM * fyd * (dMm - 0.4 * xMm)) / 1e6
        }

        val util = if (capKnMPerM > 0.0) muKnMPerM / capKnMPerM else Double.POSITIVE_INFINITY
        val mStripUnit = if (unitSystem == UnitSystem.SI) "kN·m/m" else "kip·ft/ft"
        val muDisp = if (unitSystem == UnitSystem.SI) muKnMPerM else muKnMPerM * (0.3048 / 1.3558179483)
        val capDisp = if (unitSystem == UnitSystem.SI) capKnMPerM else capKnMPerM * (0.3048 / 1.3558179483)

        val status = when {
            issues.any { it.severity == IssueSeverity.ERROR } || util > 1.0 -> CalcStatus.FAIL
            issues.any { it.severity == IssueSeverity.WARNING } -> CalcStatus.WARNING
            else -> CalcStatus.PASS
        }

        val steps = listOf(
            CalcStep(1, "Factored Strip Bending Moment", "Mu = qu·L² / 8", listOf("qu = ${CalcFmt.smart(qu)}", "L = ${CalcFmt.smart(L)}"), "Mu = ${CalcFmt.num(muDisp, 2)} $mStripUnit", "${CalcFmt.num(muDisp, 2)} $mStripUnit"),
            CalcStep(2, "Flexural Strip Capacity", "b = 1 unit width", listOf("d = ${CalcFmt.smart(d)}", "As = ${CalcFmt.smart(asStrip)}"), "Cap = ${CalcFmt.num(capDisp, 2)} $mStripUnit", "Util = ${CalcFmt.num(util, 3)}")
        )

        return CalcResult(
            moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
            status = status, governingCheck = "Flexure Mu / Cap = ${CalcFmt.num(util, 3)}", utilizationRatio = util,
            summaryMetrics = listOf(
                OutputMetric("Factored Strip Moment", CalcFmt.num(muDisp, 2), mStripUnit, muDisp),
                OutputMetric("Design Strip Capacity", CalcFmt.num(capDisp, 2), mStripUnit, capDisp, highlight = true),
                OutputMetric("Flexural Utilization", CalcFmt.num(util, 3), "ratio", util, highlight = true)
            ),
            steps = steps, issues = issues,
            resolvedAssumptions = mapOf("min_thick_denom" to kThick.toInt().toString()),
            scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
        )
    }
}

class StructuralLoadComboCalcModule : CalcModule {
    override val id = CalcModuleId.STRUCTURAL_LOAD_COMBO
    override val category = CalcCategory.STRUCTURAL
    override val name = "Basic Strength Load Combinations"
    override val shortDescription = "Evaluate ASCE 7-22 LRFD or EN 1990 ULS load combinations and identify the governing envelope."
    override val nature = CalculationNature.CODE_BASED_CHECK

    override val supportedStandards = listOf(
        DesignStandardSpec("ASCE_7_22", "ASCE/SEI 7-22", "2022 Edition", "Strength (LRFD) basic gravity & wind combinations (§2.3.1).", StandardFamily.ACI_ASCE),
        DesignStandardSpec("EN_1990", "Eurocode 0 (EN 1990)", "2002 + A1:2005", "ULS STR/GEO Eq. (6.10) fundamental load combinations.", StandardFamily.EUROCODE)
    )

    override val scopeAndLimitations = listOf(
        "Evaluates scalar co-linear load magnitudes (D, L, Lr, W) without reversing direction sign checks beyond uplift 0.9D + W.",
        "Seismic (E), snow (S), flood, and notional horizontal loads are excluded in this MVP calculator."
    )

    override val engineeringNotes = listOf(
        "Confirm whether reduced live load factor 0.5 applies per ASCE 7-22 §2.3.1 Exception 1 (L0 ≤ 100 psf / 4.79 kPa, non-garage/assembly)."
    )

    override fun inputSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        InputFieldSpec("d_load", "Dead Load (D / Gk)", "kN", "kip", "Permanent gravity load", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("l_load", "Floor Live Load (L / Qk)", "kN", "kip", "Occupancy live load", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("lr_load", "Roof Live Load (Lr)", "kN", "kip", "Roof maintenance / live load", minInclusive = 0.0, allowZero = true),
        InputFieldSpec("w_load", "Wind Load (W / Wk)", "kN", "kip", "Design wind action magnitude", minInclusive = 0.0, allowZero = true)
    )

    override fun assumptionSpecs(unitSystem: UnitSystem, standardId: String) = listOf(
        AssumptionSpec("live_companion_factor", if (standardId == "EN_1990") "EN 1990 ψ0 Factor" else "ASCE 7 Wind-Combo L Factor", if (standardId == "EN_1990") "0.7" else "0.5", "factor", "0.5 or 1.0 for ASCE 7-22; ψ0 = 0.7 typical office/residential in EN 1990.")
    )

    override fun defaultInputs(unitSystem: UnitSystem, standardId: String) =
        if (unitSystem == UnitSystem.SI) {
            mapOf("d_load" to "120", "l_load" to "80", "lr_load" to "15", "w_load" to "45")
        } else {
            mapOf("d_load" to "27", "l_load" to "18", "lr_load" to "3.5", "w_load" to "10")
        }

    override fun examples() = listOf(
        ExampleCase(
            id = "ex_combo_asce",
            title = "ASCE 7-22 Gravity + Wind Envelope",
            description = "D=120 kN, L=80 kN, Lr=15 kN, W=45 kN with L-companion=0.5.",
            unitSystem = UnitSystem.SI,
            standardId = "ASCE_7_22",
            inputs = mapOf("d_load" to "120", "l_load" to "80", "lr_load" to "15", "w_load" to "45"),
            expectedNote = "1.2D + 1.6L + 0.5Lr = 279.5 kN governs"
        )
    )

    override fun evaluate(
        unitSystem: UnitSystem,
        standardId: String,
        rawInputs: Map<String, String>,
        rawAssumptions: Map<String, String>
    ): CalcResult {
        val issues = mutableListOf<ValidationIssue>()
        val std = supportedStandards.firstOrNull { it.id == standardId } ?: supportedStandards.first()
        val specs = inputSpecs(unitSystem, std.id)
        val aSpecs = assumptionSpecs(unitSystem, std.id)

        val d = CalcFmt.parseDoubleInput(rawInputs, specs[0], unitSystem, issues)
        val l = CalcFmt.parseDoubleInput(rawInputs, specs[1], unitSystem, issues)
        val lr = CalcFmt.parseDoubleInput(rawInputs, specs[2], unitSystem, issues)
        val w = CalcFmt.parseDoubleInput(rawInputs, specs[3], unitSystem, issues)
        val psi = CalcFmt.parseAssumptionDouble(rawAssumptions, aSpecs[0], 0.0, 1.0, issues)

        if (issues.any { it.severity == IssueSeverity.ERROR } || d == null || l == null || lr == null || w == null) {
            return CalcResult(
                moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
                status = CalcStatus.FAIL, governingCheck = "Input validation failed", utilizationRatio = null,
                summaryMetrics = emptyList(), steps = emptyList(), issues = issues,
                resolvedAssumptions = mapOf("live_companion_factor" to psi.toString()),
                scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
            )
        }

        val combos: List<Pair<String, Double>> = if (std.id == "ASCE_7_22") {
            listOf(
                "LC1: 1.4D" to (1.4 * d),
                "LC2: 1.2D + 1.6L + 0.5Lr" to (1.2 * d + 1.6 * l + 0.5 * lr),
                "LC3: 1.2D + 1.6Lr + ${CalcFmt.num(psi, 2)}L" to (1.2 * d + 1.6 * lr + psi * l),
                "LC4: 1.2D + 1.0W + ${CalcFmt.num(psi, 2)}L + 0.5Lr" to (1.2 * d + 1.0 * w + psi * l + 0.5 * lr),
                "LC5: 0.9D + 1.0W (Min Gravity)" to (0.9 * d + 1.0 * w)
            )
        } else {
            listOf(
                "ULS1: 1.35Gk" to (1.35 * d),
                "ULS2: 1.35Gk + 1.5Qk + 1.5(${CalcFmt.num(psi, 2)})Wk" to (1.35 * d + 1.5 * l + 1.5 * psi * w),
                "ULS3: 1.35Gk + 1.5Wk + 1.5(${CalcFmt.num(psi, 2)})Qk" to (1.35 * d + 1.5 * w + 1.5 * psi * l),
                "ULS4: 1.00Gk + 1.5Wk (Min Gravity)" to (1.0 * d + 1.5 * w)
            )
        }

        val gov = combos.maxBy { it.second }
        val unit = if (unitSystem == UnitSystem.SI) "kN" else "kip"
        val steps = combos.mapIndexed { idx, (label, value) ->
            CalcStep(
                stepNumber = idx + 1,
                title = label,
                equation = label.substringAfter(": "),
                variables = listOf("D=${CalcFmt.smart(d)}", "L=${CalcFmt.smart(l)}", "Lr=${CalcFmt.smart(lr)}", "W=${CalcFmt.smart(w)}"),
                substituted = "Factored Result = ${CalcFmt.num(value, 2)} $unit",
                resultFormatted = "${CalcFmt.num(value, 2)} $unit" + if (label == gov.first) " [GOVERNS]" else ""
            )
        }

        return CalcResult(
            moduleId = id, moduleName = name, nature = nature, unitSystem = unitSystem, standard = std,
            status = CalcStatus.PASS,
            governingCheck = "${gov.first} = ${CalcFmt.num(gov.second, 2)} $unit",
            utilizationRatio = null,
            summaryMetrics = listOf(
                OutputMetric("Governing Factored Load", CalcFmt.num(gov.second, 2), unit, gov.second, highlight = true),
                OutputMetric("Governing Case", gov.first.substringBefore(":"), "", null, highlight = true)
            ),
            steps = steps, issues = issues,
            resolvedAssumptions = mapOf("live_companion_factor" to CalcFmt.num(psi, 2)),
            scopeAndLimitations = scopeAndLimitations, engineeringNotes = engineeringNotes
        )
    }
}
