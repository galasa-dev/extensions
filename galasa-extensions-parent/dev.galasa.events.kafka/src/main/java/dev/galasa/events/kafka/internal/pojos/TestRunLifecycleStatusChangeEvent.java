/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package dev.galasa.events.kafka.internal.pojos;

import java.util.UUID;
import dev.galasa.framework.spi.events.IEvent;

public class TestRunLifecycleStatusChangeEvent implements IEvent {

    private String id;
    private String timestamp;
    private String message;

    public TestRunLifecycleStatusChangeEvent(String timestamp, String message){
        this.id = UUID.randomUUID().toString();
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getId(){
        return this.id;
    }

    public void setId(String id){
        this.id = id;
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

    @Override
    public String toString() {
        return  this.getClass().getName() + "{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                '}';
    }
    
}
