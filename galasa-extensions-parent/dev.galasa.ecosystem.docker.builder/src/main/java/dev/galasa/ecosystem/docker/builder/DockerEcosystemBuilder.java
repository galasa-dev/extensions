package dev.galasa.ecosystem.docker.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Scanner;

import org.yaml.snakeyaml.Yaml;

public class DockerEcosystemBuilder {

	public static void main(String[] args) throws FileNotFoundException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Welcome to the Galasa Ecosystem manager for Docker \n");
		System.out.println("If you have not run this program before, default values for the configuration have been supplied \n");
		System.out.println("Here is your current configuration for the Ecosystem: \n");
		String configLocation = "classes/";
		
		try {
			BufferedReader dis = new BufferedReader(new FileReader(configLocation + "config.yml"));
			String line;
			while((line = dis.readLine()) != null)
			{
			    System.out.println(line);
			}
			dis.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		
		System.out.println("\n" + "Would you like to alter any of these properties? (Y or N, Default Y)");
		String reuse = scanner.nextLine();
		while (!(reuse.toUpperCase().equals("Y") || reuse.toUpperCase().equals("N") || reuse.isEmpty())) {	
			System.out.println("Please enter Y or N");
			reuse = scanner.nextLine();
		}
		if (reuse.toUpperCase().equals("Y") || reuse.isEmpty()) {			
			alterProps();
		
		}
		else if (reuse.toUpperCase().equals("N")){
			System.out.println("Generating script..." + "\n");
			try {
				generateScript();
			} catch (FileNotFoundException e) {
				System.out.println("Error");
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				System.out.println("Error");
				e.printStackTrace();
			}
		}
	    scanner.close();
		
	}
	
	static void alterProps() throws FileNotFoundException {
		System.out.println("\n" + "Which of these properties would you like to change? Please enter the property name(s) from above you wish to change all one line with spaces between each");
		System.out.println("\n" + "Enter exit to return to the main screen");
		Scanner scanner = new Scanner(System.in);
		String propList = scanner.nextLine();
		while (propList.isEmpty()) {
			System.out.println("Please enter which properties you would like to edit" + "\n");
			alterProps();
		}
		if (propList.toUpperCase().contains("EXIT")) {
			System.out.println("Returning you to the main screen...");
			main(null);
		}
		if (propList.toUpperCase().contains("HOSTNAME")) {
			System.out.println("What would you like to update the hostname the docker container will use to?" + "\n");
			String hostname = scanner.nextLine();
			System.out.println("Hostname will be changed to " + hostname + "\n");
			String key = "hostname";
			String value = hostname;
			updateyml(key, value);
			
		}
		if (propList.toUpperCase().contains("NETWORKNAME")) {
			System.out.println("What would you like to update the network name to?" + "\n");
			String netName = scanner.nextLine();
			System.out.println("Network name will be changed to " + netName + "\n");
			String key = "networkName";
			String value = netName;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("RESTARTPOLICY")) {
			System.out.println("What would you like to set the restart policy as - no, on-failure, always or unless-stopped (default always):");
			String ResPol = scanner.nextLine();
			  while (!(ResPol.toUpperCase().equals("NO") || ResPol.toUpperCase().equals("ON-FAILURE") || ResPol.toUpperCase().equals("ALWAYS") || ResPol.toUpperCase().equals("UNLESS-STOPPED") || ResPol.isEmpty())) {	
			      System.out.println("I'm sorry but that isn't a valid input, please ensure you have entered a restart policy listed above");
			      ResPol = scanner.nextLine();
				}
			  if (ResPol.isEmpty() || ResPol.toUpperCase().equals("ALWAYS")) {
				  ResPol = "always";
			  }
			System.out.println("Restart Policy set to: " + ResPol);
			String key = "restartPolicy";
			String value = ResPol;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("APICONTAINERNAME")) {
			System.out.println("What would you like the API container name set to: " + "\n");
			String apiName = scanner.nextLine();
			System.out.println("API container name set to: " + apiName);
			String key = "apiContainerName";
			String value = apiName;
			updateyml(key, value);
			
		}
		if (propList.toUpperCase().contains("APIVOLNAME")) {
			System.out.println("Please enter the name of the volume you would like to create for the API container: " + "\n");
			String apiVol = scanner.nextLine();
			System.out.println("Api Volume name set to: " + apiVol);
			String key = "apiVolName";
			String value = apiVol;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("APIMOUNTTARGET")) {
			System.out.println("Please enter the directory you would like the volume mounted to: " + "\n");
			String apiMount = scanner.nextLine();
			System.out.println("Api Mount point set to: " + apiMount);
			String key = "apiMountTarget";
			String value = apiMount;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("APIPORT")) {
			System.out.println("what port would you like the API server to listen on? (Default 8181)");
			String apiPort = null;
			 while (true) {
				    apiPort = scanner.nextLine();
				    if (apiPort.isEmpty() || apiPort.equals("8181")) {
				        apiPort = "8181";
				        break;
				    } else {
				        try {
				        	Integer.valueOf(apiPort);
				            break;
				        } catch (NumberFormatException ex) {
				            System.out.println("Please enter port as an integer");
				        }
				    }
				}
			 System.out.println("Api Port set to: " + apiPort);
				String key = "apiPort";
				String value = apiPort;
				updateyml(key, value);
		}
		if (propList.toUpperCase().contains("CONTROLLERCONTAINERNAME")) {
			System.out.println("Please enter the name you would like for the controller container: " + "\n");
			String contName = scanner.nextLine();
			System.out.println("Controller container name set to: " + contName);
			String key = "controllerContainerName";
			String value = contName;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("CPSCONTAINERNAME")) {
			System.out.println("Please enter the name you would like for the cps container: " + "\n");
			String cpsName = scanner.nextLine();
			System.out.println("CPS Container name set to: " + cpsName);
			String key = "cpsContainerName";
			String value = cpsName;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("CPSVOLNAME")) {
			System.out.println("Please enter the name of the volume you would like to create for the CPS container: " + "\n");
			String cpsVol = scanner.nextLine();
			System.out.println("CPS Volume name set to: " + cpsVol);
			String key = "cpsVolName";
			String value = cpsVol;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("CPSMOUNTTARGET")) {
			System.out.println("Please enter the directory location to mount the CPS volume: " + "\n");
			String cpsMount = scanner.nextLine();
			System.out.println("CPS Mount target name set to: " + cpsMount);
			String key = "cpsMountTarget";
			String value = cpsMount;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("CPSPORT")) {
			System.out.println("what port would you like the CPS server to listen on? (Default 2379)");
			String cpsPort = null;
			 while (true) {
				    cpsPort = scanner.nextLine();
				    if (cpsPort.isEmpty() || cpsPort.equals("2379")) {
				        cpsPort = "2379";
				        break;
				    } else {
				        try {
				        	Integer.valueOf(cpsPort);
				            break;
				        } catch (NumberFormatException ex) {
				            System.out.println("Please enter port as an integer");
				        }
				    }
				}
			 System.out.println("CPS Port set to: " + cpsPort);
				String key = "cpsPort";
				String value = cpsPort;
				updateyml(key, value);
		}
		if (propList.toUpperCase().contains("RASCONTAINERNAME")) {
			System.out.println("Please enter the name you would like for the RAS container: " + "\n");
			String rasName = scanner.nextLine();
			System.out.println("RAS container name set to: " + rasName);
			String key = "rasContainerName";
			String value = rasName;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("RASVOLNAME")) {
			System.out.println("Please enter name of the volume you would like to create for the RAS container: " + "\n");
			String rasVol = scanner.nextLine();
			System.out.println("RAS volume name set to: " + rasVol);
			String key = "rasVolName";
			String value = rasVol;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("RASMOUNTTARGET")) {
			System.out.println("Please enter the directory location to mount the RAS volume: " + "\n");
			String rasMount = scanner.nextLine();
			System.out.println("RAS Mount target name set to: " + rasMount);
			String key = "rasMountTarget";
			String value = rasMount;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("RASPORT")) {
			System.out.println("what port would you like the RAS server to listen on? (Default 5984)");
			String rasPort = null;
			 while (true) {
				    rasPort = scanner.nextLine();
				    if (rasPort.isEmpty() || rasPort.equals("5984")) {
				    	rasPort = "5984";
				        break;
				    } else {
				        try {
				        	Integer.valueOf(rasPort);
				            break;
				        } catch (NumberFormatException ex) {
				            System.out.println("Please enter port as an integer");
				        }
				    }
				}
			 System.out.println("RAS Port set to: " + rasPort);
			 String key = "rasPort";
			 String value = rasPort;
			 updateyml(key, value);
		}
		if (propList.toUpperCase().contains("RESOURCEMONITORCONTAINERNAME")) {
			System.out.println("Please enter the name you would like for the Resource Monitor container: " + "\n");
			String resmonName = scanner.nextLine();
			System.out.println("Resource monitor container name to be set to: " + resmonName);
			String key = "resourceMonitorContainerName";
			String value = resmonName;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("RESOURCECONTAINERNAME")) {
			System.out.println("Please enter the name you would like for the Resource container" + "\n");
			String resName = scanner.nextLine();
			System.out.println("Resource container name will be changed to " + resName + "\n");
			String key = "resourceContainerName";
			String value = resName;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("RESOURCEPORT")) {
			System.out.println("what port would you like the RAS server to listen on? (Default 8080)");
			String resPort = null;
			 while (true) {
				    resPort = scanner.nextLine();
				    if (resPort.isEmpty() || resPort.equals("8080")) {
				    	resPort = "8080";
				        break;
				    } else {
				        try {
				        	Integer.valueOf(resPort);
				            break;
				        } catch (NumberFormatException ex) {
				            System.out.println("Please enter port as an integer");
				        }
				    }
				}
			 System.out.println("Resource Port set to: " + resPort);
			 String key = "resourcePort";
			 String value = resPort;
			 updateyml(key, value);
		}
		if (propList.toUpperCase().contains("SIMBANKCONTAINERNAME")) {
			System.out.println("Please enter the name you would like for the SimBank container" + "\n");
			String sbName = scanner.nextLine();
			System.out.println("Hostname will be changed to " + sbName + "\n");
			String key = "simbankContainerName";
			String value = sbName;
			updateyml(key, value);
		}
		if (propList.toUpperCase().contains("SIMBANKPORT")) {
			System.out.println("what port would you like the SimBank server to listen on? (Default 2080)");
			String sbPort = null;
			 while (true) {
				    sbPort = scanner.nextLine();
				    if (sbPort.isEmpty() || sbPort.equals("2080")) {
				    	sbPort = "2080";
				        break;
				    } else {
				        try {
				        	Integer.valueOf(sbPort);
				            break;
				        } catch (NumberFormatException ex) {
				            System.out.println("Please enter port as an integer");
				        }
				    }
				}
			 System.out.println("SimBank Port set to: " + sbPort);
			 String key = "simbankPort";
			 String value = sbPort;
			 updateyml(key, value);
		}
		else if (!(propList.toUpperCase().contains("HOSTNAME") || propList.toUpperCase().contains("NETWORKNAME") || propList.toUpperCase().contains("RESTARTPOLICY") || propList.toUpperCase().contains("APICONTAINERNAME") || propList.toUpperCase().contains("APIVOLNAME") || propList.toUpperCase().contains("APIMOUNTTARGET") || propList.toUpperCase().contains("APIPORT") || propList.toUpperCase().contains("CONTROLLERCONTAINERNAME") || propList.toUpperCase().contains("CPSCONTAINERNAME") || propList.toUpperCase().contains("CPSVOLNAME") || propList.toUpperCase().contains("CPSMOUNTTARGET") || propList.toUpperCase().contains("CPSPORT") || propList.toUpperCase().contains("RASCONTAINERNAME") || propList.toUpperCase().contains("RASVOLNAME") || propList.toUpperCase().contains("RASMOUNTTARGET") || propList.toUpperCase().contains("RASPORT") || propList.toUpperCase().contains("RESOURCEMONITORCONTAINERNAME") || propList.toUpperCase().contains("RESOURCECONTAINERNAME") || propList.toUpperCase().contains("RESOURCEPORT") || propList.toUpperCase().contains("SIMBANKCONTAINERNAME") || propList.toUpperCase().contains("SIMBANKPORT") || propList.contains(" "))) {
			System.out.println("Error - Ensure you have spelled properties correctly, none found with those names");
			alterProps();
		}
		System.out.println("Properties updated, returning to main menu" + "\n");
		main(null);
	}
	
	static void updateyml (String key, String value) throws FileNotFoundException {
		
		String configLocation = "classes/";
		Yaml yaml = new Yaml();
		InputStream inputStream = yaml.getClass().getClassLoader().getResourceAsStream(configLocation + "config.yml");
		Map<String, Object> obj = yaml.load(inputStream);
		System.out.println(key + " was: " + obj.get(key));
		obj.replace(key, value);
		ymlgen(obj);
		
		
	}
	
	static void ymlgen (Map<String, Object> obj) throws FileNotFoundException {
		
		String configLocation = "classes/";
		
		File yaml = new File(configLocation + "config.yml");
		if (yaml.exists()) {
			yaml.delete();
		}
		
		PrintWriter writer = new PrintWriter(configLocation + "config.yml");
		writer.println("hostname: " + obj.get("hostname") + "\n");
		writer.println("networkName: " + obj.get("networkName") + "\n");
		writer.println("restartPolicy: " + obj.get("restartPolicy") + "\n");
		writer.println("apiContainerName: " + obj.get("apiContainerName") + "\n");
		writer.println("apiVolName: " + obj.get("apiVolName") + "\n");
		writer.println("apiMountTarget: " + obj.get("apiMountTarget") + "\n");
		writer.println("apiPort: " + obj.get("apiPort") + "\n");
		writer.println("controllerContainerName: " + obj.get("controllerContainerName") + "\n");
		writer.println("cpsContainerName: " + obj.get("cpsContainerName") + "\n");
		writer.println("cpsVolName: " + obj.get("cpsVolName") + "\n");
		writer.println("cpsMountTarget: " + obj.get("cpsMountTarget") + "\n");
		writer.println("cpsPort: " + obj.get("cpsPort") + "\n");
		writer.println("rasContainerName: " + obj.get("rasContainerName") + "\n");
		writer.println("rasVolName: " + obj.get("rasVolName") + "\n");
		writer.println("rasMountTarget: " + obj.get("rasMountTarget") + "\n");
		writer.println("rasPort: " + obj.get("rasPort") + "\n");
		writer.println("resourceMonitorContainerName: " + obj.get("resourceMonitorContainerName") + "\n");
		writer.println("resourceContainerName: " + obj.get("resourceContainerName") + "\n");
		writer.println("resourcePort: " + obj.get("resourcePort") + "\n");
		writer.println("simbankContainerName: " + obj.get("simbankContainerName") + "\n");
		writer.println("simbankPort: " + obj.get("simbankPort") + "\n");
		writer.close();
		
	}
	
	
	static void generateScript () throws FileNotFoundException, UnsupportedEncodingException {
		
		String configLocation = "classes/";
		Yaml yaml = new Yaml();
		InputStream inputStream = yaml.getClass().getClassLoader().getResourceAsStream(configLocation + "config.yml");
		Map<String, Object> obj = yaml.load(inputStream);
		
		String resourceVersion = "0.6.0";
		String bootVersion = "0.6.0";
		String apiVersion = "0.5.0-SNAPSHOT";
		
		
		Object hostname = obj.get("hostname");
		Object networkName = obj.get("networkName");
		Object resPol = obj.get("restartPolicy");
		Object cpsMountSource = obj.get("cpsVolName");
		Object rasMountSource = obj.get("rasVolName");
		Object apiPort = obj.get("apiPort");
		Object apiMountSource = obj.get("apiVolName");
		Object apiMountTarget = obj.get("apiMountTarget");
		Object apiContainerName = obj.get("apiContainerName");
		Object controllerContainerName = obj.get("controllerContainerName");
		Object cpsContainerName = obj.get("cpsContainerName");
		Object cpsMountTarget = obj.get("cpsMountTarget");
		Object cpsPort = obj.get("cpsPort");
		Object rasContainerName = obj.get("rasContainerName");
		Object rasMountTarget = obj.get("rasMountTarget");
		Object rasPort = obj.get("rasPort");
		Object resourceMonitorContainerName = obj.get("resourceMonitorContainerName");
		Object resourceContainerName = obj.get("resourceContainerName");
		Object resourcePort = obj.get("resourcePort");
		Object sbName = obj.get("simbankContainerName");
		Object sbPort = obj.get("simbankPort");
		
		
		String dockerNet = "docker network create \\\n" + 
				"               --driver=bridge \\\n" +
		        "               " + networkName + "\n";
		String volCreate = "docker volume create " + cpsMountSource + "\n" + 
				"docker volume create " + rasMountSource + "\n" + 
				"docker volume create " + apiMountSource + "\n";
		
		String apiCont = "docker run --name " + apiContainerName + " \\\n" + 
				"           --network " + networkName + " \\\n" + 
				"           --restart " + resPol + " \\\n" + 
				"           --detach \\\n" + 
				"           --mount source=" + apiMountSource + ",target=" + apiMountTarget + " \\\n" + 
				"           --publish " + apiPort + ":8181 \\\n" + 
				"           --publish " + hostname + ":8101:8101 \\\n" + 
				"           docker.galasa.dev/galasa-master-api-amd64:" + apiVersion + " \\\n" + 
				"           /galasa/bin/karaf server\n";
		
		String contCont = "docker run --name " + controllerContainerName +  " \\\n" + 
				"           --network " + networkName + " \\\n" + 
				"           --restart " + resPol + " \\\n" + 
				"           --detach \\\n" +
				"           --env engine_image=galasa/galasa-boot-embedded-amd64:" + bootVersion + " \\\n" +
				"           --env run_poll=10 \\\n" +
				"           --env max_engines=2 \\\n" +
				"           --env network=" + networkName + " \\\n" +
				"           docker.galasa.dev/galasa-boot-embedded-amd64:" + bootVersion + " \\\n" + 
				"           java -jar boot.jar --obr file:galasa.obr --dockercontroller --bootstrap http://" + apiMountSource + ":8181/bootstrap" + "\n";
		String cpsCont = "docker run --name " + cpsContainerName + " \\\n" + 
				"           --network " + networkName +  " \\\n" + 
				"           --restart " + resPol + " \\\n" + 
				"           --detach \\\n" + 
				"           --mount source=" + cpsMountSource + ",target=" + cpsMountTarget + " \\\n" + 
				"           --publish " + cpsPort + ":2379 \\\n" + 
				"           quay.io/coreos/etcd:v3.2.25 \\\n" + 
				"           etcd --data-dir /var/run/etcd/default.etcd --initial-cluster default=http://" + hostname + ":2380 --listen-client-urls http://0.0.0.0:2379 --listen-peer-urls http://0.0.0.0:2380 --initial-advertise-peer-urls http://127.0.0.1:2380 --advertise-client-urls http://127.0.0.1:2379\n";
		String rasCont = "docker run --name " + rasContainerName + " \\\n" + 
				"           --network " + networkName + " \\\n" + 
				"           --restart " + resPol + " \\\n" + 
				"           --detach \\\n" + 
				"           --env COUCHDB_USER=galasa \\\n" +
				"           --env COUCHDB_PASSWORD=galasa \\\n" +
				"           --mount source=" + rasMountSource + ",target=" + rasMountTarget + " \\\n" + 
				"           --publish " + rasPort + ":5984 \\\n" + 
				"           couchdb:2 \n" ;
		String resmonCont = "docker run --name " + resourceMonitorContainerName + " \\\n" + 
				"           --network " + networkName + " \\\n" + 
				"           --restart " + resPol + " \\\n" + 
				"           --detach \\\n" + 
				"           docker.galasa.dev/galasa-boot-embedded-amd64:" + bootVersion + " \\\n" + 
				"           java -jar boot.jar --obr file:galasa.obr --resourcemanagement --bootstrap http://" + apiMountSource + ":8181/bootstrap \n";
		String resCont = "docker run --name " + resourceContainerName + " \\\n" + 
				"           --network " + networkName + " \\\n" + 
				"           --restart " + resPol + " \\\n" + 
				"           --detach \\\n" + 
				"           --publish " + resourcePort + ":80 \\\n" + 
				"           docker.galasa.dev/galasa-resources-amd64:" + resourceVersion +  "\n";
		String sbCont = "docker run --name " + sbName + " \\\n" + 
				"           --network " + networkName + " \\\n" + 
				"           --restart " + resPol + " \\\n" + 
				"           --detach \\\n" + 
				"           --publish " + sbPort + ":2080 \\\n" + 
				"           --publish 2023:2023 \\\n" + 
				"           docker.galasa.dev/galasa-boot-embedded-amd64:" + bootVersion + " \\\n" + 
				"           java -jar simframe.jar";
		
		File script = new File("docker-ecosystem-setup.sh");
		if (script.exists()) {
			script.delete();
		}
		
		PrintWriter writer = new PrintWriter("docker-ecosystem-setup.sh", "UTF-8");
		writer.println("#!/bin/bash \n");
		writer.println(dockerNet);
		writer.println(volCreate);
		writer.println(apiCont);
		writer.println(contCont);
		writer.println(cpsCont);
		writer.println(rasCont);
		writer.println(resmonCont);
		writer.println(resCont);
		writer.println(sbCont);
		writer.close();

		System.out.println("Script generated, updating permissions... \n");
		
		try {
			Runtime.getRuntime().exec("chmod u+x docker-ecosystem-setup.sh");
		} catch (IOException e) {
			System.out.println("Error setting file permissions");
			e.printStackTrace();
		}
		
		System.out.println("Permissions updated \n");
		
		System.out.println("---The script docker-ecosystem-setup.sh has been created--- \n");
		
		System.out.println("---If you want to run this script execute it with ./docker-ecosystem-setup.sh--- \n");
		
		System.out.println("---Use a text editor of your choice if you wish to check and make any edits in the script--- \n");
	}

}