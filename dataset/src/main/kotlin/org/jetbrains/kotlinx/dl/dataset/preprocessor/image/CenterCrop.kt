/*
 * Copyright 2022 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dl.dataset.preprocessor.image

import org.jetbrains.kotlinx.dl.dataset.image.*
import org.jetbrains.kotlinx.dl.dataset.preprocessor.ImageShape
import org.jetbrains.kotlinx.multik.ndarray.data.D3
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.data.get

/**
 * This image preprocessor defines centerCrop operation.
 * It crops the given image at the center. If the image size is smaller than the output size along any edge,
 * the image is padded with 0 and then center cropped.
 *
 * @property [size] target image size.
 */
public class CenterCrop(public var size: Int = -1) : ImagePreprocessorBase() {

    override fun getOutputShape(inputShape: ImageShape): ImageShape {
        if (size <= 0) return inputShape
        return ImageShape(size.toLong(), size.toLong(), inputShape.channels)
    }

    override fun apply(image: MkImage): MkImage {
        if (size <= 0 || (image.width == size && image.height == size)) return image

        val paddedImage = padIfNecessary(image)
        val x = (paddedImage.width - size) / 2
        val y = (paddedImage.height - size) / 2
        // todo
        return NDImage(paddedImage[y..y + size, x..x + size].copy() as NDArray<Float, D3>, image.colorMode)
    }

    private fun padIfNecessary(image: MkImage): MkImage {
        if (image.width < size || image.height < size) {
            val verticalSpace = (size - image.height).coerceAtLeast(0)
            val horizontalSpace = (size - image.width).coerceAtLeast(0)
            val top = verticalSpace / 2
            val left = horizontalSpace / 2
            return Padding(
                top = top, bottom = verticalSpace - top,
                left = left, right = horizontalSpace - left,
                mode = PaddingMode.Black
            ).apply(image)
        }
        return image
    }
}