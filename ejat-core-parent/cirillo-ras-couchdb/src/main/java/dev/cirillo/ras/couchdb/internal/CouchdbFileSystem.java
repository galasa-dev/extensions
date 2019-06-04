package dev.cirillo.ras.couchdb.internal;

import io.ejat.framework.spi.ras.ResultArchiveStoreFileSystem;
import io.ejat.framework.spi.ras.ResultArchiveStoreFileSystemProvider;

public class CouchdbFileSystem extends ResultArchiveStoreFileSystem {

	public CouchdbFileSystem(ResultArchiveStoreFileSystemProvider fileSystemProvider) {
		super(fileSystemProvider);
	}

}
