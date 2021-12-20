#!/usr/bin/env kotlin

@file:JvmName("ReleaseUpdateChecker")
@file:CompilerOptions("-jvm-target", "11")
@file:Repository("https://repo.maven.apache.org/maven2")
@file:Repository("https://jcenter.bintray.com")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.rometools:rome:1.16.0")

// NOTE: See https://youtrack.jetbrains.com/issue/KT-42101
@file:Import("try-to-get.main.kts")

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val datesPattern = DateTimeFormatter.ofPattern("EEE LLL dd HH:mm:ss z yyyy")
val feedUrl = URL("https://developer.android.com/feeds/androidx-release-notes.xml")
val reader = tryToGet(
    { XmlReader(feedUrl) },
    retryCount = 5,
    "Failed to initialize the feed reader",
    "All attempts to initialize the feed reader failed."
)

reader.use {
    val feed = SyndFeedInput().build(it)
    val lastUpdated = File("last-rss-update.txt").readText().trim()
    val currentDate = feed.publishedDate.toString()
    val freshness = if (currentDate == lastUpdated) "latest" else "stale"

    val lastDateTime = lastUpdated.toLocalDate()
    val currentDateTime = currentDate.toLocalDate()
    val time = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
    val dateTag = "$currentDateTime" + if (currentDateTime == lastDateTime) "T$time" else ""

    // To log for debug
    // see https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-a-debug-message
    // Example: println("::debug::Last RSS publish date: $lastUpdated")
    // To log a regular message, use the plain println() function without any format
    println("Last RSS publish date: ${lastUpdated.ifBlank { "::EMPTY::" }}")
    println("Current RSS publish date: $currentDate")

    // To set output for a job step
    // see https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-an-output-parameter
    // and https://stackoverflow.com/a/59201610
    println("::set-output name=result::$freshness")
    println("::set-output name=dateTag::$dateTag")
}

fun String.toLocalDate() = runCatching { LocalDate.parse(this, datesPattern) }
    .getOrDefault(LocalDate.of(1900, 1, 1))
