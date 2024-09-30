package io.github.takusan23.androidcalcimagehash

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.core.graphics.get
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageHash1 {

    /**
     * [Bitmap]を取得する
     *
     * @param context [Context]
     * @param uri PhotoPicker 等で取得した[Uri]
     * @return [Bitmap]
     */
    suspend fun loadBitmap(
        context: Context,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }

/** aHash を求める */
suspend fun calcAHash(bitmap: Bitmap) = withContext(Dispatchers.Default) {
    // 幅 8、高さ 8 の Bitmap にリサイズする
    val scaledBitmap = bitmap.scale(width = 8, height = 8)
    // モノクロにする
    val monochromeBitmap = scaledBitmap.toMonoChrome()
    // 色の平均を出す
    var totalColor = 0
    repeat(8) { y ->
        repeat(8) { x ->
            totalColor += monochromeBitmap[x, y]
        }
    }
    val averageColor = totalColor / 64
    // 縦 8、横 8 のループを回す
    // 8x8 なので結果は 64 ビットになる。ULong で格納できる
    // 各ピクセルと平均を比較して、平均よりも大きい場合は 1 を立てる
    // ビットの立て方は以下に従う
    // 左上[0,0]から開始し、一番右まで読み取る。[0,7]
    // 一番右まで読み取ったらひとつ下に下がってまた読み出す[1,0]
    // ビッグエンディアンを採用するので、一番右のビットが[0,0]の結果になります
    var resultBit = 0UL
    var bitCount = 63
    repeat(8) { y ->
        repeat(8) { x ->
            val currentColor = monochromeBitmap[x, y]
            // ビットを立てる
            if (averageColor < currentColor) {
                resultBit = resultBit or (1UL shl bitCount)
            }
            bitCount--
        }
    }
    return@withContext resultBit
}

    /** [Bitmap]をモノクロにする */
    private fun Bitmap.toMonoChrome(): Bitmap {
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.setColorFilter(filter)
        canvas.drawBitmap(this, 0f, 0f, paint)
        return bmpGrayscale
    }

}