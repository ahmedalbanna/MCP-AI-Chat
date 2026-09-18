package com.aistudio.mcpchat.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicNumeralsTest {

    @Test
    fun `convert maps latin digits to arabic-indic`() {
        assertEquals("٠١٢٣٤٥٦٧٨٩", ArabicNumerals.convert("0123456789"))
    }

    @Test
    fun `convert leaves non-digits untouched`() {
        assertEquals("٥,٠٠٠ USD", ArabicNumerals.convert("5,000 USD"))
    }

    @Test
    fun `formatCurrency groups thousands and appends symbol`() {
        assertEquals("٥,٠٠٠ ر.ي", ArabicNumerals.formatCurrency(5000.0))
    }

    @Test
    fun `formatCurrency honors custom symbol`() {
        assertEquals("١,٠٠٠ $", ArabicNumerals.formatCurrency(1000.0, "$"))
    }

    @Test
    fun `formatPercent converts digits keeping the sign`() {
        assertEquals("٥٠%", ArabicNumerals.formatPercent(50))
    }

    @Test
    fun `formatNumber groups thousands`() {
        assertEquals("١,٠٠٠,٠٠٠", ArabicNumerals.formatNumber(1_000_000L))
    }
}
