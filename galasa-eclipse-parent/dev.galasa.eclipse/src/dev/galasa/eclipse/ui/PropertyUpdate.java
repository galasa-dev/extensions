/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui;

import java.time.Instant;

public class PropertyUpdate {

    public enum Type {
        UPDATE,
        DELETE
    }

    private final String  key;
    private final String  value;
    private final Type    type;
    private final Instant when = Instant.now();

    public PropertyUpdate(String key, String value, Type type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    public Instant getWhen() {
        return when;
    }

}
