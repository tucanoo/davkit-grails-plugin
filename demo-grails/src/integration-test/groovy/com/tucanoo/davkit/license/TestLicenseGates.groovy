package com.tucanoo.davkit.license

import java.time.Clock
import java.time.LocalDate

/** Integration-test licensing for the unpublished demo application. */
final class TestLicenseGates {

    private TestLicenseGates() {
    }

    static LicenseGate commercial(String licensee) {
        LicenseGate.of(new License(licensee, License.Type.COMMERCIAL,
                LocalDate.of(2026, 1, 1), null, null, 1, []),
                null, Clock.systemUTC())
    }
}
