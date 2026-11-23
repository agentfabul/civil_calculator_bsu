package com.example;

import com.example.domain.calc.EngineeringKernel;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pure Java 11 unit tests verifying the EngineeringKernel math & mechanics kernel directly.
 */
public class JavaKernelTest {

    @Test
    public void beamAci318_and_eurocode2_matchHandCheck() {
        EngineeringKernel.BeamAciResult aci = EngineeringKernel.evaluateBeamAci318(
                300.0, 450.0, 28.0, 420.0, 1472.0
        );
        assertEquals(0.85, aci.beta1, 1e-6);
        assertEquals(0.90, aci.phiFlex, 1e-6);
        assertEquals(226.30, aci.phiMnKnM, 0.25);

        EngineeringKernel.BeamEurocodeResult ec2 = EngineeringKernel.evaluateBeamEurocode2(
                300.0, 450.0, 28.0, 500.0, 1472.0
        );
        assertTrue(ec2.mRdKnM > 230.0);
        assertTrue(ec2.vRdCKn > 50.0);
    }

    @Test
    public void footingKern_and_manning_and_shoelace_matchHandCheck() {
        EngineeringKernel.FootingPressureResult footingOk = EngineeringKernel.computeFootingPressure(
                650.0, 65.0, 2.2, 2.5, 1.5, 0.50, 24.0, 18.0, 200.0
        );
        assertFalse(footingOk.outsideKern);
        assertFalse(footingOk.overturning);
        assertEquals(176.55, footingOk.qMax, 0.1);

        EngineeringKernel.FootingPressureResult footingOverturn = EngineeringKernel.computeFootingPressure(
                100.0, 500.0, 2.0, 2.0, 1.0, 0.40, 24.0, 18.0, 250.0
        );
        assertTrue(footingOverturn.overturning);
        assertTrue(Double.isInfinite(footingOverturn.qMax));

        EngineeringKernel.ManningResult manning = EngineeringKernel.computeManningTrapezoidal(
                1.50, 0.80, 2.0, 0.004, 0.015, true
        );
        assertEquals(2.48, manning.area, 1e-6);
        assertEquals(6.486, manning.discharge, 0.01);

        List<Double> xs = Arrays.asList(0.0, 50.0, 60.0, 10.0);
        List<Double> ys = Arrays.asList(0.0, 0.0, 40.0, 40.0);
        double doubleArea = EngineeringKernel.shoelaceDoubleSignedArea(xs, ys);
        assertEquals(2000.0, 0.5 * Math.abs(doubleArea), 1e-6);
    }

    @Test
    public void formatting_and_unitConversion_keepPrecision() {
        double psi = EngineeringKernel.convertViaBase(28.0, 1.0e6, 6894.757293168361);
        assertEquals(4061.0566, psi, 0.01);
        assertEquals("4061.06", EngineeringKernel.formatNumber(psi, 2));
        assertEquals("0.00", EngineeringKernel.formatNumber(-1e-15, 2));
    }
}
