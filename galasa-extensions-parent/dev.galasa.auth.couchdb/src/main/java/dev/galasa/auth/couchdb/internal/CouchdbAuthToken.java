/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.auth.couchdb.internal;

import java.time.Instant;

import com.google.gson.annotations.SerializedName;
import dev.galasa.framework.spi.auth.IAuthToken;
import dev.galasa.framework.spi.auth.User;

public class CouchdbAuthToken implements IAuthToken {

    @SerializedName("_id")
    private String documentId;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("description")
    private String description;

    @SerializedName("creation_time")
    private Instant creationTime;

    @SerializedName("owner")
    private User owner;

    public CouchdbAuthToken(String clientId, String description, Instant creationTime, User owner) {
        this.clientId = clientId;
        this.description = description;
        this.creationTime = creationTime;
        this.owner = owner;
    }

    public CouchdbAuthToken(String documentId, String clientId, String description, Instant creationTime, User owner) {
        this(clientId, description, creationTime, owner);
        this.documentId = documentId;
    }

    public String getTokenId() {
        return documentId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public User getOwner() {
        return owner;
    }
}
