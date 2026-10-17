package com.jakedlr.healthtracker.domain

import com.jakedlr.healthtracker.domain.ProductBarcodeFormat.EAN_13
import com.jakedlr.healthtracker.domain.ProductBarcodeFormat.EAN_8
import com.jakedlr.healthtracker.domain.ProductBarcodeFormat.UPC_A
import com.jakedlr.healthtracker.domain.ProductBarcodeFormat.UPC_E
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeTest {
    @Test
    fun upcA_isStoredAsEan13_soBothReadingsMatch() {
        assertEquals("0036000291452", normalizeBarcode("036000291452", UPC_A))
        assertEquals(normalizeBarcode("0036000291452", EAN_13), normalizeBarcode("036000291452", UPC_A))
    }

    @Test
    fun otherFormats_areUnchanged() {
        assertEquals("4006381333931", normalizeBarcode("4006381333931", EAN_13))
        assertEquals("96385074", normalizeBarcode("96385074", EAN_8))
        assertEquals("01234565", normalizeBarcode("01234565", UPC_E))
    }

    @Test
    fun whitespaceIsTrimmed() {
        assertEquals("4006381333931", normalizeBarcode(" 4006381333931\n", EAN_13))
    }

    @Test
    fun rejectsBlankNonDigitsAndWrongLength() {
        assertNull(normalizeBarcode("", EAN_13))
        assertNull(normalizeBarcode("40063813339a1", EAN_13))
        assertNull(normalizeBarcode("400638133393", EAN_13))
        assertNull(normalizeBarcode("0036000291452", UPC_A))
        assertNull(normalizeBarcode("9638507", EAN_8))
    }
}
