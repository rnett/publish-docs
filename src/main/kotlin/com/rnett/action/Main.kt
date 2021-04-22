package com.rnett.action

import com.rnett.action.core.fail
import com.rnett.action.core.inputs
import com.rnett.action.core.log
import com.rnett.action.core.runOrFail
import com.rnett.action.exec.exec

sealed class PublishTo {
    data class Version(val version: String, val latest: Boolean) : PublishTo()
    data class Custom(val value: String) : PublishTo()
    object Main : PublishTo()
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
    val _currents = inputs.getOrElse("publish-to") { if (version != null) "version+latest" else "." }

    val publishTo = when (_currents.toLowerCase()) {
        "version+latest" -> PublishTo.Version(
            version ?: fail("Set 'publish-to' to 'version+latest' but didn't specify a version"), true
        )
        "version + latest" -> PublishTo.Version(
            version ?: fail("Set 'publish-to' to 'version + latest' but didn't specify a version"), true
        )
        "version" -> PublishTo.Version(
            version ?: fail("Set 'publish-to' to 'version' but didn't specify a version"),
            false
        )
        else -> PublishTo.Custom(_currents)
    }

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

    when (publishTo) {
        is PublishTo.Version -> {
            updateDocs(Path.cwd / publishTo.version, fromPath)
            if (publishTo.latest) {
                if (publishTo.version.toLowerCase().endsWith("snapshot")) {
                    updateDocs(Path.cwd / "snapshot", fromPath)
                } else {
                    updateDocs(Path.cwd / "release", fromPath)
                }
            }
        }
        is PublishTo.Custom -> updateDocs(Path.cwd / publishTo.value, fromPath)
        PublishTo.Main -> updateDocs(Path.cwd, fromPath)
    }

    exec.execCommand("git add .")

    exec.execCommandAndCapture("git -c user.name=\'$authorName\' -c user.email=\'$authorEmail\' " +
            "commit -q -m \"${message.replace("\$version", version!!)}\"").apply {
        if (returnCode != 0) {
            println("Commit failed:\n$stderr")
        }
    }

    exec.execCommandAndCapture("git push origin $branch").apply {
        if (returnCode != 0) {
            println("Push failed:\n$stderr")
        }
    }

    if (restoreDir != null) {
        log.info("Restoring working directory")
        Path.cwd.children.filter { it.name != ".git" }.forEach { it.delete(true) }
        restoreDir.moveChildren(Path.cwd)
        restoreDir.delete(true)
    }
}