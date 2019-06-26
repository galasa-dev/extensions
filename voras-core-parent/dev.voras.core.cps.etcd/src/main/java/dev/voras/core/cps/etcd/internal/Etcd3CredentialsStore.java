package dev.voras.core.cps.etcd.internal;

import static com.google.common.base.Charsets.UTF_8;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import dev.voras.ICredentials;
import dev.voras.framework.spi.creds.CredentialsException;
import dev.voras.framework.spi.creds.CredentialsToken;
import dev.voras.framework.spi.creds.CredentialsUsername;
import dev.voras.framework.spi.creds.CredentialsUsernamePassword;
import dev.voras.framework.spi.creds.ICredentialsStore;
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
	private KV kvClient;

	/**
	 * This constructor instantiates the Key value client that can retrieve values from the etcd
	 * store.
	 * 
	 * @param etcd - URI location of ETCD store.
	 */
	public Etcd3CredentialsStore(URI etcd) {
			Client client = Client.builder().endpoints(etcd).build();
			kvClient = client.getKVClient();
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
			return new CredentialsToken(token);       
		}

		String username = get("secure.credentials." + credentialsId + ".username");
		String password = get("secure.credentials." + credentialsId + ".password");
		
		if (username == null) {
			return null;
		}

		if (password == null) {
			return new CredentialsUsername(username);
		}
		return new CredentialsUsernamePassword(username, password);
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
	
}