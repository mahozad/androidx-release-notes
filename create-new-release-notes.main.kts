#!/usr/bin/env kotlin

@file:JvmName("ReleaseNotesGenerator")
@file:CompilerOptions("-jvm-target", "11")
@file:Repository("https://repo.maven.apache.org/maven2")
@file:Repository("https://jcenter.bintray.com")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.rometools:rome:1.16.0")
@file:DependsOn("org.jsoup:jsoup:1.14.3")

// jsoup does not work with Kotlin 1.6.x, for now.
// See https://youtrack.jetbrains.com/issue/KT-50378

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.net.URL
import kotlin.time.Duration.Companion.seconds

val waitTime = 10.seconds
val feedUrl = URL("https://developer.android.com/feeds/androidx-release-notes.xml")
val writer = File("release-notes.md").bufferedWriter()
val reader = tryToGet(
    { XmlReader(feedUrl) },
    5,
    "Failed to initialize the feed reader",
    "All attempts to initialize the feed reader failed."
)

/**
 * Try for at most retryCount times to run the block without exception.
 */
fun <T> tryToGet(
    block: () -> T,
    retryCount: Int,
    failMessage: String,
    errorMessage: String
): T {
    repeat(retryCount) {
        val result = runCatching(block)
        if (result.isSuccess)
            return result.getOrThrow()
        println("$failMessage; attempting again in $waitTime")
        Thread.sleep(waitTime.inWholeMilliseconds)
    }
    error(errorMessage)
}

fun toLink(element: Element) = element.attr("href")

reader.use { reader ->
    val feed = SyndFeedInput().build(reader)
    val newReleases = feed.entries/*.first()*/[1]
    val newReleaseUrls = newReleases.contents.first().value

    val releaseLinks = Jsoup
        .parse(newReleaseUrls, "UTF-8")
        .select("a")
        .map(::toLink)

    for (releaseLink in releaseLinks) {
        val document = tryToGet(
            { Jsoup.connect(releaseLink).get() },
            5,
            "Failed to get $releaseLink",
            "All attempts to get the document failed."
        )

        val name = document.select("h1").text()
        val fragmentId = releaseLink.substringAfter("#")
        val version = document
            // .select("h3#$fragmentId") // Why this does not work?
            .select("[id=$fragmentId]")
            .takeIf { it.`is`("h3") }
            ?.attr("data-text")
            ?: document.firstElementSibling().attr("data-text")

        val changelog = document
            .select("h3[id=$fragmentId] ~ *")
            .takeWhile { it.`is`(":not(h3)") && it.`is`(":not(h2)") }
            .joinToString("\n")

        val output = createEntry(name, version, changelog)
        writer.write(output)
    }
}

writer.close()

fun createEntry(
    name: String,
    version: String,
    changelog: String
) = buildString {
    appendLine("<h2>$name: $version</h2>")
    appendLine(changelog)
    appendLine()
}
