import com.tucanoo.davkit.demo.GormDocumentProvider
import grails.util.Environment

// application.yml explicitly enables the starter; its auto-configuration then collects every
// DavResourceProvider bean into the registry.
beans = {
    gormDocumentProvider(GormDocumentProvider)

    if (Environment.current == Environment.TEST) {
        // Test support is loaded by name because it exists only in the integration-test
        // source set; normal demo builds have no unsigned licence construction path.
        Class testLicenseStates = Class.forName('com.tucanoo.davkit.boot.TestLicenseStates')
        testDavKitLicenseState(testLicenseStates, 'DavKit Grails demo tests') { bean ->
            bean.factoryMethod = 'commercial'
            bean.primary = true
        }
    }
}
