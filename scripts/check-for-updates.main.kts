#!/usr/bin/env kotlin

@file:JvmName("ReleaseUpdateChecker")
@file:CompilerOptions("-jvm-target", "11")
@file:Repository("https://repo.maven.apache.org/maven2")
@file:Repository("https://jcenter.bintray.com")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.rometools:rome:1.16.0")

// NOTE: See https://youtrack.jetbrains.com/issue/KT-42101
@file:Import("retry.main.kts")

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import java.io.File
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME

val datesPattern = DateTimeFormatter.ofPattern("EEE LLL dd HH:mm:ss z yyyy")
val feedUrl = URL("https://developer.android.com/feeds/androidx-release-notes.xml")
val reader = tryTo("initialize the feed reader") { XmlReader(feedUrl) }
val file = File("last-rss-update.txt")
val feed = SyndFeedInput().build(reader)
val ours = file.readText().trimEnd().parse()
val ourDate = ours.toLocalDate().toString()
val theirs = feed.publishedDate.toString().parse()
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
val stepsOutputFile = File(System.getenv("GITHUB_OUTPUT"))
val lineFeed: String = System.lineSeparator()
stepsOutputFile.appendText("result=$freshness$lineFeed")
stepsOutputFile.appendText("dateTag=$dateTag$lineFeed")

fun String.parse() = runCatching { LocalDateTime.parse(this, datesPattern) }
    .getOrDefault(LocalDateTime.of(1, 1, 1, 1, 1))
