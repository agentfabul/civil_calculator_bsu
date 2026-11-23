package com.example

import com.example.domain.calc.CalcFmt
import com.example.domain.calc.CalcModuleId
import com.example.domain.calc.CalcRegistry
import com.example.domain.calc.CalcStatus
import com.example.domain.calc.IssueSeverity
import com.example.domain.calc.ReportFormatter
import com.example.domain.calc.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CivilSpecCalcEngineTest {

    @Test
    fun unitConversion_mpaToPsi_and_cfsToCumecs_matchesHandCalculation() {
        val mod = CalcRegistry.byId(CalcModuleId.UNIT_CONVERSION)
        val resMpa = mod.evaluate(
            UnitSystem.SI,
            "NIST_SP811",
            mapOf("category" to "Stress / Pressure", "input_value" to "28.0", "from_unit" to "MPa", "to_unit" to "psi"),
            mapOf("display_decimals" to "2")
        )
        assertTrue(resMpa.isValid)
        val psi = resMpa.summaryMetrics.first().rawValue!!
        assertEquals(4061.0566, psi, 0.01)
        assertEquals("4061.06", resMpa.summaryMetrics.first().formattedValue)

        val resFlow = mod.evaluate(
            UnitSystem.US,
            "NIST_SP811",
            mapOf("category" to "Flow Rate", "input_value" to "120.0", "from_unit" to "cfs", "to_unit" to "m3/s"),
            mapOf("display_decimals" to "4")
        )
        assertTrue(resFlow.isValid)
        assertEquals(3.3980, resFlow.summaryMetrics.first().rawValue!!, 0.0005)
    }

    @Test
    fun unitConversion_incompatibleDimensions_flagsError() {
        val mod = CalcRegistry.byId(CalcModuleId.UNIT_CONVERSION)
        val res = mod.evaluate(
            UnitSystem.SI,
            "NIST_SP811",
            mapOf("category" to "Length", "input_value" to "10", "from_unit" to "m", "to_unit" to "kPa"),
            emptyMap()
        )
        assertFalse(res.isValid)
        assertEquals(CalcStatus.FAIL, res.status)
        assertTrue(res.issues.any { it.severity == IssueSeverity.ERROR && it.fieldKey == "to_unit" })
    }

    @Test
    fun quantityTakeoff_trapezoidalTrench_matchesHandCalculation() {
        val mod = CalcRegistry.byId(CalcModuleId.QUANTITY_TAKEOFF)
        val res = mod.evaluate(
            UnitSystem.SI,
            "CSI_TAKEOFF",
            mapOf("b_bottom" to "1.20", "b_top" to "2.40", "depth" to "1.50", "length" to "45.0", "count" to "1"),
            mapOf("waste_pct" to "5.0", "swell_pct" to "20.0")
        )
        assertTrue(res.isValid)
        val gross = res.summaryMetrics.first { it.label == "Gross Volume" }.rawValue!!
        val net = res.summaryMetrics.first { it.label == "Net Geometric Volume" }.rawValue!!
        assertEquals(121.50, net, 1e-6)
        assertEquals(153.09, gross, 1e-6)
    }

    @Test
    fun structuralBeam_aci318_matchesHandCalculation_andFlagsOverReinforced() {
        val mod = CalcRegistry.byId(CalcModuleId.STRUCTURAL_BEAM)
        val res = mod.evaluate(
            UnitSystem.SI,
            "ACI_318_19",
            mapOf(
                "span" to "6.0", "w_u" to "28.0", "p_u" to "0.0", "w_s" to "19.5",
                "b" to "300", "d" to "450", "fc" to "28", "fy" to "420", "as_area" to "1472"
            ),
            emptyMap()
        )
        assertTrue(res.isValid)
        assertEquals(CalcStatus.PASS, res.status)
        val mu = res.summaryMetrics.first { it.label.contains("Factored Moment") }.rawValue!!
        val phiMn = res.summaryMetrics.first { it.label.contains("Design Moment") }.rawValue!!
        assertEquals(126.0, mu, 0.01)
        assertEquals(226.30, phiMn, 0.2)
        assertTrue(res.utilizationRatio!! < 0.60)

        // Over-reinforced section should fail ductility check (eps_t < 0.004)
        val overRes = mod.evaluate(
            UnitSystem.SI,
            "ACI_318_19",
            mapOf(
                "span" to "6.0", "w_u" to "28.0", "p_u" to "0.0", "w_s" to "19.5",
                "b" to "250", "d" to "350", "fc" to "25", "fy" to "420", "as_area" to "4000"
            ),
            emptyMap()
        )
        assertFalse(overRes.isValid)
        assertEquals(CalcStatus.FAIL, overRes.status)
    }

    @Test
    fun structuralColumn_and_slab_and_loadCombo_matchHandCalculations() {
        val col = CalcRegistry.byId(CalcModuleId.STRUCTURAL_COLUMN)
        val colRes = col.evaluate(
            UnitSystem.SI,
            "ACI_318_19",
            mapOf("p_u" to "1650", "b" to "350", "h" to "350", "fc" to "30", "fy" to "420", "ast" to "2454"),
            emptyMap()
        )
        assertTrue(colRes.isValid)
        val cap = colRes.summaryMetrics.first { it.label == "Design Axial Capacity" }.rawValue!!
        assertEquals(2127.76, cap, 0.5)
        assertEquals(CalcStatus.PASS, colRes.status)

        val slab = CalcRegistry.byId(CalcModuleId.STRUCTURAL_SLAB)
        val slabRes = slab.evaluate(
            UnitSystem.SI,
            "ACI_318_19",
            mapOf("span" to "3.6", "h" to "180", "d" to "150", "q_u" to "12.5", "fc" to "28", "fy" to "420", "as_strip" to "565"),
            emptyMap()
        )
        assertTrue(slabRes.isValid)
        val slabMu = slabRes.summaryMetrics.first { it.label == "Factored Strip Moment" }.rawValue!!
        assertEquals(20.25, slabMu, 0.01)

        val combo = CalcRegistry.byId(CalcModuleId.STRUCTURAL_LOAD_COMBO)
        val comboRes = combo.evaluate(
            UnitSystem.SI,
            "ASCE_7_22",
            mapOf("d_load" to "120", "l_load" to "80", "lr_load" to "15", "w_load" to "45"),
            emptyMap()
        )
        assertTrue(comboRes.isValid)
        val gov = comboRes.summaryMetrics.first { it.label == "Governing Factored Load" }.rawValue!!
        assertEquals(279.5, gov, 0.01)
    }

    @Test
    fun concreteRebar_preliminaryEstimate_matchesHandCalculation() {
        val mod = CalcRegistry.byId(CalcModuleId.CONCRETE_REBAR)
        val res = mod.evaluate(
            UnitSystem.SI,
            "NOMINAL_VOL_EST",
            mapOf("length" to "6.0", "width" to "4.0", "thickness" to "0.20", "count" to "1", "sand_part" to "1.5", "agg_part" to "3.0", "bar_dia" to "12", "bar_spacing" to "200"),
            emptyMap()
        )
        assertTrue(res.isValid)
        assertEquals(CalcStatus.ESTIMATE, res.status)
        val wet = res.summaryMetrics.first { it.label.contains("Wet Concrete") }.rawValue!!
        val bars = res.summaryMetrics.first { it.label.contains("Rebar Count") }.rawValue!!
        assertEquals(5.04, wet, 1e-4)
        assertEquals(20.0, bars, 1e-4)
    }

    @Test
    fun geotechFooting_kernBoundary_and_overturning_behaveCorrectly() {
        val mod = CalcRegistry.byId(CalcModuleId.GEOTECH_FOOTING)

        // Concentric / inside kern
        val resOk = mod.evaluate(
            UnitSystem.SI,
            "ALLOWABLE_STRESS_GEOTECH",
            mapOf("p_service" to "650", "m_service" to "65", "footing_b" to "2.2", "footing_l" to "2.5", "depth_df" to "1.5", "thickness_tf" to "0.50", "q_allow" to "200"),
            emptyMap()
        )
        assertTrue(resOk.isValid)
        assertEquals(CalcStatus.PASS, resOk.status)
        val qMaxOk = resOk.summaryMetrics.first { it.label.contains("Max Bearing") }.rawValue!!
        // P_self = 2.2*2.5*(0.5*24 + 1.0*18) = 5.5*30 = 165 kN; P_total = 815 kN; e = 65/815 = 0.07975 m < 2.5/6 = 0.4167 m
        // q_max = (815/5.5)*(1 + 6*0.0797546/2.5) = 148.1818 * 1.19141 = 176.545 kPa
        assertEquals(176.55, qMaxOk, 0.1)

        // Outside kern (L/6 < e < L/2) -> warning & triangular distribution
        val resKernWarn = mod.evaluate(
            UnitSystem.SI,
            "ALLOWABLE_STRESS_GEOTECH",
            mapOf("p_service" to "400", "m_service" to "350", "footing_b" to "2.0", "footing_l" to "2.4", "depth_df" to "1.0", "thickness_tf" to "0.40", "q_allow" to "400"),
            emptyMap()
        )
        assertTrue(resKernWarn.issues.any { it.severity == IssueSeverity.WARNING && it.title.contains("Kern Limit") })

        // Overturning (e >= L/2) -> error & FAIL
        val resOverturn = mod.evaluate(
            UnitSystem.SI,
            "ALLOWABLE_STRESS_GEOTECH",
            mapOf("p_service" to "100", "m_service" to "500", "footing_b" to "2.0", "footing_l" to "2.0", "depth_df" to "1.0", "thickness_tf" to "0.40", "q_allow" to "250"),
            emptyMap()
        )
        assertFalse(resOverturn.isValid)
        assertEquals(CalcStatus.FAIL, resOverturn.status)
    }

    @Test
    fun hydrology_rationalAndManning_matchHandCalculations() {
        val rational = CalcRegistry.byId(CalcModuleId.HYDROLOGY_RATIONAL)
        val rRes = rational.evaluate(
            UnitSystem.SI,
            "RATIONAL_METHOD",
            mapOf("c_coeff" to "0.65", "intensity" to "85.0", "area" to "4.5"),
            emptyMap()
        )
        assertTrue(rRes.isValid)
        assertEquals(0.690625, rRes.summaryMetrics.first().rawValue!!, 1e-6)

        val manning = CalcRegistry.byId(CalcModuleId.HYDROLOGY_MANNING)
        val mRes = manning.evaluate(
            UnitSystem.SI,
            "MANNING_OPEN_CHANNEL",
            mapOf("b_width" to "1.50", "y_depth" to "0.80", "z_slope" to "2.0", "bed_slope" to "0.004", "manning_n" to "0.015"),
            emptyMap()
        )
        assertTrue(mRes.isValid)
        // A = (1.5 + 1.6)*0.8 = 2.48; P = 1.5 + 1.6*sqrt(5) = 5.07770876; Rh = 0.488409; V = (1/0.015)*Rh^(2/3)*sqrt(0.004) = 2.61529; Q = 6.4859 m3/s
        assertEquals(6.486, mRes.summaryMetrics.first { it.label.contains("Discharge") }.rawValue!!, 0.01)
    }

    @Test
    fun surveying_levelingAndShoelace_matchHandCalculations() {
        val level = CalcRegistry.byId(CalcModuleId.SURVEYING_LEVELING)
        val lRes = level.evaluate(
            UnitSystem.SI,
            "PLANE_SURVEY_LEVEL",
            mapOf("bm_rl" to "100.000", "bs_series" to "1.425, 1.810", "fs_series" to "1.120, 0.945", "horiz_dist" to "120.0", "known_close_rl" to "101.172"),
            emptyMap()
        )
        assertTrue(lRes.isValid)
        assertEquals(101.170, lRes.summaryMetrics.first { it.label.contains("Final Elevation") }.rawValue!!, 1e-6)
        assertEquals(0.975, lRes.summaryMetrics.first { it.label.contains("Average Grade") }.rawValue!!, 1e-6)

        val areaVol = CalcRegistry.byId(CalcModuleId.SURVEYING_AREA_VOLUME)
        val avRes = areaVol.evaluate(
            UnitSystem.SI,
            "SHOELACE_AEA",
            mapOf("coords" to "0,0; 50,0; 60,40; 10,40", "area_1" to "18.5", "area_2" to "24.0", "station_len" to "25.0"),
            emptyMap()
        )
        assertTrue(avRes.isValid)
        assertEquals(2000.0, avRes.summaryMetrics.first { it.label == "Polygon Area" }.rawValue!!, 1e-6)
        assertEquals(531.25, avRes.summaryMetrics.first { it.label == "Earthwork Volume" }.rawValue!!, 1e-6)
    }

    @Test
    fun reportFormatter_includesRequiredTraceabilityAndSafeguardNotice() {
        val mod = CalcRegistry.byId(CalcModuleId.STRUCTURAL_BEAM)
        val inputs = mod.defaultInputs(UnitSystem.SI, "ACI_318_19")
        val res = mod.evaluate(UnitSystem.SI, "ACI_318_19", inputs, emptyMap())
        val report = ReportFormatter.formatCalculationReport(
            projectName = "Civic Plaza",
            projectLocation = "Sector 4",
            calculationTitle = "Girder B-1",
            revisionNumber = 2,
            engineerName = "J. Doe, EIT",
            module = mod,
            result = res,
            rawInputs = inputs,
            notes = "Check stirrup spacing separately."
        )
        assertNotNull(report)
        assertTrue(report.contains("ACI 318-19"))
        assertTrue(report.contains("SAFEGUARD NOTICE"))
        assertTrue(report.contains("Girder B-1 (Rev 2)"))
        assertTrue(report.contains("EXPLICIT ASSUMPTIONS"))
    }

    @Test
    fun structuralBeam_eurocode2_and_boundaryInvalidInputs_behaveCorrectly() {
        val beam = CalcRegistry.byId(CalcModuleId.STRUCTURAL_BEAM)
        val ecRes = beam.evaluate(
            UnitSystem.SI,
            "EUROCODE_2",
            mapOf(
                "span" to "6.0", "w_u" to "28.0", "p_u" to "0.0", "w_s" to "19.5",
                "b" to "300", "d" to "450", "fc" to "28", "fy" to "500", "as_area" to "1472"
            ),
            emptyMap()
        )
        assertTrue(ecRes.isValid)
        assertEquals("EUROCODE_2", ecRes.standard?.id)
        assertTrue(ecRes.summaryMetrics.any { it.label.contains("Design Moment") })

        // Negative / zero / non-numeric inputs must fail cleanly without NaN
        val badRes = beam.evaluate(
            UnitSystem.SI,
            "ACI_318_19",
            mapOf(
                "span" to "0", "w_u" to "-5", "p_u" to "abc", "w_s" to "10",
                "b" to "300", "d" to "450", "fc" to "28", "fy" to "420", "as_area" to "1472"
            ),
            emptyMap()
        )
        assertFalse(badRes.isValid)
        assertEquals(CalcStatus.FAIL, badRes.status)
        assertTrue(badRes.issues.count { it.severity == IssueSeverity.ERROR } >= 3)
    }

    @Test
    fun roundingAndPrecision_keepsFullPrecisionInternallyWhileFormattingConsistently() {
        val raw = 123.4567891234
        val rounded2 = CalcFmt.num(raw, 2)
        assertEquals("123.46", rounded2)
        val mod = CalcRegistry.byId(CalcModuleId.UNIT_CONVERSION)
        val res = mod.evaluate(
            UnitSystem.SI,
            "NIST_SP811",
            mapOf("category" to "Length", "input_value" to "1.0", "from_unit" to "in", "to_unit" to "mm"),
            mapOf("display_decimals" to "1")
        )
        assertTrue(res.isValid)
        val metric = res.summaryMetrics.first()
        assertEquals("25.4", metric.formattedValue)
        assertEquals(25.4, metric.rawValue!!, 1e-12)
    }
}
