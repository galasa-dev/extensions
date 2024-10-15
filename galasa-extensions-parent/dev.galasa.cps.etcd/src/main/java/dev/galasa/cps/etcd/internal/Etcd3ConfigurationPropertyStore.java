/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.cps.etcd.internal;

import static com.google.common.base.Charsets.UTF_8;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStore;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;

/**
 * This class impletements the CPS for etcd using the JETCD client.
 * 
 * @author James Davies
 * @author Matthew Chivers
 */
public class Etcd3ConfigurationPropertyStore extends Etcd3Store implements IConfigurationPropertyStore {

    public Etcd3ConfigurationPropertyStore(Client client) {
        super(client);
    }

    /**
     * This constructor create a priate KVClient from JETCD for store interactions.
     * 
     * @param cpsUri - location of the etcd
     */
    public Etcd3ConfigurationPropertyStore(URI cpsUri) {
        this(Client.builder().endpoints(cpsUri).build());
    }

    /**
     * This is the only method for CPS as managers should only need to get
     * properties from the CPS and not set or watch any.
     * 
     * @param key The property to get
     */
    @Override
    public @Null String getProperty(@NotNull String key) throws ConfigurationPropertyStoreException {
        String value = null;
        try {
            value = getValueFromStore(key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConfigurationPropertyStoreException("Could not retrieve key, interrupted", e);
        } catch (ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ConfigurationPropertyStoreException("Could not retrieve key", e);
        }
        return value;
    }

    @Override
    public @NotNull Map<String, String> getPrefixedProperties(@NotNull String prefix)
            throws ConfigurationPropertyStoreException {
        
        HashMap<String, String> returnValues = new HashMap<>();
        
        ByteSequence bsKey = ByteSequence.from(prefix, UTF_8);
        GetOption option = GetOption.newBuilder().withPrefix(bsKey).build();
        CompletableFuture<GetResponse> getFuture = kvClient.get(bsKey, option);  
        try {
            GetResponse response = getFuture.get();
            List<KeyValue> kvs = response.getKvs();
            for(KeyValue kv : kvs) {
                returnValues.put(kv.getKey().toString(UTF_8), kv.getValue().toString(UTF_8));
            }

            return returnValues;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConfigurationPropertyStoreException("Could not retrieve key, interrupted", e);
        } catch (ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ConfigurationPropertyStoreException("Could not retrieve key", e);
        }
    }

    @Override
    public void shutdown() throws ConfigurationPropertyStoreException {
        super.shutdownStore();
    }

    @Override
    public void setProperty(@NotNull String key, @NotNull String value) throws ConfigurationPropertyStoreException {
        try {
            setPropertyInStore(key, value);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ConfigurationPropertyStoreException("Could not set key and value.", e);
        }
    }
    
    @Override
    public void deleteProperty(@NotNull String key) throws ConfigurationPropertyStoreException {
        try {
            deletePropertyFromStore(key);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ConfigurationPropertyStoreException("Could not delete key.", e);
        }
    }

    @Override
    public Map<String, String> getPropertiesFromNamespace(String namespace) throws ConfigurationPropertyStoreException {
        ByteSequence bsNamespace = ByteSequence.from(namespace + ".", UTF_8);
        GetOption option = GetOption.newBuilder()
                .withSortField(GetOption.SortTarget.KEY)
                .withSortOrder(GetOption.SortOrder.DESCEND)
                .withRange(bsNamespace)
                .withPrefix(ByteSequence.from(namespace + ".", UTF_8))
                .build();

        CompletableFuture<GetResponse> futureResponse = client.getKVClient().get(bsNamespace, option);
        Map<String, String> results = new HashMap<>();
        try {
            GetResponse response = futureResponse.get();
            List<KeyValue> kvs = response.getKvs();
            for(KeyValue kv : kvs) {
                results.put(kv.getKey().toString(UTF_8), kv.getValue().toString(UTF_8));
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
        return results;
    }

    @Override
    public List<String> getNamespaces() throws ConfigurationPropertyStoreException {
        ByteSequence empty = ByteSequence.from("\0", UTF_8);
        GetOption option = GetOption.newBuilder()
                .withSortField(GetOption.SortTarget.KEY)
                .withSortOrder(GetOption.SortOrder.DESCEND)
                .withRange(empty)
                .build();

        CompletableFuture<GetResponse> futureResponse = client.getKVClient().get(empty, option);
        List<String> results = new ArrayList<>();
        try {
            GetResponse response = futureResponse.get();
            List<KeyValue> kvs = response.getKvs();
            for(KeyValue kv : kvs) {
                String key = kv.getKey().toString(UTF_8);
                // Ignore any etcd keys which don't have a '.' character.
                int indexOfFirstDot = key.indexOf(".");
                if (indexOfFirstDot >= 0) {
                    String namespace = key.substring(0,indexOfFirstDot);
                    if(!results.contains(namespace)) {
                        results.add(namespace);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
        return results;
    }



}
