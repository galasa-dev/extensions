/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

import java.nio.file.Path;

import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.ResultArchiveStoreException;
import dev.galasa.framework.spi.teststructure.TestStructure;
import dev.galasa.ras.couchdb.internal.pojos.TestStructureCouchdb;

public class CouchdbRunResult implements IRunResult {

    private final TestStructureCouchdb   testStructure;
    private final Path            path;
    private final CouchdbRasStore store;

    public CouchdbRunResult(CouchdbRasStore store, TestStructureCouchdb testStructure, Path path) {
        this.store = store;
        if (testStructure == null) {
            this.testStructure = new TestStructureCouchdb();
        } else {
            this.testStructure = testStructure;
        }
        this.path = path;
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

	@Override
	public void discard() throws ResultArchiveStoreException {
        CouchdbDirectoryService storeService =  (CouchdbDirectoryService) store.getDirectoryServices().get(0);
        storeService.discardRun(this.testStructure._id);
	}

    @Override
    public String getRunId() {
        return "cdb-" + this.testStructure._id;
    }

}
