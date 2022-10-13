/*
 * Copyright 2020-2022 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dl.api.inference.onnx.posedetection

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import org.jetbrains.kotlinx.dl.api.inference.InferenceModel
import org.jetbrains.kotlinx.dl.api.inference.onnx.CameraXCompatibleModel
import org.jetbrains.kotlinx.dl.api.inference.onnx.OnnxInferenceModel
import org.jetbrains.kotlinx.dl.api.inference.onnx.executionproviders.ExecutionProvider
import org.jetbrains.kotlinx.dl.dataset.preprocessing.*
import org.jetbrains.kotlinx.dl.dataset.shape.TensorShape
import org.jetbrains.kotlinx.dl.api.inference.onnx.ONNXModels
import org.jetbrains.kotlinx.dl.api.inference.onnx.doWithRotation
import org.jetbrains.kotlinx.dl.api.inference.posedetection.DetectedPose
import org.jetbrains.kotlinx.dl.dataset.preprocessing.camerax.toBitmap


/**
 * SinglePoseDetectionModel is an ultra-fast and accurate model that detects 17 keypoints and 18 basic edges of a body.
 *
 * It internally uses [ONNXModels.PoseDetection.MoveNetSinglePoseLighting]
 * or [ONNXModels.PoseDetection.MoveNetSinglePoseThunder] under the hood to make predictions.
 *
 * @param internalModel model used to make predictions
 */
public class SinglePoseDetectionModel(override val internalModel: OnnxInferenceModel) :
    SinglePoseDetectionModelBase<Bitmap>(), InferenceModel by internalModel, CameraXCompatibleModel {
    override val preprocessing: Operation<Bitmap, Pair<FloatArray, TensorShape>>
        get() = pipeline<Bitmap>()
            .resize {
                outputHeight = internalModel.inputDimensions[0].toInt()
                outputWidth = internalModel.inputDimensions[1].toInt()
            }
            .rotate { degrees = targetRotation.toFloat() }
            .toFloatArray { layout = TensorLayout.NHWC }

    override var targetRotation: Int = 0

    /**
     * Constructs the pose detection model from a model bytes.
     * @param [modelBytes]
     */
    public constructor (modelBytes: ByteArray) : this(OnnxInferenceModel(modelBytes)) {
        internalModel.initializeWith(ExecutionProvider.CPU())
    }

    override fun close() {
        internalModel.close()
    }
}

/**
 * Detects a pose for the given [imageProxy].
 * Internal preprocessing is updated to rotate image to match target orientation.
 * After prediction, internal preprocessing is restored to the original state.
 *
 * @param [imageProxy] input image.
 */
public fun SinglePoseDetectionModelBase<Bitmap>.detectPose(imageProxy: ImageProxy): DetectedPose =
    when (this) {
        is CameraXCompatibleModel -> {
            doWithRotation(imageProxy.imageInfo.rotationDegrees) { detectPose(imageProxy.toBitmap()) }
        }
        else -> detectPose(imageProxy.toBitmap(applyRotation = true))
    }