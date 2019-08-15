package dev.galasa.core.ras.couchdb.internal;

import dev.galasa.framework.spi.ras.ResultArchiveStoreFileSystem;
import dev.galasa.framework.spi.ras.ResultArchiveStoreFileSystemProvider;

public class CouchdbFileSystem extends ResultArchiveStoreFileSystem {

	public CouchdbFileSystem(ResultArchiveStoreFileSystemProvider fileSystemProvider) {
		super(fileSystemProvider);
	}

}
