/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.runs;

import java.time.Instant;

import dev.galasa.eclipse.ui.PropertyUpdate.Type;

public class Run {

    private final String runName;

    private String       status;

    private Instant      lastPropertyModification = Instant.now();

    private Instant      queued                   = Instant.ofEpochSecond(0);

    public Run(String runName) {
        this.runName = runName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(runName);
        sb.append(" - ");
        if (status != null) {
            sb.append(status);
        } else {
            sb.append("unknown");
        }
        return sb.toString();
    }

    public synchronized void propertyUpdate(String key, String value, Type type) {
        if (type == Type.DELETE) {
            value = null;
        }

        switch (key) {
            case "status":
                this.status = value;
                break;
            case "queued":
                if (value == null) {
                    this.queued = Instant.ofEpochSecond(0);
                } else {
                    try {
                        this.queued = Instant.parse(value);
                    } catch (Throwable e) {
                        this.queued = Instant.ofEpochSecond(0);
                    }
                }
                break;
        }

        if (type != Type.DELETE) {
            this.lastPropertyModification = Instant.now();
        }

        return;
    }

    public synchronized boolean isValid() {
        if (this.status != null) {
            return true;
        }

        return false;
    }

    public synchronized boolean updatedRecently() {
        Instant fiveSecondsAgo = Instant.now().minusSeconds(5);
        if (lastPropertyModification.compareTo(fiveSecondsAgo) >= 0) {
            return true;
        }
        return false;
    }

    public String getRunName() {
        return this.runName;
    }

    public Instant getQueued() {
        return this.queued;
    }

}
