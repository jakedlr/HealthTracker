package com.jakedlr.healthtracker.ui.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real bundled ML Kit model on drawn barcodes. Also proves scanning works with the
 * telemetry components stripped from the manifest and no network permission.
 */
@RunWith(AndroidJUnit4::class)
class BarcodeScannerTest {
    private val scanner = productBarcodeScanner()

    @After
    fun tearDown() = scanner.close()

    private fun scan(bitmap: Bitmap): String? {
        val barcodes = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)), 10, TimeUnit.SECONDS)
        return barcodes.firstProductBarcode()
    }

    @Test
    fun readsEan13() {
        assertEquals("4006381333931", scan(drawEan13("4006381333931")))
    }

    @Test
    fun upcA_readsAsNormalizedEan13() {
        // A UPC-A is an EAN-13 with a leading 0; ML Kit reports it as UPC-A (12 digits).
        assertEquals("0036000291452", scan(drawEan13("0036000291452")))
    }

    @Test
    fun blankImage_findsNothing() {
        val blank = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        assertNull(scan(blank))
    }

    private companion object {
        val L_CODES = listOf(
            "0001101", "0011001", "0010011", "0111101", "0100011",
            "0110001", "0101111", "0111011", "0110111", "0001011",
        )
        val PARITY = listOf(
            "LLLLLL", "LLGLGG", "LLGGLG", "LLGGGL", "LGLLGG",
            "LGGLLG", "LGGGLL", "LGLGLG", "LGLGGL", "LGGLGL",
        )

        fun rCode(d: Int) = L_CODES[d].map { if (it == '0') '1' else '0' }.joinToString("")
        fun gCode(d: Int) = rCode(d).reversed()

        /** Draws a 13-digit EAN-13 (check digit included) as black bars on white. */
        fun drawEan13(code: String, moduleWidth: Int = 4, height: Int = 240): Bitmap {
            val digits = code.map { it - '0' }
            val modules = buildString {
                append("101")
                for (i in 1..6) {
                    append(if (PARITY[digits[0]][i - 1] == 'L') L_CODES[digits[i]] else gCode(digits[i]))
                }
                append("01010")
                for (i in 7..12) append(rCode(digits[i]))
                append("101")
            }
            val quiet = 12
            val bitmap = Bitmap.createBitmap((modules.length + 2 * quiet) * moduleWidth, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint = Paint().apply { color = Color.BLACK }
            modules.forEachIndexed { i, bit ->
                if (bit == '1') {
                    val left = ((quiet + i) * moduleWidth).toFloat()
                    canvas.drawRect(left, 20f, left + moduleWidth, height - 20f, paint)
                }
            }
            return bitmap
        }
    }
}
