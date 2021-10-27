package org.jetbrains.kotlinx.dl.dataset.image

import org.jetbrains.kotlinx.dl.dataset.preprocessor.ImageShape
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.D3
import org.jetbrains.kotlinx.multik.ndarray.data.MultiArray
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import java.awt.image.BufferedImage

public val MultiArray<*, D3>.height: Int get() = this.shape[0]
public val MultiArray<*, D3>.width: Int get() = this.shape[1]
public val MultiArray<*, D3>.channels: Int get() = this.shape[2]

public fun MultiArray<*, D3>.getShape(): ImageShape = ImageShape(width.toLong(), height.toLong(), channels.toLong())

public fun MultiArray<Float, D3>.copyToNDArray(): NDArray<Float, D3> = deepCopy() as NDArray<Float, D3> // TODO


public fun BufferedImage.toMkImage(): MkImage = NDImage(mk.zeros(1, 1, 1), this.colorMode()) // TODO