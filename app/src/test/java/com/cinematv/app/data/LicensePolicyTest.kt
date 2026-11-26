package com.cinematv.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensePolicyTest {
    @Test
    fun acceptsPublicDomainAndPermissiveAttributionLicences() {
        assertTrue(LicensePolicy.isAllowed("https://creativecommons.org/publicdomain/mark/1.0/"))
        assertTrue(LicensePolicy.isAllowed("https://creativecommons.org/licenses/by/4.0/"))
        assertTrue(LicensePolicy.isAllowed("https://creativecommons.org/licenses/by-sa/3.0/"))
    }

    @Test
    fun rejectsMissingUnknownAndRestrictiveLicences() {
        assertFalse(LicensePolicy.isAllowed(null))
        assertFalse(LicensePolicy.isAllowed(""))
        assertFalse(LicensePolicy.isAllowed("https://creativecommons.org/licenses/by-nc-nd/4.0/"))
        assertFalse(LicensePolicy.isAllowed("https://example.com/license"))
    }

    @Test
    fun licenceDisplayNamesAreHumanReadable() {
        assertEquals("CC BY 4.0", LicensePolicy.displayName("https://creativecommons.org/licenses/by/4.0/"))
        assertEquals("Public Domain Mark 1.0", LicensePolicy.displayName("https://creativecommons.org/publicdomain/mark/1.0/"))
        assertEquals("Licence not verified", LicensePolicy.displayName(null))
    }
}
