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
    //  XmlReader(File("test-feed-result.xml"))
    XmlReader(feedUrl)
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

// Create a raw text version as well in case someone needs it
val text = Jsoup.parse(resultFile, null).wholeText()
File("release-notes.txt").writeText(text)

// TODO: Duplicate; use the retry.main.kts script.
//  See other scripts for example usage.
//  NOTE that currently, IntelliJ code features break when importing external scripts.
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
    return createItemString(name, version, changelog)
}

fun createItemString(
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

/* -------------------------------------------------------------------------- */

// This is a coroutine version of the code.
// Needs `org.jetbrains.kotlinx:kotlinx-coroutines-core`
// which seems to not work in Kotlin scripts.
/*
fun main() = runBlocking {
    val init = async(Dispatchers.IO) {
        flow {
            emit(XmlReader(javaClass.getResource("/test-feed-result.xml")))
            // emit(XmlReader(feedUrl))
        }.retry(10) {
            println("Failed to create the reader. Retrying in $waitTime...")
            delay(waitTime.inWholeMilliseconds)
            it is Exception
        }.single()
    }
    val reader = init.await()
    val feed = SyndFeedInput().build(reader)
    val latestRelease = feed.entries.first()
    val latestReleaseItems = latestRelease.contents.first().value

    var totalItems = 0
    var processed = 0.0

    // See https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/collect.html
    val job = launch(Dispatchers.IO) {
        Jsoup
            .parse(latestReleaseItems)
            .select("a")
            .also { totalItems = it.size }
            .asFlow()
            .onStart { println("Downloading items started...") }
            .map(::toLink)
            // Make toDocument and tryTo suspendable and use delay in tryTo?
            .map(::toDocument)
            // .catch {  }
            // NOTE that retry will start the whole flow over again.
            .retry(5) {
                println("Some error happened. Starting over in $waitTime...")
                delay(waitTime.inWholeMilliseconds)
                it is Exception
            }
            .onEach { processed++ }
            .onEach { println("Successful download: ${it.first}") }
            .map(::toReleaseNote)
            // .flowOn(Dispatchers.IO)
            .onEmpty { emit("No libraries entries!") }
            .onEach(writer::write)
            .onCompletion { println("All items downloaded.") }
            .collect()
            .also { writer.close() }
            .also { reader.close() }
    }

    var previousProgress = -1
    while (!job.isCompleted) {
        delay(100)
        val progress = (processed / totalItems * 100).roundToInt()
        if (progress != previousProgress) println("Progress: $progress%")
        previousProgress = progress
        // yield()
    }

    // Create a raw text version as well just if someone needs it
    val text = Jsoup.parse(resultFile, null).wholeText()
    File("release-notes.txt").writeText(text)
}
*/
