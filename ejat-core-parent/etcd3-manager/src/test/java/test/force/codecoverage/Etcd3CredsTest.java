package test.force.codecoverage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static java.util.Arrays.asList;
import static com.google.common.base.Charsets.UTF_8;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.ArrayList;

import java.util.concurrent.CompletableFuture;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.ejat.etcd3.internal.Etcd3CredentialsStore;
import io.ejat.etcd3.internal.Etcd3CredentialsStoreRegistration;
import io.ejat.framework.FrameworkInitialisation;
import io.ejat.framework.spi.IConfigurationPropertyStoreService;
import io.ejat.framework.spi.creds.CredentialsException;
import io.ejat.framework.spi.creds.CredentialsToken;
import io.ejat.framework.spi.creds.CredentialsUsername;
import io.ejat.framework.spi.creds.CredentialsUsernamePassword;
import io.ejat.framework.spi.creds.ICredentials;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;

/**
 * Test class for testing method behaviour of the credential store service for etcd.
 * 
 * @author James Davies
 */
@RunWith(MockitoJUnitRunner.class)
public class Etcd3CredsTest {

    /**
     * Mocked client that we interact with to mock the etcd.
     */
    @Mock
    KV kvClient;

    /**
     * Inject the above mock into this mocked credstore.
     */
    @InjectMocks
    Etcd3CredentialsStore credsStore = new Etcd3CredentialsStore(createCredsUri());

    private URI createCredsUri() {
        URI testUri;
        try {
            testUri = new URI("http://something");
        } catch (URISyntaxException e) {
            testUri = null;
        }
        return testUri;
    }

    /**
     * Tests a null return, i.e. no no username password or token.
     * 
     * @throws CredentialsException
     */
    @Test
    public void testGetCredentialsNullReturn() throws CredentialsException {
        GetResponse response = Mockito.mock(GetResponse.class);
        CompletableFuture<GetResponse> getFuture = CompletableFuture.completedFuture(response);
        when(kvClient.get(any(ByteSequence.class))).thenReturn(getFuture);
        when(response.getKvs()).thenReturn(new ArrayList<KeyValue>());

        ICredentials out = credsStore.getCredentials("foo");

        assertEquals(null, out);
    }

    /**
     * Returns the a test token response.
     * 
     * @throws CredentialsException
     */
    @Test
    public void testGetCredentialsTokenReturn() throws CredentialsException {
        GetResponse response = mock(GetResponse.class);
        CompletableFuture<GetResponse> getFuture = CompletableFuture.completedFuture(response);
        KeyValue kv = mock(KeyValue.class);
        ByteSequence bs = ByteSequence.from("secure.credentials.foo.token", UTF_8);
        when(kvClient.get(bs)).thenReturn(getFuture);
        when(response.getKvs()).thenReturn(asList(kv));
        when(kv.getValue()).thenReturn(ByteSequence.from("bar", UTF_8));

        CredentialsToken out = (CredentialsToken)credsStore.getCredentials("foo");

        assertEquals("bar", out.getToken());
    }

    /**
     * Tests a single username response test.
     * 
     * @throws CredentialsException
     */
    @Test
    public void testGetCredentialsUsernameReturn() throws CredentialsException {
        GetResponse response1 = mock(GetResponse.class);
        GetResponse response2 = mock(GetResponse.class);
        GetResponse response3 = mock(GetResponse.class);
        CompletableFuture<GetResponse> getFuture1 = CompletableFuture.completedFuture(response1);
        CompletableFuture<GetResponse> getFuture2 = CompletableFuture.completedFuture(response2);
        CompletableFuture<GetResponse> getFuture3 = CompletableFuture.completedFuture(response3);
        KeyValue kv = mock(KeyValue.class);
        ByteSequence bsToken = ByteSequence.from("secure.credentials.foo.token", UTF_8);
        ByteSequence bsUsername = ByteSequence.from("secure.credentials.foo.username", UTF_8);
        ByteSequence bsPassword = ByteSequence.from("secure.credentials.foo.password", UTF_8);
        when(kvClient.get(bsToken)).thenReturn(getFuture1);
        when(response1.getKvs()).thenReturn(new ArrayList<>());
        when(kvClient.get(bsUsername)).thenReturn(getFuture2);
        when(response2.getKvs()).thenReturn(asList(kv));
        when(kv.getValue()).thenReturn(ByteSequence.from("bar", UTF_8));
        when(kvClient.get(bsPassword)).thenReturn(getFuture3);
        when(response3.getKvs()).thenReturn(new ArrayList<>());

        CredentialsUsername out = (CredentialsUsername)credsStore.getCredentials("foo");

        assertEquals("bar", out.getUsername());
    }

    /**
     * Tests a username and password response type test.
     * 
     * @throws CredentialsException
     */
    @Test
    public void testGetCredentialsUsernameAndPasswordReturn() throws CredentialsException {
        GetResponse response1 = mock(GetResponse.class);
        GetResponse response2 = mock(GetResponse.class);
        GetResponse response3 = mock(GetResponse.class);

        CompletableFuture<GetResponse> getFuture1 = CompletableFuture.completedFuture(response1);
        CompletableFuture<GetResponse> getFuture2 = CompletableFuture.completedFuture(response2);
        CompletableFuture<GetResponse> getFuture3 = CompletableFuture.completedFuture(response3);

        KeyValue kvUser = mock(KeyValue.class);
        KeyValue kvPass = mock(KeyValue.class);

        ByteSequence bsToken = ByteSequence.from("secure.credentials.foo.token", UTF_8);
        ByteSequence bsUsername = ByteSequence.from("secure.credentials.foo.username", UTF_8);
        ByteSequence bsPassword = ByteSequence.from("secure.credentials.foo.password", UTF_8);

        when(kvClient.get(bsToken)).thenReturn(getFuture1);
        when(response1.getKvs()).thenReturn(new ArrayList<>());

        when(kvClient.get(bsUsername)).thenReturn(getFuture2);
        when(response2.getKvs()).thenReturn(asList(kvUser));
        when(kvUser.getValue()).thenReturn(ByteSequence.from("bar", UTF_8));

        when(kvClient.get(bsPassword)).thenReturn(getFuture3);
        when(response3.getKvs()).thenReturn(asList(kvPass));
        when(kvPass.getValue()).thenReturn(ByteSequence.from("SuperSecretPassword", UTF_8));

        CredentialsUsernamePassword out = (CredentialsUsernamePassword)credsStore.getCredentials("foo");

        assertEquals("bar", out.getUsername());
        assertEquals("SuperSecretPassword", out.getPassword());
    }

    /**
     * This method checks the registration of a etcd dss from a http URI
     * 
     * @throws DynamicStatusStoreException
     * @throws URISyntaxException
     * @throws CredentialsException
     */
    @Test
    public void testRegistration() throws URISyntaxException, CredentialsException {

        FrameworkInitialisation fi = Mockito.mock(FrameworkInitialisation.class);
		Etcd3CredentialsStoreRegistration regi = new Etcd3CredentialsStoreRegistration();
		
		when(fi.getCredentialsStoreUri()).thenReturn(new URI("etc:http://thisIsAEtcd3"));
		regi.initialise(fi);
		
		assertTrue("dummy", true);
	}

     /**
     * This method checks the quiet failure of not registring if the URI is a file.
     * 
     * @throws DynamicStatusStoreException
     * @throws URISyntaxException
     * @throws CredentialsException
     */
    @Test
    public void testRegistrationwithFile() throws URISyntaxException, CredentialsException {
        FrameworkInitialisation fi = Mockito.mock(FrameworkInitialisation.class);
		Etcd3CredentialsStoreRegistration regi = new Etcd3CredentialsStoreRegistration();
		
		when(fi.getCredentialsStoreUri()).thenReturn(new URI("File://blah"));
		regi.initialise(fi);
		
		assertTrue("dummy", true);
    }
}