#!/usr/bin/env kotlin

import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.math.roundToInt

typealias Point = Pair<Int, Int>

// Target cursor points
val center = 640 to 450
val watch = 948 to 222
val custom = 958 to 480
val release = 456 to 312
val apply = 818 to 456

val waitAtCenter = interpolate(center, center, 60).map(::moveTo)
val moveFromCenterToWatch = interpolate(center, watch, 30).map(::moveTo)
val waitAtWatch = interpolate(watch, watch, 15).map(::moveTo)
val moveFromWatchToCustom = interpolate(watch, custom, 30).map(::moveTo)
val waitAtCustom = interpolate(custom, custom, 15).map(::moveTo)
val moveFromCustomToRelease = interpolate(custom, release, 30).map(::moveTo)
val waitAtRelease = interpolate(release, release, 15).map(::moveTo)
val moveFromReleaseToApply = interpolate(release, apply, 30).map(::moveTo)
val waitAtApply = interpolate(apply, apply, 15).map(::moveTo)
val moveFromApplyToCenter = interpolate(apply, center, 30).map(::moveTo)

(
    waitAtCenter +
    moveFromCenterToWatch + waitAtWatch + press(watch) + release(watch) + waitAtWatch +
    moveFromWatchToCustom + waitAtCustom + press(custom) + release(custom) + waitAtCustom +
    moveFromCustomToRelease + waitAtRelease + press(release) + release(release) + waitAtRelease +
    moveFromReleaseToApply + waitAtApply + press(apply) + release(apply) + waitAtApply + waitAtApply +
    moveFromApplyToCenter + waitAtCenter
)
    .joinToString(separator = "\n")
    .let { Path("mouse-movements.arf").writeText(it) }

fun interpolate(
    a: Point,
    b: Point,
    count: Int
): List<Point> {
    val (x1, y1) = a
    val (x2, y2) = b
    val deltaX = x2 - x1
    val deltaY = y2 - y1
    val gradient = if (deltaX == 0) 0f else deltaY.toFloat() / deltaX
    val xIncrement = deltaX.toFloat() / count
    val yIncrement = xIncrement * gradient
    return buildList {
        for (i in 0..count) {
            val x = x1 + i * xIncrement
            val y = y1 + i * yIncrement
            add(x.roundToInt() to y.roundToInt())
        }
    }
}

fun moveTo(point: Point) = "${point.first}|${point.second}|mov"

fun press(point: Point) = "${point.first}|${point.second}|ltd"

fun release(point: Point) = "${point.first}|${point.second}|ltu"
