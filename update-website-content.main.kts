#!/usr/bin/env kotlin

@file:JvmName("WebsiteUpdater")
@file:CompilerOptions("-jvm-target", "11")

import java.io.File

val body = File("release-notes.html")
    .readText()
    .trimEnd()

// language=HTML
val html = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>AndroidX latest release notes</title>
</head>
<body>
<h1>Latest update</h1>
$body
</body>
</html>
"""

File("docs/index.html").writeText(html)
