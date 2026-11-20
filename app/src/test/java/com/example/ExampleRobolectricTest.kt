package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RestaurantTable
import com.example.util.QrCardExporter
import com.example.util.TableQrParser
import com.example.util.ZxingQrDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Asil Al Tawooq", appName)
  }

  @Test
  fun `qr card exporter encodes unique table urls verified via zxing`() {
    val t1 = RestaurantTable(
      id = "table_01",
      tableNumber = 1,
      label = "T01",
      zone = "Main Hall",
      capacity = 4,
      qrPayload = "https://asilaltawooq.sa/menu?table=T01"
    )
    val t2 = RestaurantTable(
      id = "table_02",
      tableNumber = 2,
      label = "T02",
      zone = "Family Majlis",
      capacity = 6,
      qrPayload = "https://asilaltawooq.sa/menu?table=T02"
    )

    val decoder = ZxingQrDecoder()
    val bmp1 = QrCardExporter.renderTableQrCardBitmap(t1)
    val decoded1 = decoder.decodeBitmap(bmp1)
    assertEquals("https://asilaltawooq.sa/menu?table=T01", decoded1)
    assertEquals("T01", TableQrParser.parseTableLabel(decoded1!!))

    val bmp2 = QrCardExporter.renderTableQrCardBitmap(t2)
    val decoded2 = decoder.decodeBitmap(bmp2)
    assertNotNull(decoded2)
    assertEquals("https://asilaltawooq.sa/menu?table=T02", decoded2)
    assertEquals("T02", TableQrParser.parseTableLabel(decoded2!!))
  }
}
