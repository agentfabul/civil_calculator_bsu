const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_eng_123";
const BOB_UID = "bob_eng_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write profiles or projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      name: "Bridge Pier",
      defaultUnitSystem: "SI",
      defaultStandard: "ACI_ASCE",
      calculationCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("User profile create, partial update, ghost field rejection, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Valid create matching Kotlin UserProfile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Engineer",
      email: "alice@example.com",
      organization: "Apex Civil",
      preferredUnitSystem: "SI",
      preferredStandard: "ACI_ASCE",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      organization: "Apex Structural Group",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / role escalation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      preferredUnitSystem: "US",
      preferredStandard: "ACI_ASCE",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Projects, calculations, and immutable revisions full lifecycle and security bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const t0 = new Date(Date.now() - 10000);
  const t1 = new Date(Date.now() - 2000);

  const projRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  await assertSucceeds(
    projRef.set({
      userId: ALICE_UID,
      name: "North Culvert Crossing",
      clientName: "City DPW",
      location: "Zone 2",
      notes: "Preliminary sizing",
      defaultUnitSystem: "SI",
      defaultStandard: "ACI_ASCE",
      calculationCount: 0,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Reject @DocumentId "id" inside body
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_bad").set({
      id: "proj_bad",
      userId: ALICE_UID,
      name: "Bad Project",
      defaultUnitSystem: "SI",
      defaultStandard: "ACI_ASCE",
      calculationCount: 0,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Reject orphan calculation (parent project missing)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("missing_proj")
      .collection("calculations").doc("calc_1").set({
        userId: ALICE_UID,
        projectId: "missing_proj",
        moduleId: "STRUCTURAL_BEAM",
        title: "Girder G1",
        unitSystem: "SI",
        standardId: "ACI_318_19",
        latestRevisionNumber: 1,
        latestStatus: "PASS",
        latestSummary: "Utilization 0.62",
        latestUtilization: 0.62,
        latestInputs: { span: "6.0", w: "20.0" },
        latestAssumptions: { support: "simply_supported" },
        createdAt: t0,
        updatedAt: t0,
      })
  );

  // Valid calculation create under existing project
  const calcRef = projRef.collection("calculations").doc("calc_1");
  await assertSucceeds(
    calcRef.set({
      userId: ALICE_UID,
      projectId: "proj_1",
      moduleId: "STRUCTURAL_BEAM",
      title: "Girder G1",
      unitSystem: "SI",
      standardId: "ACI_318_19",
      latestRevisionNumber: 1,
      latestStatus: "PASS",
      latestSummary: "Mmax = 90.0 kN·m | Utilization 0.62",
      latestUtilization: 0.62,
      latestInputs: { span: "6.0", w: "20.0" },
      latestAssumptions: { support: "simply_supported" },
      latestNotes: "Initial run",
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Valid calculation update (revising)
  await assertSucceeds(
    calcRef.update({
      latestRevisionNumber: 2,
      latestStatus: "WARNING",
      latestSummary: "Deflection near L/360",
      latestUtilization: 0.88,
      latestInputs: { span: "6.0", w: "25.0" },
      updatedAt: t1,
    })
  );

  // Reject moduleId mutation on update
  await assertFails(
    calcRef.update({
      moduleId: "HYDROLOGY_RATIONAL",
      updatedAt: t1,
    })
  );

  // Valid immutable revision create
  const revRef = calcRef.collection("revisions").doc("rev_1");
  await assertSucceeds(
    revRef.set({
      userId: ALICE_UID,
      projectId: "proj_1",
      calcId: "calc_1",
      revisionNumber: 1,
      revisionNote: "Baseline load",
      moduleId: "STRUCTURAL_BEAM",
      title: "Girder G1",
      unitSystem: "SI",
      standardId: "ACI_318_19",
      standardLabel: "ACI 318-19 / ASCE 7-22",
      status: "PASS",
      governingCheck: "Flexure Mu / phi*Mn = 0.62",
      utilizationRatio: 0.62,
      inputs: { span: "6.0", w: "20.0" },
      assumptions: { support: "simply_supported" },
      primaryOutputs: ["Mmax = 90.00 kN·m", "Vmax = 60.00 kN"],
      warnings: [],
      softwareVersion: "1.0.0",
      createdAt: t0,
    })
  );

  // Reject revision update (immutable audit snapshot)
  await assertFails(
    revRef.update({
      status: "FAIL",
    })
  );

  // List queries succeed for owner, fail for Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertSucceeds(projRef.collection("calculations").get());
  await assertSucceeds(calcRef.collection("revisions").get());

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1")
      .collection("calculations").get()
  );
});
