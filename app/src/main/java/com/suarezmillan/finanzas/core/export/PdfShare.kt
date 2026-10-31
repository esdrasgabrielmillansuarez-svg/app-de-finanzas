package com.suarezmillan.finanzas.core.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Comparte el PDF del mes por el selector del sistema.
 *
 * Igual que el CSV: se escribe en `cacheDir/exports`, se expone con FileProvider
 * y el usuario decide a dónde va. Nada sale del teléfono por su cuenta.
 */
fun sharePdf(context: Context, bytes: ByteArray, fileName: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeBytes(bytes)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Reporte del mes · Finanzas")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Compartir reporte"))
}
