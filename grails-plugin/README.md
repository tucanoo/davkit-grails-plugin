# grails-plugin

Thin Grails 7/8 wrapper over `davkit-spring-boot`. Bean
registration comes from the starter's auto-configuration (empty `doWithSpring`); this module adds
the `davkit:editLink` taglib and the `GormDavResourceProvider` base class. The Java 17 artifact is
built and tested against Grails 7.2.2, then validated in a real Grails 8 host before release.

The plugin activates the starter by default and derives signed-URL authentication from the
validated licence key. Set `davkit.enabled=false` to disable its servlet, filters and firewall.
