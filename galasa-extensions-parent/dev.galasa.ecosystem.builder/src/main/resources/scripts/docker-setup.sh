#!/bin/bash

source docker/config.properties

echo "Pulling etcd"
docker pull quay.io/coreos/etcd:v3.2.25
           
echo "Pulling couchdb"
docker pull couchdb:2

#echo "Pulling couchdb init"
#docker pull galasa/galasa-ras-couchdb-init-amd64:LATEST

echo "Pulling api master server"
docker pull docker.galasa.dev/galasa-master-api-amd64:0.5.0-SNAPSHOT

echo "Pulling boot"
docker pull docker.galasa.dev/galasa-boot-embedded-amd64:0.5.0

echo "Pulling resources"
docker pull docker.galasa.dev/galasa-resources-amd64:0.5.1

echo "Pulling complete"

echo "Setting up network"

docker network create \
               --driver=bridge \
               --subnet=$subnet \
               --gateway=$gateway \
               $networkName
               
echo "Network created" 

echo "Creating docker volumes"

docker volume create $cpsMountSource
docker volume create $rasMountSource
docker volume create $apiMountSource

echo "Volumes created"

echo "Setting up API container"

docker run --name $apiContainerName \
           --network $networkName \
           --restart $apiResPol \
           --detach \
           -v $(pwd)/docker/$apiVolName:/galasa/etc/ \
           --mount source=$apiMountSource,target=$apiMountTarget \
           --publish $apiPort \
           --publish 127.0.0.1:8101:8101 \
           docker.galasa.dev/galasa-master-api-amd64:0.5.0-SNAPSHOT \
           /galasa/bin/karaf server

echo "API container set up complete"

echo "Setting up Controller container"

docker run --name $controllerContainerName \
           --network $networkName \
           --restart $controllerResPol \
           --detach \
           -v $(pwd)/docker/$controllerVolume:/controller.properties \
           -e CONFIG=file:/$controllerVolume \
           docker.galasa.dev/galasa-boot-embedded-amd64:0.5.0 \
           java -jar boot.jar --obr file:galasa.obr --dockercontroller --bootstrap http://galasa-api:8181/bootstrap

echo "Controller container set up complete"

echo "Setting up ETCD container"

docker run --name $cpsContainerName \
           --network $networkName \
           --restart $cpsResPol \
           --detach \
           --mount source=$cpsMountSource,target=$cpsMountTarget \
           --publish $cpsPort \
           quay.io/coreos/etcd:v3.2.25 \
           etcd --data-dir /var/run/etcd/default.etcd --initial-cluster default=http://127.0.0.1:2380 --listen-client-urls http://0.0.0.0:2379 --listen-peer-urls http://0.0.0.0:2380 --initial-advertise-peer-urls http://127.0.0.1:2380 --advertise-client-urls http://127.0.0.1:2379

echo "ETCD container set up complete"

echo "Setting up RAS CouchDB container"

docker run --name $rasContainerName \
           --network $networkName \
           --restart $rasResPol \
           --detach \
           --env-file docker/$rasEnvFile \
           --mount source=$rasMountSource,target=$rasMountTarget \
           --publish $rasPort \
           couchdb:2

echo "RAS CouchDB container set up complete"

         
echo "Setting up Resource Monitor container"

docker run --name $resourceMonitorContainerName \
           --network $networkName \
           --restart $resourceMonitorResolutionPolicy \
           --detach \
           docker.galasa.dev/galasa-boot-embedded-amd64:0.5.0 \
           java -jar boot.jar --obr file:galasa.obr --resourcemanagement --bootstrap http://galasa-api:8181/bootstrap
           
echo "Resource Monitor container set up complete"

echo "Setting up Resource container"

docker run --name $resourceContainerName \
           --network $networkName \
           --restart $resourceRestartPolicy \
           --detach         \
           --publish $resourcePort \
           docker.galasa.dev/galasa-resources-amd64:0.5.1
           
echo "Resource container set up complete"

echo "Setting up SimBank container"

docker run --name $simbankContainerName \
           --network $networkName \
           --restart $simbankResPol \
           --detach \
           --publish $simbankPort \
           --publish 2023:2023 \
           docker.galasa.dev/galasa-boot-embedded-amd64:0.5.0 \
           java -jar simframe.jar
           
           
echo "Simbank container set up complete"

echo "Docker EcoSystem set up complete"




