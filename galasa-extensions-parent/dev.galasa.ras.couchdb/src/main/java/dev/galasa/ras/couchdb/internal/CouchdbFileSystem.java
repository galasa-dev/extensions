/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.ras.couchdb.internal;

import dev.galasa.framework.spi.ras.ResultArchiveStoreFileSystem;

public class CouchdbFileSystem extends ResultArchiveStoreFileSystem {
	
	private final CouchdbRasFileSystemProvider actualFileSystemProvider;

	public CouchdbFileSystem(CouchdbRasFileSystemProvider fileSystemProvider) {
		super(fileSystemProvider);
		this.actualFileSystemProvider = fileSystemProvider;
	}
	
	public CouchdbRasFileSystemProvider getActualFileSystemProvider() {
		return this.actualFileSystemProvider;
	}

}
