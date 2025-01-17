#!/usr/bin/env kotlin

@file:JvmName("Retry")
@file:CompilerOptions("-jvm-target", "11")

import kotlin.time.Duration.Companion.seconds

val waitTime = 10.seconds

/**
 * Try [forAtMost] times to run the block without exception.
 *
 * We could also make subsequent runs wait
 * for an [exponential delay](https://en.wikipedia.org/wiki/Exponential_backoff).
 * See [this article](https://dzone.com/articles/understanding-retry-pattern-with-exponential-back).
 *
 * I wrote this function myself.
 * It is interesting that [this solution](https://stackoverflow.com/a/46890009)
 * proposed by Roman Elizarov is very similar to mine.
 */
fun <T> tryTo(
    description: String,
    forAtMost: Int = 5,
    block: () -> T
): T {
    repeat(forAtMost) {
        runCatching(block).onSuccess { return it }
        println("Failed to $description.")
        println("Trying again in $waitTime\n")
        Thread.sleep(waitTime.inWholeMilliseconds)
    }
    error("All attempts to $description failed.")
}
