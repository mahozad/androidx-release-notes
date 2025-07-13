#!/usr/bin/env kotlin

@file:JvmName("ReleaseNotesGenerator")
@file:CompilerOptions("-jvm-target", "17")
@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("com.rometools:rome:2.1.0")
@file:DependsOn("org.jsoup:jsoup:1.21.1")

@file:Import("retry.kts")

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.writeText

typealias LinkString = String
typealias LinkDocument = Pair<LinkString, Document>

val resultFile = Path("release-notes.html")
val feedUrl = URI("https://developer.android.com/feeds/androidx-release-notes.xml").toURL()
val writer = resultFile.bufferedWriter()
val reader = tryTo("initialize the feed reader") {
    // NOTE: Use this to test for a complicated release notes
    // XmlReader(File("test-feed-result.xml"))
    XmlReader(feedUrl.openStream())
}
val feed = SyndFeedInput().build(reader)
val latestRelease = feed.entries.first()
val latestReleaseItems = latestRelease.contents.first().value

// Specifies the base URL for relative links in the document
writer.write("""<base href="https://developer.android.com/" target="_blank"/>""")
writer.write("\n\n")

Jsoup
    .parse(latestReleaseItems)
    .select("a")
    .asSequence()
    .map(Element::toLink)
    .map(LinkString::toLinkDocument)
    .map(LinkDocument::toReleaseNote)
    .forEach(writer::write)
    .also { writer.close() }
    .also { reader.close() }

// Creates a raw text version as well in case someone needs it
val text = Jsoup.parse(resultFile).wholeText()
Path("release-notes.txt").writeText(text)

fun Element.toLink() = this.attr("href")

// FIXME: Use plain Jsoup.connect()... and remove Pair creation (to)
//  See https://github.com/jhy/jsoup/issues/1686 for the reason.
fun LinkString.toLinkDocument() = tryTo("get $this") {
    this to Jsoup.connect(this).get()
}

fun LinkDocument.toReleaseNote(): String {
    val (link, document) = this
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

// See https://github.com/jhy/jsoup/issues/1055
fun Document.extractName(id: String) = this
    .select("[id=$id]")
    .prev("h2")
    .text()
    .substringBefore("Version")
    .replace(Regex("""^\d+.*"""), "")
    .ifBlank { select("h1").text() }
    .trim()

fun Document.extractVersion(id: String) = this
    .select("[id=$id]")
    .attr("data-text")
    .replace(Regex(".*Version "), "v")

fun Document.extractChangelog(id: String) = this
    .select("[id=$id] ~ *")
    .takeWhile { it.`is`(":not(h2)") }
    .takeWhile { it.`is`(":not(h3)") }
    .joinToString("\n")



/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */
/* -------------------------------------------------------------------------- */



// This is a coroutine version of the code.
// Needs @file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
// which seems to not work in Kotlin scripts.
// Probably fixed in Kotlin 1.7 or newer
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
            // See https://stackoverflow.com/a/46890009
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
