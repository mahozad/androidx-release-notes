#!/usr/bin/env kotlin

// See https://github.com/Kotlin/kotlin-script-examples/blob/master/jvm/main-kts/scripts/kotlinx-html.main.kts

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
