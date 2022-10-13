package org.jetbrains.kotlinx.dl.api.inference.onnx.classification

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import org.jetbrains.kotlinx.dl.api.inference.imagerecognition.ImageRecognitionModelBase
import org.jetbrains.kotlinx.dl.api.inference.onnx.CameraXCompatibleModel
import org.jetbrains.kotlinx.dl.api.inference.onnx.ExecutionProviderCompatible
import org.jetbrains.kotlinx.dl.api.inference.onnx.OnnxInferenceModel
import org.jetbrains.kotlinx.dl.api.inference.onnx.doWithRotation
import org.jetbrains.kotlinx.dl.api.inference.onnx.executionproviders.ExecutionProvider
import org.jetbrains.kotlinx.dl.dataset.Imagenet
import org.jetbrains.kotlinx.dl.dataset.preprocessing.*
import org.jetbrains.kotlinx.dl.dataset.preprocessing.camerax.toBitmap
import org.jetbrains.kotlinx.dl.dataset.shape.TensorShape

/**
 * The light-weight API for Classification task with one of the Model Hub models.
 */
public open class ImageRecognitionModel(
    internalModel: OnnxInferenceModel,
    private val channelsFirst: Boolean,
    private val preprocessor: Operation<Pair<FloatArray, TensorShape>, Pair<FloatArray, TensorShape>> = Identity(),
    override val classLabels: Map<Int, String> = Imagenet.V1k.labels()
) : ImageRecognitionModelBase<Bitmap>(internalModel), ExecutionProviderCompatible, CameraXCompatibleModel {
    override var targetRotation: Int = 0

    override val preprocessing: Operation<Bitmap, Pair<FloatArray, TensorShape>>
        get() {
            val (width, height) = if (channelsFirst)
                Pair(internalModel.inputDimensions[1], internalModel.inputDimensions[2])
            else
                Pair(internalModel.inputDimensions[0], internalModel.inputDimensions[1])

            return pipeline<Bitmap>()
                .resize {
                    outputHeight = height.toInt()
                    outputWidth = width.toInt()
                }
                .rotate { degrees = targetRotation.toFloat() }
                .toFloatArray { layout = if (channelsFirst) TensorLayout.NCHW else TensorLayout.NHWC }
                .call(preprocessor)
        }

    override fun initializeWith(vararg executionProviders: ExecutionProvider) {
        (internalModel as OnnxInferenceModel).initializeWith(*executionProviders)
    }
}

/**
 * Predicts object for the given [imageProxy].
 * Internal preprocessing is updated to rotate image to match target orientation.
 * After prediction, internal preprocessing is restored to the original state.
 *
 * @param [imageProxy] Input image.
 *
 * @return The label of the recognized object with the highest probability.
 */
public fun ImageRecognitionModelBase<Bitmap>.predictObject(imageProxy: ImageProxy): String =
    when (this) {
        is CameraXCompatibleModel -> {
            doWithRotation(imageProxy.imageInfo.rotationDegrees) { predictObject(imageProxy.toBitmap()) }
        }
        else -> predictObject(imageProxy.toBitmap(applyRotation = true))
    }

/**
 * Predicts [topK] objects for the given [imageProxy].
 * Internal preprocessing is updated to rotate image to match target orientation.
 * After prediction, internal preprocessing is restored to the original state.
 *
 * @param [imageProxy] Input image.
 * @param [topK] Number of top ranked predictions to return
 *
 * @return The list of pairs <label, probability> sorted from the most probable to the lowest probable.
 */
public fun ImageRecognitionModelBase<Bitmap>.predictTopKObjects(
    imageProxy: ImageProxy,
    topK: Int = 5
): List<Pair<String, Float>> =
    when (this) {
        is CameraXCompatibleModel -> {
            doWithRotation(imageProxy.imageInfo.rotationDegrees) { predictTopKObjects(imageProxy.toBitmap(), topK) }
        }
        else -> predictTopKObjects(imageProxy.toBitmap(applyRotation = true), topK)
    }