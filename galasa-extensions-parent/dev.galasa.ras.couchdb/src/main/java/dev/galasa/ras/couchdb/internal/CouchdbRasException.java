package dev.galasa.ras.couchdb.internal;

import dev.galasa.framework.spi.ResultArchiveStoreException;

public class CouchdbRasException extends ResultArchiveStoreException {

	private static final long serialVersionUID = 1L;

	/**
	 * @see java.lang.Exception#Exception()
	 */
	public CouchdbRasException() {
		super();
	}

	/**
	 * @see java.lang.Exception#Exception(String)
	 */
	public CouchdbRasException(String message) {
		super(message);
	}

	/**
	 * @see java.lang.Exception#Exception(Throwable)
	 */
	public CouchdbRasException(Throwable cause) {
		super(cause);
	}

	/**
	 * @see java.lang.Exception#Exception(String, Throwable)
	 */
	public CouchdbRasException(String message, Throwable cause) {
		super(message, cause);
	}

}
