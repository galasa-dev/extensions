/*
 * Copyright contributors to the Galasa project
 */
package dev.galasa.ras.couchdb.internal;

import dev.galasa.framework.spi.ResultArchiveStoreException;

public class CouchdbRasException extends ResultArchiveStoreException {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    public CouchdbRasException() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public CouchdbRasException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public CouchdbRasException(Throwable cause) {
        super(cause);
    }

    /**
     * {@inheritDoc}
     */
    public CouchdbRasException(String message, Throwable cause) {
        super(message, cause);
    }

}
