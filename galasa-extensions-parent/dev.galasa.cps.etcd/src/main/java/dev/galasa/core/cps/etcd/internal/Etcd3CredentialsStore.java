package dev.galasa.core.cps.etcd.internal;

import static com.google.common.base.Charsets.UTF_8;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.crypto.spec.SecretKeySpec;

import dev.galasa.ICredentials;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.creds.CredentialsException;
import dev.galasa.framework.spi.creds.CredentialsToken;
import dev.galasa.framework.spi.creds.CredentialsUsername;
import dev.galasa.framework.spi.creds.CredentialsUsernamePassword;
import dev.galasa.framework.spi.creds.CredentialsUsernameToken;
import dev.galasa.framework.spi.creds.ICredentialsStore;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;

/**
 * This class implements the credential store in a etcd store. Usernames, Passwords and 
 * tokens can be retrieved from etc using the correct key format:
 * 
 * "secure.credentials.<SomeCredentialId>.username"
 * 
 * @author James Davies
 */
public class Etcd3CredentialsStore implements ICredentialsStore {
	private final Client client;
	private final KV kvClient;
	private final SecretKeySpec key;

	/**
	 * This constructor instantiates the Key value client that can retrieve values from the etcd
	 * store.
	 * 
	 * @param etcd - URI location of ETCD store.
	 */
	public Etcd3CredentialsStore(IFramework framework, URI etcd) throws CredentialsException {
		try {
			client = Client.builder().endpoints(etcd).build();
			kvClient = client.getKVClient();
			
			IConfigurationPropertyStoreService cpsService = framework.getConfigurationPropertyService("secure");         
			String encryptionKey = cpsService.getProperty("credentials.file", "encryption.key");
			if (encryptionKey != null) {
				key = createKey(encryptionKey);
			} else {
				key = null;
			}
		} catch(Exception e) {
			throw new CredentialsException("Unable to initialise the etcd credentials store", e);
		}
	}
	
	/**
	 * This method checks for the three available credential types in the credentials stores and returns the appropiate response.
	 * 
	 * A token will be returned on its own as a toekn object, a username can be passed back on its own, or with a passowrd
	 * if there is one available.
	 * 
	 * @param credentialsId a key which contains the credential id.
	 * @throws CredentialsException
	 */
	public ICredentials getCredentials(String credentialsId) throws CredentialsException {
		String token = get("secure.credentials." + credentialsId + ".token");
		if (token != null) {
			String username = get("secure.credentials." + credentialsId + ".username");

			if (username != null) {
				return new CredentialsUsernameToken(key, username, token);       
			}
			return new CredentialsToken(key, token);       
		}

		String username = get("secure.credentials." + credentialsId + ".username");
		String password = get("secure.credentials." + credentialsId + ".password");

		if (username == null) {
			return null;
		}

		if (password == null) {
			return new CredentialsUsername(key, username);
		}

		return new CredentialsUsernamePassword(key, username, password);
	}	

	/**
	 * A get method which interacts with the etcd client correctly.
	 * 
	 * @param key - the full key to search for with CredId and type of credential.
	 * @return String vaule response.
	 * @throws CredentialsException
	 */
	private String get(String key) throws CredentialsException {
		ByteSequence bsKey = ByteSequence.from(key, UTF_8);
		CompletableFuture<GetResponse> getFuture = kvClient.get(bsKey);
		try {
			GetResponse response = getFuture.get();
			List<KeyValue> kvs = response.getKvs();
			if (kvs.isEmpty()){
				return null;
			}
			return kvs.get(0).getValue().toString(UTF_8);
		} catch (InterruptedException | ExecutionException e){
			Thread.currentThread().interrupt();
			throw new CredentialsException("Could not retrieve key.", e);
		}
	}
	
	private static SecretKeySpec createKey(String secret) throws UnsupportedEncodingException, NoSuchAlgorithmException {	
		byte[] key = secret.getBytes("UTF-8");
		MessageDigest sha = MessageDigest.getInstance("SHA-256");
		key = sha.digest(key);
		return new SecretKeySpec(key, "AES");
	}

	@Override
	public void shutdown() throws CredentialsException {
		kvClient.close();
		client.close();
	}

	
}