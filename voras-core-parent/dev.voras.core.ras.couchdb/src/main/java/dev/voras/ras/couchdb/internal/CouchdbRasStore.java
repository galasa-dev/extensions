package dev.voras.ras.couchdb.internal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.voras.ras.couchdb.internal.pojos.Artifacts;
import dev.voras.ras.couchdb.internal.pojos.Find;
import dev.voras.ras.couchdb.internal.pojos.FoundRuns;
import dev.voras.ras.couchdb.internal.pojos.LogLines;
import dev.voras.ras.couchdb.internal.pojos.PutPostResponse;
import dev.voras.ras.couchdb.internal.pojos.Selector;
import dev.voras.ras.couchdb.internal.pojos.TestStructureCouchdb;
import dev.voras.ras.couchdb.internal.pojos.Welcome;
import dev.voras.framework.spi.IFramework;
import dev.voras.framework.spi.IFrameworkInitialisation;
import dev.voras.framework.spi.IResultArchiveStoreService;
import dev.voras.framework.spi.IRun;
import dev.voras.framework.spi.IRunResult;
import dev.voras.framework.spi.ResultArchiveStoreException;
import dev.voras.framework.spi.ras.ResultArchiveStoreFileStore;
import dev.voras.framework.spi.teststructure.TestStructure;
import dev.voras.framework.spi.utils.CirilloGsonBuilder;

public class CouchdbRasStore implements IResultArchiveStoreService {

	private final Log        logger = LogFactory.getLog(getClass());

	private static final Charset           UTF8 = Charset.forName("utf-8");

	private final IFramework                     framework;                      // NOSONAR
	private final URI                            rasUri;

	private final CloseableHttpClient            httpClient;

	private final Gson                           gson = CirilloGsonBuilder.build();

	private final CouchdbRasFileSystemProvider   provider;

	private final IRun                           run;
	private String                               runDocumentId;
	private String                               runDocumentRevision;

	private long                                 logOrder = 0;

	private final ArrayList<String>              logCache = new ArrayList<>(100);

	private ArrayList<String>                    logIds = new ArrayList<>();
	private ArrayList<String>                    artifactDocumentId = new ArrayList<>();;
	private String                               artifactDocumentRev;

	private TestStructure                        lastTestStructure;

	public CouchdbRasStore(IFramework framework, URI rasUri) throws CouchdbRasException {
		this.framework = framework;
		this.rasUri    = rasUri;

		//*** Validate the connection to the server and it's version
		this.httpClient = HttpClients.createDefault();

		HttpGet httpGet = new HttpGet(rasUri);

		try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
				throw new CouchdbRasException("Validation failed to CouchDB server - " + statusLine.toString());
			}

			HttpEntity entity = response.getEntity();
			Welcome welcome = gson.fromJson(EntityUtils.toString(entity), Welcome.class);
			if (!"Welcome".equals(welcome.couchdb) || welcome.version == null) {
				throw new CouchdbRasException("Validation failed to CouchDB server - invalid json response");
			}

			// TODO check the minimum version

			// TODO check the databases exist
			logger.debug("RAS CouchDB at " + this.rasUri.toString() + " validated");
		} catch(CouchdbRasException e) {
			throw e;
		} catch(Exception e) {
			throw new CouchdbRasException("Validation failed", e);
		}

		this.run = this.framework.getTestRun();

		//*** If this is a run, ensure we can create the run document
		if (this.run != null) {
			lastTestStructure = new TestStructure();
			lastTestStructure.setRunName(this.run.getName());
			try {
				updateTestStructure(lastTestStructure);
			} catch (ResultArchiveStoreException e) {
				throw new CouchdbRasException("Validation failed - unable to create initial run document", e);
			}

			createArtifactDocument();
		}

		ResultArchiveStoreFileStore fileStore = new ResultArchiveStoreFileStore();
		this.provider = new CouchdbRasFileSystemProvider(fileStore, this);
	}

	private void createArtifactDocument() throws CouchdbRasException {
		Artifacts artifacts = new Artifacts();
		artifacts.runId = this.runDocumentId;
		artifacts.runName = this.run.getName();

		String jsonArtifacts = gson.toJson(artifacts);

		HttpPost request = new HttpPost(this.rasUri + "/cirillo_artifacts");
		request.setEntity(new StringEntity(jsonArtifacts, UTF8));
		request.addHeader("Accept", "application/json");
		request.addHeader("Content-Type", "application/json");

		try (CloseableHttpResponse response = httpClient.execute(request)) {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
				throw new CouchdbRasException("Unable to store the artifacts document - " + statusLine.toString());
			}

			HttpEntity entity = response.getEntity();
			PutPostResponse putPostResponse = gson.fromJson(EntityUtils.toString(entity), PutPostResponse.class);
			if (putPostResponse.id == null || putPostResponse.rev == null) {
				throw new CouchdbRasException("Unable to store the artifacts document - Invalid JSON response");
			}

			this.artifactDocumentId.add(putPostResponse.id);
			this.artifactDocumentRev = putPostResponse.rev;
		} catch(CouchdbRasException e) {
			throw e;
		} catch(Exception e) {
			throw new CouchdbRasException("Unable to store the artifacts document", e);
		}
	}

	@Override
	public void initialise(@NotNull IFrameworkInitialisation frameworkInitialisation)
			throws ResultArchiveStoreException {}

	@Override
	public void writeLog(@NotNull String message) throws ResultArchiveStoreException {
		if (this.run == null) {
			throw new ResultArchiveStoreException("Not a run");
		}

		String[] lines = message.split("\r\n?|\n");

		synchronized (logCache) {
			logCache.addAll(Arrays.asList(lines));
			if (logCache.size() >= 100) {
				flushLogCache();
			}
		}
	}

	private void flushLogCache() throws ResultArchiveStoreException {
		LogLines logLines = new LogLines();
		synchronized (logCache) {
			if (logCache.isEmpty()) {
				return;
			}
			logLines.lines = new ArrayList<>(logCache);
			logOrder++;
			logLines.order = logOrder;

			logCache.clear();
		}
		logLines.runName = this.run.getName();
		logLines.runId   = this.runDocumentId;

		String jsonStructure = gson.toJson(logLines);

		HttpPost request = new HttpPost(this.rasUri + "/cirillo_log");
		request.setEntity(new StringEntity(jsonStructure, UTF8));
		request.addHeader("Accept", "application/json");
		request.addHeader("Content-Type", "application/json");

		try (CloseableHttpResponse response = httpClient.execute(request)) {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
				throw new CouchdbRasException("Unable to store the test log - " + statusLine.toString());
			}

			HttpEntity entity = response.getEntity();
			PutPostResponse putPostResponse = gson.fromJson(EntityUtils.toString(entity), PutPostResponse.class);
			if (putPostResponse.id == null || putPostResponse.rev == null) {
				throw new CouchdbRasException("Unable to store the test log - Invalid JSON response");
			}

			this.logIds.add(putPostResponse.id);

			this.updateTestStructure(lastTestStructure);
		} catch(CouchdbRasException e) {
			throw e;
		} catch(Exception e) {
			throw new ResultArchiveStoreException("Unable to store the test log", e);
		}
	}

	@Override
	public void writeLog(@NotNull List<String> messages) throws ResultArchiveStoreException {
		if (this.run == null) {
			throw new ResultArchiveStoreException("Not a run");
		}

		for(String message : messages) {
			writeLog(message);
		}
	}

	@Override
	public synchronized void updateTestStructure(@NotNull TestStructure testStructure) throws ResultArchiveStoreException {
		if (this.run == null) {
			throw new ResultArchiveStoreException("Not a run");
		}

		this.lastTestStructure = testStructure;
		this.lastTestStructure.setLogRecordIds(this.logIds);		
		this.lastTestStructure.setArtifactRecordIds(this.artifactDocumentId);		

		String jsonStructure = gson.toJson(testStructure);

		HttpEntityEnclosingRequestBase request;
		if (runDocumentId == null) {
			request = new HttpPost(this.rasUri + "/cirillo_run");
		} else {
			request = new HttpPut(this.rasUri + "/cirillo_run/" + runDocumentId);
			request.addHeader("If-Match", runDocumentRevision);
		}
		request.setEntity(new StringEntity(jsonStructure, UTF8));
		request.addHeader("Accept", "application/json");
		request.addHeader("Content-Type", "application/json");

		try (CloseableHttpResponse response = httpClient.execute(request)) {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
				if (statusLine.getStatusCode() == HttpStatus.SC_CONFLICT) {
					logger.error("The run document has been updated by another engine, terminating now to avoid corruption");
					System.exit(0);
				}

				throw new CouchdbRasException("Unable to store the test structure - " + statusLine.toString());
			}

			HttpEntity entity = response.getEntity();
			PutPostResponse putPostResponse = gson.fromJson(EntityUtils.toString(entity), PutPostResponse.class);
			if (putPostResponse.id == null || putPostResponse.rev == null) {
				throw new CouchdbRasException("Unable to store the test structure - Invalid JSON response");
			}
			this.runDocumentId       = putPostResponse.id;
			this.runDocumentRevision = putPostResponse.rev;

		} catch(CouchdbRasException e) {
			throw e;
		} catch(Exception e) {
			throw new ResultArchiveStoreException("Unable to store the test structure", e);
		}
	}

	@Override
	public List<IRunResult> getRuns(String runName) throws ResultArchiveStoreException {
		Objects.requireNonNull(runName);

		ArrayList<IRunResult> runs = new ArrayList<>();

		HttpPost httpPost = new HttpPost(this.rasUri + "/cirillo_run/_find");
		Find find = new Find();
		find.selector = new Selector();
		find.selector.runName = runName;
		find.execution_stats = true;

		httpPost.addHeader("Accept", "application/json");
		httpPost.addHeader("Content-Type", "application/json");


		while(true) {
			String requestContent = gson.toJson(find);
			httpPost.setEntity(new StringEntity(requestContent, UTF8));

			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
					throw new CouchdbRasException("Unable to find runs - " + statusLine.toString());
				}

				HttpEntity entity = response.getEntity();
				String responseEntity = EntityUtils.toString(entity);
				FoundRuns found = gson.fromJson(responseEntity, FoundRuns.class);
				if (found.docs == null) {
					throw new CouchdbRasException("Unable to find runs - Invalid JSON response");
				}

				if (found.warning != null) {
					logger.warn("CouchDB warning detected - " + found.warning);
				}

				if (found.docs.isEmpty()) {
					break;
				}

				for(TestStructureCouchdb ts : found.docs ) {
					Path runArtifactPath = getRunArtifactPath(ts);

					//*** Add this run to the results
					CouchdbRunResult cdbrr = new CouchdbRunResult(this, ts, runArtifactPath);
					runs.add(cdbrr);
				}

				//*** find the next batch of runs
				find.bookmark = found.bookmark;
			} catch(CouchdbRasException e) {
				throw e;
			} catch(Exception e) {
				throw new ResultArchiveStoreException("Unable to find runs", e);
			}


		}


		return runs;

	}



	private Path getRunArtifactPath(TestStructureCouchdb ts) throws CouchdbRasException {

		ResultArchiveStoreFileStore fileStore = new ResultArchiveStoreFileStore();
		CouchdbRasFileSystemProvider runProvider = new CouchdbRasFileSystemProvider(fileStore, this);
		if (ts.getArtifactRecordIds() == null || ts.getArtifactRecordIds().isEmpty()) {
			return runProvider.getRoot();
		}
		
		for(String artifactRecordId : ts.getArtifactRecordIds()) {
			HttpGet httpGet = new HttpGet(this.rasUri + "/cirillo_artifacts/" + artifactRecordId);
			httpGet.addHeader("Accept", "application/json");

			try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) { // TODO Ignore it for now
					continue;
				}
				if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
					throw new CouchdbRasException("Unable to find artifacts - " + statusLine.toString());
				}

				HttpEntity entity = response.getEntity();
				String responseEntity = EntityUtils.toString(entity);
				JsonObject artifactRecord = gson.fromJson(responseEntity, JsonObject.class);

				JsonElement attachmentsElement = artifactRecord.get("_attachments");

				if (attachmentsElement != null) {
					if (attachmentsElement instanceof JsonObject) {
						JsonObject attachments = (JsonObject) attachmentsElement;
						Set<Entry<String, JsonElement>> entries = attachments.entrySet();
						if (entries != null) {
							for(Entry<String, JsonElement> entry : entries) {
								JsonElement elem = entry.getValue();
								if (elem instanceof JsonObject) {
									runProvider.addPath(new CouchdbArtifactPath(runProvider.getActualFileSystem(), entry.getKey(), (JsonObject)elem, artifactRecordId));
								}
							}
						}
					}
				}
			} catch(CouchdbRasException e) {
				throw e;
			} catch(Exception e) {
				throw new CouchdbRasException("Unable to find runs", e);
			}
		}

		return runProvider.getRoot();
	}
	
	public void retrieveArtifact(CouchdbArtifactPath path, Path cachePath) throws CouchdbRasException {
		String artifactRecordId = path.getArtifactRecordId();
		String encodedPath;
		try {
			encodedPath = URLEncoder.encode(path.toString(), "utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new CouchdbRasException("Problem encoding artifact path", e);
		}
		
		HttpGet httpGet = new HttpGet(this.rasUri + "/cirillo_artifacts/" + artifactRecordId + "/" + encodedPath);

		try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) { 
				throw new CouchdbRasException("Not found - " + path.toString());
			}
			if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
				throw new CouchdbRasException("Unable to find artifact - " + statusLine.toString());
			}

			HttpEntity entity = response.getEntity();
			Files.copy(entity.getContent(), cachePath, StandardCopyOption.REPLACE_EXISTING);			
		} catch(CouchdbRasException e) {
			throw e;
		} catch(Exception e) {
			throw new CouchdbRasException("Unable to find runs", e);
		}
	}
	
	public String getLog(TestStructure ts) throws CouchdbRasException {
		StringBuilder sb = new StringBuilder();
		
		for(String logRecordId : ts.getLogRecordIds()) {
			HttpGet httpGet = new HttpGet(this.rasUri + "/cirillo_log/" + logRecordId);
			httpGet.addHeader("Accept", "application/json");

			try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
				StatusLine statusLine = response.getStatusLine();
				if (statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND) { // TODO Ignore it for now
					continue;
				}
				if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
					throw new CouchdbRasException("Unable to find logs - " + statusLine.toString());
				}

				HttpEntity entity = response.getEntity();
				String responseEntity = EntityUtils.toString(entity);
				LogLines logLines = gson.fromJson(responseEntity, LogLines.class);
				if (logLines.lines != null) {
					for(String line : logLines.lines) {
						if (sb.length() > 0) {
							sb.append("\n");
						}
						sb.append(line);
					}
				}
			} catch(CouchdbRasException e) {
				throw e;
			} catch(Exception e) {
				throw new CouchdbRasException("Unable to find runs", e);
			}
		}
		return sb.toString();
	}




	@Override
	public Path getStoredArtifactsRoot() {
		if (this.run == null) {
			return null;
		}
		return provider.getActualFileSystem().getPath("/");
	}

	@Override
	public void flush() {
		try {
			flushLogCache();
		} catch (ResultArchiveStoreException e) {
			logger.error("Error with heartbeat flush",e);
		}
	}

	@Override
	public void shutdown() {
		try {
			flushLogCache();
		} catch (ResultArchiveStoreException e) {
			logger.error("Error with shutdown flush",e);
		}

		try {
			this.httpClient.close();
		} catch (IOException e) {
		}
	}

	public CloseableHttpClient getHttpClient() {
		return this.httpClient;
	}

	public String getArtifactDocumentId() {
		return this.artifactDocumentId.get(0);
	}

	public String getArtifactDocumentRev() {
		return this.artifactDocumentRev;
	}

	public URI getCouchdbUri() {
		return this.rasUri;
	}

	public Gson getGson() {
		return this.gson;
	}

	public void updateArtifactDocumentRev(String newArtifactDocumentRev) {
		this.artifactDocumentRev = newArtifactDocumentRev;

	}

}
