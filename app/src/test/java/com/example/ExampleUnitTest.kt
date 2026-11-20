package com.example

import com.example.util.TableQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun parseTableLabel_handlesSupportedFormats() {
    assertEquals("T04", TableQrParser.parseTableLabel("asil://table/T04"))
    assertEquals("T07", TableQrParser.parseTableLabel("https://asilaltawooq.sa/t/7"))
    assertEquals("T12", TableQrParser.parseTableLabel("https://asilaltawooq.sa/menu?table=12&lang=ar"))
    assertEquals("T01", TableQrParser.parseTableLabel("t1"))
    assertEquals("T09", TableQrParser.parseTableLabel("9"))
    assertNull(TableQrParser.parseTableLabel("https://example.com/menu"))
    assertNull(TableQrParser.parseTableLabel("+4"))
    assertNull(TableQrParser.parseTableLabel("T0"))
    assertNull(TableQrParser.parseTableLabel(""))
  }
}
