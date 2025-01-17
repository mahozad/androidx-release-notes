#!/usr/bin/env kotlin

@file:JvmName("TimestampUpdater")
@file:CompilerOptions("-jvm-target", "11")
@file:Repository("https://repo.maven.apache.org/maven2")
@file:Repository("https://jcenter.bintray.com")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.rometools:rome:2.1.0")

// NOTE: See https://youtrack.jetbrains.com/issue/KT-42101
// NOTE that currently, IntelliJ code features break when importing external scripts.
@file:Import("retry.main.kts")

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.writeText

val feedUrl = URI("https://developer.android.com/feeds/androidx-release-notes.xml").toURL()
val reader = tryTo("initialize the feed reader") { XmlReader(feedUrl) }
val feed = SyndFeedInput().build(reader)
val date = feed.publishedDate.toString()
Path("last-rss-update.txt").writeText("$date\n")
reader.close()
