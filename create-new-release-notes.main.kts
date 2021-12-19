#!/usr/bin/env kotlin

@file:JvmName("ReleaseNotesGenerator")
@file:CompilerOptions("-jvm-target", "11")
@file:Repository("https://repo.maven.apache.org/maven2")
@file:Repository("https://jcenter.bintray.com")
@file:Repository("https://jitpack.io")
@file:DependsOn("com.rometools:rome:1.16.0")

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import java.net.URL
import kotlin.time.Duration.Companion.seconds

val feedUrl = URL("https://developer.android.com/feeds/androidx-release-notes.xml")
val retryDelay = 10.seconds
lateinit var feedReader: XmlReader

// Try for at most 5 times to initialize the feed reader
for (i in 1..5) {
    val initResult = tryToInitializeReader()
    if (initResult.isSuccess) break
    Thread.sleep(retryDelay.inWholeMilliseconds)
}

fun tryToInitializeReader() =
    runCatching { XmlReader(feedUrl) }
        .onSuccess { feedReader = it }
        .onFailure { println("Feed reader init failed; attempting again in $retryDelay") }

feedReader.use {
    val feed = SyndFeedInput().build(it)
    val newReleases = feed.entries[1]
    println("newReleases.contents: ${newReleases.contents.first().value}")

    /*
        // To log, see https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-a-debug-message
        println("::debug::Last RSS publish date: $lastUpdated")
        println("::debug::Current RSS publish date: $currentDate")
        // To set output for a job step
        // see https://docs.github.com/en/actions/learn-github-actions/workflow-commands-for-github-actions#setting-an-output-parameter
        // and https://stackoverflow.com/a/59201610
        println("::set-output name=result::$result")*/
}
