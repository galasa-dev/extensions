/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.mocks;

import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;

public class MockStatusLine implements StatusLine {

    // Status code defaults to OK.
    private int code = HttpStatus.SC_OK;

    @Override
    public ProtocolVersion getProtocolVersion() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProtocolVersion'");
    }

    @Override
    public String getReasonPhrase() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReasonPhrase'");
    }

    public void setStatusCode(int newCode) {
        this.code = newCode ;
    }

    @Override
    public int getStatusCode() {
        return this.code;
    }
    
}