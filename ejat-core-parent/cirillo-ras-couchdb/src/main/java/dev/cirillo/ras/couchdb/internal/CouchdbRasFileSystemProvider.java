package dev.cirillo.ras.couchdb.internal;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import io.ejat.ResultArchiveStoreContentType;
import io.ejat.framework.spi.ras.ResultArchiveStoreFileSystemProvider;

public class CouchdbRasFileSystemProvider extends ResultArchiveStoreFileSystemProvider {

	private final HashMap<Path, ResultArchiveStoreContentType> contentTypes = new HashMap<>();
	
	private final CouchdbRasStore couchdbRasStore;

	protected CouchdbRasFileSystemProvider(FileStore fileSystemStore, CouchdbRasStore couchdbRasStore) {
		super(fileSystemStore);
		this.couchdbRasStore = couchdbRasStore;
	}


	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {

		boolean write = options.contains(StandardOpenOption.WRITE);

		if (!write) {
			throw new IOException("Does not support read yet");
		}


		if (write) {
			Path absolute = path.toAbsolutePath();
			ResultArchiveStoreContentType contentType = null;
			for(FileAttribute<?> attr : attrs) {
				if (attr instanceof ResultArchiveStoreContentType) {
					contentType = (ResultArchiveStoreContentType) attr;
					contentTypes.put(absolute, contentType);
				}
			}
			if (contentType == null) {
				contentType = contentTypes.get(absolute);
			}
			
			HashSet<OpenOption> passThroughOptions = new HashSet<>();
			passThroughOptions.add(StandardOpenOption.WRITE);
			passThroughOptions.add(StandardOpenOption.TRUNCATE_EXISTING);

			return new CouchdbRasByteChannel(this.couchdbRasStore, absolute, contentType, passThroughOptions, attrs);
		}

		return null;
	}

}
