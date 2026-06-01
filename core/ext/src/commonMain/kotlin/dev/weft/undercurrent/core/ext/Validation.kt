package dev.weft.undercurrent.core.ext

private val EMAIL_REGEX = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

const val MAX_DISPLAY_NAME_LENGTH: Int = 40
const val MIN_PASSWORD_LENGTH: Int = 8

fun emailLooksValid(raw: String): Boolean = EMAIL_REGEX.matches(raw.trim())

fun displayNameValid(raw: String): Boolean {
    val trimmed = raw.trim()
    return trimmed.isNotEmpty() && trimmed.length <= MAX_DISPLAY_NAME_LENGTH
}
