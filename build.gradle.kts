buildscript {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.6.0-alpha08")
        classpath("com.google.gms:google-services:4.3.15")
    }
}

plugins {
    id("com.android.application") version "8.8.2" apply false
    id("com.android.library") version "8.8.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
}