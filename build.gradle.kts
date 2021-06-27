import com.rnett.action.generateAutoBuildWorkflow
import com.rnett.action.githubAction

plugins {
    kotlin("js") version "1.5.20"
    id("com.github.rnett.ktjs-github-action") version "1.4.0-SNAPSHOT"
}

group = "com.github.rnett.github-actions"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    jcenter()
}

dependencies {
    implementation("com.github.rnett.ktjs-github-action:kotlin-js-action:1.4.0-SNAPSHOT")
}

generateAutoBuildWorkflow()

kotlin {
    js(IR) {
        githubAction()
    }
}