package com.suarezmillan.finanzas.core.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Comparte el CSV por el selector del sistema.
 *
 * Se escribe en `cacheDir/exports` y se expone con FileProvider: no sube nada a
 * ningún servidor y el usuario decide a dónde va el archivo.
 */
fun shareCsv(context: Context, csv: String, fileName: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeText(csv)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Movimientos · Finanzas")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Exportar movimientos"))
}
