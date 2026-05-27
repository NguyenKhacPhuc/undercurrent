package dev.weft.undercurrent.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Access to the root project's `libs.versions.toml` catalog from inside
 * convention plugins. Used by every plugin to look up library
 * coordinates + plugin IDs by alias.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Names prefixed with `android` to avoid colliding with the legacy
// `Project.compileSdkVersion: String?` accessor AGP installs onto
// every project — that one returns a String and is the wrong type
// for our int-based config blocks.
internal val Project.androidCompileSdk: Int
    get() = libs.findVersion("compileSdk").get().toString().toInt()

internal val Project.androidMinSdk: Int
    get() = libs.findVersion("minSdk").get().toString().toInt()

internal val Project.androidTargetSdk: Int
    get() = libs.findVersion("targetSdk").get().toString().toInt()
