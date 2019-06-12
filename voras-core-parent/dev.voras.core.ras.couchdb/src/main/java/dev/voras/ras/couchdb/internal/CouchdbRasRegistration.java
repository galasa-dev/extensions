package dev.voras.ras.couchdb.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.osgi.service.component.annotations.Component;

import dev.voras.framework.spi.IFramework;
import dev.voras.framework.spi.IFrameworkInitialisation;
import dev.voras.framework.spi.IResultArchiveStoreService;
import dev.voras.framework.spi.IRunResult;
import dev.voras.framework.spi.ResultArchiveStoreException;
import dev.voras.framework.spi.teststructure.TestStructure;


@Component(service = { IResultArchiveStoreService.class })
public class CouchdbRasRegistration implements IResultArchiveStoreService {

	private IFramework                     framework;                    
	private URI                            rasUri;
	
	@Override
	public void initialise(@NotNull IFrameworkInitialisation frameworkInitialisation)
			throws ResultArchiveStoreException {
		this.framework = frameworkInitialisation.getFramework();

		// *** See if this RAS is to be activated, will eventually allow multiples of
		// itself
		final List<URI> rasUris = frameworkInitialisation.getResultArchiveStoreUris();
		for (final URI uri : rasUris) {
			if ("couchdb".equals(uri.getScheme())) {
				if (this.rasUri != null) {
					throw new ResultArchiveStoreException(
							"The CouchDB RAS currently does not support multiple instances of itself");
				}
				this.rasUri = uri;
			}
		}
		
		if (this.rasUri == null) {
			return;
		}

		//*** Test we can contact the CouchDB server
		CouchdbRasStore store;
		try {
			store = new CouchdbRasStore(framework, new URI(this.rasUri.toString().substring(8)));
		} catch (URISyntaxException e) {
			throw new ResultArchiveStoreException("Invalid CouchDB URI " + this.rasUri.getPath());
		}
		
		//*** All good, register it
		frameworkInitialisation.registerResultArchiveStoreService(store);
	}

	@Override
	public void writeLog(@NotNull String message) throws ResultArchiveStoreException {
	}

	@Override
	public void writeLog(@NotNull List<String> messages) throws ResultArchiveStoreException {
	}

	@Override
	public void updateTestStructure(@NotNull TestStructure testStructure) throws ResultArchiveStoreException {
	}

	@Override
	public Path getStoredArtifactsRoot() {
		return null;
	}

	@Override
	public void flush() {
	}

	@Override
	public void shutdown() {
	}
	
	@Override
	public List<IRunResult> getRuns(String runName) throws ResultArchiveStoreException {
		throw new UnsupportedOperationException("Should not have been called");  //  temporary until convert to proper registration
	}



}
