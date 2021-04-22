package com.rnett.action

import com.rnett.action.core.fail
import com.rnett.action.core.inputs
import com.rnett.action.core.log
import com.rnett.action.core.runOrFail
import com.rnett.action.exec.exec

fun parseLocation(location: String, version: String?, isSnapshot: Boolean?, latestSnapshot: String, latestRelease: String): String {
    if("\$version" in location){
        fail("\$version used in publish-to, but version is not set.")
    }
    if("\$latest" in location){
        fail("\$latest used in publish-to, but version is not set.")
    }
    return location.replace("\$version", version ?: "")
        .replace("\$latest", if(isSnapshot == true) latestSnapshot else latestRelease)
        .replace("\\,", ",")
        .replace("\\\\", "\\")
}

suspend fun updateDocs(folder: Path, from: Path) {
    folder.mkdir()
    log.info("Copying docs to $folder")
    folder.children.forEach { it.delete(true) }
    from.copyChildren(folder)
}

suspend fun main() = runOrFail {

    val from by inputs
    val branch by inputs

    val authorName = inputs["author-name"]
    val authorEmail = inputs["author-email"]

    val version by inputs.optional

    val (latestSnapshot, latestRelease) = inputs.getOrElse("latests") { "snapshot|release" }.let {
        if('|' !in it || it.count { it == '|' } > 1)
            fail("latests must be two strings seperated by a '|'")
        it.substringBefore('|') to it.substringAfter('|')
    }

    val restore = inputs["restore"].toLowerCase() != "false"

    /**
     * Replaces $version with version, or errors if it isn't set
     */
    /**
     * Replaces $version with version, or errors if it isn't set
     */
    val message by inputs.withDefault { if (version != null) "Docs for \$version" else "Docs update" }

    if ("\$version" in message && version == null)
        fail("'\$version' used in message, but version not set.")

    /*
     Supports:
       "version" -> version
       "version+latest"
       else uses as dir ("." for current)
     */
    val isSnapshot = version?.toLowerCase()?.contains("snapshot")
    val publishTo = inputs.getOrElse("publish-to") { if (version != null) "\$version,\$latest" else "." }
        .split(',').filter { it.isNotBlank() }
        .filterNot {
            when(isSnapshot){
                true -> it.startsWith('!')
                false -> it.startsWith('?')
                else -> false
            }
        }
        .map { parseLocation(it.trimStart('!', '?'), version, isSnapshot, latestSnapshot, latestRelease) }

    val fromPath = Path("../docs-temp/").apply { mkdir() }
    Path(from).moveChildren(fromPath)

    val restoreDir = if (restore) {
        log.info("Saving working directory")
        Path("../restore-temp/").apply { mkdir() }
    } else
        null

    //TODO this can fail silently
    Path.cwd.children.filter { it.name != ".git" }.forEach {
        if (restoreDir != null) {
            it.move(restoreDir)
        } else {
            it.delete(true)
        }
    }

    exec.execCommand("git fetch")
    val existing = exec.execCommandAndCapture("git branch -r").stdout
    val remoteExists = "origin/$branch" in existing

    if (remoteExists) {
        exec.execCommand("git checkout -q $branch")
    } else {
        exec.execCommand("git checkout -q -B $branch")
    }

    publishTo.forEach {
        updateDocs(Path.cwd / it, fromPath)
    }

    exec.execCommand("git add .")

    exec.execCommand("git -c user.name=\'$authorName\' -c user.email=\'$authorEmail\' " +
            "commit -q -m \"${message.replace("\$version", version ?: "")}\"")

    exec.execCommand("git push origin $branch")

    if (restoreDir != null) {
        log.info("Restoring working directory")
        Path.cwd.children.filter { it.name != ".git" }.forEach { it.delete(true) }
        restoreDir.moveChildren(Path.cwd)
        restoreDir.delete(true)
    }
}