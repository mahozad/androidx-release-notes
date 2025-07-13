#!/usr/bin/env kotlin

@file:JvmName("ReleaseUpdateChecker")
@file:CompilerOptions("-jvm-target", "17")
@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("com.rometools:rome:2.1.0")

@file:Import("retry.kts")

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.*

val datesPattern = DateTimeFormatter.ofPattern("EEE LLL dd HH:mm:ss z yyyy")
val feedUrl = URI("https://developer.android.com/feeds/androidx-release-notes.xml").toURL()
val reader = tryTo("initialize the feed reader") { XmlReader(feedUrl.openStream()) }
val file = Path("last-rss-update.txt").takeIf { it.exists() }
val feed = SyndFeedInput().build(reader)
val ours = file?.readText()?.trimEnd().parseAsLocalDateTime()
val ourDate = ours.toLocalDate().toString()
val theirs = feed.publishedDate.toString().parseAsLocalDateTime()
val theirDate = theirs.toLocalDate().toString()
val theirTime = theirs.toLocalTime().format(DateTimeFormatter.ofPattern("hha"))
val areTheSame = ours == theirs
val freshness = if (areTheSame) "latest" else "stale"
val dateTag = theirDate + if (ourDate == theirDate) "@$theirTime" else ""
reader.close()

// To log for debug
// see https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-a-debug-message
// Example: println("::debug::Last RSS publish date: $lastDate")
// To log a regular message, use the plain println() function without any format
println("Our RSS publish date:   $ours")
println("Their RSS publish date: $theirs")

// To set output for a job step
// see https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-an-output-parameter
// and https://stackoverflow.com/a/59201610
val stepsOutputFile = (System.getenv("GITHUB_OUTPUT") ?: "output.txt")
    .let(::Path)
    .also { if (it.notExists()) it.createFile() }
val lineFeed: String = System.lineSeparator()
stepsOutputFile.appendText("result=$freshness$lineFeed")
stepsOutputFile.appendText("dateTag=$dateTag$lineFeed")

fun String?.parseAsLocalDateTime() = this
    ?.runCatching { LocalDateTime.parse(this, datesPattern) }
    ?.getOrNull()
    ?: LocalDateTime.of(1, 1, 1, 1, 1)
