/*
 * Copyright 2020 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dl.dataset.preprocessor

import org.jetbrains.kotlinx.dl.dataset.image.MkImage
import org.jetbrains.kotlinx.dl.dataset.preprocessor.image.ImagePreprocessor
import org.jetbrains.kotlinx.multik.ndarray.operations.divAssign

/**
 * This preprocessor defines the Rescaling operation.
 * It scales each pixel  pixel_i = pixel_i / [scalingCoefficient].
 *
 * @property [scalingCoefficient] Scaling coefficient.
 */
public class Rescaling(public var scalingCoefficient: Float = 255f) : ImagePreprocessor {
    override fun apply(image: MkImage): MkImage {
        image /= scalingCoefficient
        for (i in image.channelMin.indices) image.channelMin[i] = image.channelMin[i] / scalingCoefficient
        for (i in image.channelMax.indices) image.channelMax[i] = image.channelMax[i] / scalingCoefficient
        return image
    }
}
