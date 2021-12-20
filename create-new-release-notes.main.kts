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
import java.time.Duration

// TODO: Use Kotlin Duration when/if switched to v1.6.x or higher
val waitTime = Duration.ofSeconds(10)
val feedUrl = URL("https://developer.android.com/feeds/androidx-release-notes.xml")
val writer = File("release-notes.html").bufferedWriter()
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
        if (result.isSuccess) return result.getOrThrow()
        println("$failMessage; attempting again in ${waitTime.seconds} seconds")
        Thread.sleep(waitTime.toMillis())
    }
    error(errorMessage)
}

reader.use { reader ->
    val feed = SyndFeedInput().build(reader)
    val latestRelease = feed.entries.first()
    val latestReleaseUrls = latestRelease.contents.first().value
    Jsoup
        .parse(latestReleaseUrls)
        .select("a")
        .asSequence()
        .map(::toLink)
        .map(::toDocument)
        .map(::toReleaseNote)
        .forEach(writer::write)
        .also { writer.close() }
}

// Create a raw text version as well just if someone needs it
val text = Jsoup
    .parse(File("release-notes.html"), "UTF-8")
    .wholeText()
File("release-notes.txt").writeText(text)

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
    val (link, document) = pair
    val id = link.substringAfter("#")
    val name = document.extractName(id)
    val version = document.extractVersion(id)
    val changelog = document.extractChangelog(id)
    return createEntry(name, version, changelog)
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

// See https://github.com/jhy/jsoup/issues/1055 and 1441
fun Document.extractName(id: String) = this
    .select("[id=$id]")
    .prev("h2")
    .text()
    .substringBefore("Version")
    .replace(Regex("""^\d+.*"""), "")
    .ifBlank { select("h1").text() }
    .trim()

// See https://github.com/jhy/jsoup/issues/1055 and 1441
fun Document.extractVersion(id: String) = this
    .select("[id=$id]")
    .attr("data-text")
    .replace(Regex(".*Version "), "v")

// See https://github.com/jhy/jsoup/issues/1055 and 1441
fun Document.extractChangelog(id: String) = this
    .select("h3[id=$id] ~ *")
    .takeWhile { it.`is`(":not(h2)") }
    .takeWhile { it.`is`(":not(h3)") }
    .joinToString("\n")
