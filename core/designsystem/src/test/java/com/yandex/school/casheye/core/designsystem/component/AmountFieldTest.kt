package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AmountFieldTest {
    @Test
    fun `formats amount with separators from current locale`() {
        assertEquals("1 126,57", formatAmount("1126.57", Locale.forLanguageTag("ru-RU")))
        assertEquals("1,126.57", formatAmount("1126.57", Locale.US))
        assertEquals("1.126,57", formatAmount("1126.57", Locale.GERMANY))
    }

    @Test
    fun `decimal separator creates two editable fraction cells`() {
        val update = update("", "", 0, ".", 1, Locale.forLanguageTag("ru-RU"))

        assertEquals("0.00", update.canonicalAmount)
        assertEquals("0,00", update.fieldValue.text)
        assertEquals(2, update.fieldValue.selection.start)
    }

    @Test
    fun `second decimal separator is ignored without moving cursor`() {
        val update = update("69.09", "69,09", 3, "69,,09", 4, Locale.forLanguageTag("ru-RU"))

        assertEquals("69.09", update.canonicalAmount)
        assertEquals("69,09", update.fieldValue.text)
        assertEquals(3, update.fieldValue.selection.start)
    }

    @Test
    fun `typing in fraction replaces cells instead of appending`() {
        val first = update("0.00", "0,00", 2, "0,100", 3, Locale.forLanguageTag("ru-RU"))
        val second = update(first.canonicalAmount, first.fieldValue.text, 3, "0,120", 4, Locale.forLanguageTag("ru-RU"))

        assertEquals("0.12", second.canonicalAmount)
        assertEquals("0,12", second.fieldValue.text)
    }

    @Test
    fun `backspace shifts fraction and keeps two cells`() {
        val afterLastDigit = update("69.09", "69,09", 5, "69,0", 4, Locale.forLanguageTag("ru-RU"))
        val afterFirstDigit = update("69.09", "69,09", 4, "69,9", 3, Locale.forLanguageTag("ru-RU"))

        assertEquals("69.00", afterLastDigit.canonicalAmount)
        assertEquals("69,00", afterLastDigit.fieldValue.text)
        assertEquals(4, afterLastDigit.fieldValue.selection.start)
        assertEquals("69.90", afterFirstDigit.canonicalAmount)
        assertEquals("69,90", afterFirstDigit.fieldValue.text)
        assertEquals(3, afterFirstDigit.fieldValue.selection.start)
    }

    @Test
    fun `removing decimal separator joins integer and fraction`() {
        val update = update("69.00", "69,00", 3, "6900", 2, Locale.forLanguageTag("ru-RU"))

        assertEquals("6900", update.canonicalAmount)
        assertEquals("6 900", update.fieldValue.text)
        assertEquals(3, update.fieldValue.selection.start)
    }

    @Test
    fun `leading zero is replaced by following digit`() {
        val update = update("0", "0", 1, "05", 2, Locale.US)

        assertEquals("5", update.canonicalAmount)
        assertEquals("5", update.fieldValue.text)
    }

    @Test
    fun `tenth integer digit is ignored`() {
        val update = update("999999999.99", "999,999,999.99", 11, "999,999,9999.99", 12, Locale.US)

        assertEquals("999999999.99", update.canonicalAmount)
        assertEquals("999,999,999.99", update.fieldValue.text)
        assertEquals(11, update.fieldValue.selection.start)
    }

    @Test
    fun `backspace keeps decimal editable at integer limit`() {
        val update = update("999999999.12", "999,999,999.12", 12, "999,999,99912", 11, Locale.US)

        assertEquals("999999999.99", update.canonicalAmount)
        assertEquals("999,999,999.99", update.fieldValue.text)
        assertEquals(11, update.fieldValue.selection.start)
    }

    private fun update(
        canonical: String,
        previousText: String,
        previousSelection: Int,
        proposedText: String,
        proposedSelection: Int,
        locale: Locale,
    ): AmountFieldUpdate =
        updateAmountField(
            canonicalAmount = canonical,
            previousValue = TextFieldValue(previousText, TextRange(previousSelection)),
            proposedValue = TextFieldValue(proposedText, TextRange(proposedSelection)),
            locale = locale,
        )
}
