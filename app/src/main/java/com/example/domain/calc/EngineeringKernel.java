package com.example.domain.calc;

import java.util.List;
import java.util.Locale;

/**
 * Pure Java 11 civil engineering math and mechanics kernel.
 *
 * <p>Provides deterministic, UI-independent equations with full IEEE-754 double precision
 * internally and consistent formatting helpers. Designed for cross-language interoperability
 * with the Kotlin domain wrappers and Android Jetpack Compose UI.
 */
public final class EngineeringKernel {

    public static final double GRAVITY_SI = 9.80665;
    public static final double GRAVITY_US = 32.17405;
    public static final double STEEL_DENSITY_KG_M3 = 7850.0;
    public static final double CEMENT_BULK_DENSITY_KG_M3 = 1440.0;

    private EngineeringKernel() {
        // Utility class
    }

    // -------------------------------------------------------------------------
    // Formatting & Precision
    // -------------------------------------------------------------------------

    public static String formatNumber(double value, int decimals) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "\u2014";
        }
        double rounded = Math.abs(value) < 1e-12 ? 0.0 : value;
        return String.format(Locale.US, "%." + decimals + "f", rounded);
    }

    public static String formatSmart(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "\u2014";
        }
        double absVal = Math.abs(value);
        if (absVal == 0.0) {
            return "0.00";
        } else if (absVal >= 1000.0) {
            return formatNumber(value, 2);
        } else if (absVal >= 10.0) {
            return formatNumber(value, 2);
        } else if (absVal >= 0.1) {
            return formatNumber(value, 3);
        } else {
            return String.format(Locale.US, "%.4g", value);
        }
    }

    // -------------------------------------------------------------------------
    // Unit Conversions & Quantity Takeoff
    // -------------------------------------------------------------------------

    public static double convertViaBase(double value, double fromToBaseFactor, double toToBaseFactor) {
        return value * (fromToBaseFactor / toToBaseFactor);
    }

    public static double trapezoidalArea(double bottomWidth, double topWidth, double depth) {
        return 0.5 * (bottomWidth + topWidth) * depth;
    }

    public static double adjustedGrossVolume(double netVolume, double wastePct, double swellPct) {
        return netVolume * (1.0 + wastePct / 100.0) * (1.0 + swellPct / 100.0);
    }

    // -------------------------------------------------------------------------
    // Structural Mechanics (ACI 318-19 & Eurocode 2)
    // -------------------------------------------------------------------------

    public static final class BeamAciResult {
        public final double aMm;
        public final double beta1;
        public final double cMm;
        public final double epsT;
        public final double phiFlex;
        public final double rho;
        public final double rhoMin;
        public final double phiMnKnM;
        public final double phiVcKn;

        public BeamAciResult(
                double aMm,
                double beta1,
                double cMm,
                double epsT,
                double phiFlex,
                double rho,
                double rhoMin,
                double phiMnKnM,
                double phiVcKn
        ) {
            this.aMm = aMm;
            this.beta1 = beta1;
            this.cMm = cMm;
            this.epsT = epsT;
            this.phiFlex = phiFlex;
            this.rho = rho;
            this.rhoMin = rhoMin;
            this.phiMnKnM = phiMnKnM;
            this.phiVcKn = phiVcKn;
        }
    }

    public static BeamAciResult evaluateBeamAci318(
            double bMm,
            double dMm,
            double fcMpa,
            double fyMpa,
            double asMm2
    ) {
        double aMm = (asMm2 * fyMpa) / (0.85 * fcMpa * bMm);
        double rawBeta1 = 0.85 - 0.05 * ((fcMpa - 28.0) / 7.0);
        double beta1 = Math.max(0.65, Math.min(0.85, rawBeta1));
        double cMm = aMm / beta1;
        double epsT = 0.003 * ((dMm - cMm) / cMm);

        double phiFlex;
        if (epsT >= 0.005) {
            phiFlex = 0.90;
        } else if (epsT <= 0.002) {
            phiFlex = 0.65;
        } else {
            phiFlex = 0.65 + 0.25 * ((epsT - 0.002) / 0.003);
        }

        double rho = asMm2 / (bMm * dMm);
        double rhoMin = Math.max(0.25 * Math.sqrt(fcMpa) / fyMpa, 1.4 / fyMpa);
        double mnKnM = (asMm2 * fyMpa * (dMm - aMm / 2.0)) / 1.0e6;
        double phiMnKnM = phiFlex * mnKnM;
        double phiVcKn = 0.75 * (0.17 * Math.sqrt(fcMpa) * bMm * dMm) / 1000.0;

        return new BeamAciResult(aMm, beta1, cMm, epsT, phiFlex, rho, rhoMin, phiMnKnM, phiVcKn);
    }

    public static final class BeamEurocodeResult {
        public final double fcdMpa;
        public final double fydMpa;
        public final double xMm;
        public final double zMm;
        public final double rho;
        public final double rhoMin;
        public final double mRdKnM;
        public final double vRdCKn;

        public BeamEurocodeResult(
                double fcdMpa,
                double fydMpa,
                double xMm,
                double zMm,
                double rho,
                double rhoMin,
                double mRdKnM,
                double vRdCKn
        ) {
            this.fcdMpa = fcdMpa;
            this.fydMpa = fydMpa;
            this.xMm = xMm;
            this.zMm = zMm;
            this.rho = rho;
            this.rhoMin = rhoMin;
            this.mRdKnM = mRdKnM;
            this.vRdCKn = vRdCKn;
        }
    }

    public static BeamEurocodeResult evaluateBeamEurocode2(
            double bMm,
            double dMm,
            double fcMpa,
            double fyMpa,
            double asMm2
    ) {
        double fcd = 0.85 * fcMpa / 1.50;
        double fyd = fyMpa / 1.15;
        double xMm = (asMm2 * fyd) / (0.8 * fcd * bMm);
        double zMm = dMm - 0.4 * xMm;
        double rho = asMm2 / (bMm * dMm);
        double rhoMin = Math.max(0.26 * (0.30 * Math.pow(fcMpa, 2.0 / 3.0)) / fyMpa, 0.0013);
        double mRdKnM = (asMm2 * fyd * zMm) / 1.0e6;

        double k = Math.min(2.0, 1.0 + Math.sqrt(200.0 / dMm));
        double rhoL = Math.min(0.02, rho);
        double vRdC = Math.max(
                (0.18 / 1.50) * k * Math.pow(100.0 * rhoL * fcMpa, 1.0 / 3.0),
                0.035 * Math.pow(k, 1.5) * Math.sqrt(fcMpa)
        ) * bMm * dMm / 1000.0;

        return new BeamEurocodeResult(fcd, fyd, xMm, zMm, rho, rhoMin, mRdKnM, vRdC);
    }

    // -------------------------------------------------------------------------
    // Concrete Batching & Rebar Takeoff
    // -------------------------------------------------------------------------

    public static final class ConcreteRebarResult {
        public final double wetWithWasteM3;
        public final double dryVolM3;
        public final double cementMassKg;
        public final double bags50Kg;
        public final double bags94Lb;
        public final double sandVolM3;
        public final double aggVolM3;
        public final int totalBars;
        public final double totalBarLenM;
        public final double unitWeightKgPerM;
        public final double totalSteelKg;

        public ConcreteRebarResult(
                double wetWithWasteM3,
                double dryVolM3,
                double cementMassKg,
                double bags50Kg,
                double bags94Lb,
                double sandVolM3,
                double aggVolM3,
                int totalBars,
                double totalBarLenM,
                double unitWeightKgPerM,
                double totalSteelKg
        ) {
            this.wetWithWasteM3 = wetWithWasteM3;
            this.dryVolM3 = dryVolM3;
            this.cementMassKg = cementMassKg;
            this.bags50Kg = bags50Kg;
            this.bags94Lb = bags94Lb;
            this.sandVolM3 = sandVolM3;
            this.aggVolM3 = aggVolM3;
            this.totalBars = totalBars;
            this.totalBarLenM = totalBarLenM;
            this.unitWeightKgPerM = unitWeightKgPerM;
            this.totalSteelKg = totalSteelKg;
        }
    }

    public static ConcreteRebarResult computeConcreteAndRebarSi(
            double lengthM,
            double widthM,
            double thicknessM,
            int count,
            double sandPart,
            double aggPart,
            double barDiaMm,
            double barSpacingMm,
            double wastePct,
            double dryFactor,
            double coverMm,
            double lapPct
    ) {
        double wetVolM3 = lengthM * widthM * thicknessM * count;
        double wetWithWasteM3 = wetVolM3 * (1.0 + wastePct / 100.0);
        double dryVolM3 = wetWithWasteM3 * dryFactor;
        double totalParts = 1.0 + sandPart + aggPart;

        double cementVolM3 = dryVolM3 * (1.0 / totalParts);
        double cementMassKg = cementVolM3 * CEMENT_BULK_DENSITY_KG_M3;
        double bags50Kg = Math.ceil(cementMassKg / 50.0);
        double bags94Lb = Math.ceil((cementMassKg * 2.20462262) / 94.0);
        double sandVolM3 = dryVolM3 * (sandPart / totalParts);
        double aggVolM3 = dryVolM3 * (aggPart / totalParts);

        double effDistWidthMm = Math.max(0.0, widthM * 1000.0 - 2.0 * coverMm);
        int barsPerMember = (int) Math.floor(effDistWidthMm / barSpacingMm) + 1;
        int totalBars = barsPerMember * Math.max(1, count);
        double barCutLenM = Math.max(0.0, lengthM - 2.0 * (coverMm / 1000.0)) * (1.0 + lapPct / 100.0);
        double totalBarLenM = totalBars * barCutLenM;
        double unitWeightKgPerM = STEEL_DENSITY_KG_M3 * (Math.PI * (barDiaMm / 1000.0) * (barDiaMm / 1000.0) / 4.0);
        double totalSteelKg = totalBarLenM * unitWeightKgPerM * (1.0 + wastePct / 100.0);

        return new ConcreteRebarResult(
                wetWithWasteM3, dryVolM3, cementMassKg, bags50Kg, bags94Lb,
                sandVolM3, aggVolM3, totalBars, totalBarLenM, unitWeightKgPerM, totalSteelKg
        );
    }

    // -------------------------------------------------------------------------
    // Geotechnical Footing Bearing & Kern Check
    // -------------------------------------------------------------------------

    public static final class FootingPressureResult {
        public final double area;
        public final double selfWeight;
        public final double pTotal;
        public final double eccentricity;
        public final double kernLimit;
        public final double qMax;
        public final double qMin;
        public final boolean overturning;
        public final boolean outsideKern;
        public final double bReqSquare;

        public FootingPressureResult(
                double area,
                double selfWeight,
                double pTotal,
                double eccentricity,
                double kernLimit,
                double qMax,
                double qMin,
                boolean overturning,
                boolean outsideKern,
                double bReqSquare
        ) {
            this.area = area;
            this.selfWeight = selfWeight;
            this.pTotal = pTotal;
            this.eccentricity = eccentricity;
            this.kernLimit = kernLimit;
            this.qMax = qMax;
            this.qMin = qMin;
            this.overturning = overturning;
            this.outsideKern = outsideKern;
            this.bReqSquare = bReqSquare;
        }
    }

    public static FootingPressureResult computeFootingPressure(
            double pService,
            double mService,
            double bWidth,
            double lLength,
            double depthDf,
            double thicknessTf,
            double gammaConc,
            double gammaSoil,
            double qAllow
    ) {
        double area = bWidth * lLength;
        double soilDepth = Math.max(0.0, depthDf - thicknessTf);
        double selfWeight = area * (thicknessTf * gammaConc + soilDepth * gammaSoil);
        double pTotal = pService + selfWeight;
        double ecc = Math.abs(mService) / pTotal;
        double kern = lLength / 6.0;

        double qMax;
        double qMin;
        boolean overturning = false;
        boolean outsideKern = false;

        if (ecc >= lLength / 2.0) {
            overturning = true;
            qMax = Double.POSITIVE_INFINITY;
            qMin = 0.0;
        } else if (ecc > kern) {
            outsideKern = true;
            qMax = (2.0 * pTotal) / (3.0 * bWidth * (lLength / 2.0 - ecc));
            qMin = 0.0;
        } else {
            qMax = (pTotal / area) * (1.0 + 6.0 * ecc / lLength);
            qMin = (pTotal / area) * (1.0 - 6.0 * ecc / lLength);
        }

        double bReqSquare = Math.sqrt(pTotal / qAllow);
        return new FootingPressureResult(
                area, selfWeight, pTotal, ecc, kern, qMax, qMin, overturning, outsideKern, bReqSquare
        );
    }

    // -------------------------------------------------------------------------
    // Hydrology (Rational Method & Manning's Open Channel)
    // -------------------------------------------------------------------------

    public static double rationalPeakRunoff(double cEff, double intensity, double area, boolean isSi) {
        if (isSi) {
            return (cEff * intensity * area) / 360.0;
        }
        return cEff * intensity * area * 1.0083333333;
    }

    public static final class ManningResult {
        public final double area;
        public final double wettedPerimeter;
        public final double hydraulicRadius;
        public final double topWidth;
        public final double hydraulicDepth;
        public final double velocity;
        public final double discharge;
        public final double froudeNumber;

        public ManningResult(
                double area,
                double wettedPerimeter,
                double hydraulicRadius,
                double topWidth,
                double hydraulicDepth,
                double velocity,
                double discharge,
                double froudeNumber
        ) {
            this.area = area;
            this.wettedPerimeter = wettedPerimeter;
            this.hydraulicRadius = hydraulicRadius;
            this.topWidth = topWidth;
            this.hydraulicDepth = hydraulicDepth;
            this.velocity = velocity;
            this.discharge = discharge;
            this.froudeNumber = froudeNumber;
        }
    }

    public static ManningResult computeManningTrapezoidal(
            double bottomWidth,
            double flowDepth,
            double sideSlopeZ,
            double bedSlope,
            double manningN,
            boolean isSi
    ) {
        double k = isSi ? 1.0 : 1.48591867;
        double g = isSi ? GRAVITY_SI : GRAVITY_US;
        double area = (bottomWidth + sideSlopeZ * flowDepth) * flowDepth;
        double wetPerim = bottomWidth + 2.0 * flowDepth * Math.sqrt(1.0 + sideSlopeZ * sideSlopeZ);
        double rh = area / wetPerim;
        double topWidth = bottomWidth + 2.0 * sideSlopeZ * flowDepth;
        double hydDepth = area / topWidth;
        double velocity = (k / manningN) * Math.pow(rh, 2.0 / 3.0) * Math.sqrt(bedSlope);
        double discharge = area * velocity;
        double froude = velocity / Math.sqrt(g * hydDepth);

        return new ManningResult(area, wetPerim, rh, topWidth, hydDepth, velocity, discharge, froude);
    }

    // -------------------------------------------------------------------------
    // Surveying & Geometry (Shoelace Polygon & Earthwork)
    // -------------------------------------------------------------------------

    public static double shoelaceDoubleSignedArea(List<Double> xCoords, List<Double> yCoords) {
        int n = Math.min(xCoords.size(), yCoords.size());
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            sum += (xCoords.get(i) * yCoords.get(next)) - (xCoords.get(next) * yCoords.get(i));
        }
        return sum;
    }

    public static double polygonPerimeter(List<Double> xCoords, List<Double> yCoords) {
        int n = Math.min(xCoords.size(), yCoords.size());
        double perimeter = 0.0;
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            perimeter += Math.hypot(xCoords.get(next) - xCoords.get(i), yCoords.get(next) - yCoords.get(i));
        }
        return perimeter;
    }

    public static double averageEndAreaVolume(double area1, double area2, double stationLength, double shrinkFactor) {
        return 0.5 * (area1 + area2) * stationLength * shrinkFactor;
    }
}
