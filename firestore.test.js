const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-snackrou-17f8b2d0-faf2-4987-b293-b1960e9298c8";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validShopPayload(uid) {
  return {
    userId: uid,
    shopCode: "SHP-101",
    name: "Malabar Bakery & Snacks",
    ownerName: "Suresh Nair",
    phone: "9847012345",
    secondaryPhone: "9847099999",
    address: "MG Road, Kannur",
    pinCode: "670001",
    routeId: "R01-KANNUR-BANGALORE",
    photoUri: "",
    latitude: 11.8745,
    longitude: 75.3704,
    note: "Prefers morning delivery",
    status: "active",
    inquiryStatus: "pending_inquiry",
    enquiryDate: "2026-11-16",
    callDate: "2026-11-16",
    lastOrderDate: "2026-11-12",
    lastCallDate: "2026-11-14",
    confirmedPriceNotes: "BC100: ₹42",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validOrderPayload(uid) {
  return {
    userId: uid,
    orderCode: "ORD-2041",
    shopId: "shop_1",
    shopCode: "SHP-101",
    shopName: "Malabar Bakery & Snacks",
    routeId: "R01-KANNUR-BANGALORE",
    status: "confirmed",
    totalAmount: 4200.0,
    itemCount: 80,
    itemsSummary: "BC100 x50, JB200 x30",
    orderDate: "2026-11-16",
    note: "Urgent",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validStockPayload(uid) {
  return {
    userId: uid,
    productCode: "BC100",
    productName: "Banana Chips",
    packSize: "100g",
    category: "Banana Chips",
    quantity: 120,
    minThreshold: 40,
    unitPrice: 45.0,
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

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

test("Unauthenticated user cannot read or write shops", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("shops").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("shops").doc("shop_1").set(validShopPayload(ALICE_UID))
  );
});

test("Authenticated owner can create, read, and update own shop", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("shops").doc("shop_1");
  await assertSucceeds(ref.set(validShopPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("shops").get());
  await assertSucceeds(
    ref.update({
      inquiryStatus: "confirmed",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user cannot read or write another user's shops", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("shops").doc("shop_1").set(validShopPayload(ALICE_UID))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("shops").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("shops").doc("shop_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("shops").doc("shop_2").set(validShopPayload(BOB_UID))
  );
});

test("Rejects shadow ghost field on shop creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const badPayload = { ...validShopPayload(ALICE_UID), isAdmin: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("shops").doc("shop_1").set(badPayload)
  );
});

test("Rejects terminal order update once delivered", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("ord_1");
  await assertSucceeds(ref.set({ ...validOrderPayload(ALICE_UID), status: "delivered" }));
  await assertFails(ref.update({ status: "pending", updatedAt: new Date() }));
});

test("Owner can create and update stock item quantity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("stocks").doc("stk_1");
  await assertSucceeds(ref.set(validStockPayload(ALICE_UID)));
  await assertSucceeds(ref.update({ quantity: 200, updatedAt: new Date() }));
});
