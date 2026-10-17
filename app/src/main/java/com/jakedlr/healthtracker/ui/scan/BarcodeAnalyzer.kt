package com.jakedlr.healthtracker.ui.scan

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.jakedlr.healthtracker.domain.ProductBarcodeFormat
import com.jakedlr.healthtracker.domain.normalizeBarcode
import java.util.concurrent.atomic.AtomicBoolean

/** ML Kit scanner (bundled model, runs on-device) limited to retail product barcodes. */
fun productBarcodeScanner(): BarcodeScanner = BarcodeScanning.getClient(
    BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
        )
        .build(),
)

/** The first detected product barcode in normalized form, or null. */
fun List<Barcode>.firstProductBarcode(): String? = firstNotNullOfOrNull { barcode ->
    val format = when (barcode.format) {
        Barcode.FORMAT_EAN_13 -> ProductBarcodeFormat.EAN_13
        Barcode.FORMAT_EAN_8 -> ProductBarcodeFormat.EAN_8
        Barcode.FORMAT_UPC_A -> ProductBarcodeFormat.UPC_A
        Barcode.FORMAT_UPC_E -> ProductBarcodeFormat.UPC_E
        else -> null
    }
    format?.let { f -> barcode.rawValue?.let { normalizeBarcode(it, f) } }
}

/**
 * Reports the first product barcode seen, exactly once, then ignores further frames.
 * [onBarcode] is called on the main thread.
 */
class BarcodeAnalyzer(private val onBarcode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = productBarcodeScanner()
    private val found = AtomicBoolean(false)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (found.get() || mediaImage == null) {
            image.close()
            return
        }
        scanner.process(InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes ->
                val code = barcodes.firstProductBarcode()
                if (code != null && found.compareAndSet(false, true)) onBarcode(code)
            }
            .addOnCompleteListener { image.close() }
    }

    fun close() = scanner.close()
}
