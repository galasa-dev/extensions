package dev.cirillo.ras.couchdb.internal;

import java.nio.file.Path;

import io.ejat.framework.spi.IRunResult;
import io.ejat.framework.spi.ResultArchiveStoreException;
import io.ejat.framework.spi.teststructure.TestStructure;

public class CouchdbRunResult implements IRunResult {
	
	private final TestStructure   testStructure;
	private final Path            path;
	private final CouchdbRasStore store;
	
	public CouchdbRunResult(CouchdbRasStore store, TestStructure testStructure, Path path) {
		this.store         = store;
		this.testStructure = testStructure;
		this.path          = path;
	}

	@Override
	public TestStructure getTestStructure() throws ResultArchiveStoreException {
		return this.testStructure;
	}

	@Override
	public Path getArtifactsRoot() throws ResultArchiveStoreException {
		return this.path;
	}

	@Override
	public String getLog() throws ResultArchiveStoreException {
		return this.store.getLog(this.testStructure);
	}

}
