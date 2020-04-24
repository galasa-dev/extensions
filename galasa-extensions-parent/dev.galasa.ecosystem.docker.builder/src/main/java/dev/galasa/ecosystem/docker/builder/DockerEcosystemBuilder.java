package dev.galasa.ecosystem.docker.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Scanner;

import org.yaml.snakeyaml.Yaml;

public class DockerEcosystemBuilder {

	public static void main(String[] args) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Welcome to the Galasa Ecosystem manager for Docker \n");
		System.out.println("If you have not run this program before, default values for the configuration have been supplied \n");
		Map<String, Object> obj = null;
		Yaml yaml = new Yaml();
		InputStream config = DockerEcosystemBuilder.class.getResourceAsStream("/config.yml");
		obj = yaml.load(config);
		showProps(obj);
		config.close();
	}
	
	private static void showProps(Map<String, Object> obj) throws IOException {
		
		System.out.println("Here is your current configuration for the Ecosystem: \n");
				
		System.out.println("1-Docker_server_hostname: " + obj.get("1-Docker_server_hostname") + "\n");
		System.out.println("2-Docker_network_name: " + obj.get("2-Docker_network_name") + "\n");
		System.out.println("3-Container_restart_policy: " + obj.get("3-Container_restart_policy") + "\n");
		System.out.println("4-Api_container_name: " + obj.get("4-Api_container_name") + "\n");
		System.out.println("5-Api_volume_name: " + obj.get("5-Api_volume_name") + "\n");
		System.out.println("6-Api_mount_target: " + obj.get("6-Api_mount_target") + "\n");
		System.out.println("7-Api_container_port: " + obj.get("7-Api_container_port") + "\n");
		System.out.println("8-Controller_container_name: " + obj.get("8-Controller_container_name") + "\n");
		System.out.println("9-Cps_container_name: " + obj.get("9-Cps_container_name") + "\n");
		System.out.println("10-Cps_volume_name: " + obj.get("10-Cps_volume_name") + "\n");
		System.out.println("11-Cps_mount_target: " + obj.get("11-Cps_mount_target") + "\n");
		System.out.println("12-Cps_container_port: " + obj.get("12-Cps_container_port") + "\n");
		System.out.println("13-Ras_container_name: " + obj.get("13-Ras_container_name") + "\n");
		System.out.println("14-Ras_volume_name: " + obj.get("14-Ras_volume_name") + "\n");
		System.out.println("15-Ras_mount_target: " + obj.get("15-Ras_mount_target") + "\n");
		System.out.println("16-Ras_container_port : " + obj.get("16-Ras_container_port") + "\n");
		System.out.println("17-ResourceMonitor_container_name: " + obj.get("17-ResourceMonitor_container_name") + "\n");
		System.out.println("18-Resource_container_name: " + obj.get("18-Resource_container_name") + "\n");
		System.out.println("19-Resource_container_port: " + obj.get("19-Resource_container_port") + "\n");
		System.out.println("20-Simbank_container_name: " + obj.get("20-Simbank_container_name") + "\n");
		System.out.println("21-Simbank_port: " + obj.get("21-Simbank_port"));
		
		//config.close();
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("\n" + "Would you like to Change any properties, Generate the script or Exit (C, G, or X, Default: C)");
		String reuse = scanner.nextLine();
		while (!(reuse.toUpperCase().equals("C") || reuse.toUpperCase().equals("G") || reuse.toUpperCase().equals("X") || reuse.isEmpty())) {	
			System.out.println("Please enter C, G or X");
			reuse = scanner.nextLine();
		}
		if (reuse.toUpperCase().equals("C") || reuse.isEmpty()) {
			changeProps(obj);
		
		}
		else if (reuse.toUpperCase().equals("G")){
			System.out.println("Generating script..." + "\n");
				generateScript(obj);
		}
		else if (reuse.toUpperCase().equals("X")) {
			System.out.println("Exiting program...");
		}
	    scanner.close();
		
		
	}

	static void changeProps(Map<String, Object> obj) throws IOException {
		
		System.out.println("\n" + "Which of these properties would you like to change? Please enter the number next to the property you wish to change");
		System.out.println("\n" + "Enter 0 to return to the main screen");
		String propList = null;
		System.out.println("Config object at change props stage: " + obj);
		Scanner scanner = new Scanner(System.in);
		while (true) {
			propList = scanner.nextLine();
		        try {
		        	Integer.valueOf(propList);
		        	Integer props = Integer.valueOf(propList);
		        	alterProps(obj, props);
		            break;
		        } catch (NumberFormatException ex) {
		            System.out.println("Please enter properties to change as a number");
		        }
		    }
	}
	
	
	static void alterProps(Map<String, Object> obj, Integer props) throws IOException {		
		
		switch (props) {
		
		case 0:
			main(null);
			break;
			
		case 1:
			hostname(obj);
			break;
			
		case 2:
			networkName(obj);
			break;
			
		case 3:
			resPol(obj);
			break;
			
		case 4:
			apiName(obj);
			break;
			
		case 5:
			apiVol(obj);
			break;
			
		case 6:
			apiMount(obj);
			break;
			
		case 7:
			apiPort(obj);
			break;
			
		case 8:
			controllerName(obj);
			break;
			
		case 9:
			cpsName(obj);
			break;
			
		case 10:
			cpsVol(obj);
			break;
			
		case 11:
			cpsMount(obj);
			break;
			
		case 12:
			cpsPort(obj);
			break;
			
		case 13:
			rasName(obj);
			break;
		
		case 14:
			rasVol(obj);
			break;
			
		case 15:
			rasMount(obj);
			break;
			
		case 16:
			rasPort(obj);
			break;
			
		case 17:
			resmonName(obj);
			break;
			
		case 18:
			resName(obj);
			break;
			
		case 19:
			resPort(obj);
			break;
			
		case 20:
			sbName(obj);
			break;
			
		case 21:
			sbPort(obj);
			break;
			
	  }
		
		
	}
	
	
	
	private static void sbPort(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
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
		 String key = "21-Simbank_port";
		 String value = sbPort;
		 updateyml(obj, key, value);
		
	}

	private static void sbName(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the name you would like for the SimBank container" + "\n");
		String sbName = scanner.nextLine();
		System.out.println("Hostname will be changed to " + sbName + "\n");
		String key = "20-Simbank_container_name";
		String value = sbName;
		updateyml(obj, key, value);
		
	}

	private static void resPort(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
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
		 String key = "19-Resource_container_port";
		 String value = resPort;
		 updateyml(obj, key, value);
		
	}

	private static void resName(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the name you would like for the Resource container" + "\n");
		String resName = scanner.nextLine();
		System.out.println("Resource container name will be changed to " + resName + "\n");
		String key = "18-Resource_container_name";
		String value = resName;
		updateyml(obj, key, value);
		
	}

	private static void resmonName(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the name you would like for the Resource Monitor container: " + "\n");
		String resmonName = scanner.nextLine();
		System.out.println("Resource monitor container name to be set to: " + resmonName);
		String key = "17-ResourceMonitor_container_name";
		String value = resmonName;
		updateyml(obj, key, value);
		
	}

	private static void rasPort(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
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
		 String key = "16-Ras_container_port";
		 String value = rasPort;
		 updateyml(obj, key, value);
		
	}

	private static void rasMount(Map<String, Object> obj) throws IOException{
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the directory location to mount the RAS volume: " + "\n");
		String rasMount = scanner.nextLine();
		System.out.println("RAS Mount target name set to: " + rasMount);
		String key = "15-Ras_mount_target";
		String value = rasMount;
		updateyml(obj, key, value);
		
	}

	private static void rasVol(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter name of the volume you would like to create for the RAS container: " + "\n");
		String rasVol = scanner.nextLine();
		System.out.println("RAS volume name set to: " + rasVol);
		String key = "14-Ras_volume_name";
		String value = rasVol;
		updateyml(obj, key, value);
		
	}

	private static void rasName(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the name you would like for the RAS container: " + "\n");
		String rasName = scanner.nextLine();
		System.out.println("RAS container name set to: " + rasName);
		String key = "13-Ras_container_name";
		String value = rasName;
		updateyml(obj, key, value);
		
	}

	private static void cpsPort(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
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
		 String key = "12-Cps_container_port";
		 String value = cpsPort;
		 updateyml(obj, key, value);
		
	}

	private static void cpsMount(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the directory location to mount the CPS volume: " + "\n");
		String cpsMount = scanner.nextLine();
		System.out.println("CPS Mount target name set to: " + cpsMount);
		String key = "11-Cps_mount_target";
		String value = cpsMount;
		updateyml(obj, key, value);
		
	}

	private static void cpsVol(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the name of the volume you would like to create for the CPS container: " + "\n");
		String cpsVol = scanner.nextLine();
		System.out.println("CPS Volume name set to: " + cpsVol);
		String key = "10-Cps_volume_name";
		String value = cpsVol;
		updateyml(obj, key, value);
		
	}

	private static void cpsName(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the name you would like for the cps container: " + "\n");
		String cpsName = scanner.nextLine();
		System.out.println("CPS Container name set to: " + cpsName);
		String key = "9-Cps_container_name";
		String value = cpsName;
		updateyml(obj, key, value);
		
	}

	private static void controllerName(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the name you would like for the controller container: " + "\n");
		String contName = scanner.nextLine();
		System.out.println("Controller container name set to: " + contName);
		String key = "8-Controller_container_name";
		String value = contName;
		updateyml(obj, key, value);
		
	}

	private static void apiPort(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
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
			String key = "7-Api_container_port";
			String value = apiPort;
			updateyml(obj, key, value);
		
	}

	private static void apiMount(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the directory you would like the volume mounted to: " + "\n");
		String apiMount = scanner.nextLine();
		System.out.println("Api Mount point set to: " + apiMount);
		String key = "6-Api_mount_target";
		String value = apiMount;
		updateyml(obj, key, value);
		
	}

	private static void apiVol(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please enter the name of the volume you would like to create for the API container: " + "\n");
		String apiVol = scanner.nextLine();
		System.out.println("Api Volume name set to: " + apiVol);
		String key = "5-Api_volume_name";
		String value = apiVol;
		updateyml(obj, key, value);
	}

	private static void apiName(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("What would you like the API container name set to: " + "\n");
		String apiName = scanner.nextLine();
		System.out.println("API container name set to: " + apiName);
		String key = "4-Api_container_name";
		String value = apiName;
		updateyml(obj, key, value);
		
	}

	private static void resPol(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
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
		String key = "3-Container_restart_policy";
		String value = ResPol;
		updateyml(obj, key, value);
		
	}

	private static void networkName(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("What would you like to update the network name to?" + "\n");
		String netName = scanner.nextLine();
		System.out.println("Network name will be changed to " + netName + "\n");
		scanner.close();
		String key = "2-Docker_network_name";
		String value = netName;
		updateyml(obj, key, value);
		
	}

	private static void hostname(Map<String, Object> obj) throws IOException {
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("What would you like to update the hostname the docker container will use to?" + "\n");
		String hostname = scanner.nextLine();
		System.out.println("Hostname will be changed to " + hostname + "\n");
		scanner.close();
		String key = "1-Docker_server_hostname";
		String value = hostname;
		updateyml(obj, key, value);
		
		
	}

	static void updateyml (Map<String, Object> obj, String key, String value) throws IOException {
		
		//String configLocation = "classes/";
		//Yaml yaml = new Yaml();
		//InputStream config = DockerEcosystemBuilder.class.getResourceAsStream("/config.yml");
		//InputStream inputStream = yaml.getClass().getClassLoader().getResourceAsStream("config.yml");
		//Map<String, Object> obj2 = yaml.load(config);
		//Map<String, Object> obj = yaml.load(inputStream);
		System.out.println("Object list at update yml time: \n" + obj + "\n");
		//System.out.println("Input Stream method fancy \n" + obj2 + "\n");
		System.out.println(key + " was: " + obj.get(key));
		obj.replace(key, value);
		System.out.println(obj);
		ymlgen(obj);
		
	}
	
	static void ymlgen (Map<String, Object> obj) throws IOException {
		
		String configLocation = "classes/";
		
		File config = new File(configLocation + "config.yml");
		if (config.exists()) {
			config.delete();
			System.out.println("config deleted");
		}
		
		PrintWriter writer = new PrintWriter(configLocation + "config.yml");
		writer.println("1-Docker_server_hostname: " + obj.get("1-Docker_server_hostname") + "\n");
		writer.println("2-Docker_network_name: " + obj.get("2-Docker_network_name") + "\n");
		writer.println("3-Container_restart_policy: " + obj.get("3-Container_restart_policy") + "\n");
		writer.println("4-Api_container_name: " + obj.get("4-Api_container_name") + "\n");
		writer.println("5-Api_volume_name: " + obj.get("5-Api_volume_name") + "\n");
		writer.println("6-Api_mount_target: " + obj.get("6-Api_mount_target") + "\n");
		writer.println("7-Api_container_port: " + obj.get("7-Api_container_port") + "\n");
		writer.println("8-Controller_container_name: " + obj.get("8-Controller_container_name") + "\n");
		writer.println("9-Cps_container_name: " + obj.get("9-Cps_container_name") + "\n");
		writer.println("10-Cps_volume_name: " + obj.get("10-Cps_volume_name") + "\n");
		writer.println("11-Cps_mount_target: " + obj.get("11-Cps_mount_target") + "\n");
		writer.println("12-Cps_container_port: " + obj.get("12-Cps_container_port") + "\n");
		writer.println("13-Ras_container_name: " + obj.get("13-Ras_container_name") + "\n");
		writer.println("14-Ras_volume_name: " + obj.get("14-Ras_volume_name") + "\n");
		writer.println("15-Ras_mount_target: " + obj.get("15-Ras_mount_target") + "\n");
		writer.println("16-Ras_container_port : " + obj.get("16-Ras_container_port") + "\n");
		writer.println("17-ResourceMonitor_container_name: " + obj.get("17-ResourceMonitor_container_name") + "\n");
		writer.println("18-Resource_container_name: " + obj.get("18-Resource_container_name") + "\n");
		writer.println("19-Resource_container_port: " + obj.get("19-Resource_container_port") + "\n");
		writer.println("20-Simbank_container_name: " + obj.get("20-Simbank_container_name") + "\n");
		writer.println("21-Simbank_port: " + obj.get("21-Simbank_port"));
		writer.close();
		
		System.out.println("Property updated");
		System.out.println(obj);
		writer.close();
		showProps(obj);
		
	}
	
	
	static void generateScript (Map<String, Object> obj) throws FileNotFoundException, UnsupportedEncodingException {
		
		String configLocation = "classes/";
		String dir = System.getProperty("user.dir");
		System.out.println(dir);
		System.out.println(dir + "/" + configLocation + "config.yml");
		
		String resourceVersion = "0.6.0";
		String bootVersion = "0.6.0";
		String apiVersion = "0.5.0-SNAPSHOT";
		
		
		Object hostname = obj.get("1-Docker_server_hostname");
		Object networkName = obj.get("2-Docker_network_name");
		Object resPol = obj.get("3-Container_restart_policy");
		Object cpsMountSource = obj.get("10-Cps_volume_name");
		Object rasMountSource = obj.get("14-Ras_volume_name");
		Object apiPort = obj.get("7-Api_container_port");
		Object apiMountSource = obj.get("5-Api_volume_name");
		Object apiMountTarget = obj.get("6-Api_mount_target");
		Object apiContainerName = obj.get("4-Api_container_name");
		Object controllerContainerName = obj.get("8-Controller_container_name");
		Object cpsContainerName = obj.get("9-Cps_container_name");
		Object cpsMountTarget = obj.get("11-Cps_mount_target");
		Object cpsPort = obj.get("12-Cps_container_port");
		Object rasContainerName = obj.get("13-Ras_container_name");
		Object rasMountTarget = obj.get("15-Ras_mount_target");
		Object rasPort = obj.get("16-Ras_container_port");
		Object resourceMonitorContainerName = obj.get("17-ResourceMonitor_container_name");
		Object resourceContainerName = obj.get("18-Resource_container_name");
		Object resourcePort = obj.get("19-Resource_container_port");
		Object sbName = obj.get("20-Simbank_container_name");
		Object sbPort = obj.get("21-Simbank_port");
		System.out.println(sbPort);
		
		
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

		System.out.println("Script generated... \n");
		
		System.out.println("---The script docker-ecosystem-setup.sh has been created--- \n");
		
		System.out.println("---If you want to run this script execute it with ./docker-ecosystem-setup.sh--- \n");
		
		System.out.println("---Use a text editor of your choice if you wish to check and make any edits in the script--- \n");
	    
	
	
	}

}
