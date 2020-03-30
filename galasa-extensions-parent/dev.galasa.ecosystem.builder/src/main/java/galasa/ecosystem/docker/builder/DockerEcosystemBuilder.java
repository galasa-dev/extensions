package galasa.ecosystem.docker.builder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

public class DockerEcosystemBuilder {

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		
		  String subnet = null;
		  String gateway = null;
		  String apiName = null;
		  String netName = null;
		  String apiResPol = null;
		  String apiVol = null;
		  String apiVolSrc = null;
		  String apiVolTrg = null;
		  String apiPort = null;
		  String contName = null;
		  String contResPol = null;
		  String contVol = null;
		  String cpsName = null;
		  String cpsResPol = null;
		  String cpsVolSrc = null;
		  String cpsVolTrg = null;
		  String cpsPort = null;
		  String cdbiName = "init";
		  String cdbiVolSrc = "init";
		  String cdbiVolTrg = "init";
		  String cdbName = null;
		  String cdbResPol = null;
		  String cdbEnv = null;
		  String cdbVolSrc = null;
		  String cdbVolTrg = null;
		  String cdbPort = null;
		  String resmonName = null;
		  String resmonResPol = null;
		  String resName = null;
		  String resResPol = null;
		  String resPort = null;
		  String sbName = null;
		  String sbResPol = null;
		  String sbPort = null;
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Welcome to the Galasa Ecosystem for Docker set up" + "\n");
		System.out.println("Would you like to reuse old config (default Y):");
		String reuse = scanner.nextLine();
	    while (!(reuse.toUpperCase().equals("Y") || reuse.toUpperCase().equals("N") || reuse.isEmpty())) {	
			System.out.println("Please enter Y or N");
			reuse = scanner.nextLine();
		}
		if ((reuse.toUpperCase().equals("Y") || (reuse.isEmpty())))	{
			System.out.println("Using previously set config");	
			
		} else {
		  System.out.println("Creating new config");
		  

		  
		  System.out.println("Network set up...");
		  System.out.println("What is your subnet IP (default: 172.21.0.0/16):");
		  subnet = scanner.nextLine();
		  if (subnet.isEmpty() || subnet.equals("172.21.0.0/16")) {
			  subnet = "172.21.0.0/16";
			  System.out.println("Using default subnet...");
		  }
		  else {
			  System.out.println("Using new subnet...");
		  }
		  
		  System.out.println("Subnet set to: " + subnet);
		  
		  System.out.println("What would you like your gateway set to (default 172.21.0.1):");
		  gateway = scanner.nextLine();
		  if (gateway.isEmpty() || gateway.equals("172.21.0.1")) {
			  gateway = "172.21.0.1";
			  System.out.println("Using default gateway...");
		  }
		  else {
			  System.out.println("Using new gateway...");
		  }
		  	  		  
		  System.out.println("Gateway set to: " + gateway);
		  
		  System.out.println("Network name (default galasa): ");
		  netName = scanner.nextLine();
		  if (netName.isEmpty() || netName.equals("galasa")) {
			  netName = "galasa";
			  System.out.println("Using default Network name");
		  }
		  else {
			  System.out.println("Using new Network name");
		  }
		  
		  System.out.println("Network name set to: " + netName + "\n");
		  System.out.println("Moving on to API container set up...");
		  
		  System.out.println("What name would you like for the container (default galasa-api):");
		  apiName = scanner.nextLine();
		  if (apiName.isEmpty() || apiName.equals("galasa-api")) {
			  apiName = "galasa-api";
			  System.out.println("Using default container name...");
		  }
		  else {
			  System.out.println("Using new container name...");
		  }
		  
		  System.out.println("API Container name set to: " + apiName + "\n");
		  
		  System.out.println("Using network name previously entered..." + "\n");
		  
		  System.out.println("Restart policy - no, on-failure, always or unless-stopped (default always):");
		  apiResPol = scanner.nextLine();
		  while (!(apiResPol.toUpperCase().equals("NO") || apiResPol.toUpperCase().equals("ON-FAILURE") || apiResPol.toUpperCase().equals("ALWAYS") || apiResPol.toUpperCase().equals("UNLESS-STOPPED") || apiResPol.isEmpty())) {	
		      System.out.println("I'm sorry but that isn't a valid input, please ensure you have entered a restart policy listed above");
		      apiResPol = scanner.nextLine();
			}
		  if (apiResPol.isEmpty() || apiResPol.equals("always")) {
			  apiResPol = "always";
			  System.out.println("Using default restart policy...");
		  }
		  else {
			  System.out.println("Using new restart policy...");
		  }
		  
		  System.out.println("Restart policy set to: " + apiResPol + "\n");
		  
		  System.out.print("Volume to mount - will be prefixed with $(pwd): (default bootstrap.properties): " );
		  apiVol = scanner.nextLine();
		  while (!(apiVol.toUpperCase().contains("PROPERTIES") || apiVol.isEmpty())) {
			  System.out.println("Please use a format ending in .properties and with no spaces");
			  apiVol = scanner.nextLine();
		  }		  
		  if (apiVol.isEmpty() || apiVol.equals("bootstrap.properties")) {
			  apiVol = "bootstrap.properties";
			  System.out.println("Using default volume...");
		  }
		  else {
			  System.out.println("Using new volume");
		  }
		  
		  System.out.println("Volume for mount set to: " + apiVol + "\n");
		  
		  System.out.println("Filesystem source to mount to container: (default: galasa-api)");
		  apiVolSrc = scanner.nextLine();
		  if (apiVolSrc.isEmpty() || apiVolSrc.equals("galasa-api")) {
			  apiVolSrc = "galasa-api";
			  System.out.println("Using default source...");
		  }
		  else {
			  System.out.println("Using new source...");
		  }
		  
		  System.out.println("Volume source set to: " + apiVolSrc);
		  
		  System.out.println("Target location for volume source: (default: /galasa/data/galasa): ");
		  apiVolTrg = scanner.nextLine();
		  if (apiVolTrg.isEmpty() || apiVolTrg.equals("/galasa/data/galasa")) {
			  apiVolTrg = "/galasa/data/galasa";
			  System.out.println("Using default target...");
		  }
		  else {
			  System.out.println("Using new target...");
		  }
		  
		  System.out.println("Volume target set to: " + apiVolTrg);
		  
		  System.out.println("Port to publish to host: (default 8181:8181)");
		  
		  while (true) {
			    apiPort = scanner.nextLine();
			    if (apiPort.isEmpty() || apiPort.equals("8181:8181")) {
			        apiPort = "8181:8181";
			        break;
			    } else {
			        try {
			        	String port1 = null;
			        	String port2 = null;
			        	port1 = apiPort.substring(0, apiPort.indexOf(":"));
			        	port2 = apiPort.substring(apiPort.lastIndexOf(":") + 1);
			        	Integer.valueOf(port1);
			            Integer.valueOf(port2);
			            break;
			        } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
			            System.out.println("Please enter a port in the format nnnn:nnnn");
			        }
			    }
			}
		  
		  System.out.println("Published port set to: " + apiPort + "\n");
		  
		  System.out.println("Moving on to controller container set up" + "\n");
		  
		  System.out.println("Controller container name: (default galasa - controller): ");
		  contName = scanner.nextLine();
		  if (contName.isEmpty() || contName.equals("galasa-controller")) {
			  contName = "galasa-controller";
			  System.out.println("Using default container name...");
		  }
		  else {
			  System.out.println("Using new container name...");
		  }
		  
		  System.out.println("Controller container name set to: " + contName + "\n");
		  
		  System.out.println("Setting network name for container to: " + netName + "..." + "\n");
		  
		  System.out.println("Restart policy - no, on-failure, always or unless-stopped (default always): ");
		  contResPol = scanner.nextLine();
		  while (!(contResPol.toUpperCase().equals("NO") || contResPol.toUpperCase().equals("ON-FAILURE") || contResPol.toUpperCase().equals("ALWAYS") || contResPol.toUpperCase().equals("UNLESS-STOPPED") || contResPol.isEmpty())) {	
		      System.out.println("I'm sorry but that isn't a valid input, please ensure you have entered a restart policy listed above");
		      contResPol = scanner.nextLine();
			}
		  if (contResPol.isEmpty() || contResPol.equals("always")) {
			  contResPol = "always";
			  System.out.println("Using default restart policy...");
		  }
		  else {
			  System.out.println("Using new restart policy...");
		  }
		  
		  System.out.println("Restart policy set to: " + contResPol + "\n");
		  
		  System.out.print("Volume to mount, will be prefixed by $(pwd): (default controller.properties): " );
		  contVol = scanner.nextLine();
		  while (!(contVol.toUpperCase().contains("PROPERTIES") || contVol.isEmpty())) {
			  System.out.println("Please use a format ending in .properties and with no spaces");
			  contVol = scanner.nextLine();
		  }
		  if (contVol.isEmpty() || contVol.equals("controller.properties")) {
			  contVol = "controller.properties";
			  System.out.println("Using default volume...");
		  }
		  else {
			  System.out.println("Using new volume");
		  }
		  
		  System.out.println("Volume set to: " + contVol);
		  
		  System.out.println("Moving on to CPS container set-up..." + "\n");
		  
		  System.out.println("CPS Container name: (default galasa-cps): ");
		  cpsName = scanner.nextLine();
		  if (cpsName.isEmpty() || cpsName.equals("galasa-cps")) {
			  cpsName = "galasa-cps";
			  System.out.println("Using default container name...");
		  }
		  else {
			  System.out.println("Using new container name");
		  }
		  
		  System.out.println("CPS container name set to: " + cpsName + "\n");
		  
		  System.out.println("Setting network name for container to: " + netName + "..." + "\n");
		  
		  System.out.println("Restart policy - no, on-failure, always or unless-stopped (default always): ");
		  cpsResPol = scanner.nextLine();
		  while (!(cpsResPol.toUpperCase().equals("NO") || cpsResPol.toUpperCase().equals("ON-FAILURE") || cpsResPol.toUpperCase().equals("ALWAYS") || cpsResPol.toUpperCase().equals("UNLESS-STOPPED") || cpsResPol.isEmpty())) {	
		      System.out.println("I'm sorry but that isn't a valid input, please ensure you have entered a restart policy listed above");
		      cpsResPol = scanner.nextLine();
			}
		  if (cpsResPol.isEmpty() || cpsResPol.equals("always")) {
			  cpsResPol = "always";
			  System.out.println("Using default restart policy...");
		  }
		  else {
			  System.out.println("Using new restart policy");
		  }
		  
		  System.out.println("Restart policy set to: " + cpsResPol);
		  
		  System.out.println("Filesystem source to mount to container: (default: galasa-etcd)");
		  cpsVolSrc = scanner.nextLine();
		  if (cpsVolSrc.isEmpty() || cpsVolSrc.equals("galasa-etcd")) {
			  cpsVolSrc = "galasa-etcd";
			  System.out.println("Using default source...");
		  }
		  else {
			  System.out.println("Using new source...");
		  }
		  
		  System.out.println("Volume source set to: " + cpsVolSrc);
		  
		  System.out.println("Target location for volume source: (default: /var/run/etcd/default.etcd): ");
		  cpsVolTrg = scanner.nextLine();
		  if (cpsVolTrg.isEmpty() || cpsVolTrg.equals("/var/run/etcd/default.etcd")) {
			  cpsVolTrg = "/var/run/etcd/default.etcd";
			  System.out.println("Using default target...");
		  }
		  else {
			  System.out.println("Using new target...");
		  }
		  
		  System.out.println("Volume target set to: " + cpsVolTrg);
		  
		  System.out.println("Port to publish to host: (default 2379:2379)");
		  while (true) {
			    cpsPort = scanner.nextLine();
			    if (cpsPort.isEmpty() || cpsPort.equals("2379:2379")) {
			        cpsPort = "2379:2379";
			        break;
			    } else {
			        try {
			        	String port1 = null;
			        	String port2 = null;
			        	port1 = cpsPort.substring(0, cpsPort.indexOf(":"));
			        	port2 = cpsPort.substring(cpsPort.lastIndexOf(":") + 1);
			        	Integer.valueOf(port1);
			            Integer.valueOf(port2);
			            break;
			        } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
			            System.out.println("Please enter a port in the format nnnn:nnnn");
			        }
			    }
			}
		  
		  System.out.println("Published port set to: " + cpsPort + "\n");
		  
		  System.out.println("Moving on to RAS CouchDB container set up...");
		  
		  System.out.println("RAS CouchDB container name: (default: galasa-ras");
		  cdbName = scanner.nextLine();
		  if (cdbName.isEmpty() || cdbName.equals("galasa-ras")) {
			  cdbName = "galasa-ras";
			  System.out.println("Using default container name...");
		  }
		  else {
			  System.out.println("Using new container name...");
		  }
		  
		  System.out.println("RAS CouchDb container name set to: " + cdbName);
		  
		  System.out.println("Restart policy - no, on-failure, always or unless-stopped (default always): ");
		  cdbResPol = scanner.nextLine();
		  while (!(cdbResPol.toUpperCase().equals("NO") || cdbResPol.toUpperCase().equals("ON-FAILURE") || cdbResPol.toUpperCase().equals("ALWAYS") || cdbResPol.toUpperCase().equals("UNLESS-STOPPED") || cdbResPol.isEmpty())) {	
		      System.out.println("I'm sorry but that isn't a valid input, please ensure you have entered a restart policy listed above");
		      cdbResPol = scanner.nextLine();
			}
		  if (cdbResPol.isEmpty() || cdbResPol.equals("always")) {
			  cdbResPol = "always";
			  System.out.println("Using default restart policy...");
		  }
		  else {
			  System.out.println("Using new restart policy");
		  }
		  
		  System.out.println("Restart policy set to: " + cdbResPol);
		  
		  System.out.println("Environment variables file: (default galasa-env.properties): ");
		  cdbEnv = scanner.nextLine();
		  while (!(cdbEnv.toUpperCase().contains("PROPERTIES") || cdbEnv.isEmpty())) {
			  System.out.println("Please use a format ending in .properties and with no spaces");
			  cdbEnv = scanner.nextLine();
		  }
		  if (cdbEnv.isEmpty() || cdbEnv.equals("galasa-env.properties")) {
			  cdbEnv = "galasa-env.properties";
			  System.out.println("Using default environment variables file");				  
		  }
		  else {
			  System.out.println("Using new environment variables file");
		  }
		  
		  System.out.println("Environment variables file: " + cdbEnv);
		  
		  System.out.println("Filesystem source to mount to container: (default: galasa-couchdb)");
		  cdbVolSrc = scanner.nextLine();
		  if (cdbVolSrc.isEmpty() || cdbVolSrc.equals("galasa-couchdb")) {
			  cdbVolSrc = "galasa-couchdb";
			  System.out.println("Using default source...");
		  }
		  else {
			  System.out.println("Using new source...");
		  }
		  
		  System.out.println("Volume source set to: " + cdbVolSrc);
		  
		  System.out.println("Target location for volume source: (default: /var/run/etcd/default.etcd): ");
		  cdbVolTrg = scanner.nextLine();
		  if (cdbVolTrg.isEmpty() || cdbVolTrg.equals("/var/run/etcd/default.etcd")) {
			  cdbVolTrg = "/var/run/etcd/default.etcd";
			  System.out.println("Using default target...");
		  }
		  else {
			  System.out.println("Using new target...");
		  }
		  
		  System.out.println("Volume target set to: " + cdbVolTrg);
		  
		  System.out.println("Port to publish to host: (default 5984:5984)");
		  while (true) {
			    cdbPort = scanner.nextLine();
			    if (cdbPort.isEmpty() || cdbPort.equals("5984:5984")) {
			        cdbPort = "5984:5984";
			        break;
			    } else {
			        try {
			        	String port1 = null;
			        	String port2 = null;
			        	port1 = cdbPort.substring(0, cdbPort.indexOf(":"));
			        	port2 = cdbPort.substring(cdbPort.lastIndexOf(":") + 1);
			        	Integer.valueOf(port1);
			            Integer.valueOf(port2);
			            break;
			        } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
			            System.out.println("Please enter a port in the format nnnn:nnnn");
			        }
			    }
			}
		  System.out.println("Setting port to: " + cdbPort);
		  
		  System.out.println("Moving on to Resource Monitor container set up..." + "\n");
		  
		  System.out.println("Resource Monitor container name: (default galasa-resmon): ");
		  resmonName = scanner.nextLine();
		  if (resmonName.isEmpty() || resmonName.equals("galasa-resmon")) {
			  resmonName = "galasa-resmon";
			  System.out.println("Using default container name...");
			  
		  }
		  else {
			  System.out.println("Using new container name...");
		  }
		  
		  System.out.println("Resource monitor container name set to: " + resmonName + "\n");
		  
		  System.out.println("Using previously set network: " + netName + "..." + "\n");
		  
		  System.out.println("Restart policy - no, on-failure, always or unless-stopped (default always): ");
		  resmonResPol = scanner.nextLine();
		  while (!(resmonResPol.toUpperCase().equals("NO") || resmonResPol.toUpperCase().equals("ON-FAILURE") || resmonResPol.toUpperCase().equals("ALWAYS") || resmonResPol.toUpperCase().equals("UNLESS-STOPPED") || resmonResPol.isEmpty())) {	
		      System.out.println("I'm sorry but that isn't a valid input, please ensure you have entered a restart policy listed above");
		      resmonResPol = scanner.nextLine();
			}
		  if (resmonResPol.isEmpty() || resmonResPol.equals("always")) {
			  resmonResPol = "always";
			  System.out.println("Using default restart policy...");
		  }
		  else {
			  System.out.println("Using new restart policy");
		  }
		  
		  System.out.println("Restart policy set to: " + resmonResPol + "\n");
		  
		  System.out.println("Moving on to resource container set up...");
		  
		  System.out.println("Resource container name: (default galasa-resources): ");
		  resName = scanner.nextLine();
		  if (resName.isEmpty() || resName.equals("galasa-resources")) {
			  resName = "galasa-resources";
			  System.out.println("Using default container name...");
			  
		  }
		  else {
			  System.out.println("Using new container name...");
		  }
		  
		  System.out.println("Resource monitor container name set to: " + resName + "\n");
		  
		  System.out.println("Using previously set network name: " + netName + "..." + "\n");
		  
		  System.out.println("Restart policy - no, on-failure, always or unless-stopped (default always): ");
		  resResPol = scanner.nextLine();
		  while (!(resResPol.toUpperCase().equals("NO") || resResPol.toUpperCase().equals("ON-FAILURE") || resResPol.toUpperCase().equals("ALWAYS") || resResPol.toUpperCase().equals("UNLESS-STOPPED") || resResPol.isEmpty())) {	
		      System.out.println("I'm sorry but that isn't a valid input, please ensure you have entered a restart policy listed above");
		      resResPol = scanner.nextLine();
			}
		  if (resResPol.isEmpty() || resResPol.equals("always")) {
			  resResPol = "always";
			  System.out.println("Using default restart policy...");
		  }
		  else {
			  System.out.println("Using new restart policy");
		  }
		  
		  System.out.println("Restart policy set to: " + resResPol + "\n");
		  
		  System.out.println("Port to publish to host: (default 8080:80)");
		  while (true) {
			    resPort = scanner.nextLine();
			    if (resPort.isEmpty() || resPort.equals("8080:80")) {
			        resPort = "8080:80";
			        break;
			    } else {
			        try {
			        	String port1 = null;
			        	String port2 = null;
			        	port1 = resPort.substring(0, resPort.indexOf(":"));
			        	port2 = resPort.substring(resPort.lastIndexOf(":") + 1);
			        	Integer.valueOf(port1);
			            Integer.valueOf(port2);
			            break;
			        } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
			            System.out.println("Please enter a port in the format nnnn:nnnn");
			        }
			    }
			}
		  System.out.println("Setting port to: " + resPort + "\n");
		  
		  System.out.println("Moving on to SimBank container setup..." + "\n");
		  
		  System.out.println("SimBank container name: (default galasa): ");
		  sbName = scanner.nextLine();
		  if (sbName.isEmpty() || sbName.equals("galasa")) {
			  sbName = "galasa";
			  System.out.println("Using default container name...");
		  }
		  else {
			  System.out.println("Using new container name...");
		  }
		  
		  System.out.println("SimBank container name set to: " + sbName + "\n");
		  
		  System.out.println("Using previously set network name: " + netName + "\n");
		  
		  System.out.println("Restart policy - no, on-failure, always or unless-stopped (default always): ");
		  sbResPol = scanner.nextLine();
		  while (!(sbResPol.toUpperCase().equals("NO") || sbResPol.toUpperCase().equals("ON-FAILURE") || sbResPol.toUpperCase().equals("ALWAYS") || sbResPol.toUpperCase().equals("UNLESS-STOPPED") || sbResPol.isEmpty())) {	
		      System.out.println("I'm sorry but that isn't a valid input, please ensure you have entered a restart policy listed above");
		      sbResPol = scanner.nextLine();
			}
		  if (sbResPol.isEmpty() || sbResPol.equals("always")) {
			  sbResPol = "always";
			  System.out.println("Using default restart policy...");
		  }
		  else {
			  System.out.println("Using new restart policy");
		  }
		  
		  System.out.println("Restart policy set to: " + sbResPol + "\n");
		  
		  System.out.println("Port to publish to host: (default 2080:2080)");
		  while (true) {
			    sbPort = scanner.nextLine();
			    if (sbPort.isEmpty() || sbPort.equals("2080:2080")) {
			        sbPort = "2080:2080";
			        break;
			    } else {
			        try {
			        	String port1 = null;
			        	String port2 = null;
			        	port1 = sbPort.substring(0, sbPort.indexOf(":"));
			        	port2 = sbPort.substring(sbPort.lastIndexOf(":") + 1);
			        	Integer.valueOf(port1);
			            Integer.valueOf(port2);
			            break;
			        } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
			            System.out.println("Please enter a port in the format nnnn:nnnn");
			        }
			    }
			}
		  
		  System.out.println("Setting port to: " + sbPort + "\n");
		  
		  System.out.println("Docker config set up complete..." + "\n");
		  
		  System.out.println("Updating Ecosystem config file..." + "\n");
		  
		  ProcessBuilder processBuilder = new ProcessBuilder();

	        processBuilder.command("./scripts/docker-update.sh", subnet, gateway, apiName, netName, apiResPol, apiVol, apiVolSrc, apiVolTrg, apiPort, contName, contResPol, contVol, cpsName, cpsResPol, cpsVolSrc, cpsVolTrg, cpsPort, cdbiName, cdbiVolSrc, cdbiVolTrg, cdbName, cdbResPol, cdbEnv, cdbVolSrc, cdbVolTrg, cdbPort, resmonName, resmonResPol, resName, resResPol, resPort, sbName, sbResPol, sbPort);

	        try {

	            Process process = processBuilder.start();

				// blocked :(
	            BufferedReader reader =
	                    new BufferedReader(new InputStreamReader(process.getInputStream()));

	            String line;
	            while ((line = reader.readLine()) != null) {
	                System.out.println(line);
	            }

	            int exitCode = process.waitFor();
	            System.out.println("\nExited with error code : " + exitCode);

	        } catch (IOException e) {
	            e.printStackTrace();
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }

			PrintWriter writer = new PrintWriter("docker/config.properties", "UTF-8");
			  writer.println("subnet=" + subnet + "\n" + "\n" + "gateway=" + gateway + "\n" + "\n" + "networkName=" + netName + "\n" + "\n" + "apiContainerName=" + apiName + "\n" + "\n" + "apiResPol=" + apiResPol + "\n" + "\n" + "apiVolName=" + apiVol + "\n" + "\n" + "apiMountSource=" + apiVolSrc + "\n" + "\n" + "apiMountTarget=" + apiVolTrg + "\n" + "\n" + "apiPort=" + apiPort + "\n" + "\n" + "controllerContainerName=" + contName + "\n" + "\n" + "controllerResPol=" + contResPol + "\n" + "\n" + "controllerVolume=" + contVol + "\n" + "\n" + "cpsContainerName=" + cpsName + "\n" + "\n" + "cpsResPol=" + cpsResPol + "\n" + "\n" + "cpsMountSource=" + cpsVolSrc + "\n" + "\n" + "cpsMountTarget=" + cpsVolTrg + "\n" + "\n" + "cpsPort=" + cpsPort + "\n" + "\n" + "couchdbInitContainerName=" + cdbiName + "\n" + "\n" + "couchdbInitMountSource=" + cdbiVolSrc + "\n" + "\n" + "couchdbInitMountTarget=" + cdbiVolTrg + "\n" + "\n" + "rasContainerName=" + cdbName + "\n" + "\n" + "rasResPol=" + cdbResPol + "\n" + "\n" + "rasEnvFile=" + cdbEnv + "\n" + "\n" + "rasMountSource=" + cdbVolSrc + "\n" + "\n" + "rasMountTarget=" + cdbVolTrg + "\n" + "\n" + "rasPort=" + cdbPort + "\n" + "\n" + "resourceMonitorContainerName=" + resmonName + "\n" + "\n" + "resourceMonitorResolutionPolicy=" + resmonResPol + "\n" + "\n" + "resourceContainerName=" + resName + "\n" + "\n" + "resourceRestartPolicy=" + resResPol + "\n" + "\n" + "resourcePort=" + resPort + "\n" + "\n" + "simbankContainerName=" + sbName + "\n" + "\n" + "simbankResPol=" + sbResPol + "\n" + "\n" + "simbankPort=" + sbPort);
			
			writer.close();
		  
        }
		
		scanner.close();
		
		System.out.println("Executing ecosystem set up..." + "\n");

		ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command("./scripts/docker-setup.sh");

        try {

            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);
            

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



	}
	
}
