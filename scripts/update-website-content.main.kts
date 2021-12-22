#!/usr/bin/env kotlin

@file:JvmName("WebsiteUpdater")
@file:CompilerOptions("-jvm-target", "11")

import java.io.File

val body = File("release-notes.html")
    .readText()
    .trimEnd()
val html = File("docs/template.html")
    .readText()
    .replace("{{ body }}", body)

File("docs/index.html").writeText(html)
