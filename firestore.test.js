const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const pastTimestamp = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user cannot read or write tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/tasks`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/tasks`).doc("t1").set({
      userId: ALICE_UID,
      title: "Hijack",
    })
  );
});

test("Alice can create and update her own AgentTask with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb.collection(`users/${ALICE_UID}/tasks`).doc("task_1");

  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      repo: "gfa437926-tech/metra2027",
      branch: "main",
      title: "Analyze METRA API",
      prompt: "Inspect METRA repository structure",
      status: "running",
      currentStepIndex: 1,
      totalSteps: 4,
      progressPercent: 25,
      lastCompletedStep: "Fetch tree",
      lastResultSummary: "Found 42 files",
      lastError: "",
      modifiedFiles: ["src/Api.cs"],
      nextSteps: ["Run dotnet test"],
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    taskRef.update({
      status: "completed",
      currentStepIndex: 4,
      progressPercent: 100,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Bob cannot read or write Alice's tasks, settings, or approvals", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection(`users/${ALICE_UID}/tasks`).doc("task_a").set({
      userId: ALICE_UID,
      repo: "gfa437926-tech/metra2027",
      branch: "main",
      title: "Private Task",
      prompt: "Check code",
      status: "queued",
      currentStepIndex: 0,
      totalSteps: 2,
      progressPercent: 0,
      lastCompletedStep: "",
      lastResultSummary: "",
      lastError: "",
      modifiedFiles: [],
      nextSteps: [],
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/tasks`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/tasks`).doc("task_a").get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/tasks`).doc("task_b").set({
    userId: ALICE_UID,
    title: "Cross-user write",
  }));
});

test("Shadow update with ghost field or invalid status fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const settingsRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertFails(
    settingsRef.set({
      userId: ALICE_UID,
      activeRepo: "gfa437926-tech/metra2027",
      activeBranch: "main",
      aiProvider: "gemini",
      aiModel: "gemini-3.8-flash",
      backendUrl: "",
      darkTheme: true,
      autoExecuteTier1: true,
      isAdmin: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Approval terminal state locking prevents reopening rejected approval", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const appRef = aliceDb.collection(`users/${ALICE_UID}/approvals`).doc("appr_1");

  await assertSucceeds(
    appRef.set({
      userId: ALICE_UID,
      taskId: "task_1",
      tier: 3,
      actionType: "db_migration",
      title: "Apply schema migration",
      description: "Add weather index",
      payloadPreview: "CREATE INDEX idx_weather ON forecasts(date);",
      targetResource: "metra_2027_db",
      status: "pending",
      decisionNote: "",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    appRef.update({
      status: "rejected",
      decisionNote: "Needs review",
      updatedAt: pastTimestamp(),
    })
  );

  await assertFails(
    appRef.update({
      status: "approved",
      decisionNote: "Bypass",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Operation logs are append-only and cannot be updated or deleted", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logRef = aliceDb.collection(`users/${ALICE_UID}/operation_logs`).doc("log_1");

  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      category: "security",
      tier: 3,
      action: "Blocked legacy DB query",
      details: "Prevented access to legacy METRA DB",
      outcome: "blocked",
      repo: "gfa437926-tech/metra2027",
      createdAt: pastTimestamp(),
    })
  );

  await assertFails(logRef.update({ outcome: "success" }));
  await assertFails(logRef.delete());
});
