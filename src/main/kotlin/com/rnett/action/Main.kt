package com.rnett.action

import com.rnett.action.core.fail
import com.rnett.action.core.inputs
import com.rnett.action.core.log
import com.rnett.action.core.runOrFail
import com.rnett.action.exec.exec

fun parseLocation(location: String, version: String?, isSnapshot: Boolean?, latestSnapshot: String, latestRelease: String): String {
    if("\$version" in location && version == null){
        fail("\$version used in publish-to, but version is not set.")
    }
    if("\$latest" in location && version == null){
        fail("\$latest used in publish-to, but version is not set.")
    }
    return location.replace("\$version", version ?: "")
        .replace("\$latest", if(isSnapshot == true) latestSnapshot else latestRelease)
        .replace("\\,", ",")
        .replace("\\\\", "\\")
}

suspend fun updateDocs(folder: Path, from: Path, delete: Boolean) {
    folder.mkdir()
    log.info("Copying docs to $folder")
    if(delete)
        folder.children.forEach { it.delete(true) }
    from.copyChildren(folder)
}

suspend fun main() = runOrFail {

    val from by inputs.optional
    val fromFile by inputs.optional

    if(from.isNullOrBlank() && fromFile.isNullOrBlank()){
        fail("Should only specify one of 'from' and 'from-file'.")
    }
    if(from.isNullOrBlank() && fromFile.isNullOrBlank()){
        fail("Must specify exactly one of: 'from', 'from-file'.")
    }

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

    val fromPath = Path("../docs-temp/").apply {
        mkdir()
        children.forEach { it.delete(true) }
    }
    if(!from.isNullOrBlank())
        Path(from!!).moveChildren(fromPath)
    else
        Path(fromFile!!).move(fromPath)

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

    if(restoreDir != null){
        (Path.cwd / ".git").copy(restoreDir)
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
        updateDocs(Path.cwd / it, fromPath, !from.isNullOrBlank())
    }

    exec.execCommand("git add .")

    val changed = exec.execCommand("git diff-index --quiet HEAD", ignoreReturnCode = true) != 0

    if(changed) {
        exec.execCommand(
            "git -c user.name=\'$authorName\' -c user.email=\'$authorEmail\' " +
                    "commit -q -m \"${message.replace("\$version", version ?: "")}\""
        )

        exec.execCommand("git push origin $branch")
    } else{
        println("No changes")
    }

    if (restoreDir != null) {
        log.info("Restoring working directory")
        Path.cwd.children.forEach { it.delete(true) }
        restoreDir.moveChildren(Path.cwd)
        restoreDir.delete(true)
    }
    fromPath.delete(true)
}