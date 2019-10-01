# Framework Test Rig - Apache Karaf

This project provides a set of commands to test the eJAT Framework.  Commands that are available are:-

* `ejat:init -b /home/mikebyls/.ejat/bootstrap.properties -o /home/mikebyls/.ejat/overrides.properties`   (-b -o both optional)
* `ejat:namespace nnnnnnnn`
* `cps:get -n nnnnnnn suffix prefix infix1 infix2`  (-n optional if namespace set)

## Setup Apache Karaf
To setup Apache Karaf to be able to run the Framework and test rig, perform the following actions:-
1. Download and extract the Apache Karaf runtime from [here](https://karaf.apache.org/download.html).
1. Amend `etc/org.ops4j.pax.url.mvn.cfg` file 
1. locate the `org.ops4j.pax.url.mvn.settings` property.
1. Set to your settings.xml file, either `/home/mikebyls/.m2/settings.xml` or a workspace copy.
1. Uncomment the property
1. Locate the `org.ops4j.pax.url.mvn.repositories` property
1. add `https://eu.artifactory.swg-devops.com:443/artifactory/cicsts-ejatv3-maven-local@id=ejat.repo@snapshots, \` as the first repo.
1. Amend `etc/config.properties`
1. Locate the `obr.repository.url` property
1. Set to `obr.repository.url = mvn:ejat-common/ejat-uber-obr/0.2.0-SNAPSHOT/obr`
1. Amend `etc/org.apache.karaf.features.cfg`
1. Locate the `featuresRepositories` property
1. Add `mvn:ejat-common/ejat-uber-karaffeature/0.2.0-SNAPSHOT/xml, \` and `mvn:ejat/ejat-devtools-karaffeature/0.2.0-SNAPSHOT/xml` to the end of the list
1. Locate the `featuresBoot` property
1. Add `obr/4.2.3, \` and `scr/4.2.3, \` and `maven/4.2.3` to the list
1. Add logging improvements for the test rig by amending file `org.ops4j.pax.logging.cfg` after the `# Loggers configuration` line with:-
   * `log4j2.logger.ejat.name = io.ejat`
   * `log4j2.logger.ejat.level = TRACE`
   * `log4j2.logger.ejat.appenderRef.Console.ref = Console`
1. Test the setup by:-
1. Running command `bin/karaf clean`
1. within Karaf, type `feature:install ejat ejat-devtools`  should not print any error message.
1. test the test rig by running command `ejat:init`
1. set a namespace `ejat:namespace zos`
1. get a property from the CPS `cps:get cluster MA`

## Example Karaf commands
* `maven:summary`
* `bundle:list -s`
* `bundle:headers io.ejat.framework`
* `scr:list`
* `obr:url-list`
* `obr:list`
* `feature:list`
