package dev.voras.ras.couchdb.internal;

import dev.voras.framework.spi.ras.ResultArchiveStoreFileSystem;
import dev.voras.framework.spi.ras.ResultArchiveStoreFileSystemProvider;

public class CouchdbFileSystem extends ResultArchiveStoreFileSystem {

	public CouchdbFileSystem(ResultArchiveStoreFileSystemProvider fileSystemProvider) {
		super(fileSystemProvider);
	}

}
