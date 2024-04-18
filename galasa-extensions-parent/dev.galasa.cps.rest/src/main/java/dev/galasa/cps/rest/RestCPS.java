/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.cps.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import dev.galasa.framework.api.beans.GalasaProperty;
import dev.galasa.framework.api.beans.GalasaPropertyData;
import dev.galasa.framework.api.beans.GalasaPropertyMetadata;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStore;
import dev.galasa.framework.spi.utils.GalasaGsonBuilder;

import static dev.galasa.cps.rest.Errors.*;
import dev.galasa.extensions.common.api.*;
import dev.galasa.extensions.common.impl.*;

/**
 * <p>
 * This class is used when the RestCPS class is being operated as the Key-Value
 * store for the Configuration property store. This class registers the
 * Configuration property store as the only CPS.
 * </p>
 */
public class RestCPS implements IConfigurationPropertyStore {

    private static final String REST_API_VERSION_CODED_AGAINST = "0.33.0" ; 

    public static final String URL_SCHEMA_REST = "galasacps";

    public static final boolean PRETTY_PRINTING_ENABLED = true;

    public static final String NULL_PREFIX = null ;
    public static final String NULL_INFIX = null ;
    public static final String NULL_SUFFIX = null ;

    public Gson gson = new GalasaGsonBuilder(PRETTY_PRINTING_ENABLED).getGson();

    private class PropertyName {
        String namespace;
        String simpleName;
    }

    private URI ecosystemRestApiUri ;
    private CloseableHttpClient apiClient;

    // The jwt we will use to contact the remote Galasa system.
    private String jwt; 

    public RestCPS(URI ecosystemRestApiUri) throws ConfigurationPropertyStoreException {
        this(ecosystemRestApiUri,new HttpClientFactoryImpl() , new JwtProviderEnvironment() );
    }

    public RestCPS(URI ecosystemRestApiUri, HttpClientFactory httpClientFactory, JwtProvider jwtProvider) throws ConfigurationPropertyStoreException {

        // Check that the URL passed starts with "galasacps"
        if (!ecosystemRestApiUri.toString().startsWith(URL_SCHEMA_REST+"://")) {
            String msg = ERROR_GALASA_API_SERVER_URI_DOESNT_START_WITH_REST_SCHEME.getMessage(ecosystemRestApiUri.toString(),URL_SCHEMA_REST+"://");
            throw new ConfigurationPropertyStoreException(msg);
        }

        // Replace the 'galasacps' part with 'https'
        try {
            this.ecosystemRestApiUri = new URI(ecosystemRestApiUri.toString().replaceAll(URL_SCHEMA_REST,"https"));
        } catch(URISyntaxException ex) {
            String msg = ERROR_GALASA_API_SERVER_URI_IS_INVALID.getMessage(ecosystemRestApiUri.toString(),ex.toString());
            throw new ConfigurationPropertyStoreException(msg, ex);
        }


        this.jwt = jwtProvider.getJwt();

        this.apiClient = httpClientFactory.createClient();
    }

    /**
     * <p>
     * This method implements the getProperty method from the framework property
     * file class, returning a string value from a key inside the property file, or
     * null if empty.
     * </p>
     * 
     * @param fullyQualifiedPropertyName The key of the property to get.
     * @throws ConfigurationPropertyStoreException - Something went wrong.
     */
    @Override
    public @Null String getProperty(@NotNull String fullyQualifiedPropertyName) throws ConfigurationPropertyStoreException {
        PropertyName propName = splitPropName(fullyQualifiedPropertyName);

        String propertyValueResult = null ;

        URI targetUri = constructGetPropertyURI(propName);
        HttpGet req = constructGetRequest(targetUri, this.jwt);

        // Note: The response is always closed properly.
        try ( CloseableHttpResponse response = (CloseableHttpResponse) this.apiClient.execute(req) ) {

            checkResponseHttpCode(response, ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTY_FAILED_NON_OK_STATUS, targetUri);

            GalasaProperty[] properties = extractPropertiesFromPayload(response, targetUri);

            if (properties.length>1) {
                String msg = Errors.ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTY_TOO_FEW_OR_MANY_RETURNED.getMessage(targetUri.toString(),1,properties.length);
                throw new ConfigurationPropertyStoreException(msg);
            } else if(properties.length==0) {
                // Property wasn't found, so return null from this API call.
                propertyValueResult = null ;
            } else {
                propertyValueResult = properties[0].getData().getValue();
            }

        } catch(IOException ioEx) {
            String msg = ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTY_FAILED.getMessage(targetUri.toString(),ioEx.getMessage());
            throw new ConfigurationPropertyStoreException(msg,ioEx);
        } 

        return propertyValueResult;
    }

    private void checkResponseHttpCode(CloseableHttpResponse response, Errors errorIfBad, URI targetUri) throws ConfigurationPropertyStoreException {
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();

        if (statusCode!=HttpStatus.SC_OK) {
            String msg = errorIfBad.getMessage(targetUri.toString(),Integer.toString(statusCode));
            throw new ConfigurationPropertyStoreException(msg);
        }
    }

    private GalasaProperty[] extractPropertiesFromPayload(CloseableHttpResponse response, URI targetUri) throws ConfigurationPropertyStoreException {

        GalasaProperty[] properties ;

        String contentJson ;
        try {
            InputStream inStream = response.getEntity().getContent();
            try {
                contentJson = IOUtils.toString(inStream, "UTF-8");
            } finally {
                // Make sure input streams are always closed.
                inStream.close();
            }

            properties = this.gson.fromJson(contentJson, GalasaProperty[].class);

        } catch(JsonSyntaxException syntaxEx) {
            String msg = ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTY_BAD_JSON_RETURNED.getMessage(targetUri.toString(),syntaxEx.getMessage());
            throw new ConfigurationPropertyStoreException(msg,syntaxEx);
        } catch(IOException ioEx) {
            String msg = ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTY_FAILED.getMessage(targetUri.toString(),ioEx.getMessage());
            throw new ConfigurationPropertyStoreException(msg,ioEx);
        } 

        checkPropertiesAreWellFormed(properties);

        return properties; 
    }

    /**
     * Double-check that the server returned what we expected. Belt and braces approach.
     * @param properties The properties we want to check.
     * @throws ConfigurationPropertyStoreException Something was wrong with the properties returned from the Galasa server.
     */
    private void checkPropertiesAreWellFormed(GalasaProperty[] properties) throws ConfigurationPropertyStoreException {
        for( GalasaProperty property : properties ) {
            GalasaPropertyMetadata metadata = property.getMetadata();
            if (metadata==null) {
                String msg = Errors.ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_METADATA.getMessage();
                throw new ConfigurationPropertyStoreException(msg);
            }
            String namespace = metadata.getNamespace();
            if (namespace==null) {
                String msg = Errors.ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_NAMESPACE.getMessage();
                throw new ConfigurationPropertyStoreException(msg);
            }
            String name = metadata.getName();
            if (name==null) {
                String msg = Errors.ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_NAME.getMessage();
                throw new ConfigurationPropertyStoreException(msg);
            }
            GalasaPropertyData data = property.getData();
            if (data==null) {
                String msg = Errors.ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_DATA.getMessage();
                throw new ConfigurationPropertyStoreException(msg);
            }
            String value = data.getValue();
            if (value==null) {
                String msg = Errors.ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_VALUE.getMessage();
                throw new ConfigurationPropertyStoreException(msg);
            }
        }
    }

    private HttpGet constructGetRequest(URI targetUri, String jwt) {
        HttpGet req = new HttpGet();
        req.setURI(targetUri);
        req.addHeader(HttpHeaders.AUTHORIZATION,"Bearer "+this.jwt);
        req.addHeader(HttpHeaders.CONTENT_TYPE,"application/json");
        req.addHeader(HttpHeaders.ACCEPT,"application/json");

        // Tell the server which version of the API we are coded and tested against.
        req.addHeader("ClientApiVersion",REST_API_VERSION_CODED_AGAINST);
        return req;
    }

    private URI constructGetPropertyURI( PropertyName propName) throws ConfigurationPropertyStoreException {
        URI targetUri ;
        try {
            String baseUri = this.ecosystemRestApiUri +"/cps/"+propName.namespace+"/properties/"+propName.simpleName;
            targetUri = new URIBuilder(baseUri)
                .build();
        } catch(URISyntaxException ex ) {
            String msg = ERROR_GALASA_CONSTRUCTED_URL_TO_REMOTE_CPS_INVALID_SYNTAX.getMessage(this.ecosystemRestApiUri.toString(),ex.getMessage());
            throw new ConfigurationPropertyStoreException(msg,ex);
        }
        return targetUri;
    }

    /**
     * @param fullyQualifiedPropertyName Of the form namespace.propertyName
     * @return The property name broken into pieces.
     */
    private PropertyName splitPropName(String fullyQualifiedPropertyName ) {
        String[] parts = fullyQualifiedPropertyName.split("[.]");

        PropertyName propName = new PropertyName();
        propName.namespace = parts[0];

        // The simple property name is got by stripping out the leading namespace, plus the '.' separator character.
        propName.simpleName = fullyQualifiedPropertyName.substring( propName.namespace.length()+1 );

        return propName;
    }
    
    @Override
    public @NotNull Map<String, String> getPrefixedProperties(@NotNull String prefixWithNamespace)
            throws ConfigurationPropertyStoreException {
        PropertyName propName = splitPropName(prefixWithNamespace);

        String namespace = propName.namespace;
        String prefix = propName.simpleName;

        Map<String,String> results = new HashMap<String,String>();

        URI targetUri = calculateQueryPropertyUri(namespace, prefix, NULL_SUFFIX, NULL_INFIX);
        HttpGet req = constructGetRequest(targetUri, this.jwt);

        // Note: The response is always closed properly.
        try ( CloseableHttpResponse response = (CloseableHttpResponse) this.apiClient.execute(req) ) {

            checkResponseHttpCode(response, ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTIES_FAILED_NON_OK_STATUS, targetUri);

            GalasaProperty[] properties = extractPropertiesFromPayload(response, targetUri);

            results = propertiesToMap(properties);

        } catch(IOException ioEx) {
            String msg = ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTIES_FAILED.getMessage(targetUri.toString(),ioEx.getMessage());
            throw new ConfigurationPropertyStoreException(msg,ioEx);
        } 
        return results;
    }

    private URI calculateQueryPropertyUri( String namespace, String prefix, String suffix, String infix) throws ConfigurationPropertyStoreException {
        URI targetUri ;
        try {
            // format is /cps/{namespace}/properties?prefix=&suffix=&infix="
            String baseUri = this.ecosystemRestApiUri +"/cps/"+namespace+"/properties";
            URIBuilder uriBuilder = new URIBuilder(baseUri);
            if (prefix!=null) {
                uriBuilder = uriBuilder.addParameter("prefix", prefix);
            }
            if (suffix!=null) {
                uriBuilder = uriBuilder.addParameter("suffix", suffix);
            }
            if (infix!=null) {
                uriBuilder = uriBuilder.addParameter("infix", infix);
            }
            targetUri = uriBuilder.build();
            
        } catch(URISyntaxException ex ) {
            String msg = ERROR_GALASA_CONSTRUCTED_URL_TO_REMOTE_CPS_INVALID_SYNTAX.getMessage(this.ecosystemRestApiUri.toString(),ex.getMessage());
            throw new ConfigurationPropertyStoreException(msg,ex);
        }
        return targetUri;
    }

    /**
     * <p>
     * This method implements the setProperty method from the framework property
     * file class.
     * </p>
     * 
     * @param key The key of the property to be set
     * @param value The value we set the property to
     * @throws ConfigurationPropertyStoreException  - Something went wrong.
     */
    @Override
    public void setProperty(@NotNull String key, @NotNull String value) throws ConfigurationPropertyStoreException {
        String msg = ERROR_GALASA_CPS_SET_OPERATIONS_NOT_PERMITTED.getMessage();
        throw new ConfigurationPropertyStoreException(msg);
    }
    
    @Override
    public void deleteProperty(@NotNull String key) throws ConfigurationPropertyStoreException {
        String msg = ERROR_GALASA_CPS_DELETE_OPERATIONS_NOT_PERMITTED.getMessage();
        throw new ConfigurationPropertyStoreException(msg);
    }

    /**
     * <p>
     * This method returns all properties for a given namespace from the framework property
     * file class.
     * </p>
     * 
     * @param namespace The namespace we want to get the property from.
     * @return The properties returned.
     */
    @Override
    public Map<String,String> getPropertiesFromNamespace(String namespace) {

        Map<String,String> results = new HashMap<String,String>();

        // TODO: The interface is wrong. It should allow throwing of an exception.
        try {
            URI targetUri = calculateQueryPropertyUri(namespace, NULL_PREFIX, NULL_SUFFIX, NULL_INFIX);
            HttpGet req = constructGetRequest(targetUri, this.jwt);

            // Note: The response is always closed properly.
            try ( CloseableHttpResponse response = (CloseableHttpResponse) this.apiClient.execute(req) ) {

                checkResponseHttpCode(response, ERROR_GALASA_REST_CALL_TO_GET_ALL_CPS_PROPERTIES_NON_OK_STATUS, targetUri);
                GalasaProperty[] properties = extractPropertiesFromPayload(response, targetUri);
                results = propertiesToMap(properties);

            } catch(IOException ioEx) {
                String msg = ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTIES_FAILED.getMessage(targetUri.toString(),ioEx.getMessage());
                throw new ConfigurationPropertyStoreException(msg,ioEx);
            }

        } catch ( ConfigurationPropertyStoreException ex) {
            // TODO: Temporarily turn the exception into a runtime exception... as that can be used and still maintain the interface.
            throw new RuntimeException(ex);
        }
        return results;
    }

    private Map<String,String> propertiesToMap(GalasaProperty[] properties) throws ConfigurationPropertyStoreException {
        Map<String,String> results = new HashMap<String,String>();

        for( GalasaProperty property : properties ) {
            // Calculate the fully qualified property name from the metadata available.
            GalasaPropertyMetadata metadata = property.getMetadata();
            String namespace = metadata.getNamespace();
            String name = metadata.getName();
            String fullyQualifiedPropName = namespace + "." + name;

            GalasaPropertyData data = property.getData();
            String value = data.getValue();

            results.put(fullyQualifiedPropName, value);
        }
        return results;
    }

    /**
     * <p>
     * Return all Namespaces for the framework property file
     * </p>
     * 
     * @return - List of namespaces
     */
    @Override
    public List<String> getNamespaces() {
        List<String> results = new ArrayList<String>();

        // TODO: The interface is wrong. It should allow throwing of an exception.
        try {
            URI targetUri = calculateQueryNamespaceUri();
            HttpGet req = constructGetRequest(targetUri, this.jwt);

            // Note: The response is always closed properly.
            try ( CloseableHttpResponse response = (CloseableHttpResponse) this.apiClient.execute(req) ) {

                checkResponseHttpCode(response, ERROR_GALASA_REST_CALL_TO_GET_ALL_CPS_NAMESPACES_NON_OK_STATUS, targetUri);
                results = extractNamespacesFromPayload(response, targetUri);

            } catch(IOException ioEx) {
                String msg = ERROR_GALASA_REST_CALL_TO_GET_CPS_NAMESPACES_FAILED.getMessage(targetUri.toString(),ioEx.getMessage());
                throw new ConfigurationPropertyStoreException(msg,ioEx);
            } 
        } catch ( ConfigurationPropertyStoreException ex) {
            // TODO: Temporarily turn the exception into a runtime exception... as that can be used and still maintain the interface.
            throw new RuntimeException(ex);
        }
        return results;
    }

    private List<String> extractNamespacesFromPayload(CloseableHttpResponse response, URI targetUri) throws ConfigurationPropertyStoreException {

        List<String> namespaces = new ArrayList<String>();

        String contentJson ;
        try {
            InputStream inStream = response.getEntity().getContent();
            try {
                contentJson = IOUtils.toString(inStream, "UTF-8");
            } finally {
                // Make sure input streams are always closed.
                inStream.close();
            }

            String[] namespacesArray = this.gson.fromJson(contentJson, String[].class);

            for(String namespace : namespacesArray) {
                namespaces.add(namespace);
            }

        } catch(JsonSyntaxException syntaxEx) {
            String msg = ERROR_GALASA_REST_CALL_TO_GET_CPS_NAMESPACES_BAD_JSON_RETURNED.getMessage(targetUri.toString(),syntaxEx.getMessage());
            throw new ConfigurationPropertyStoreException(msg,syntaxEx);
        } catch(IOException ioEx) {
            String msg = ERROR_GALASA_REST_CALL_TO_GET_CPS_NAMESPACES_FAILED.getMessage(targetUri.toString(),ioEx.getMessage());
            throw new ConfigurationPropertyStoreException(msg,ioEx);
        } 

        return namespaces; 
    }

    private URI calculateQueryNamespaceUri() throws ConfigurationPropertyStoreException {
        URI targetUri ;
        try {
            String baseUri = this.ecosystemRestApiUri +"/cps/namespace/";
            targetUri = new URI(baseUri);            
        } catch(URISyntaxException ex ) {
            String msg = ERROR_GALASA_CONSTRUCTED_URL_TO_REMOTE_CPS_INVALID_SYNTAX.getMessage(this.ecosystemRestApiUri.toString(),ex.getMessage());
            throw new ConfigurationPropertyStoreException(msg,ex);
        }
        return targetUri;
    }


    @Override
    public void shutdown() throws ConfigurationPropertyStoreException {
        try {
            if (this.apiClient!=null) {
                this.apiClient.close();
            }
        } catch(IOException ioEx) {
            String msg = Errors.ERROR_GALASA_CPS_SHUTDOWN_FAILED.getMessage(ioEx.getMessage());
            throw new ConfigurationPropertyStoreException(msg,ioEx);
        }
    }
}
