package com.jakedlr.healthtracker.domain

/** Retail product barcode formats we scan for. */
enum class ProductBarcodeFormat { EAN_13, EAN_8, UPC_A, UPC_E }

/**
 * Canonical form used for storage and lookup, or null if [raw] isn't a valid code of [format].
 * A 12-digit UPC-A gets a leading 0 so it matches the same product read as EAN-13.
 */
fun normalizeBarcode(raw: String, format: ProductBarcodeFormat): String? {
    val digits = raw.trim()
    if (digits.isEmpty() || !digits.all { it in '0'..'9' }) return null
    return when (format) {
        ProductBarcodeFormat.UPC_A -> digits.takeIf { it.length == 12 }?.let { "0$it" }
        ProductBarcodeFormat.EAN_13 -> digits.takeIf { it.length == 13 }
        ProductBarcodeFormat.EAN_8, ProductBarcodeFormat.UPC_E -> digits.takeIf { it.length == 8 }
    }
}
