#!/usr/bin/env kotlin

@file:JvmName("TimestampUpdater")
@file:CompilerOptions("-jvm-target", "17")
@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("com.rometools:rome:2.1.0")

@file:Import("retry.kts")

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import java.net.URI
import kotlin.io.path.Path
import kotlin.io.path.writeText

val feedUrl = URI("https://developer.android.com/feeds/androidx-release-notes.xml").toURL()
val reader = tryTo("initialize the feed reader") { XmlReader(feedUrl.openStream()) }
val feed = SyndFeedInput().build(reader)
val date = feed.publishedDate.toString()
Path("last-rss-update.txt").writeText("$date\n")
reader.close()
