package dev.galasa.auth.couchdb.internal.beans;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;


import com.google.gson.annotations.SerializedName;

import dev.galasa.framework.spi.auth.IFrontEndClient;
import dev.galasa.framework.spi.auth.IUser;

public class UserDoc implements IUser{

    @SerializedName("_id")
    private String userNumber;

    @SerializedName("_rev")
    private String version;

    @SerializedName("login-id")
    private String loginId;

    @SerializedName("activity")
    private List<FrontEndClient> clients;

    public UserDoc(String loginId, List<FrontEndClient> clients) {
        this.loginId = loginId;
        setClients( clients);
    }

    public UserDoc(IUser user){
        this.loginId = user.getLoginId();
        this.version = user.getVersion();
        this.userNumber = user.getUserNumber();
        setClients(user.getClients());
    }

    @Override
    public String getUserNumber(){
        return userNumber;
    }

    public void setUserNumber(String userNumber){
        this.userNumber = userNumber;
    }

    public String getVersion(){
        return version;
    }

    public void setVersion(String version){
        this.version = version;
    }

    @Override
    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    @Override
    public Collection<IFrontEndClient> getClients() {
        Collection<IFrontEndClient> results = new ArrayList<IFrontEndClient>();
        for( FrontEndClient client : this.clients) {
            results.add(client);
        }
        return results;
    }

    public void setClients(List<FrontEndClient> clients) {
        this.clients = new ArrayList<FrontEndClient>();
        if( clients != null) {
            for (IFrontEndClient clientIn: clients) {
                addClient(clientIn);
            }
        }
    }

    // Setter for clients. Takes a deep copy of any clients it is passed.
    public void setClients(Collection<IFrontEndClient> clients) {

        this.clients = new ArrayList<FrontEndClient>();
        if( clients != null) {
            for (IFrontEndClient clientIn: clients) {
                addClient(clientIn);
            }
        }
    }

    @Override
    public IFrontEndClient getClient(String clientName) {
        IFrontEndClient match = null; 
        if (clientName != null) {
            for (FrontEndClient frontEndClient : clients) {
                if(clientName.equals(frontEndClient.getClientName())){
                    match = frontEndClient;
                    break;
                }
            }
        }
        return match;
    }

    @Override
    public void addClient(IFrontEndClient client) {
        clients.add( new FrontEndClient(client));
    }

    
}
