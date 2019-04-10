package io.ejat.etcd3.internal;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import static com.google.common.base.Charsets.UTF_8;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.ejat.framework.spi.ConfigurationPropertyStoreException;
import io.ejat.framework.spi.IConfigurationPropertyStore;

/**
 * This class impletements the CPS for etcd using the JETCD client.
 * 
 * @author James Davies
 */
public class Etcd3ConfigurationPropertyStore implements IConfigurationPropertyStore {
	private KV kvClient;

	/**
	 * This constructor create a priate KVClient from JETCD for store interactions.
	 * 
	 * @param cpsUri - location of the etcd
	 */
	public Etcd3ConfigurationPropertyStore(URI cpsUri) {
		Client client = Client.builder().endpoints(cpsUri).build();
		kvClient = client.getKVClient();
	}

	/**
	 * This is the only method for CPS as managers should only need to gegt properties from the CPS and
	 * not set or watch any.
	 * 
	 * @param key
	 */
	@Override
	public @Null String getProperty(@NotNull String key) throws ConfigurationPropertyStoreException {
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
			throw new ConfigurationPropertyStoreException("Could not retrieve key.", e);
		}
	}
}