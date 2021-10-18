package examples.dataset

import org.jetbrains.kotlinx.dl.dataset.image.ColorMode
import org.jetbrains.kotlinx.dl.dataset.image.draw
import org.jetbrains.kotlinx.dl.dataset.image.toImage
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.get
import java.awt.Color
import javax.swing.JFrame

fun main() {
    val image = mk.zeros<Float>(100, 100, 3)
    val bufferedImage = image.toImage(ColorMode.RGB)
    bufferedImage.draw {
        it.color = Color.RED
        it.drawRect(10, 20, 60, 50)
        it.color = Color.BLUE
        it.fillRect(20, 30, 10, 10)
    }
    println(image[20, 10])
    println(image[30, 20])

    val frame = JFrame("Multik Image")
    frame.contentPane.add(ImagePanel(bufferedImage))
    frame.pack()
    frame.isVisible = true
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isResizable = false
}