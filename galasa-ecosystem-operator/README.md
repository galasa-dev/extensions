# Galasa Ecosystem Operator

This is the **V1ALPHA** version of the ecosystem operator. 

Requirements are a Kubernetes cluster (version 1.16 or higher) and kubectl on the machine deploying the operator. At the current release, the only way to use the operator is to directly deploy the operator using the YAML provided in this repo.

The operator consists of two components, the CustomResourceDefinition which describes the galasa ecosystem, and the the operator deployment. If you are deploying the operator on a cluster which does not have access to the internet, you can build and host the docker image using the DockerFile in `build/DockerFile` and your own docker image registry.

First we need to define the CustomeResourceDefinition that describes to the operator what a Galasa ecosystem. This can be done:
```
kubectl apply -f deploy/crds/galasa.dev_galasaecosystems_crd.yaml
```

Before deploying the operator, you will have to first define a ServiceAccount and a role+roleBinding that the operator can use to perform any work. In the `deploy/` directory you can find the YAML to do this. Once the roles and service accounts are complete you can deploy the operator:

```
kubectl apply -f deploy/operator.yaml
```


The operator and definitions are now installed and are ready to bring up a Galasa ecosystem. There is a sample definition for a Galasa ecosystem provided, and if you do not want to change any of the defaults (You will still need to add a externalHostname to the yaml) you can run: 

```
kubectl apply -f deploy/crds/galasa.dev_v1alpha1_galasaecosystem_cr.yaml
```

This will take a few minutes to install, but the status can found by performing a:

```
kubectl get GalasaEcosystem
```

This will also show the bootstrap endpoint, which you can add to your Eclipse Galasa plugin.

# Editing the operator

This operator was built using the operator-sdk, so you will need to have that installed before editing. 

If any of the CustomResources are edited you will need to run:

```
operator-sdk generate crds
```
This needs to be done BEFORE re-applying the CRD to your cluster

If you change any of the Controller or API code, a similar command needs to be run:

```
operator-sdk generate k8s
```
