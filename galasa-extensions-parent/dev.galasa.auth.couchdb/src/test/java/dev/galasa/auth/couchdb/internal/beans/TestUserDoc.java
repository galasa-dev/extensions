/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal.beans;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import dev.galasa.framework.spi.auth.IFrontEndClient;
import dev.galasa.framework.spi.auth.IUser;


public class TestUserDoc {

    @Test
    public void testCanConstructUserDocGivenAUserDoc() throws Exception  {
        UserDoc doc1 = new UserDoc("myLoginId",null);
        assertThat(doc1.getLoginId()).isEqualTo("myLoginId");
    }

    @Test
    public void testCanConstructUserDocGivenAUserDocWithClient() throws Exception  {
        UserDoc doc1 = new UserDoc("myLoginId",List.of(new FrontEndClient("myClient1",Instant.MIN)));

        IFrontEndClient clientGotBack = doc1.getClient("myClient1");
        assertThat(clientGotBack).isNotNull();
        assertThat(clientGotBack.getClientName()).isEqualTo("myClient1");
        assertThat(clientGotBack.getLastLogin()).isEqualTo(Instant.MIN);
    }

    @Test
    public void testCanLookForClientWhichIsntInTheUserDocReturnsNull() throws Exception  {
        UserDoc doc1 = new UserDoc("myLoginId",List.of(new FrontEndClient("myClient1",Instant.MIN)));

        IFrontEndClient clientGotBack = doc1.getClient("myClient2"); // Client2 isn't there!
        assertThat(clientGotBack).isNull();
    }

    @Test
    public void testCanTryLookingForClientWithNullNameShouldReturnNull() throws Exception  {
        UserDoc doc1 = new UserDoc("myLoginId",List.of(new FrontEndClient("myClient1",Instant.MIN)));

        IFrontEndClient clientGotBack = doc1.getClient(null); 
        assertThat(clientGotBack).isNull();
    }

    class MockIUser implements IUser {
        private String userNumber;
        private String loginId;

        private List<IFrontEndClient> clients;

        public MockIUser(String loginId, List<IFrontEndClient> clients) {
            this.loginId = loginId;
            this.clients = clients;
        }

        @Override
        public String getUserNumber(){
            return userNumber;
        }

        @Override
        public String getLoginId() {
            return loginId;
        }

        @Override
        public IFrontEndClient getClient(String clientName) {
            return null;
        }

        @Override
        public Collection<IFrontEndClient> getClients() {
            return this.clients;
        }

        @Override
        public void addClient(IFrontEndClient client) {
            clients.add( new FrontEndClient(client));
        }

        @Override
        public String getVersion() {
            return "0" ;
        }

    }

    @Test
    public void testCanCloneUserDocFromIUser() throws Exception {
        MockIUser doc1 = new MockIUser("myLoginId",null);

        UserDoc doc2 = new UserDoc(doc1);

        assertThat(doc2).isNotNull();
        assertThat(doc2.getLoginId()).isEqualTo("myLoginId");
    }

    class MockIFrontEndClient implements IFrontEndClient {

        String name ; 
        Instant lastLogin ;

        public MockIFrontEndClient(String name, Instant lastLogin) {
            this.name = name;
            this.lastLogin = lastLogin;
        }

		@Override
		public String getClientName() {
			return name;
		}

		@Override
		public Instant getLastLogin() {
			return lastLogin;
		}

		@Override
		public void setLastLogin(Instant lastLoginTime) {
		}

    }

    @Test
    public void testCanSetIFrontEndClientsInAndGetThemBack() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId",List.of(new FrontEndClient("myClient1",Instant.MIN)));
        MockIFrontEndClient mockClient = new MockIFrontEndClient("client2", Instant.MIN.plusSeconds(2));

        doc1.addClient(mockClient);


        // Then...
        IFrontEndClient gotBack = doc1.getClient("client2");
        assertThat(gotBack).isNotNull();
        assertThat(gotBack.getLastLogin()).isEqualTo(Instant.MIN.plusSeconds(2));
    }

    @Test
    public void testCanGetClientsWhenClientsArePresent() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of(
                new FrontEndClient("client1", Instant.MIN),
                new FrontEndClient("client2", Instant.now())
        ));

        Collection<IFrontEndClient> clients = doc1.getClients();

        assertThat(clients).isNotNull();
        assertThat(clients).hasSize(2);
        assertThat(clients).extracting("clientName").containsExactlyInAnyOrder("client1", "client2");
    }

    @Test
    public void testCanGetClientsWhenNoClientsArePresent() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of());

        Collection<IFrontEndClient> clients = doc1.getClients();

        assertThat(clients).isNotNull();
        assertThat(clients).isEmpty();
    }

    @Test
    public void testCanSetAndGetUserNumber() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of());
        doc1.setUserNumber("12345");

        String userNumber = doc1.getUserNumber();

        assertThat(userNumber).isNotNull();
        assertThat(userNumber).isEqualTo("12345");
    }

    @Test
    public void testGetUserNumberReturnsNullWhenNotSet() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of());

        String userNumber = doc1.getUserNumber();

        assertThat(userNumber).isNull();
    }

    @Test
    public void testCanSetAndGetVersion() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of());
        doc1.setVersion("1.0");

        String version = doc1.getVersion();

        assertThat(version).isNotNull();
        assertThat(version).isEqualTo("1.0");
    }

    @Test
    public void testGetVersionReturnsNullWhenNotSet() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of());

        String version = doc1.getVersion();

        assertThat(version).isNull();
    }

    @Test
    public void testCanSetAndGetLoginId() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of());
        doc1.setLoginId("newLoginId");

        String loginId = doc1.getLoginId();

        assertThat(loginId).isNotNull();
        assertThat(loginId).isEqualTo("newLoginId");
    }

    @Test
    public void testGetLoginIdReturnsNullWhenNotSet() throws Exception {
        UserDoc doc1 = new UserDoc(null, List.of());

        String loginId = doc1.getLoginId();

        assertThat(loginId).isNull();
    }

    @Test
    public void testCanSetClientsSuccessfully() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of());
        Collection<IFrontEndClient> clients = List.of(
                new FrontEndClient("client1", Instant.MIN),
                new FrontEndClient("client2", Instant.now())
        );

        doc1.setClients(clients);

        Collection<IFrontEndClient> clientsGotBack = doc1.getClients();
        assertThat(clientsGotBack).isNotNull();
        assertThat(clientsGotBack).hasSize(2);
        assertThat(clientsGotBack).extracting("clientName").containsExactlyInAnyOrder("client1", "client2");
    }

    @Test
    public void testSetClientsDoesNotModifyOriginalCollection() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of());
        List<IFrontEndClient> originalClients = new ArrayList<>();
        originalClients.add(new FrontEndClient("client1", Instant.MIN));

        doc1.setClients(originalClients);

        // Modify the original collection after setting it
        originalClients.add(new FrontEndClient("client2", Instant.now()));

        Collection<IFrontEndClient> clientsGotBack = doc1.getClients();
        assertThat(clientsGotBack).hasSize(1); // Should only contain the first client
        assertThat(clientsGotBack).extracting("clientName").containsExactly("client1");
    }

    @Test
    public void testSetClientsShouldInvokeAddClientForEachClient() throws Exception {
        UserDoc doc1 = new UserDoc("myLoginId", List.of());
        Collection<IFrontEndClient> clients = List.of(
                new FrontEndClient("client1", Instant.MIN),
                new FrontEndClient("client2", Instant.now())
        );

        doc1.setClients(clients);

        Collection<IFrontEndClient> clientsGotBack = doc1.getClients();
        assertThat(clientsGotBack).hasSize(2);
        assertThat(clientsGotBack).extracting("clientName").contains("client1", "client2");
    }

}
