/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import dev.galasa.extensions.common.couchdb.pojos.Welcome;
import dev.galasa.extensions.common.api.HttpRequestFactory;
import dev.galasa.extensions.common.couchdb.CouchdbException;
import dev.galasa.extensions.common.couchdb.CouchdbValidator;
import dev.galasa.framework.spi.utils.GalasaGson;


public class CouchDbVersion implements Comparable<CouchDbVersion> {

    private int version ;
    private int release;
    private int modification;
    
    public CouchDbVersion(int version, int release, int modification) {
        this.version = version ;
        this.release = release ;
        this.modification = modification;    
    }

    public CouchDbVersion(String dotSeparatedVersion ) throws CouchdbException {
        Pattern vrm = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
        Matcher m = vrm.matcher(dotSeparatedVersion);

        if (!m.find()) {
            throw new CouchdbException("Invalid CouchDB version " + dotSeparatedVersion); // TODO: Make this error msg better.
        }

        try {
            this.version  = Integer.parseInt(m.group(1));
            this.release = Integer.parseInt(m.group(2));
            this.modification = Integer.parseInt(m.group(3));
        } catch (NumberFormatException e) {
            throw new CouchdbException("Unable to determine CouchDB version " + dotSeparatedVersion, e);
        }
    }

    public int getVersion() {
        return this.version ;
    }

    public int getRelease() {
        return this.release;
    }

    public int getModification() {
       return this.modification;
    }

    @Override
    public boolean equals(Object other) {
        boolean isSame = false ;

        if( other != null ) {
            if( other instanceof CouchDbVersion) {
                CouchDbVersion otherVersion = (CouchDbVersion)other;
                if(otherVersion.version == this.version){
                    if (otherVersion.release == this.release) {
                        if( otherVersion.modification == this.modification) {
                            isSame = true;   
                        }
                    }
                }
            }
        }

        return isSame;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (int) this.version;
        hash = 31 * hash + (int) this.release;
        hash = 31 * hash + (int) this.modification;
        return hash;
    }

    @Override
    public int compareTo(CouchDbVersion other) {
        int result ;
        if (this.version > other.version) {
            result = +1;
        } else if (this.version < other.version) {
            result = -1;
        } else if (this.release > other.release) {
            result = +1;
        } else if (this.release < other.release) {
            result = -1;
        } else if (this.modification > other.modification) {
            result = +1;
        } else if (this.modification < other.modification) {
            result = -1;
        } else {
            result = 0;
        }
        return result;
    }


}