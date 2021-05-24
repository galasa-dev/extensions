# ALPHA - PROTOTYPE - Docker Operator for creating Galasa ecosystems in Docker

## What is an Galasa ecosystem

A Galasa ecosystem contains all the servers and monitors required to run Galasa tests in an automated environment or pipeline.  The ecosystem contains the following servers:-

* **etcd** - Contains the Configuration Property Store (CPS), the Dynamic Status Store (DSS) and the Credentials Store (CREDs).  The CPS, DSS are for use with by all users, the CREDs are for automation runs only
* **couchdb** - Contains the Result Archive Store (RAS) which contains the full record of an automated run
* **API** - Is the Galasa API server which includes the bootstrap
* **RESMAN** - The Resource Manager service, handles the cleaning up and management of resources in Galasa
* **ENGINE** - The Engine Controller is responsible for spinning up docker containers to execute individual Galasa automation runs.
* **METRICS** - A metrics server to indicate the health of the ecosystem to a Prometheus server

The following are not required by the ecosystem but are deployed by the Docker Operator to help understand how everything works:-
* **NEXUS** - A Nexus server to provide an easy entry for deploying Maven artifacts to the ecosystem.
* **JENKINS** - A demonstration Jenkins server to show how to run Galasa tests in a pipeline
* **SIMPLATFORM** - Provide an instance of SimBank so that IVTs and demonstration pipelines can be run.

The following services will be included in future updates to the Docker Operator:-
* **ELASTIC** - Provides an Elastic search instance to record the results of automated test runs
* **KABANA** - A dashboard for the Elastic search database
* **PROMETHEUS** - A prometheus server to record the health metrics of the ecosystem
* **GRAFANA** - A dashboard for the prometheus metrics.

## Notes

To date, the Docker Operator only supports the amd64 platform, a s390x (zLinux) implementation will follow soon, but this requires a little more work and testing.

## Requirements

These instructions assume a basic understanding of how Docker works.

To be able to deploy a Galasa ecosystem using the Docker Operator, you will need a Docker Engine, preferably on a linux server, but can be run on a laptop.  If using a Mac or Windows OS, you will need Docker Desktop running a "socat" container.

The ecosystem requires a minimum of 4GB of memory to run everything, although 4GB is very tight and will only allow a maximum of 2 parallel running tests.

We are not aware of a minimum version of Docker Engine the Docker Operator requires.

If the server/workstation has a firewall running, you may need to open the following ports:-

* 2379 - etcd
* 5984 - couchdb
* 8080 - API
* 8081 - Nexus
* 8082 - Jenkins

## Installing a Galasa ecosystem in a Docker Engine

Due to the way the various servers communicate, the ecosystem will need to know the hostname or ip address of the server/workstation the Docker Engine is running on.  Due to the way Docker works, you will not be able to use 127.0.0.1 or localhost, will need to be the actual workstation/server hostname or ip address. This will be provided in a config.yaml.

Also, as the Docker Operator is being distributed from 0.10.0-SNAPSHOT, an unreleased version of Galasa, certain versions of the Galasa ecosystem servers will need to be provided in the config.yaml.  Please copy the following YAML and create a config.yaml file on your server/workstation and note the full path of the file:-

```
hostname: {hostname}
galasaRegistry: docker.galasa.dev
version: 0.14.0
engineController:
  controllerVersion: 0.14.0
  engineVersion: 0.14.0
```

Change the {hostname} to be your hostname.  Note the 2 spaces on the last 2 lines,  they are important in YAML.

Please also check the version numbers. 0.14.0 was the official release at the time of updating these docs.

To deploy the Galasa ecosystem, issue the following Docker command:-

```
docker run -it -v /var/run/docker.sock:/var/run/docker.sock -v {path}/config.yaml:/config.yaml docker.galasa.dev/galasa-docker-operator-amd64:0.9.0
```

where {path} is the full pathname to the directory containing your config.yaml.

When the command is complete all 9 docker containers should be running, you can use the command `docker ps` to list the active containers.

## Testing the ecosystem

The Jenkins server will have a SimBank_IVT build job that will run a Jenkins pipeline to request the SimBankIVT test run in the Galasa ecosystem.   Go to http://{hostname}:8082, use the username of `admin` and password of `galasaadmin` (would be wise if you changed the password at some point).
Run the SimBankIVT job and follow it's progress in the job console.   You can also issue the docker command `docker ps -a` to see the run container being created.  The run should finish and the jenkins job should report that the test passed.

To view the output of the automated run, change the Eclipse preferences for Galasa, the bootstrap preference should be set to `http://{hostname}:8080/bootstrap`.  After applying and closing the preferences,  select, on the Eclipse menu, the Galasa->Initialise Galasa Framework.  You should see the Galasa console reporting that all is ok.  On the Eclipse toolbar is the Galasa icon, click that.   Two new Galasa views should open, Galasa Results and Galasa Runs.  Depended on how quickly you actioned the Eclipse side of things, you may still see the run from Jenkins in Galasa Runs.  You can always run the Jenkins job again to see the new run on the view.

The Galasa results view will contain two RASs, your local one and the automation one.  On the Automation branch, expand "All runs" then "Today's runs".  This should present the automation run from Jenkins.  If you double-click the run name, it will open the results window, which you can use to explore the test result.

You can also submit automation tests from within Eclipse instead of Jenkins, on the Eclipse menu there is a Galasa->Submit tests to automation option,  select that and you choose the runs you want to submit.

## Reporting problems

This version of the code is very much a prototype and we are trying to discover any problems that may exist in the many difference configurations that exist in the world.

If you have a problem with the Docker Operator or any of the ecosystem servers, then please raise an issue at https://github.com/galasa-dev/projectmanagement/issues .

## Production use of the Ecosystem

The Galasa ecosystem has been designed for scale to allow a pipeline to run 100s, maybe even 1000s, of automated tests in parallel.  However, the ecosystem the Docker Operator creates will not be able to manage high levels of parallel runs.  The Docker Operator is there to provide a demonstration ecosystem to allow people to understand what is needed and how it all connects together.  For long term production usage, the Galasa team recommends running Galasa in a Kuberenetes environment.   However, if that is not possible, you can still achieve scale using multiple Docker Engines, but this will require manual setup at this time.  If there is demand, then the Docker Operator could be extended to support multiple Docker Engines and setting up etcd/couchdb clusters, if you would like this, please raise an issue.
