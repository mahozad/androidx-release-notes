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

val feedUrl = URL("https://developer.android.com/feeds/androidx-release-notes.xml")
val reader = tryToGet(
    { XmlReader(feedUrl) },
    5,
    "Failed to initialize the feed reader",
    "All attempts to initialize the feed reader failed."
)

reader.use {
    val feed = SyndFeedInput().build(it)
    val lastUpdated = File("last-rss-update.txt").readText().trim()
    val currentDate = feed.publishedDate.toString()
    val result = if (currentDate == lastUpdated) "latest" else "stale"

    // To log for debug
    // see https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-a-debug-message
    // Example: println("::debug::Last RSS publish date: $lastUpdated")
    // To log a regular message, use the plain println() function without any format
    println("Last RSS publish date: $lastUpdated")
    println("Current RSS publish date: $currentDate")

    // To set output for a job step
    // see https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-an-output-parameter
    // and https://stackoverflow.com/a/59201610
    println("::set-output name=result::$result")
    println("::set-output name=date::$currentDate")
}
