#!/bin/bash

subnet=$1
gateway=$2
apiName=$3
netName=$4
apiResPol=$5
apiVol=$6
apiVolSrc=$7
apiVolTrg=$8
apiPort=$9
contName=$10
contResPol=$11
contVol=$12
cpsName=$13
cpsResPol=$14
cpsVolSrc=$15
cpsVolTrg=$16
cpsPort=$17
cdbiName=$18
cdbiVolSrc=$19
cdbiVolTrg=$20
cdbName=$21
cdbResPol=$22
cdbEnv=$23
cdbVolSrc=$24
cdbVolTrg=$25
cdbPort=$26
resmonName=$27
resmonResPol=$28
resName=$29
resResPol=$30
resPort=$31
sbName=$32
sbResPol=$33
sbPort=$34


echo "Updating properties"

sed -i "/subnet=/ s/=.*/=$subnet/" docker/config.properties

sed -i "/gateway=/ s/=.*/=$gateway/" docker/config.properties

sed -i "/networkName=/ s/=.*/=$netName/" docker/config.properties

sed -i "/apiContainerName=/ s/=.*/=$apiName/" docker/config.properties

sed -i "/apiResPol=/ s/=.*/=$apiResPol/" docker/config.properties

sed -i "/apiVolName=/ s/=.*/=$apiVol/" docker/config.properties

sed -i "/apiMountSource=/ s/=.*/=$apiVolSrc/" docker/config.properties

sed -i "/apiMountTarget=/ s/=.*/=$apiVolTrg/" docker/config.properties

sed -i "/apiPort=/ s/=.*/=$apiPort/" docker/config.properties

sed -i "/controllerContainerName=/ s/=.*/=$contName/" docker/config.properties

sed -i "/controllerResPol=/ s/=.*/=$contResPol/" docker/config.properties

sed -i "/controllerVolume=/ s/=.*/=$contVol/" docker/config.properties

sed -i "/cpsContainerName=/ s/=.*/=$cpsName/" docker/config.properties

sed -i "/cpsResPol=/ s/=.*/=$cpsResPol/" docker/config.properties

sed -i "/cpsMountSource=/ s/=.*/=$cpsVolSrc/" docker/config.properties

sed -i "/cpsMountTarget=/ s/=.*/=$cpsVolTrg/" docker/config.properties

sed -i "/cpsPort=/ s/=.*/=$cpsPort/" docker/config.properties

sed -i "/rasContainerName=/ s/=.*/=$cdbName/" docker/config.properties

sed -i "/rasResPol=/ s/=.*/=$cdbResPol/" docker/config.properties

sed -i "/rasEnvFile=/ s/=.*/=$cdbEnvfile/" docker/config.properties

sed -i "/rasMountSource=/ s/=.*/=$cdbVolSrc/" docker/config.properties

sed -i "/rasMountTarget=/ s/=.*/=$cdbVolTrg/" docker/config.properties

sed -i "/resourceMonitorContainerName=/ s/=.*/=$resmonName/" docker/config.properties

sed -i "/resourceMonitorResolutionPolicy=/ s/=.*/=$resmonResPol/" docker/config.properties

sed -i "/resourceContainerName=/ s/=.*/=$resName/" docker/config.properties

sed -i "/resourceRestartPolicy=/ s/=.*/=$resResPol/" docker/config.properties

sed -i "/resourcePort=/ s/=.*/=$resPort/" docker/config.properties

sed -i "/simbankContainerName=/ s/=.*/=$sbName/" docker/config.properties

sed -i "/simbankResPol=/ s/=.*/=$sbResPol/" docker/config.properties

sed -i "/simbankPort=/ s/=.*/=$sbPort/" docker/config.properties


echo "Properties updated"



