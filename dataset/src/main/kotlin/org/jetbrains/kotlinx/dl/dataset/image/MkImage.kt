package org.jetbrains.kotlinx.dl.dataset.image

import org.jetbrains.kotlinx.multik.api.empty
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D3
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.*
import java.awt.image.DataBuffer.TYPE_FLOAT

public fun NDArray<Float, D3>.toImage(colorMode: ColorMode): BufferedImage {
    val cm = when (colorMode) {
        ColorMode.RGB -> object: ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            false, false, Transparency.OPAQUE, TYPE_FLOAT
        ) {
            override fun isCompatibleRaster(raster: Raster): Boolean {
                return isCompatibleSampleModel(raster.sampleModel)
            }

            override fun isCompatibleSampleModel(sm: SampleModel): Boolean {
                return sm is MkSampleModel
            }
        }
        ColorMode.BGR -> ComponentColorModel(
            ColorSpace.getInstance(ColorSpace.CS_sRGB),
            false, false, Transparency.OPAQUE, TYPE_FLOAT
        )
        ColorMode.GRAYSCALE -> ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                   false, false, Transparency.OPAQUE, TYPE_FLOAT)
    }
    val sm: SampleModel = MkSampleModel(this.shape[1], this.shape[0], this.shape[2])
    return BufferedImage(cm, Raster.createWritableRaster(sm, MkDataBuffer(this), null), true, null)
}

internal class MkDataBuffer(internal val ndArray: NDArray<Float, D3>) : DataBuffer(TYPE_FLOAT, ndArray.size) {

    override fun getElemFloat(bank: Int, i: Int): Float {
        require(bank == 0)
        val (y, x, c) = i.unwrap()
        return ndArray[y, x, c]
    }

    override fun setElemFloat(bank: Int, i: Int, value: Float) {
        require(bank == 0)
        val (y, x, c) = i.unwrap()
        ndArray[y, x, c] = value
    }

    override fun getElem(bank: Int, i: Int): Int {
        return getElemFloat(bank, i).toInt()
    }

    override fun setElem(bank: Int, i: Int, value: Int) {
        setElemFloat(bank, i, value.toFloat())
    }

    private fun Int.unwrap(): Triple<Int, Int, Int> {
        val x = (this / ndArray.channels) % ndArray.width
        val y = (this / ndArray.channels) / ndArray.width
        val c = this % ndArray.channels
        return Triple(y, x, c)
    }

    private fun Triple<Int, Int, Int>.wrap(): Int {
        val (y, x, c) = this
        return (x + y * ndArray.width) * ndArray.channels + c
    }
}

private class MkSampleModel(width: Int, height: Int, channels: Int) :
    SampleModel(TYPE_FLOAT, width, height, channels) {

    override fun getNumDataElements(): Int = numBands

    override fun getDataElements(x: Int, y: Int, obj: Any?, data: DataBuffer): Any {
        val buffer = obj as? FloatArray ?: FloatArray(numBands)
        for (i in buffer.indices) {
            buffer[i] = (data as MkDataBuffer).ndArray[y, x, i]
        }
        return buffer
    }

    override fun setDataElements(x: Int, y: Int, obj: Any, data: DataBuffer) {
        for (i in (obj as FloatArray).indices) {
            (data as MkDataBuffer).ndArray[y, x, i] = obj[i]
        }
    }

    override fun getSampleFloat(x: Int, y: Int, b: Int, data: DataBuffer?): Float {
        return (data as MkDataBuffer).ndArray[y, x, b]
    }

    override fun setSample(x: Int, y: Int, b: Int, s: Float, data: DataBuffer?) {
        (data as MkDataBuffer).ndArray[y, x, b] = s
    }

    override fun getSample(x: Int, y: Int, b: Int, data: DataBuffer): Int = getSampleFloat(x, y, b, data).toInt()
    override fun setSample(x: Int, y: Int, b: Int, s: Int, data: DataBuffer) = setSample(x, y, b, s.toFloat(), data)

    override fun createCompatibleSampleModel(w: Int, h: Int): SampleModel = MkSampleModel(w, h, numBands)
    override fun createSubsetSampleModel(bands: IntArray?): SampleModel = throw RasterFormatException("I can't do that")
    override fun createDataBuffer(): DataBuffer = MkDataBuffer(mk.zeros(height, width, numBands))

    override fun getSampleSize(): IntArray = IntArray(numBands) { getSampleSize(it) }
    override fun getSampleSize(band: Int): Int = DataBuffer.getDataTypeSize(dataType)
}