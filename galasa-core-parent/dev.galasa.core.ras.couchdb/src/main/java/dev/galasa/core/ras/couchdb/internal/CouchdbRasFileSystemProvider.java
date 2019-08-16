package dev.galasa.core.ras.couchdb.internal;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import dev.galasa.ResultArchiveStoreContentType;
import dev.galasa.framework.spi.ras.ResultArchiveStoreFileSystemProvider;

public class CouchdbRasFileSystemProvider extends ResultArchiveStoreFileSystemProvider {

	private final HashMap<Path, ResultArchiveStoreContentType> contentTypes = new HashMap<>();

	private final HashSet<CouchdbArtifactPath> paths = new HashSet<>();

	private final CouchdbRasStore couchdbRasStore;

	protected CouchdbRasFileSystemProvider(FileStore fileSystemStore, CouchdbRasStore couchdbRasStore) {
		super(fileSystemStore, null);
		fileSystem = new CouchdbFileSystem(this);
		this.couchdbRasStore = couchdbRasStore;
		paths.add(new CouchdbArtifactPath(fileSystem, "/"));
	}
	
	protected void addPath(CouchdbArtifactPath path) {
		path = path.toAbsolutePath();
		paths.add(path);
		
		CouchdbArtifactPath parentPath = (CouchdbArtifactPath) path.getParent();
		while(parentPath != null && !paths.contains(parentPath)) {
			paths.add(parentPath);
			parentPath = parentPath.getParent();
		}
		
	}


	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {

		boolean write = options.contains(StandardOpenOption.WRITE);

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

			return new CouchdbRasWriteByteChannel(this.couchdbRasStore, absolute, contentType, passThroughOptions, attrs);
		} else {
			CouchdbArtifactPath cdbPath = (CouchdbArtifactPath) path;
			Path cachePath = Files.createTempFile("galasa_couchdb", "temp");
			try {
				couchdbRasStore.retrieveArtifact(cdbPath, cachePath);
			} catch (CouchdbRasException e) {
				throw new IOException("Unable to retrieve artifact", e);
			}
			return new CouchdbRasReadByteChannel(cachePath);
		}
	}


	public Path getRoot() {
		return new CouchdbArtifactPath(this.getActualFileSystem(), "/");
	}
	
	@Override
	public Path getPath(URI uri) {
		return new CouchdbArtifactPath(this.getActualFileSystem(), uri.toString());
	}
	
	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		return new CouchdbDirectoryStream(dir, filter, paths);		
	}
	
    @SuppressWarnings("unchecked") // NOSONAR
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
            throws IOException {
    	if (path instanceof CouchdbArtifactPath) {
    		return (A)((CouchdbArtifactPath)path).readAttributes();
    	}
    	return null;
    }

}
