import com.rnett.action.githubAction
import com.rnett.action.useAutoBuildWorkflow

plugins {
    kotlin("js") version "1.4.30"
    id("com.github.rnett.ktjs-github-action") version "0.1.4-SNAPSHOT"
}

group = "com.github.rnett.github-actions"
version = "0.1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    testImplementation(kotlin("test-js"))


    implementation("com.github.rnett.ktjs-github-action:kotlin-js-action:0.1.4-SNAPSHOT")
}

useAutoBuildWorkflow()

kotlin {
    js(IR) {
        githubAction()
    }
}