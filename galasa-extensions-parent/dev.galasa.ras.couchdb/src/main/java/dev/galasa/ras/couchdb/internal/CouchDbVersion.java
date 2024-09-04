/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.galasa.extensions.common.couchdb.CouchdbException;


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

    @Override
    public String toString() {
        StringBuilder buffer  = new StringBuilder();

        buffer.append(this.version);
        buffer.append('.');
        buffer.append(this.release);
        buffer.append('.');
        buffer.append(this.modification);

        return buffer.toString();
    }

}