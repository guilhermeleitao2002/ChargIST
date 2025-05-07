// data/repository/ImageStorageRepository.kt
package pt.ist.cmu.chargist.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale



/**
 * 100 % local helper – **no Firebase Storage needed**.
 * Encodes the picked image into a compressed Base‑64 JPEG string
 * so it can be embedded directly in the Firestore document.
 */
class ImageStorageRepository(
    private val context: Context
) {

    /** Uri → Base‑64 JPEG (≈ 150 kB) */
    suspend fun encodeImage(uri: Uri): String =
        ImageCodec.uriToBase64(context, uri)
}



object ImageCodec {

    /** Uri → compressed JPEG → Base‑64 */
    suspend fun uriToBase64(context: Context, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(uri)
            )

            /* down‑scale for safety (long side ≈ 1 000 px) */
            val target = 1_000
            val scaled  =
                bitmap.scale(target, (bitmap.height * target.toFloat() / bitmap.width).toInt())

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)   // ~150 kB
            Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        }

    /** Base‑64 → ByteArray (for Coil) */
    fun base64ToBytes(data: String): ByteArray =
        Base64.decode(data, Base64.DEFAULT)
}

