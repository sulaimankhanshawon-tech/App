package com.example.data.repository

import com.example.data.model.GameType
import com.example.data.model.KitchenStation
import com.example.data.model.MenuCategory
import com.example.data.model.RESTAURANT_ID
import com.example.data.model.SpiceLevel
import com.example.data.model.StaffRole
import com.example.data.model.TriviaQuestion
import com.google.firebase.firestore.FieldValue

object DefaultSeedData {

    fun tableQrUrl(label: String): String = "https://asilaltawooq.sa/menu?table=${label.uppercase()}"

    fun defaultMenuPayloads(): List<Pair<String, Map<String, Any>>> {
        val items = listOf(
            // 1. Chicken Tawook
            Triple("menu_tawook_royal", MenuCategory.CHICKEN_TAWOOK, "grills") to mapOf(
                "nameEn" to "Royal Charcoal Chicken Tawook",
                "nameAr" to "طاووق دجاج مشوي على الفحم الملكي",
                "descriptionEn" to "Tender marinated chicken breast cubes grilled over natural oak charcoal, served on warm saj bread with garlic toum, grilled tomatoes, and sumac onions.",
                "descriptionAr" to "مكعبات صدر دجاج متبلة بالزبادي والليمون مشوية على الفحم الطبيعي مع خبز الصاج والثومية.",
                "ingredientsEn" to "Chicken breast, yogurt, garlic, lemon zest, Aleppo pepper, olive oil, saj bread",
                "ingredientsAr" to "صدر دجاج، زبادي، ثوم، ليمون، فلفل حلبي، زيت زيتون، خبز صاج",
                "allergens" to listOf("Dairy", "Gluten"),
                "kitchenStation" to KitchenStation.GRILL.name,
                "spiceLevel" to SpiceLevel.MILD.name,
                "portionSize" to "2 Skewers (320g)",
                "priceSar" to 46.0,
                "prepMinutes" to 14,
                "featured" to true,
                "isMealDeal" to false,
                "offerLabel" to "Signature Jeddah",
                "sizes" to listOf("Regular", "Large", "Family"),
                "extras" to listOf("Extra Garlic Toum +4 SAR", "Grilled Halloumi +10 SAR", "Spicy Garlic Dip +5 SAR")
            ),
            Triple("menu_tawook_spicy", MenuCategory.CHICKEN_TAWOOK, "grills") to mapOf(
                "nameEn" to "Jeddah Fiery Harissa Tawook",
                "nameAr" to "طاووق هريسة حارة على طريقة جدة",
                "descriptionEn" to "Charcoal-seared chicken tawook glazed with roasted red pepper harissa and pomegranate molasses.",
                "descriptionAr" to "شيش طاووق مشوي على الفحم بصلصة الهريسة الحارة ودبس الرمان.",
                "ingredientsEn" to "Chicken breast, red chili harissa, pomegranate molasses, garlic, oregano",
                "ingredientsAr" to "دجاج، هريسة فلفل أحمر، دبس رمان، ثوم، أوريجانو",
                "allergens" to listOf("Gluten"),
                "kitchenStation" to KitchenStation.GRILL.name,
                "spiceLevel" to SpiceLevel.HOT.name,
                "portionSize" to "2 Skewers (320g)",
                "priceSar" to 49.0,
                "prepMinutes" to 15,
                "featured" to true,
                "isMealDeal" to false,
                "sizes" to listOf("Regular", "Large", "Family"),
                "extras" to listOf("Extra Garlic Toum +4 SAR", "Pickled Jalapeno +3 SAR")
            ),
            // 2. Charcoal Grills
            Triple("menu_grill_mixed", MenuCategory.CHARCOAL_GRILLS, "grills") to mapOf(
                "nameEn" to "Asil Grand Charcoal Mixed Grill",
                "nameAr" to "مشويات أصيل المشكلة على الفحم",
                "descriptionEn" to "A feast of Chicken Tawook, Halabi Lamb Kabab, and Grilled Awsal skewers with saffron rice and grilled vegetables.",
                "descriptionAr" to "تشكيلة فاخرة من الطاووق وكباب اللحم الحلبي وأوصال اللحم مع أرز بالزعفران.",
                "ingredientsEn" to "Chicken tawook, minced lamb kabab, beef tenderloin awsal, parsley, sumac, biwaz",
                "ingredientsAr" to "طاووق دجاج، كباب لحم، أوصال لحم، بقدونس، سماق، بصل",
                "allergens" to listOf("Dairy", "Gluten"),
                "kitchenStation" to KitchenStation.GRILL.name,
                "spiceLevel" to SpiceLevel.MEDIUM.name,
                "portionSize" to "4 Skewers (550g)",
                "priceSar" to 88.0,
                "prepMinutes" to 18,
                "featured" to true,
                "isMealDeal" to false,
                "offerLabel" to "Chef Choice",
                "sizes" to listOf("Regular", "Large", "Family"),
                "extras" to listOf("Extra Skewer Kabab +18 SAR", "Extra Garlic Toum +4 SAR")
            ),
            // 3. Shawarma
            Triple("menu_shawarma_arabi", MenuCategory.SHAWARMA, "shawarma") to mapOf(
                "nameEn" to "Authentic Chicken Shawarma Arabi",
                "nameAr" to "شاورما دجاج عربي أصيلة",
                "descriptionEn" to "Slow-roasted rotisserie chicken shaved thin, wrapped in toasted saj, cut into bites with fries, pickles, and whipped garlic toum.",
                "descriptionAr" to "شرائح شاورما دجاج محمصة بخبز الصاج مقطعة مع البطاطس المقرمشة والمخلل والثومية.",
                "ingredientsEn" to "Marinated chicken thighs, saj bread, garlic toum, pickled cucumber, potato fries",
                "ingredientsAr" to "دجاج متبل، خبز صاج، ثومية، مخلل خيار، بطاطس",
                "allergens" to listOf("Gluten", "Eggs"),
                "kitchenStation" to KitchenStation.SHAWARMA.name,
                "spiceLevel" to SpiceLevel.MILD.name,
                "portionSize" to "8 Pieces + Fries",
                "priceSar" to 34.0,
                "prepMinutes" to 10,
                "featured" to true,
                "isMealDeal" to false,
                "sizes" to listOf("Regular", "Large", "Family"),
                "extras" to listOf("Melted Mozzarella +6 SAR", "Extra Garlic Toum +4 SAR", "Pomegranate Drizzle +3 SAR")
            ),
            // 4. Rice Meals
            Triple("menu_rice_kabsa", MenuCategory.RICE_MEALS, "rice") to mapOf(
                "nameEn" to "Saffron Rice & Charcoal Tawook Bowl",
                "nameAr" to "وجبة أرز الزعفران مع طاووق الفحم",
                "descriptionEn" to "Fragrant basmati rice infused with saffron and cardamom, topped with charcoal tawook, roasted almonds, and spicy daqoos.",
                "descriptionAr" to "أرز بسمتي بالزعفران والهيل مع دجاج طاووق مشوي ولوز محمص وصلصة الدقوس.",
                "ingredientsEn" to "Basmati rice, saffron, cardamom, charcoal chicken tawook, toasted almonds, daqoos",
                "ingredientsAr" to "أرز بسمتي، زعفران، هيل، طاووق دجاج، لوز، دقوس",
                "allergens" to listOf("Tree Nuts", "Dairy"),
                "kitchenStation" to KitchenStation.GENERAL.name,
                "spiceLevel" to SpiceLevel.MEDIUM.name,
                "portionSize" to "450g Platter",
                "priceSar" to 52.0,
                "prepMinutes" to 14,
                "featured" to false,
                "isMealDeal" to false,
                "sizes" to listOf("Regular", "Large"),
                "extras" to listOf("Extra Daqoos +4 SAR", "Roasted Pine Nuts +8 SAR")
            ),
            // 5. Sandwiches
            Triple("menu_sandwich_tawook", MenuCategory.SANDWICHES, "shawarma") to mapOf(
                "nameEn" to "Brioche Charcoal Tawook Sandwich",
                "nameAr" to "ساندويتش طاووق فحم بالبريوش",
                "descriptionEn" to "Warm toasted potato brioche bun filled with smoky chicken tawook, coleslaw, pickles, and signature garlic aioli.",
                "descriptionAr" to "خبز بريوش محمص محشو بدجاج طاووق مدخن مع كول سلو ومخلل وصوص الثوم.",
                "ingredientsEn" to "Brioche bun, charcoal tawook, cabbage slaw, garlic aioli, pickles",
                "ingredientsAr" to "خبز بريوش، طاووق فحم، كول سلو، صوص ثوم، مخلل",
                "allergens" to listOf("Gluten", "Eggs", "Dairy"),
                "kitchenStation" to KitchenStation.SANDWICH.name,
                "spiceLevel" to SpiceLevel.MILD.name,
                "portionSize" to "1 Sandwich + Fries",
                "priceSar" to 29.0,
                "prepMinutes" to 9,
                "featured" to false,
                "isMealDeal" to false,
                "sizes" to listOf("Regular", "Large"),
                "extras" to listOf("Cheddar Cheese +6 SAR", "Turkey Bacon +8 SAR")
            ),
            // 6. Pasta
            Triple("menu_pasta_alfredo", MenuCategory.PASTA, "pasta") to mapOf(
                "nameEn" to "Smoked Tawook Creamy Alfredo Pasta",
                "nameAr" to "باستا ألفريدو الكريمية بطاووق الفحم",
                "descriptionEn" to "Al dente penne tossed in rich parmesan garlic cream sauce topped with sliced charcoal chicken tawook and fresh basil.",
                "descriptionAr" to "باستا بيني بصلصة الكريمة والبارميزان الغنية مع شرائح طاووق الدجاج المشوي.",
                "ingredientsEn" to "Penne pasta, cooking cream, aged parmesan, butter, garlic, charcoal chicken tawook",
                "ingredientsAr" to "مكرونة بيني، كريمة طبخ، جبن بارميزان، زبدة، ثوم، طاووق دجاج",
                "allergens" to listOf("Gluten", "Dairy"),
                "kitchenStation" to KitchenStation.PASTA.name,
                "spiceLevel" to SpiceLevel.MILD.name,
                "portionSize" to "380g Bowl",
                "priceSar" to 44.0,
                "prepMinutes" to 13,
                "featured" to false,
                "isMealDeal" to false,
                "sizes" to listOf("Regular", "Large"),
                "extras" to listOf("Extra Parmesan +6 SAR", "Sauteed Mushrooms +5 SAR")
            ),
            // 7. Appetizers
            Triple("menu_app_hummus", MenuCategory.APPETIZERS, "rice") to mapOf(
                "nameEn" to "Beiruti Hummus & Warm Saj",
                "nameAr" to "حمص بيروتي مع خبز الصاج الدافئ",
                "descriptionEn" to "Silky chickpea hummus blended with tahini, lemon, and extra virgin olive oil, garnished with parsley and sumac.",
                "descriptionAr" to "حمص ناعم بالطحينة والليمون وزيت الزيتون البكر مع البقدونس والسماق.",
                "ingredientsEn" to "Chickpeas, sesame tahini, lemon juice, olive oil, cumin, sumac",
                "ingredientsAr" to "حمص، طحينة سمسم، عصير ليمون، زيت زيتون، كمون، سماق",
                "allergens" to listOf("Sesame", "Gluten"),
                "kitchenStation" to KitchenStation.GENERAL.name,
                "spiceLevel" to SpiceLevel.MILD.name,
                "portionSize" to "220g Bowl",
                "priceSar" to 19.0,
                "prepMinutes" to 6,
                "featured" to false,
                "isMealDeal" to false,
                "sizes" to listOf("Regular", "Large"),
                "extras" to listOf("Extra Saj Bread +4 SAR", "Topped with Shawarma +14 SAR")
            ),
            // 8. Sauces
            Triple("menu_sauce_toum", MenuCategory.SAUCES, "pasta") to mapOf(
                "nameEn" to "Signature Whipped Garlic Toum & Chili Duo",
                "nameAr" to "ثنائي الثومية المخفوقة والصلصة الحارة",
                "descriptionEn" to "House-made fluffy Levantine garlic cream paired with roasted red chili shatta.",
                "descriptionAr" to "ثومية شامية محضرة يومياً مع شطة الفلفل الأحمر المشوي.",
                "ingredientsEn" to "Fresh garlic, sunflower oil, lemon juice, sea salt, red chili",
                "ingredientsAr" to "ثوم طازج، زيت نباتي، عصير ليمون، فلفل أحمر",
                "allergens" to emptyList<String>(),
                "kitchenStation" to KitchenStation.GENERAL.name,
                "spiceLevel" to SpiceLevel.MEDIUM.name,
                "portionSize" to "2 Cups (120g)",
                "priceSar" to 8.0,
                "prepMinutes" to 2,
                "featured" to false,
                "isMealDeal" to false,
                "sizes" to listOf("Regular"),
                "extras" to listOf("Extra Cup +4 SAR")
            ),
            // 9. Cold Drinks
            Triple("menu_drink_mint", MenuCategory.COLD_DRINKS, "drinks") to mapOf(
                "nameEn" to "Jeddah Fresh Mint Lemonade",
                "nameAr" to "ليمون بالنعناع الطازج المنعش",
                "descriptionEn" to "Hand-pressed green lime and lemon blended with garden mint leaves and crushed ice.",
                "descriptionAr" to "ليمون طازج مع أوراق النعناع الخضراء والثلج المجروش.",
                "ingredientsEn" to "Fresh lemon, fresh mint, cane syrup, crushed ice",
                "ingredientsAr" to "ليمون طازج، نعناع، ثلج مجروش",
                "allergens" to emptyList<String>(),
                "kitchenStation" to KitchenStation.DRINKS.name,
                "spiceLevel" to SpiceLevel.MILD.name,
                "portionSize" to "400ml Tall Glass",
                "priceSar" to 16.0,
                "prepMinutes" to 4,
                "featured" to true,
                "isMealDeal" to false,
                "sizes" to listOf("Regular", "Large"),
                "extras" to listOf("Add Ginger Shot +3 SAR", "Zero Sugar Option +0 SAR")
            ),
            // 10. Special Deals
            Triple("menu_deal_family", MenuCategory.SPECIAL_DEALS, "grills") to mapOf(
                "nameEn" to "Asil Family Tawook Box (4 Persons)",
                "nameAr" to "بوكس العائلة أصيل الطاووق (4 أشخاص)",
                "descriptionEn" to "8 charcoal tawook skewers, 2 chicken shawarma arabi plates, saffron rice, hummus, 4 garlic dips, and 4 fresh mint lemonades.",
                "descriptionAr" to "8 أسياخ طاووق على الفحم، صحنان شاورما عربي، أرز بالزعفران، حمص، 4 ثومية، و4 ليمون بالنعناع.",
                "ingredientsEn" to "Chicken tawook, chicken shawarma, saffron rice, hummus, saj bread, mint lemonade",
                "ingredientsAr" to "طاووق، شاورما، أرز زعفران، حمص، خبز صاج، ليمون نعناع",
                "allergens" to listOf("Dairy", "Gluten", "Sesame", "Eggs"),
                "kitchenStation" to KitchenStation.GRILL.name,
                "spiceLevel" to SpiceLevel.MILD.name,
                "portionSize" to "Feeds 4-5 Guests",
                "priceSar" to 169.0,
                "prepMinutes" to 22,
                "featured" to true,
                "isMealDeal" to true,
                "dealSummary" to "Includes 8 Tawook Skewers + 2 Shawarma + Rice + 4 Drinks",
                "offerLabel" to "Save 25%",
                "sizes" to listOf("Family"),
                "extras" to listOf("Extra 2 Skewers +28 SAR", "Extra Garlic Toum +4 SAR")
            )
        )

        return items.map { (meta, body) ->
            val (docId, category, imageKey) = meta
            val fullMap = body.toMutableMap().apply {
                put("restaurantId", RESTAURANT_ID)
                put("category", category.name)
                put("available", true)
                put("hidden", false)
                put("imageKey", imageKey)
                put("demoData", false)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docId to fullMap
        }
    }

    fun defaultTablePayloads(): List<Pair<String, Map<String, Any>>> {
        val waiters = listOf("Tariq Al-Harbi", "Omar Al-Ghamdi", "Khalid Al-Zahrani", "Faisal Al-Otaibi")
        val zones = listOf("Main Hall", "Main Hall", "Family Majlis", "Jeddah Patio")
        val capacities = listOf(4, 4, 6, 4)
        return (1..4).map { num ->
            val label = "T%02d".format(num)
            val docId = "table_%02d".format(num)
            docId to mapOf(
                "restaurantId" to RESTAURANT_ID,
                "tableNumber" to num,
                "label" to label,
                "zone" to zones[num - 1],
                "capacity" to capacities[num - 1],
                "active" to true,
                "status" to "AVAILABLE",
                "assignedWaiterName" to waiters[(num - 1) % waiters.size],
                "billStatus" to "NONE",
                "needsAssistance" to false,
                "activeRequestCount" to 0,
                "qrCodeId" to "qr_$label",
                "qrPayload" to tableQrUrl(label),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
    }

    fun defaultQrCodePayloads(): List<Pair<String, Map<String, Any>>> {
        return (1..4).map { num ->
            val label = "T%02d".format(num)
            val docId = "qr_$label"
            docId to mapOf(
                "restaurantId" to RESTAURANT_ID,
                "tableId" to "table_%02d".format(num),
                "tableNumber" to num,
                "tableLabel" to label,
                "qrPayload" to tableQrUrl(label),
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
    }

    fun defaultRolePayloads(): List<Pair<String, Map<String, Any>>> {
        val defs = listOf(
            StaffRole.OWNER to listOf("ADMIN_DASHBOARD", "MENU_CRUD", "TABLE_QR_CRUD", "STAFF_RBAC", "FLOOR_MAP", "KDS", "PAYMENTS"),
            StaffRole.MANAGER to listOf("ADMIN_DASHBOARD", "MENU_CRUD", "TABLE_QR_CRUD", "STAFF_RBAC", "FLOOR_MAP", "KDS", "PAYMENTS"),
            StaffRole.WAITER to listOf("FLOOR_MAP", "CONFIRM_ORDERS", "SERVICE_REQUESTS", "CASH_PAYMENT_VERIFY"),
            StaffRole.KITCHEN to listOf("KDS", "STATION_FILTER", "PREP_STATUS_UPDATE")
        )
        return defs.map { (role, perms) ->
            "role_${role.name.lowercase()}" to mapOf(
                "restaurantId" to RESTAURANT_ID,
                "code" to role.name,
                "titleEn" to role.labelEn,
                "titleAr" to role.labelAr,
                "permissions" to perms,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
    }

    fun defaultCategoryPayloads(): List<Pair<String, Map<String, Any>>> {
        return MenuCategory.entries.mapIndexed { idx, cat ->
            "cat_${cat.name.lowercase()}" to mapOf(
                "restaurantId" to RESTAURANT_ID,
                "code" to cat.name,
                "nameEn" to cat.labelEn,
                "nameAr" to cat.labelAr,
                "defaultStation" to cat.defaultStation.name,
                "sortOrder" to idx,
                "active" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
    }

    fun defaultKitchenStationPayloads(): List<Pair<String, Map<String, Any>>> {
        return KitchenStation.entries.mapIndexed { idx, st ->
            "station_${st.name.lowercase()}" to mapOf(
                "restaurantId" to RESTAURANT_ID,
                "code" to st.name,
                "nameEn" to st.labelEn,
                "nameAr" to st.labelAr,
                "active" to true,
                "sortOrder" to idx,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
    }

    fun defaultTriviaQuestions(gameType: GameType): List<TriviaQuestion> = when (gameType) {
        GameType.FOOD_GUESSING -> listOf(
            TriviaQuestion(
                questionEn = "Which signature white sauce is whipped from fresh garlic, lemon, and oil for Chicken Tawook?",
                questionAr = "ما هي الصلصة البيضاء الشهيرة المخفوقة من الثوم الطازج والليمون والزيت التي تُقدم مع الطاووق؟",
                optionsEn = listOf("Toum", "Tahini", "Tzatziki", "Harissa"),
                optionsAr = listOf("الثومية", "الطحينة", "تزاتزيكي", "الهريسة"),
                correctIndex = 0
            ),
            TriviaQuestion(
                questionEn = "Which golden spice gives Saudi Kabsa and Mandi rice its royal aroma and color?",
                questionAr = "ما هو البهار الذهبي الذي يمنح أرز الكبسة والمندي نكهته ولونه المميز؟",
                optionsEn = listOf("Sumac", "Saffron", "Fenugreek", "Anise"),
                optionsAr = listOf("السماق", "الزعفران", "الحلبة", "اليانسون"),
                correctIndex = 1
            ),
            TriviaQuestion(
                questionEn = "Which tangy crimson berry spice is sprinkled over grilled onions and tawook flatbread?",
                questionAr = "ما هو التابل الأحمر الحامض الذي يُرش على البصل المشوي وخبز الطاووق؟",
                optionsEn = listOf("Paprika", "Sumac", "Cardamom", "Coriander"),
                optionsAr = listOf("البابريكا", "السماق", "الهيل", "الكزبرة"),
                correctIndex = 1
            )
        )
        GameType.TABLE_TRIVIA, GameType.QUICK_QUIZ -> listOf(
            TriviaQuestion(
                questionEn = "Jeddah is famous across Saudi Arabia as the Bride of which Sea?",
                questionAr = "تشتهر مدينة جدة بلقب عروس أي بحر؟",
                optionsEn = listOf("Arabian Gulf", "Red Sea", "Mediterranean Sea", "Caspian Sea"),
                optionsAr = listOf("الخليج العربي", "البحر الأحمر", "البحر المتوسط", "بحر قزوين"),
                correctIndex = 1
            ),
            TriviaQuestion(
                questionEn = "What is the standard Value Added Tax (VAT) rate applied in Saudi Arabia?",
                questionAr = "كم تبلغ نسبة ضريبة القيمة المضافة المعتمدة في المملكة العربية السعودية؟",
                optionsEn = listOf("5%", "10%", "15%", "20%"),
                optionsAr = listOf("5%", "10%", "15%", "20%"),
                correctIndex = 2
            ),
            TriviaQuestion(
                questionEn = "What does 'Tawook' (طاووق) traditionally mean in Levantine culinary heritage?",
                questionAr = "إلى ماذا يرمز اسم الطاووق في التراث الشامي للطهي؟",
                optionsEn = listOf("Chicken", "Charcoal", "Garlic", "Flatbread"),
                optionsAr = listOf("الدجاج", "الفحم", "الثوم", "الخبز"),
                correctIndex = 0
            )
        )
    }
}
