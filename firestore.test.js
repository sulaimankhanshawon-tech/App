const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const RESTAURANT_ID = "asil_al_tawooq_jeddah";
const ALICE_UID = "alice_guest_1";
const BOB_UID = "bob_guest_2";
const OWNER_UID = "owner_admin_1";
const OWNER_EMAIL = "sulaimankhanshawon@gmail.com";
const MANAGER_UID = "manager_1";
const MANAGER_EMAIL = "manager@asilaltawooq.sa";
const WAITER_UID = "waiter_1";
const WAITER_EMAIL = "waiter@asilaltawooq.sa";
const KITCHEN_UID = "kitchen_1";
const KITCHEN_EMAIL = "kitchen@asilaltawooq.sa";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function ownerContext(emailVerified = true) {
  return testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: emailVerified,
  });
}

function userContext(uid, email, emailVerified = true) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: emailVerified,
  });
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

test("Unauthenticated user cannot read or write any restaurant collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("menu_items").where("restaurantId", "==", RESTAURANT_ID).get());
  await assertFails(unauthDb.collection("product_images").where("restaurantId", "==", RESTAURANT_ID).get());
  await assertFails(unauthDb.collection("qr_codes").where("restaurantId", "==", RESTAURANT_ID).get());
  await assertFails(unauthDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(
    unauthDb.collection("orders").doc("ord_1").set({
      userId: ALICE_UID,
      restaurantId: RESTAURANT_ID,
      tableId: "table_01",
    })
  );
});

test("Owner email spoofing with email_verified: false is rejected", async () => {
  const spoofAdminDb = ownerContext(false).firestore();
  const past = new Date(Date.now() - 2000);
  await assertFails(
    spoofAdminDb.collection("menu_items").doc("dish_1").set({
      restaurantId: RESTAURANT_ID,
      nameEn: "Chicken Tawook",
      nameAr: "طاووق دجاج",
      descriptionEn: "Charcoal marinated skewers",
      category: "CHICKEN_TAWOOK",
      kitchenStation: "GRILL",
      spiceLevel: "MILD",
      portionSize: "250g",
      priceSar: 42.0,
      prepMinutes: 14,
      available: true,
      hidden: false,
      featured: true,
      imageKey: "grills",
      demoData: false,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("RBAC enforces Owner, Manager, Waiter, Kitchen, and Customer boundaries", async () => {
  const ownerDb = ownerContext(true).firestore();
  const managerDb = userContext(MANAGER_UID, MANAGER_EMAIL).firestore();
  const waiterDb = userContext(WAITER_UID, WAITER_EMAIL).firestore();
  const kitchenDb = userContext(KITCHEN_UID, KITCHEN_EMAIL).firestore();
  const aliceDb = userContext(ALICE_UID, "alice@example.com").firestore();
  const past = new Date(Date.now() - 3000);

  // Owner provisions Manager, Waiter (by email), and Kitchen Staff
  await assertSucceeds(
    ownerDb.collection("staff").doc(MANAGER_EMAIL).set({
      uid: MANAGER_EMAIL,
      restaurantId: RESTAURANT_ID,
      name: "Nasser Manager",
      email: MANAGER_EMAIL,
      role: "MANAGER",
      active: true,
      assignedZone: "All Zones",
      ordersHandled: 0,
      avgResponseSec: 25,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    managerDb.collection("staff").doc(WAITER_EMAIL).set({
      uid: WAITER_EMAIL,
      restaurantId: RESTAURANT_ID,
      name: "Tariq Waiter",
      email: WAITER_EMAIL,
      role: "WAITER",
      active: true,
      assignedZone: "Main Hall",
      ordersHandled: 0,
      avgResponseSec: 30,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    managerDb.collection("staff").doc(KITCHEN_UID).set({
      uid: KITCHEN_UID,
      restaurantId: RESTAURANT_ID,
      name: "Chef Sami",
      email: KITCHEN_EMAIL,
      role: "KITCHEN",
      active: true,
      assignedZone: "GRILL",
      ordersHandled: 0,
      avgResponseSec: 30,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Manager CANNOT escalate any account to OWNER
  await assertFails(
    managerDb.collection("staff").doc("rogue_owner").set({
      uid: "rogue_owner",
      restaurantId: RESTAURANT_ID,
      name: "Rogue",
      email: "rogue@example.com",
      role: "OWNER",
      active: true,
      assignedZone: "All",
      ordersHandled: 0,
      avgResponseSec: 10,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Manager can create tables, QR codes, product images, and menu products
  const tablePayload = {
    restaurantId: RESTAURANT_ID,
    tableNumber: 1,
    label: "T01",
    zone: "Main Hall",
    capacity: 4,
    active: true,
    status: "AVAILABLE",
    assignedWaiterName: "Tariq Waiter",
    billStatus: "NONE",
    needsAssistance: false,
    activeRequestCount: 0,
    qrCodeId: "qr_T01",
    qrPayload: "https://asilaltawooq.sa/menu?table=T01",
    createdAt: past,
    updatedAt: past,
  };
  await assertSucceeds(managerDb.collection("restaurant_tables").doc("table_01").set(tablePayload));
  await assertSucceeds(
    managerDb.collection("qr_codes").doc("qr_T01").set({
      restaurantId: RESTAURANT_ID,
      tableId: "table_01",
      tableNumber: 1,
      tableLabel: "T01",
      qrPayload: "https://asilaltawooq.sa/menu?table=T01",
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    managerDb.collection("product_images").doc("img_tawook_1").set({
      restaurantId: RESTAURANT_ID,
      productId: "dish_1",
      mimeType: "image/jpeg",
      width: 640,
      height: 480,
      sizeBytes: 1200,
      base64Data: "base64_encoded_jpeg_payload_sample",
      uploadedBy: MANAGER_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  const dishPayload = {
    restaurantId: RESTAURANT_ID,
    nameEn: "Royal Chicken Tawook",
    nameAr: "طاووق دجاج ملكي",
    descriptionEn: "Charcoal-grilled chicken breast cubes with garlic toum.",
    descriptionAr: "مكعبات دجاج مشوية على الفحم مع ثومية.",
    ingredientsEn: "Chicken breast, yogurt, garlic, lemon, paprika",
    allergens: ["Dairy"],
    category: "CHICKEN_TAWOOK",
    kitchenStation: "GRILL",
    spiceLevel: "MILD",
    portionSize: "2 Skewers + Saj",
    priceSar: 46.0,
    prepMinutes: 14,
    available: true,
    hidden: false,
    featured: true,
    isMealDeal: false,
    imageKey: "grills",
    imageId: "img_tawook_1",
    imageUrl: "cloud_img://img_tawook_1",
    sizes: ["Regular", "Large", "Family"],
    extras: ["Extra Toum +4 SAR", "Grilled Halloumi +10 SAR"],
    demoData: false,
    createdAt: past,
    updatedAt: past,
  };
  await assertSucceeds(managerDb.collection("menu_items").doc("dish_1").set(dishPayload));

  // Waiter, Kitchen, and Customer CANNOT mutate menu items, product images, or tables
  await assertFails(waiterDb.collection("menu_items").doc("dish_w").set(dishPayload));
  await assertFails(kitchenDb.collection("menu_items").doc("dish_k").set(dishPayload));
  await assertFails(aliceDb.collection("menu_items").doc("dish_c").set(dishPayload));
  await assertFails(aliceDb.collection("product_images").doc("img_c").set({
    restaurantId: RESTAURANT_ID,
    mimeType: "image/jpeg",
    width: 100,
    height: 100,
    sizeBytes: 500,
    base64Data: "abc1234567890",
    uploadedBy: ALICE_UID,
    createdAt: past,
    updatedAt: past,
  }));
  await assertFails(aliceDb.collection("restaurant_tables").doc("table_02").set({ ...tablePayload, tableNumber: 2, label: "T02" }));

  // Customers & Waiters can read menu and tables
  await assertSucceeds(aliceDb.collection("menu_items").where("restaurantId", "==", RESTAURANT_ID).get());
  await assertSucceeds(aliceDb.collection("product_images").where("restaurantId", "==", RESTAURANT_ID).get());
  await assertFails(aliceDb.collection("menu_items").get()); // unfiltered query must fail
});

test("Backend validates table identity, active table state, order items, waiter confirmation, and kitchen routing", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const past = new Date(Date.now() - 10000);
    await db.collection("restaurant_tables").doc("table_01").set({
      restaurantId: RESTAURANT_ID,
      tableNumber: 1,
      label: "T01",
      zone: "Main Hall",
      capacity: 4,
      active: true,
      status: "AVAILABLE",
      assignedWaiterName: "Tariq",
      billStatus: "NONE",
      needsAssistance: false,
      activeRequestCount: 0,
      qrPayload: "https://asilaltawooq.sa/menu?table=T01",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("restaurant_tables").doc("table_02").set({
      restaurantId: RESTAURANT_ID,
      tableNumber: 2,
      label: "T02",
      zone: "Main Hall",
      capacity: 4,
      active: false, // deactivated table
      status: "AVAILABLE",
      assignedWaiterName: "Tariq",
      billStatus: "NONE",
      needsAssistance: false,
      activeRequestCount: 0,
      qrPayload: "https://asilaltawooq.sa/menu?table=T02",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("staff").doc(WAITER_EMAIL).set({
      uid: WAITER_EMAIL,
      restaurantId: RESTAURANT_ID,
      name: "Tariq",
      email: WAITER_EMAIL,
      role: "WAITER",
      active: true,
      assignedZone: "Main Hall",
      ordersHandled: 0,
      avgResponseSec: 25,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("staff").doc(KITCHEN_UID).set({
      uid: KITCHEN_UID,
      restaurantId: RESTAURANT_ID,
      name: "Chef Sami",
      email: KITCHEN_EMAIL,
      role: "KITCHEN",
      active: true,
      assignedZone: "GRILL",
      ordersHandled: 0,
      avgResponseSec: 20,
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = userContext(ALICE_UID, "alice@example.com").firestore();
  const bobDb = userContext(BOB_UID, "bob@example.com").firestore();
  const waiterDb = userContext(WAITER_UID, WAITER_EMAIL).firestore();
  const kitchenDb = userContext(KITCHEN_UID, KITCHEN_EMAIL).firestore();
  const past = new Date(Date.now() - 2000);

  // Alice can join active T01, cannot join deactivated T02, cannot rename tableNumber/label
  await assertSucceeds(
    aliceDb.collection("restaurant_tables").doc("table_01").update({
      status: "OCCUPIED",
      billStatus: "OPEN",
      updatedAt: past,
    })
  );
  await assertFails(
    aliceDb.collection("restaurant_tables").doc("table_02").update({
      status: "OCCUPIED",
      billStatus: "OPEN",
      updatedAt: past,
    })
  );
  await assertFails(
    aliceDb.collection("restaurant_tables").doc("table_01").update({
      tableNumber: 9,
      label: "T09",
      updatedAt: past,
    })
  );

  const validOrder = {
    userId: ALICE_UID,
    restaurantId: RESTAURANT_ID,
    tableId: "table_01",
    tableNumber: 1,
    orderNumber: "AAT-1001",
    clientRequestId: "req_uuid_1",
    customerName: "Alice",
    status: "NEW_ORDER",
    priority: "NORMAL",
    waiterConfirmed: false,
    items: [
      {
        itemId: "dish_1",
        nameEn: "Royal Chicken Tawook",
        nameAr: "طاووق دجاج ملكي",
        kitchenStation: "GRILL",
        quantity: 2,
        unitPriceSar: 46.0,
        selectedSize: "Regular",
        spiceLevel: "MILD",
        extras: ["Extra Toum +4 SAR"],
        notes: "Extra crispy saj",
        allergyNote: "No nuts",
        itemStatus: "NEW_ORDER",
        prepMinutes: 14,
      },
    ],
    subtotalSar: 100.0,
    vatSar: 15.0,
    discountSar: 0.0,
    tipSar: 0.0,
    totalSar: 115.0,
    paidAmountSar: 0.0,
    paymentStatus: "UNPAID",
    demoData: false,
    createdAt: past,
    updatedAt: past,
  };

  await assertSucceeds(aliceDb.collection("orders").doc("ord_alice_1").set(validOrder));
  await assertSucceeds(
    aliceDb.collection("order_items").doc("ord_alice_1_0").set({
      orderId: "ord_alice_1",
      userId: ALICE_UID,
      restaurantId: RESTAURANT_ID,
      tableId: "table_01",
      tableNumber: 1,
      productId: "dish_1",
      nameEn: "Royal Chicken Tawook",
      nameAr: "طاووق دجاج ملكي",
      kitchenStation: "GRILL",
      quantity: 2,
      unitPriceSar: 46.0,
      lineTotalSar: 100.0,
      selectedSize: "Regular",
      spiceLevel: "MILD",
      extras: ["Extra Toum +4 SAR"],
      notes: "Extra crispy saj",
      allergyNote: "No nuts",
      itemStatus: "NEW_ORDER",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Mismatched tableNumber vs tableId or deactivated tableId must fail
  await assertFails(aliceDb.collection("orders").doc("ord_bad_num").set({ ...validOrder, tableNumber: 2 }));
  await assertFails(aliceDb.collection("orders").doc("ord_inactive_tbl").set({ ...validOrder, tableId: "table_02", tableNumber: 2 }));
  await assertFails(aliceDb.collection("orders").doc("ord_bad_table").set({ ...validOrder, tableId: "nonexistent" }));
  await assertFails(aliceDb.collection("orders").doc("ord_spoof").set({ ...validOrder, userId: BOB_UID }));

  // Bob cannot read or update Alice's order
  await assertFails(bobDb.collection("orders").doc("ord_alice_1").get());
  await assertFails(bobDb.collection("orders").doc("ord_alice_1").update({ status: "CANCELLED", updatedAt: new Date(Date.now() - 500) }));

  // Waiter can list restaurant orders, confirm & send to kitchen
  await assertSucceeds(waiterDb.collection("orders").where("restaurantId", "==", RESTAURANT_ID).get());
  await assertSucceeds(
    waiterDb.collection("orders").doc("ord_alice_1").update({
      status: "ACCEPTED",
      waiterConfirmed: true,
      waiterConfirmedBy: "Tariq",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Kitchen Staff can list restaurant orders & order_items and advance status to PREPARING / READY
  await assertSucceeds(kitchenDb.collection("orders").where("restaurantId", "==", RESTAURANT_ID).get());
  await assertSucceeds(kitchenDb.collection("order_items").where("restaurantId", "==", RESTAURANT_ID).get());
  await assertSucceeds(
    kitchenDb.collection("orders").doc("ord_alice_1").update({
      status: "PREPARING",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    kitchenDb.collection("order_items").doc("ord_alice_1_0").update({
      itemStatus: "PREPARING",
      updatedAt: new Date(Date.now() - 400),
    })
  );
});

test("Service requests, loyalty profile isolation, reviews, and multiplayer table games", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const past = new Date(Date.now() - 10000);
    await db.collection("restaurant_tables").doc("table_03").set({
      restaurantId: RESTAURANT_ID,
      tableNumber: 3,
      label: "T03",
      zone: "Jeddah Patio",
      capacity: 6,
      active: true,
      status: "OCCUPIED",
      assignedWaiterName: "Omar",
      billStatus: "OPEN",
      needsAssistance: false,
      activeRequestCount: 0,
      qrPayload: "https://asilaltawooq.sa/menu?table=T03",
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = userContext(ALICE_UID, "alice@example.com").firestore();
  const bobDb = userContext(BOB_UID, "bob@example.com").firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("service_requests").doc("req_1").set({
      userId: ALICE_UID,
      restaurantId: RESTAURANT_ID,
      tableId: "table_03",
      tableNumber: 3,
      requestType: "WATER",
      note: "Still water with lemon",
      status: "PENDING",
      demoData: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Loyalty profile is strictly private to owner
  await assertSucceeds(
    aliceDb.collection("loyalty_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      restaurantId: RESTAURANT_ID,
      displayName: "Alice",
      points: 120,
      totalSpentSar: 250.0,
      tier: "SILVER",
      redeemedRewards: 0,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(bobDb.collection("loyalty_profiles").doc(ALICE_UID).get());

  // Multiplayer game session allows restaurant guests to join and update score
  await assertSucceeds(
    aliceDb.collection("game_sessions").doc("game_1").set({
      restaurantId: RESTAURANT_ID,
      hostUserId: ALICE_UID,
      roomCode: "T3-778",
      tableNumber: 3,
      gameType: "TABLE_TRIVIA",
      status: "ACTIVE",
      currentQuestionIndex: 0,
      playerNames: ["Alice"],
      playerScores: { [ALICE_UID]: 10 },
      participantUids: [ALICE_UID],
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    bobDb.collection("game_sessions").doc("game_1").update({
      playerNames: ["Alice", "Bob"],
      playerScores: { [ALICE_UID]: 10, [BOB_UID]: 20 },
      participantUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
