package com.tucanoo.davkit.boot

import com.tucanoo.davkit.license.TestLicenseGates

/** Integration-test access to the starter's internal licence state. */
final class TestLicenseStates {

    private TestLicenseStates() {
    }

    static DavKitLicenseState commercial(String licensee) {
        new DavKitLicenseState(TestLicenseGates.commercial(licensee))
    }
}
