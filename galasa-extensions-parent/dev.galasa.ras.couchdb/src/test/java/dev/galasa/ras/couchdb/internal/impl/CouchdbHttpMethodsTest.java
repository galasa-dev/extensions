/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.impl;

import org.junit.Test;
import static org.assertj.core.api.Assertions.*;

import dev.galasa.ras.couchdb.internal.dependencies.impl.CouchdbHttpMethods;

public class CouchdbHttpMethodsTest {
    
    @Test
    public void TestGetMethodReturnsGET(){
        //Given ...
        CouchdbHttpMethods method = CouchdbHttpMethods.GET;
        
        //When ...
        String value = method.getValue();
        
        //Then ...
        assertThat(value).isEqualTo("GET");
    }

    @Test
    public void TestGetMethodReturnsHEAD(){
        //Given ...
        CouchdbHttpMethods method = CouchdbHttpMethods.HEAD;
        
        //When ...
        String value = method.getValue();
        
        //Then ...
        assertThat(value).isEqualTo("HEAD");
    }

    @Test
    public void TestGetMethodReturnsPOST(){
        //Given ...
        CouchdbHttpMethods method = CouchdbHttpMethods.POST;
        
        //When ...
        String value = method.getValue();
        
        //Then ...
        assertThat(value).isEqualTo("POST");
    }

    @Test
    public void TestGetMethodReturnsPUT(){
        //Given ...
        CouchdbHttpMethods method = CouchdbHttpMethods.PUT;
        
        //When ...
        String value = method.getValue();
        
        //Then ...
        assertThat(value).isEqualTo("PUT");
    }

    @Test
    public void TestGetMethodReturnsDELETE(){
        //Given ...
        CouchdbHttpMethods method = CouchdbHttpMethods.DELETE;
        
        //When ...
        String value = method.getValue();
        
        //Then ...
        assertThat(value).isEqualTo("DELETE");
    }
}
