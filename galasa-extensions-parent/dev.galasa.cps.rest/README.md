# CPS over REST adapter

This extension allows a local test to run, while drawing configuration data from the remote CPS
on a Galasa server.

The configuration on the ecosystem is considered 'shared'.

The configuration on the ecosystem is read-only. Set and Delete operations are not implemented.

To configure Galasa to load and use this adapter:

The galasactl tool can be configured to communicate with that CPS (see the latest docs on the galasactl tool.

To do this, assuming `https://myhost/api/bootstrap` can be used to 
communicate with the remote server, add the following to your `bootstrap.properties` file, 
```
// https://myhost/api is the location of the Galasa REST API endpoints.
framework.config.store=galasacps://myhost/api
// Tells the framework to load this extension, so it can register to react when the `galasacps` URL scheme is used.
framework.extra.bundles=dev.galasa.cps.rest