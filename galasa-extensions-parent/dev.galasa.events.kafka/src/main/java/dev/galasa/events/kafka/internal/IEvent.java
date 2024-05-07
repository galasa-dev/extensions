/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package dev.galasa.events.kafka.internal;

public interface IEvent {

    public String getId();

    public String getTimestamp();

    public void setTimestamp(String id);

    public String getMessage();

    public void setMessage(String message);

}