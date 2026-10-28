package com.suarezmillan.finanzas.core.receipt

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCR **en el dispositivo** con ML Kit (modelo incluido en el APK).
 *
 * La app no pide INTERNET: la foto de la factura no sale del teléfono.
 * Si en el futuro se añade un proveedor en la nube, tendrá que ser opt-in y
 * con una pantalla que lo explique.
 */
@Singleton
class ReceiptScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    @Throws(IOException::class)
    suspend fun scan(uri: Uri, defaultCurrency: String): ReceiptAnalysis =
        withContext(Dispatchers.IO) {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            ReceiptParser.parse(text = result.text, defaultCurrency = defaultCurrency)
        }
}
