#!/usr/bin/env kotlin

@file:JvmName("Retry")
@file:CompilerOptions("-jvm-target", "11")

import java.time.Duration

// TODO: Use Kotlin Duration when/if switched to v1.6.x or higher
val waitTime = Duration.ofSeconds(10)

/**
 * Try for at most [retryCount] times to run the block without exception.
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
    retryCount: Int = 10,
    block: () -> T
): T {
    repeat(retryCount) {
        runCatching(block).onSuccess { return it }
        println("Failed to $description.")
        println("Attempting again in ${waitTime.seconds} seconds...")
        Thread.sleep(waitTime.toMillis())
    }
    error("All attempts to $description failed.")
}
