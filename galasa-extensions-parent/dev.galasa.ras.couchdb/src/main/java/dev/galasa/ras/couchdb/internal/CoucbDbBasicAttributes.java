/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.ras.couchdb.internal;

import dev.galasa.framework.spi.ras.ResultArchiveStoreBasicAttributes;

public class CoucbDbBasicAttributes extends ResultArchiveStoreBasicAttributes {
	
	private final CouchdbArtifactPath path;

	public CoucbDbBasicAttributes(CouchdbArtifactPath path) {
		this.path = path;
	}
	
	@Override
	public boolean isRegularFile() {
		return !path.isDirectory();
	}
	
	@Override
	public boolean isDirectory() {
		return path.isDirectory();
	}

	@Override
	public long size() {
		return path.getLength();
	}
	
}
