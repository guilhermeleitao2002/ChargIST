package pt.ist.cmu.chargist.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale

class ImageStorageRepository(
    private val context: Context
) {

    suspend fun encodeImage(uri: Uri): String =
        ImageCodec.uriToBase64(context, uri)
}
object ImageCodec {
    suspend fun uriToBase64(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(uri)
            )
            /* down‑scale */
            val target = 1_000
            val scaled  =
                bitmap.scale(target, (bitmap.height * target.toFloat() / bitmap.width).toInt())

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        }
    fun base64ToBytes(data: String): ByteArray =
        Base64.decode(data, Base64.DEFAULT)
}

