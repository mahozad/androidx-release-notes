#!/usr/bin/env kotlin

@file:JvmName("TimestampUpdater")
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

val feedUrl = URL("https://developer.android.com/feeds/androidx-release-notes.xml")
val reader = tryTo("initialize the feed reader") { XmlReader(feedUrl) }
val feed = SyndFeedInput().build(reader)
val date = feed.publishedDate.toString()
File("last-rss-update.txt").writeText("$date\n")
reader.close()
