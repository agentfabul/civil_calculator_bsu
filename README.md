# CivilSpec — Civil Engineering Calculation & Specification Workspace

**CivilSpec** is a Java + Kotlin Android application (Jetpack Compose, Material 3, Firebase Auth & Cloud Firestore) providing transparent, traceable civil engineering calculations across 6 disciplines (12 calculators).

> **Engineering Safeguard Notice:** CivilSpec outputs are **decision support only**. A passing check does not constitute proof that a complete design is safe. Independent review and seal by a qualified Professional Engineer (PE/SE) is required prior to construction, fabrication, or permitting use.

---

## Install on Android (Option 1 — GitHub Releases APK)

1. Open the **Releases** page on this GitHub repository.
2. Under the latest release, download the **`.apk`** file (for example, `CivilSpec-v1.0.0.apk`).
3. Open the downloaded `.apk` on your Android device and approve installation if prompted (**Install unknown apps**).
4. Sign in with **Google** on launch to register your workspace and sync projects/revisions via Cloud Firestore.

### How to Publish a Release APK
- **From Google AI Studio:** Export/generate the APK via the **Settings** menu, push the repository to GitHub, and attach the `.apk` to a new GitHub Release.
- **Automated via GitHub Actions:** Once this repo is pushed to GitHub, creating and pushing a version tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`) or running the **Build & Publish CivilSpec Android APK** workflow manually under the **Actions** tab will run the Java + Kotlin unit tests, build the APK, and publish it directly to **GitHub Releases**.

---

## Included Calculation Modules (SI & US Customary)
1. **Units & Quantity Takeoff:** Engineering Unit Converter (NIST SP 811), Trapezoidal Trench/Material Takeoff
2. **Structural Basics (ACI 318-19 / ASCE 7-22 & Eurocode 2 / EN 1990):** RC Beam Flexure/Shear/Deflection, Short Tied RC Column, One-Way Slab, Load Combinations
3. **Concrete & Rebar (Preliminary Estimate):** Concrete Dry-Mix Batching & Bar Count/Mass
4. **Soil & Foundation Basics (Preliminary Estimate):** Spread Footing Bearing, Kern Check, Uplift & Overturning
5. **Hydrology Basics:** Rational Method Peak Runoff & Manning's Open-Channel Flow
6. **Surveying & Geometry:** Differential Leveling (HI Method & Closure Check) & Shoelace Polygon Area / Earthwork Volume
