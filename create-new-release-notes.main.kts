#!/usr/bin/env kotlin

@file:JvmName("ReleaseNotesGenerator")
@file:CompilerOptions("-jvm-target", "11")
@file:CompilerOptions("-Xopt-in", "kotlin.RequiresOptIn")
@file:OptIn(kotlin.time.ExperimentalTime::class)
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
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val resultFile = File("release-notes.html")
// TODO: Use Int::seconds when/if switched to Kotlin v1.6.x or higher
val waitTime = 10.toDuration(DurationUnit.SECONDS)
val feedUrl = URL("https://developer.android.com/feeds/androidx-release-notes.xml")
val writer = resultFile.bufferedWriter()
val reader = tryTo("initialize the feed reader") {
    // NOTE: Use this to test for a complicated release notes
    // XmlReader(File("test-feed-result.xml"))
    XmlReader(feedUrl)
}

// TODO: Duplicate; use the retry.main.kts script.
//  See other scripts for example usage.
//  NOTE that currently IntelliJ code features break when importing external script.
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

val feed = SyndFeedInput().build(reader)
val latestRelease = feed.entries.first()
val latestReleaseItems = latestRelease.contents.first().value
Jsoup
    .parse(latestReleaseItems)
    .select("a")
    .asSequence()
    .map(::toLink)
    .map(::toDocument)
    .map(::toReleaseNote)
    .forEach(writer::write)
    .also { writer.close() }
    .also { reader.close() }

// Create a raw text version as well just if someone needs it
val text = Jsoup
    .parse(resultFile, "UTF-8")
    .wholeText()
File("release-notes.txt").writeText(text)

fun toLink(element: Element) = element.attr("href")

// FIXME: Use plain Jsoup.connect()... and remove Pair()
//  See https://github.com/jhy/jsoup/issues/1686 for the reason.
fun toDocument(link: String) = tryTo("get $link") {
    Pair(link, Jsoup.connect(link).get())
}

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
    .select("[id=$id] ~ *")
    .takeWhile { it.`is`(":not(h2)") }
    .takeWhile { it.`is`(":not(h3)") }
    .joinToString("\n")
