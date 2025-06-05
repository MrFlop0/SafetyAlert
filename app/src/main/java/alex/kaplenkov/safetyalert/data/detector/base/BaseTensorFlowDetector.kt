package alex.kaplenkov.safetyalert.data.detector.base

import alex.kaplenkov.safetyalert.domain.detector.Detector
import alex.kaplenkov.safetyalert.utils.ImageUtils
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

abstract class BaseTensorFlowDetector<T>(
    context: Context,
    modelPath: String,
    private val inputSize: Int
) : Detector<T> {

    protected val interpreter: Interpreter
    protected val inputBuffer: ByteBuffer

    init {
        val options = createInterpreterOptions()
        interpreter = Interpreter(FileUtil.loadMappedFile(context, modelPath), options)
        inputBuffer = createInputBuffer()
    }

    protected open fun createInterpreterOptions(): Interpreter.Options {
        return Interpreter.Options()
    }

    protected open fun createInputBuffer(): ByteBuffer {
        return ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    protected fun preprocessImage(bitmap: Bitmap): Triple<Bitmap, Float, Pair<Float, Float>> {
        val preprocessedImage = ImageUtils.preprocessImage(bitmap, inputSize, inputSize)
        return Triple(preprocessedImage.bitmap, preprocessedImage.ratio, Pair(preprocessedImage.xOffset, preprocessedImage.yOffset))
    }

    protected fun fillInputBuffer(bitmap: Bitmap) {
        inputBuffer.rewind()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
        }
    }

    protected fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val xOverlap = max(0f, min(boxA.right, boxB.right) - max(boxA.left, boxB.left))
        val yOverlap = max(0f, min(boxA.bottom, boxB.bottom) - max(boxA.top, boxB.top))
        val intersection = xOverlap * yOverlap

        val areaA = (boxA.right - boxA.left) * (boxA.bottom - boxA.top)
        val areaB = (boxB.right - boxB.left) * (boxB.bottom - boxB.top)
        val union = areaA + areaB - intersection

        return if (union <= 0) 0f else intersection / union
    }

    override fun close() {
        interpreter.close()
    }
}