/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.extensions.common.couchdb;

import dev.galasa.framework.spi.auth.AuthStoreException;

public class CouchdbAuthStoreException extends AuthStoreException {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    public CouchdbAuthStoreException() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public CouchdbAuthStoreException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public CouchdbAuthStoreException(Throwable cause) {
        super(cause);
    }

    /**
     * {@inheritDoc}
     */
    public CouchdbAuthStoreException(String message, Throwable cause) {
        super(message, cause);
    }

}
