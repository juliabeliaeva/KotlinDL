/*
 * Copyright 2022 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dl.api.core.layer

import org.jetbrains.kotlinx.dl.api.core.initializer.Initializer
import org.jetbrains.kotlinx.dl.api.core.regularizer.Regularizer
import org.jetbrains.kotlinx.dl.api.core.util.getDType
import org.tensorflow.Operand
import org.tensorflow.Session
import org.tensorflow.Shape
import org.tensorflow.Tensor
import org.tensorflow.op.Ops
import org.tensorflow.op.core.Variable

/**
 * A class that keeps information about a single parameter of the [Layer].
 *
 * @property [name] name of the variable
 * @property [shape] shape of the variable
 * @property [variable] corresponding [Variable] object
 * @property [initializerOperation] variable initializer
 * @property [regularizer] variable regularizer
 */
public data class KVariable(
    val name: String,
    val shape: Shape,
    val variable: Variable<Float>,
    val initializerOperation: InitializerOperation,
    val regularizer: Regularizer?
)

/**
 * A class that keeps information which allows to initialize [KVariable].
 *
 * @property [assign] assign operation for the variable
 * @property [initialValue] value generated by the default initializer
 */
public data class InitializerOperation(val assign: Operand<Float>, val initialValue: Operand<Float>) {
    /**
     * Initialize the variable using the default initializer
     * @param [session] session to use
     */
    public fun run(session: Session) {
        session.runner().addTarget(assign).run()
    }

    /**
     * Fill the variable using the given data
     * @param [data] data to use, should have correct shape and type
     * @param [session] session to use
     */
    public fun fill(data: Any, session: Session) {
        var tensorData = data
        if (data is Array<*> && data.isArrayOf<Float>()) {
            tensorData = (data as Array<Float>).toFloatArray()
        }

        Tensor.create(tensorData).use { tensor ->
            session.runner()
                .feed(initialValue, tensor)
                .addTarget(assign)
                .run()
        }
    }
}

internal fun createVariable(
    tf: Ops,
    variableName: String,
    shape: Shape,
    fanIn: Int,
    fanOut: Int,
    initializer: Initializer,
    regularizer: Regularizer?
): KVariable {
    val tfVariable = tf.withName(variableName).variable(shape, getDType())
    val initializerOperation = initializer.apply(fanIn, fanOut, tf, tfVariable, variableName)
    return KVariable(
        name = variableName,
        shape = shape,
        variable = tfVariable,
        initializerOperation = initializerOperation,
        regularizer = regularizer
    )
}

internal fun List<InitializerOperation>.init(session: Session) {
    if (isEmpty()) return
    val runner = session.runner()
    map { it.assign }.forEach(runner::addTarget)
    runner.run()
}