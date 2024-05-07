/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package dev.galasa.events.kafka.internal.pojos;

import java.util.UUID;
import dev.galasa.events.kafka.internal.IEvent;

public class TestRunLifecycleStatusChangeEvent implements IEvent {

    private String timestamp;
    private String id;
    private String message;

    public TestRunLifecycleStatusChangeEvent(String timestamp, String message){
        this.id = UUID.randomUUID().toString();
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getId(){
        return this.id;
    }

    public String getTimestamp(){
        return this.timestamp;
    }

    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }

    public String getMessage(){
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
}
