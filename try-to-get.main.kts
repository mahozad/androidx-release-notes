#!/usr/bin/env kotlin

@file:JvmName("Retry")
@file:CompilerOptions("-jvm-target", "11")

import java.time.Duration

// TODO: Use Kotlin Duration when/if switched to v1.6.x or higher
val waitTime = Duration.ofSeconds(10)

/**
 * Try for at most [retryCount] times to run the block without exception.
 */
fun <T> tryToGet(
    block: () -> T,
    retryCount: Int,
    failMessage: String,
    errorMessage: String
): T {
    repeat(retryCount) {
        runCatching(block).onSuccess { return it }
        println("$failMessage; attempting again in ${waitTime.seconds} seconds")
        Thread.sleep(waitTime.toMillis())
    }
    error(errorMessage)
}
