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
    from.copy(folder)
}

suspend fun main() = runOrFail{

    val from by inputs
    val branch by inputs

    val version by inputs.optional

    val restore = inputs.getRequired("restore").toLowerCase() != "false"

    /**
     * Replaces $version with version, or errors if it isn't set
     */
    /**
     * Replaces $version with version, or errors if it isn't set
     */
    val message by inputs.withDefault { if(version != null) "Docs for \$version" else "Docs update" }

    if("\$version" in message && version == null)
        fail("'\$version' used in message, but version not set.")

    /*
     Supports:
       "version" -> version
       "version+latest"
       else uses as dir ("." for current)
     */
    val _currents = inputs.getOrElse("publish-to"){ if(version != null) "version+latest" else "." }

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

    Path.cwd.children.filter { it.name != ".git" }.forEach {
        if (restoreDir != null) {
            it.move(restoreDir)
        } else {
            it.delete(true)
        }
    }

    exec.execCommand("git checkout -q -B $branch")

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

    exec.execCommand("git add -A")
    exec.execCommand("git commit -q -m \"${message.replace("\$version", version!!)}\"")

    exec.execCommand("git push --set-upstream")


    if(restoreDir != null){
        log.info("Restoring working directory")
        Path.cwd.children.filter { it.name != ".git" }.forEach { it.delete(true) }
        restoreDir.moveChildren(Path.cwd)
        restoreDir.delete(true)
    }
}