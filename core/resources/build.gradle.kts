plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.undercurrent.kmp.library.compose)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "dev.weft.undercurrent.core.resources"
    generateResClass = always
}

android {
    namespace = "dev.weft.undercurrent.core.resources"
}
