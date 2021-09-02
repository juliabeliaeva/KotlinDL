/*
 * Copyright 2020 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dl.api.inference.keras.loaders

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import io.jhdf.HdfFile
import mu.KLogger
import mu.KotlinLogging
import org.jetbrains.kotlinx.dl.api.core.Functional
import org.jetbrains.kotlinx.dl.api.core.GraphTrainableModel
import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.inference.InferenceModel
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

private const val MODEL_CONFIG_FILE_NAME = "/modelConfig.json"
private const val WEIGHTS_FILE_NAME = "/weights.h5"

/**
 * This model loaders provides methods for loading model, its weights and ImageNet labels (for prediction purposes) to the local directory
 * [commonModelDirectory].
 *
 * @property [commonModelDirectory] The directory for all loaded models. It should be created before model loading and should have all required permissions for file writing/reading on your OS
 * @property [modelType] This value defines the way to S3 bucket with the model and its weights and also local directory for the model and its weights.
 *
 * @since 0.2
 */
public class TFModelHub(commonModelDirectory: File, modelType: ModelType): ModelHub(commonModelDirectory, modelType) {
    private val modelDirectory = "/" + modelType.modelRelativePath
    private val relativeConfigPath = modelDirectory + MODEL_CONFIG_FILE_NAME
    private val relativeWeightsPath = modelDirectory + WEIGHTS_FILE_NAME
    private val configURL = AWS_S3_URL + modelDirectory + MODEL_CONFIG_FILE_NAME
    private val weightsURL = AWS_S3_URL + modelDirectory + WEIGHTS_FILE_NAME

    /** Logger for modelZoo model. */
    private val logger: KLogger = KotlinLogging.logger {}

    init {
        if (!commonModelDirectory.exists()) {
            Files.createDirectories(commonModelDirectory.toPath())
        }
    }

    /**
     * Loads model configuration without weights.
     *
     * @param [loadingMode] Strategy of existing model use-case handling.
     * @return Raw model without weights. Needs in compilation and weights loading via [loadWeights] before usage.
     */
    public override fun loadModel(loadingMode: LoadingMode): InferenceModel {
        val jsonConfigFile = getJSONConfigFile(loadingMode)
        return when (modelType) {
            TFModels.CV.VGG_16 -> Sequential.loadModelConfiguration(jsonConfigFile)
            TFModels.CV.VGG_19 -> Sequential.loadModelConfiguration(jsonConfigFile)
            TFModels.CV.ResNet_18 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.ResNet_34 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.ResNet_50 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.ResNet_101 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.ResNet_152 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.ResNet_50_v2 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.ResNet_101_v2 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.ResNet_151_v2 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.MobileNet -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.MobileNetv2 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.Inception -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.Xception -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.DenseNet121 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.DenseNet169 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.DenseNet201 -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.NASNetMobile -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            TFModels.CV.NASNetLarge -> freezeAllLayers(Functional.loadModelConfiguration(jsonConfigFile))
            else -> TODO()
        }
    }

    private fun freezeAllLayers(model: Functional): GraphTrainableModel {
        for (layer in model.layers) {
            layer.isTrainable = false
        }
        return model
    }

    /** Forms mapping of class label to class name for the ImageNet dataset. */
    public fun loadClassLabels(): MutableMap<Int, String> {
        val pathToIndices = "/datasets/vgg/imagenet_class_index.json"

        fun parse(name: String): Any? {
            val cls = Parser::class.java
            return cls.getResourceAsStream(name)?.let { inputStream ->
                return Parser.default().parse(inputStream, Charsets.UTF_8)
            }
        }

        val classIndices = parse(pathToIndices) as JsonObject

        val imageNetClassIndices = mutableMapOf<Int, String>()

        for (key in classIndices.keys) {
            imageNetClassIndices[key.toInt()] = (classIndices[key] as JsonArray<*>)[1].toString()
        }
        return imageNetClassIndices
    }

    /**
     * Loads model weights.
     *
     * @param [loadingMode] Strategy of existing model use-case handling.
     * @return Compiled model with initialized weights.
     */
    public fun loadWeights(loadingMode: LoadingMode = LoadingMode.SKIP_LOADING_IF_EXISTS): HdfFile {
        return getWeightsFile(loadingMode)
    }

    /** Returns JSON file with model configuration, saved from Keras 2.x. */
    private fun getJSONConfigFile(loadingMode: LoadingMode): File {
        val dir = File(commonModelDirectory.absolutePath + modelDirectory)
        if (!dir.exists()) dir.mkdir()

        val fileName = commonModelDirectory.absolutePath + relativeConfigPath
        val file = File(fileName)

        if (!file.exists() || loadingMode == LoadingMode.OVERRIDE_IF_EXISTS) {
            val inputStream = URL(configURL).openStream()
            logger.debug { "Model loading is started!" }
            Files.copy(inputStream, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING)
            logger.debug { "Model loading is finished!" }
        }

        return File(fileName)
    }

    /** Returns .h5 file with model weights, saved from Keras 2.x. */
    private fun getWeightsFile(loadingMode: LoadingMode): HdfFile {
        val fileName = commonModelDirectory.absolutePath + relativeWeightsPath
        val file = File(fileName)
        if (!file.exists() || loadingMode == LoadingMode.OVERRIDE_IF_EXISTS) {
            val inputStream = URL(weightsURL).openStream()
            logger.debug { "Weights loading is started!" }
            Files.copy(inputStream, Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING)
            logger.debug { "Weights loading is finished!" }
        }

        return HdfFile(File(fileName))
    }
}



