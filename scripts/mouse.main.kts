#!/usr/bin/env kotlin

// See https://github.com/Kotlin/kotlin-script-examples/blob/master/jvm/main-kts/scripts/kotlinx-html.main.kts

import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.math.roundToInt

typealias Point = Pair<Int, Int>

val resultFile = Path("mouse-movements.arf")

// Target cursor points
val center = 640 to 450
val watch = 948 to 222
val custom = 958 to 480
val release = 456 to 312
val apply = 818 to 456

(
    waitAt(center, duration = 60) +
    move(from = center, to = watch) + waitAt(watch) + press(watch) + letGo(watch) + waitAt(watch) +
    move(from = watch, to = custom) + waitAt(custom) + press(custom) + letGo(custom) + waitAt(custom) +
    move(from = custom, to = release) + waitAt(release) + press(release) + letGo(release) + waitAt(release) +
    move(from = release, to = apply) + waitAt(apply) + press(apply) + letGo(apply) + waitAt(apply, duration = 30) +
    move(from = apply, to = center) + waitAt(center, duration = 60)
)
    .joinToString(separator = "\n")
    .let(resultFile::writeText)

fun waitAt(point: Point, duration: Int = 15) = interpolate(point, point, duration).map(::goTo)

fun move(from: Point, to: Point) = interpolate(from, to, 30).map(::goTo)

fun press(point: Point) = "${point.first}|${point.second}|ltd"

fun letGo(point: Point) = "${point.first}|${point.second}|ltu"

fun goTo(point: Point) = "${point.first}|${point.second}|mov"

fun interpolate(
    a: Point,
    b: Point,
    steps: Int
): List<Point> {
    val (x1, y1) = a
    val (x2, y2) = b
    val deltaX = x2 - x1
    val deltaY = y2 - y1
    val slope = if (deltaX == 0) 0f else deltaY / deltaX.toFloat()
    val xIncrement = deltaX.toFloat() / steps
    val yIncrement = xIncrement * slope
    return buildList {
        for (i in 0..steps) {
            val x = x1 + (i * xIncrement)
            val y = y1 + (i * yIncrement)
            add(x.roundToInt() to y.roundToInt())
        }
    }
}
