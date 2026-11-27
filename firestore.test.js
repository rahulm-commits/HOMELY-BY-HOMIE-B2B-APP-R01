const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-homelyby-4cbfe8dc-b3ba-4dfb-8466-45d0f5cddd0b";
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

function validProductPayload(uid) {
  return {
    userId: uid,
    productCode: "PRD-00101",
    sku: "SKU-BNC-001",
    barcode: "8901234567890",
    name: "Kerala Special Banana Chips",
    category: "Banana Chips",
    brand: "Homely Gold",
    baseUnit: "Gram (g)",
    sellingUnit: "Pouch / Packet",
    casePack: "24 packets per box",
    packSize: "100g",
    wholesalePrice: 42.0,
    mrp: 50.0,
    stockQuantity: 120,
    minThreshold: 30,
    photoUri: "",
    notes: "Nendran chips",
    status: "active",
    salesVelocity: 90,
    hasVariants: false,
    variantsJson: "",
    addedDate: "2026-11-18",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validPaymentPayload(uid) {
  return {
    userId: uid,
    receiptCode: "RCPT-2026-9901",
    direction: "collect",
    partyId: "shop_1",
    partyCode: "SHP-101",
    partyName: "Malabar Bakery & Snacks",
    contactName: "Suresh Nair",
    phone: "9847012345",
    invoiceRef: "ORD-2040",
    amount: 4500.0,
    paymentMethod: "upi",
    referenceNumber: "UPI/AXIS/4092188492",
    paymentDate: "2026-11-18",
    recordedBy: "Field Officer",
    note: "Counter UPI settlement",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validSupplierBillPayload(uid) {
  return {
    userId: uid,
    supplierCode: "SUP-001",
    supplierName: "Kera Pure Coconut Mills",
    contactPerson: "Ramesh Menon",
    phone: "9846077123",
    location: "Edappally, Kochi",
    category: "Cooking Oil",
    billNumber: "BILL-402",
    totalBilled: 18500.0,
    paidAmount: 6500.0,
    billDate: "2026-11-08",
    dueDate: "2026-11-15",
    status: "overdue",
    notes: "Wood-pressed coconut oil 200L batch",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validSupplierPayload(uid) {
  return {
    userId: uid,
    supplierCode: "SUP-000044",
    name: "Malabar Wholesale Distributors",
    category: "Wholesale Staples & Grains",
    contactPerson: "Mohammed Shafi",
    contactRole: "Key Account Mgr",
    altContactPerson: "",
    phone: "9876554321",
    altPhone: "",
    email: "sales@malabargrocery.com",
    address: "Kalamassery, Ernakulam, Kerala",
    pinCode: "683104",
    mapsLink: "https://maps.google.com/?q=10.0518,76.3197",
    latitude: 10.0518,
    longitude: 76.3197,
    photoUri: "",
    gstin: "32AABCM4421K1Z5",
    kycVerified: true,
    creditTerms: "Net 15 Days",
    confirmedPriceNotes: "Matta Rice 50kg: ₹2,450",
    notes: "Primary wholesale supplier",
    status: "active",
    addedDate: "2026-11-19",
    addedBy: "Field Officer",
    createdAt: new Date(),
    updatedAt: new Date(),
  };
}

function validRoutePayload(uid) {
  return {
    userId: uid,
    name: "Route 4: Kochi - Thrissur",
    vehicle: "Tata Ace (KL-07-CD-4190)",
    maxCrates: 35,
    driverName: "Kiran Das",
    driverPhone: "+91 98452 10980",
    dispatchWindow: "06:30 AM – 10:30 AM",
    stagingBay: "Bay #03 - Central Dock",
    colorIndex: 3,
    isActive: true,
    stopOrderIds: ["seed_shop_1", "seed_shop_2"],
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

test("Owner can create and update product catalog item, non-owner denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prd_1");
  await assertSucceeds(ref.set(validProductPayload(ALICE_UID)));
  await assertSucceeds(ref.update({ stockQuantity: 250, status: "inactive", updatedAt: new Date() }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prd_2").set(validProductPayload(BOB_UID))
  );
});

test("Owner can record payment and manage supplier bills, non-owner denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const payRef = aliceDb.collection("users").doc(ALICE_UID).collection("payments").doc("pay_1");
  const billRef = aliceDb.collection("users").doc(ALICE_UID).collection("supplierBills").doc("bill_1");

  await assertSucceeds(payRef.set(validPaymentPayload(ALICE_UID)));
  await assertSucceeds(billRef.set(validSupplierBillPayload(ALICE_UID)));
  await assertSucceeds(billRef.update({ paidAmount: 18500.0, status: "settled", updatedAt: new Date() }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("payments").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("supplierBills").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("payments").doc("pay_2").set(validPaymentPayload(BOB_UID))
  );
});

test("Owner can create, read, and update supplier directory item, non-owner denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const supRef = aliceDb.collection("users").doc(ALICE_UID).collection("suppliers").doc("sup_1");

  await assertSucceeds(supRef.set(validSupplierPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("suppliers").get());
  await assertSucceeds(supRef.update({ status: "inactive", kycVerified: false, updatedAt: new Date() }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("suppliers").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("suppliers").doc("sup_2").set(validSupplierPayload(BOB_UID))
  );
});

test("Owner can create, read, update delivery routes; rejects cross-user access and shadow fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const routeRef = aliceDb.collection("users").doc(ALICE_UID).collection("routes").doc("R04-KOCHI-THRISSUR");

  await assertSucceeds(routeRef.set(validRoutePayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("routes").get());
  await assertSucceeds(
    routeRef.update({
      driverName: "Suresh Babu",
      stopOrderIds: ["seed_shop_2", "seed_shop_1"],
      updatedAt: new Date(),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("routes").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("routes").doc("R04-KOCHI-THRISSUR").set(validRoutePayload(BOB_UID))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("routes").doc("R05-BAD").set({
      ...validRoutePayload(ALICE_UID),
      isAdmin: true,
    })
  );
});


