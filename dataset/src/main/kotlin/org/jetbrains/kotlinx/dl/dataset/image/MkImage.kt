package org.jetbrains.kotlinx.dl.dataset.image

import org.jetbrains.kotlinx.multik.ndarray.data.D3
import org.jetbrains.kotlinx.multik.ndarray.data.MutableMultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray

public interface MkImage : MutableMultiArray<Float, D3> {
    public val colorMode: ColorMode

    public val channelMin: FloatArray
    public val channelMax: FloatArray

    override fun copy(): MkImage
    override fun deepCopy(): MkImage
}

public class NDImage(
    private val ndArray: NDArray<Float, D3>,
    public override val colorMode: ColorMode,
    public override val channelMin: FloatArray = FloatArray(colorMode.channels) { 0f },
    public override val channelMax: FloatArray = FloatArray(colorMode.channels) { 1f },
) : MkImage, MutableMultiArray<Float, D3> by ndArray {
    override fun copy(): NDImage {
        return NDImage(ndArray.copy(), colorMode, channelMin, channelMax)
    }

    override fun deepCopy(): NDImage {
        return NDImage(ndArray.deepCopy(), colorMode, channelMin, channelMax)
    }
}