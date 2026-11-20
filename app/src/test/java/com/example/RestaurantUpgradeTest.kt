package com.example

import com.example.data.model.CartItem
import com.example.data.model.KitchenStation
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.SpiceLevel
import com.example.data.model.StaffRole
import com.example.data.model.parseExtraPrice
import com.example.data.repository.DefaultSeedData
import com.example.util.TableQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestaurantUpgradeTest {

    @Test
    fun `unique table qr url routing preserves distinct table identifiers`() {
        val t01Url = DefaultSeedData.tableQrUrl("T01")
        val t02Url = DefaultSeedData.tableQrUrl("T02")
        val t05Url = DefaultSeedData.tableQrUrl("T05")

        assertEquals("https://asilaltawooq.sa/menu?table=T01", t01Url)
        assertEquals("https://asilaltawooq.sa/menu?table=T02", t02Url)

        assertEquals("T01", TableQrParser.parseTableLabel(t01Url))
        assertEquals("T02", TableQrParser.parseTableLabel(t02Url))
        assertEquals("T05", TableQrParser.parseTableLabel(t05Url))
        assertEquals("T04", TableQrParser.parseTableLabel("asil://table/T04"))
        assertEquals("T03", TableQrParser.parseTableLabel("https://asilaltawooq.sa/t/3"))

        assertNull(TableQrParser.parseTableLabel(""))
        assertNull(TableQrParser.parseTableLabel("T0"))
        assertNull(TableQrParser.parseTableLabel("T201"))
        assertNull(TableQrParser.parseTableLabel("https://example.com/menu?foo=bar"))
    }

    @Test
    fun `menu price updates sync immediately into cart and order line totals`() {
        val originalDish = MenuItem(
            id = "menu_tawook_royal",
            nameEn = "Royal Charcoal Chicken Tawook",
            priceSar = 46.0,
            category = MenuCategory.CHICKEN_TAWOOK.name,
            kitchenStation = KitchenStation.GRILL.name
        )
        val cartItem = CartItem(
            cartKey = "menu_tawook_royal|Large|MILD|Extra Garlic Toum +4 SAR||",
            menuItem = originalDish,
            quantity = 2,
            selectedSize = "Large",
            spiceLevel = SpiceLevel.MILD,
            selectedExtras = listOf("Extra Garlic Toum +4 SAR")
        )
        assertEquals(4.0, parseExtraPrice("Extra Garlic Toum +4 SAR"), 0.001)
        // (46 * 1.25 + 4) * 2 = (57.5 + 4) * 2 = 123.0
        assertEquals(123.0, cartItem.lineTotal(), 0.001)

        // Admin raises dish price from 46 to 52 SAR
        val updatedDish = originalDish.copy(priceSar = 52.0)
        val syncedCartItem = cartItem.copy(menuItem = updatedDish)
        // (52 * 1.25 + 4) * 2 = (65 + 4) * 2 = 138.0
        assertEquals(138.0, syncedCartItem.lineTotal(), 0.001)

        val orderLine = OrderLineItem(
            itemId = updatedDish.id,
            nameEn = updatedDish.nameEn,
            quantity = 2,
            unitPriceSar = updatedDish.priceSar,
            selectedSize = "Large",
            extras = listOf("Extra Garlic Toum +4 SAR")
        )
        assertEquals(138.0, orderLine.lineTotal(), 0.001)
    }

    @Test
    fun `kitchen station routing and waiter confirmation state work properly`() {
        assertEquals(KitchenStation.GRILL, MenuCategory.CHARCOAL_GRILLS.defaultStation)
        assertEquals(KitchenStation.GRILL, MenuCategory.CHICKEN_TAWOOK.defaultStation)
        assertEquals(KitchenStation.SHAWARMA, MenuCategory.SHAWARMA.defaultStation)
        assertEquals(KitchenStation.PASTA, MenuCategory.PASTA.defaultStation)
        assertEquals(KitchenStation.DRINKS, MenuCategory.COLD_DRINKS.defaultStation)

        val order = Order(
            id = "ord_01",
            tableId = "table_02",
            tableNumber = 2,
            orderNumber = "AT-1234",
            clientRequestId = "req-abc",
            status = OrderStatus.NEW_ORDER.name,
            waiterConfirmed = false
        )
        assertEquals(2, order.tableNumber)
        assertFalse(order.waiterConfirmed)

        val confirmedOrder = order.copy(
            status = OrderStatus.ACCEPTED.name,
            waiterConfirmed = true,
            waiterConfirmedBy = "Tariq Al-Harbi"
        )
        assertTrue(confirmedOrder.waiterConfirmed)
        assertEquals(OrderStatus.ACCEPTED, confirmedOrder.statusEnum())
    }

    @Test
    fun `staff role rbac mapping handles owner manager waiter kitchen and rejects unknown`() {
        assertEquals(StaffRole.OWNER, StaffRole.fromRaw("OWNER"))
        assertEquals(StaffRole.OWNER, StaffRole.fromRaw("ADMIN"))
        assertEquals(StaffRole.MANAGER, StaffRole.fromRaw("MANAGER"))
        assertEquals(StaffRole.MANAGER, StaffRole.fromRaw("CASHIER"))
        assertEquals(StaffRole.WAITER, StaffRole.fromRaw("waiter"))
        assertEquals(StaffRole.KITCHEN, StaffRole.fromRaw("Kitchen"))
        assertNull(StaffRole.fromRaw("CUSTOMER"))
        assertNull(StaffRole.fromRaw(null))
        assertNull(StaffRole.fromRaw(""))
    }

    @Test
    fun `default seed data provides 4 active tables with distinct qr urls and reference catalogs`() {
        val tables = DefaultSeedData.defaultTablePayloads()
        assertEquals(4, tables.size)
        val labels = tables.map { it.second["label"] as String }
        val qrPayloads = tables.map { it.second["qrPayload"] as String }
        assertEquals(listOf("T01", "T02", "T03", "T04"), labels)
        assertEquals(4, qrPayloads.distinct().size)
        assertTrue(tables.all { it.second["active"] == true })

        val qrs = DefaultSeedData.defaultQrCodePayloads()
        assertEquals(4, qrs.size)
        assertEquals(4, DefaultSeedData.defaultRolePayloads().size)
        assertEquals(MenuCategory.entries.size, DefaultSeedData.defaultCategoryPayloads().size)
        assertEquals(KitchenStation.entries.size, DefaultSeedData.defaultKitchenStationPayloads().size)
    }
}
