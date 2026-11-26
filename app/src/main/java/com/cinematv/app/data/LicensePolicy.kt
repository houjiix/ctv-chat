package com.cinematv.app.data

/**
 * Conservative licence allow-list for direct in-app playback.
 * Unknown/missing licence metadata is never treated as permission.
 */
object LicensePolicy {
    private val allowedPrefixes = listOf(
        "https://creativecommons.org/publicdomain/mark/1.0/",
        "http://creativecommons.org/publicdomain/mark/1.0/",
        "https://creativecommons.org/publicdomain/zero/1.0/",
        "http://creativecommons.org/publicdomain/zero/1.0/",
        "https://creativecommons.org/licenses/by/4.0/",
        "http://creativecommons.org/licenses/by/4.0/",
        "https://creativecommons.org/licenses/by/3.0/",
        "http://creativecommons.org/licenses/by/3.0/",
        "https://creativecommons.org/licenses/by-sa/4.0/",
        "http://creativecommons.org/licenses/by-sa/4.0/",
        "https://creativecommons.org/licenses/by-sa/3.0/",
        "http://creativecommons.org/licenses/by-sa/3.0/"
    )

    fun isAllowed(licenseUrl: String?): Boolean {
        val normalized = licenseUrl?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank()) return false
        return allowedPrefixes.any { normalized.startsWith(it) }
    }

    fun displayName(licenseUrl: String?): String {
        val value = licenseUrl?.trim().orEmpty()
        return when {
            value.contains("publicdomain/mark", ignoreCase = true) -> "Public Domain Mark 1.0"
            value.contains("publicdomain/zero", ignoreCase = true) -> "CC0 1.0"
            value.contains("licenses/by-sa/4.0", ignoreCase = true) -> "CC BY-SA 4.0"
            value.contains("licenses/by-sa/3.0", ignoreCase = true) -> "CC BY-SA 3.0"
            value.contains("licenses/by/4.0", ignoreCase = true) -> "CC BY 4.0"
            value.contains("licenses/by/3.0", ignoreCase = true) -> "CC BY 3.0"
            else -> "Licence not verified"
        }
    }
}
