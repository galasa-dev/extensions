package io.ejat.etcd3.internal;

import static com.google.common.base.Charsets.UTF_8;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.validation.constraints.NotNull;

import io.ejat.framework.spi.DynamicStatusStoreException;
import io.ejat.framework.spi.IDynamicStatusStore;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;

/**
 * This class implements the DSS store for the use of etcd3 as the k-v store. It is interacting with the Jetcd Client
 * offered from coreOs.
 * 
 * @author James Davies
 */
public class Etcd3DynamicStatusStore implements IDynamicStatusStore{
    private KV kvClient;

    /**
     * The constructure sets up a private KVClient that can be used by this class to interact with the etcd3 cluster.
     * 
     * The URI passed from the registration of the EtcdDSS should check that it is a valid URI.
     * @param dssUri - http:// uri for th etcd cluster.
     */
    public Etcd3DynamicStatusStore (URI dssUri) {
        Client client = Client.builder().endpoints(dssUri).build();
		kvClient = client.getKVClient();
    }
    
    /**
     * A simple put class that adds a single key value in etcd key value store.
     * 
     * @param key
     * @param value
     * @throws DynamicStatusStoreException
     */
    @Override
    public void put(@NotNull String key, @NotNull String value) throws DynamicStatusStoreException {
        ByteSequence bsKey = ByteSequence.from(key, UTF_8);
        ByteSequence bsValue = ByteSequence.from(value, UTF_8);

        CompletableFuture<PutResponse> response = kvClient.put(bsKey, bsValue);
        try {
            response.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new DynamicStatusStoreException("Could not put key-value", e);
        }
    }

    /**
     * A map put which allows a map of k-v pairs to the etcd store.
     * 
     * @param keyValues - a map of key value pairs.
     * @throws DynamicStatusStoreException
     */
    @Override
    public void put(@NotNull Map<String, String> keyValues) throws DynamicStatusStoreException {
        Txn txn = kvClient.txn();
        PutOption options = PutOption.DEFAULT;

        ArrayList<Op> ops = new ArrayList<>();
        for (String key: keyValues.keySet()) {
            ByteSequence obsKey = ByteSequence.from(key, UTF_8);
            ByteSequence obsValue = ByteSequence.from(keyValues.get(key), UTF_8);
            ops.add(Op.put(obsKey, obsValue, options));
        }
        Txn request = txn.Then(ops.toArray(new Op[ops.size()]));
        CompletableFuture<TxnResponse> response = request.commit();
        try {
			response.get();
		} catch (InterruptedException | ExecutionException e){
			Thread.currentThread().interrupt();
			throw new DynamicStatusStoreException("", e);
		}

    }

    /**
     * A put swap method does a check before setting the key-value. 
     * 
     * If the oldValue argument is not the current value of the key the newValue is NOT set into the store.
     * 
     * @param key
     * @param oldValue - the value the key should have for the change to succeeed
     * @param newValue - the new value to set too if the old value was correct
     * @return boolean - if the swap was successful
     * @throws DynamicStatusStoreException
     */
    @Override
    public boolean putSwap(@NotNull String key, String oldValue, @NotNull String newValue) throws DynamicStatusStoreException {
        ByteSequence bsKey = ByteSequence.from(key, UTF_8);
        ByteSequence bsNewValue = ByteSequence.from(newValue, UTF_8);

        Txn txn = kvClient.txn();

        Cmp cmp = null;
        if (oldValue == null) {
            cmp = new Cmp(bsKey, Cmp.Op.EQUAL, CmpTarget.version(0));
        } else {
            ByteSequence bsOldValue = ByteSequence.from(oldValue, UTF_8);
            cmp = new Cmp(bsKey, Cmp.Op.EQUAL, CmpTarget.value(bsOldValue));
        }

        PutOption option = PutOption.DEFAULT;

        Txn check = txn.If(cmp);
        Txn request = check.Then(Op.put(bsKey, bsNewValue, option));
        CompletableFuture<TxnResponse> response = request.commit();

        try {
            return response.get().isSucceeded();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new DynamicStatusStoreException("Put Swap failed", e);
        }
    }

    /**
     * A put swap method does a check before setting the key-value. 
     * 
     * If the oldValue argument is not the current value of the key the newValue is NOT set into the store.
     * If the old value was correct then a map of other key value pairs can be then set.
     * 
     * @param key
     * @param oldValue - the value the key should have for the change to succeeed
     * @param newValue - the new value to set too if the old value was correct
     * @param others - a map of all subsequent values to set if the condition was satisfied
     * @return boolean - if the swap was successful
     * @throws DynamicStatusStoreException
     */
    @Override
    public boolean putSwap(@NotNull String key, String oldValue, @NotNull String newValue, @NotNull Map<String, String> others) throws DynamicStatusStoreException {
        ByteSequence bsKey = ByteSequence.from(key, UTF_8);
        ByteSequence bsNewValue = ByteSequence.from(newValue, UTF_8);

        Txn txn = kvClient.txn();

        Cmp cmp = null;
        if (oldValue == null) {
            cmp = new Cmp(bsKey, Cmp.Op.EQUAL, CmpTarget.version(0));
        } else {
            ByteSequence bsOldValue = ByteSequence.from(oldValue, UTF_8);
            cmp = new Cmp(bsKey, Cmp.Op.EQUAL, CmpTarget.value(bsOldValue));
        }

        ArrayList<Op> ops = new ArrayList<>();

        PutOption option = PutOption.DEFAULT;
        ops.add(Op.put(bsKey, bsNewValue, option));

        for(Entry<String, String> entry: others.entrySet()) {
            ByteSequence obsKey = ByteSequence.from(entry.getKey(), UTF_8);
            ByteSequence obsValue = ByteSequence.from(entry.getValue(), UTF_8);

            ops.add(Op.put(obsKey, obsValue, option));
        }

        CompletableFuture<TxnResponse> response = txn.If(cmp).Then(ops.toArray(new Op[ops.size()])).commit();
        try {
            return response.get().isSucceeded();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new DynamicStatusStoreException("Put Swap failed", e);
        }
        
    }
    /**
     * A simple get method that retireves on value from one key
     * 
     * @param key 
     * @return String - the value of the key and null if not exsisting.
     * @throws DynamicStatusStoreException
     */
    @Override
    public String get(@NotNull String key) throws DynamicStatusStoreException {
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
			throw new DynamicStatusStoreException("Could not retrieve key.", e);
		}
    }

    /**
     * A get of all keys and value that start with a specified prefix. They are returned in a Map<String,String>
     * 
     * @param keyPrefix - the prefix for any key(s)
     * @return Map<String, String>
     * @throws DysnamicStatusStoreException
     */
    @Override
    public @NotNull Map<String, String> getPrefix(@NotNull String keyPrefix) throws DynamicStatusStoreException {
        ByteSequence bsPrefix = ByteSequence.from(keyPrefix, UTF_8);
        GetOption options = GetOption.newBuilder().withPrefix(bsPrefix).build();
        CompletableFuture<GetResponse> getFuture = kvClient.get(bsPrefix, options);
        Map<String,String> keyValues = new HashMap<>();

		try {
			GetResponse response = getFuture.get();
            List<KeyValue> kvs = response.getKvs();
            
			if (kvs.isEmpty()){
				return new HashMap<>();
            }
            
			for (KeyValue kv : kvs){
                keyValues.put(kv.getKey().toString(UTF_8), kv.getValue().toString(UTF_8));
            }
            return keyValues;
		} catch (InterruptedException | ExecutionException e){
			Thread.currentThread().interrupt();
			throw new DynamicStatusStoreException("Could not retrieve key.", e);
		}
    }
    
    /**
     * A Simple delete of a singe Key value pair.
     * 
     * @param key - the key to delete
     * @throws DynamicStatusStoreException
     */
    @Override
    public void delete(@NotNull String key) throws DynamicStatusStoreException {
        ByteSequence bsKey = ByteSequence.from(key, UTF_8);
        CompletableFuture<DeleteResponse> deleteFuture = kvClient.delete(bsKey);

		try {
			deleteFuture.get();
		} catch (InterruptedException | ExecutionException e){
			Thread.currentThread().interrupt();
			throw new DynamicStatusStoreException("Could not delete key.", e);
		}
    }

    /**
     * A delete of a set of provided keys and there corresponding values.
     * 
     * @param keys  a set of keys to delete
     * @throws DynamicStatusStoreException
     *
     */
    @Override
    public void delete(@NotNull Set<String> keys) throws DynamicStatusStoreException {
        Txn txn = kvClient.txn();
        DeleteOption options = DeleteOption.DEFAULT;

        ArrayList<Op> ops = new ArrayList<>();
        for (String key: keys) {
            ByteSequence obsKey = ByteSequence.from(key, UTF_8);
            ops.add(Op.delete(obsKey, options));
        }

        CompletableFuture<TxnResponse> response = txn.Then(ops.toArray(new Op[ops.size()])).commit();
        try {
			response.get();
		} catch (InterruptedException | ExecutionException e){
			Thread.currentThread().interrupt();
			throw new DynamicStatusStoreException("Could not delete key(s).", e);
		}
    }

    /**
     * A delete which removes all keys with a specified prefix and there corresponding values from the store.
     * 
     * @param keyPrefix - a string prefix that all the key(s) have in common
     * @throws DynamicStatusStoreException
     */
    @Override
    public void deletePrefix(@NotNull String keyPrefix) throws DynamicStatusStoreException {
        ByteSequence bsKey = ByteSequence.from(keyPrefix, UTF_8);
        DeleteOption options = DeleteOption.newBuilder().withPrefix(bsKey).build();
        CompletableFuture<DeleteResponse> deleteFuture = kvClient.delete(bsKey, options);

		try {
			deleteFuture.get();
		} catch (InterruptedException | ExecutionException e){
			Thread.currentThread().interrupt();
			throw new DynamicStatusStoreException("Could not delete key(s).", e);
		}
    }
    
}