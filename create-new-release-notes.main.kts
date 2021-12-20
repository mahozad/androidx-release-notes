#!/usr/bin/env kotlin

@file:JvmName("ReleaseNotesGenerator")
@file:CompilerOptions("-jvm-target", "11")
@file:Repository("https://repo.maven.apache.org/maven2")
@file:Repository("https://jcenter.bintray.com")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.rometools:rome:1.16.0")
@file:DependsOn("org.jsoup:jsoup:1.14.3")

// FIXME: Cannot use jsoup in scripts with Kotlin 1.6.x, for now.
//  See https://youtrack.jetbrains.com/issue/KT-50378

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.net.URL
import kotlin.time.Duration.Companion.seconds

val waitTime = 10.seconds
val feedUrl = URL("https://developer.android.com/feeds/androidx-release-notes.xml")
val writer = File("release-notes.txt").bufferedWriter()
val reader = tryToGet(
    { XmlReader(feedUrl) },
    10,
    "Failed to initialize the feed reader",
    "All attempts to initialize the feed reader failed."
)

// TODO: Duplicate; use the try-to-get.main.kts script.
//  See other scripts for example usage.
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

reader.use { reader ->
    val feed = SyndFeedInput().build(reader)
    val newRelease = feed.entries/*.first()*/[1]
    val newReleaseUrls = newRelease.contents.first().value
    Jsoup
        .parse(newReleaseUrls)
        .select("a")
        .asSequence()
        .map(::toLink)
        .map(::toDocument)
        .map(::toReleaseNote)
        .forEach(writer::write)
        .also { writer.close() }
}

fun toLink(element: Element) = element.attr("href")

// FIXME: Use plain Jsoup.connect()... and remove Pair()
//  See https://github.com/jhy/jsoup/issues/1686 for the reason.
fun toDocument(link: String) = tryToGet(
    { Pair(link, Jsoup.connect(link).get()) },
    5,
    "Failed to get $link",
    "All attempts to get the document failed."
)

fun toReleaseNote(pair: Pair<String, Document>): String {
    val (link , document) = pair
    val fragmentId = link.substringAfter("#")
    val unitName = document.select("h1").text()
    val version = document
        // See https://github.com/jhy/jsoup/issues/1055 and 1441
        .select("[id=$fragmentId]")
        .attr("data-text")
        .replace(Regex(".*Version "), "v")

    val changelog = document
        .select("h3[id=$fragmentId] ~ *")
        .takeWhile { it.`is`(":not(h2)") }
        .takeWhile { it.`is`(":not(h3)") }
        .joinToString("\n")

    return createEntry(unitName, version, changelog)
}

fun createEntry(
    name: String,
    version: String,
    changelog: String
) = buildString {
    appendLine("<h2>$name: $version</h2>")
    appendLine(changelog)
    appendLine()
}
