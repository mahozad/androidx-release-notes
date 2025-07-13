import kotlin.time.Duration.Companion.seconds

// Updates to this file are not reflected to user scripts
// See https://youtrack.jetbrains.com/issue/KT-42101

val waitTime = 10.seconds

/**
 * Try for [maxTries] times to run the block without exception.
 *
 * We could also make subsequent runs wait
 * for an [exponential delay](https://en.wikipedia.org/wiki/Exponential_backoff).
 * See [this article](https://dzone.com/articles/understanding-retry-pattern-with-exponential-back).
 *
 * I wrote this function myself.
 * It is interesting that [this solution](https://stackoverflow.com/a/46890009)
 * proposed by Roman Elizarov is very similar to mine.
 */
@Suppress("unused")
fun <T> tryTo(
    description: String,
    maxTries: Int = 5,
    block: () -> T
): T {
    for (i in 1..maxTries) {
        runCatching(block).onSuccess { return it }
        println("Failed to $description.")
        if (i == maxTries) break
        println("Trying again in $waitTime\n")
        Thread.sleep(waitTime.inWholeMilliseconds)
    }
    error("All $maxTries attempts to $description failed.")
}
