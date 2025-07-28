#!/usr/bin/env kotlin

@file:JvmName("WebsiteUpdater")
@file:CompilerOptions("-jvm-target", "17")

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

val body = Path("release-notes.html")
    .readText()
    .trimEnd()
val html = Path("docs/template.html")
    .readText()
    .replace("{{ body }}", body)

Path("docs/index.html").writeText(html)
