/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.impl;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.junit.Test;
import static org.assertj.core.api.Assertions.*;

import dev.galasa.ras.couchdb.internal.dependencies.impl.HttpRequestFactory;
import dev.galasa.ras.couchdb.internal.mocks.MockEnvironment;

public class HttpRequestFactoryTest {

    @Test
    public void TestGETRequestReturnsRequestWithGETMethod() {
        //Given ...
        String token = "myvalue";
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setenv("GALASA_RAS_TOKEN", token);
        HttpRequestFactory requestFactory = new HttpRequestFactory(mockEnv);
        String url = "http://example.com/get";

        //When ...
        HttpGet request = requestFactory.getHttpGetRequest(url);

        //Then ...
        assertThat(request.getURI().toString()).isEqualTo(url);
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getFirstHeader("Authorization").getValue()).isEqualTo("Basic "+token);
        assertThat(request.getFirstHeader("Content-Type").getValue()).isEqualTo("application/json");
        assertThat(request.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }

    @Test
    public void TestHEADRequestReturnsRequestWithHEADMethod() {
        //Given ...
        String token = "iamnottryingtogetahead";
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setenv("GALASA_RAS_TOKEN", token);
        HttpRequestFactory requestFactory = new HttpRequestFactory(mockEnv);
        String url = "http://example.com/head";

        //When ...
        HttpHead request = requestFactory.getHttpHeadRequest(url);

        //Then ...
        assertThat(request.getURI().toString()).isEqualTo(url);
        assertThat(request.getMethod()).isEqualTo("HEAD");
        assertThat(request.getFirstHeader("Authorization").getValue()).isEqualTo("Basic "+token);
        assertThat(request.getFirstHeader("Content-Type").getValue()).isEqualTo("application/json");
        assertThat(request.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }

    @Test
    public void TestPOSTRequestReturnsRequestWithPOSTMethod() {
        //Given ...
        String token = "mysecretPOSTtoken";
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setenv("GALASA_RAS_TOKEN", token);
        HttpRequestFactory requestFactory = new HttpRequestFactory(mockEnv);
        String url = "http://example.com/post";

        //When ...
        HttpPost request = requestFactory.getHttpPostRequest(url);

        //Then ...
        assertThat(request.getURI().toString()).isEqualTo(url);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getFirstHeader("Authorization").getValue()).isEqualTo("Basic "+token);
        assertThat(request.getFirstHeader("Content-Type").getValue()).isEqualTo("application/json");
        assertThat(request.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }

    @Test
    public void TestPUTRequestReturnsRequestWithPUTMethod() {
        //Given ...
        String token = "iPut";
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setenv("GALASA_RAS_TOKEN", token);
        HttpRequestFactory requestFactory = new HttpRequestFactory(mockEnv);
        String url = "http://example.com/put";

        //When ...
        HttpPut request = requestFactory.getHttpPutRequest(url);

        //Then ...
        assertThat(request.getURI().toString()).isEqualTo(url);
        assertThat(request.getMethod()).isEqualTo("PUT");
        assertThat(request.getFirstHeader("Authorization").getValue()).isEqualTo("Basic "+token);
        assertThat(request.getFirstHeader("Content-Type").getValue()).isEqualTo("application/json");
        assertThat(request.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }
    
    @Test
    public void TestDELETERequestReturnsRequestWithDELETEMethod() {
        //Given ...
        String token = "idontneedthisanymore";
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setenv("GALASA_RAS_TOKEN", token);
        HttpRequestFactory requestFactory = new HttpRequestFactory(mockEnv);
        String url = "http://example.com/delete";

        //When ...
        HttpDelete request = requestFactory.getHttpDeleteRequest(url);

        //Then ...
        assertThat(request.getURI().toString()).isEqualTo(url);
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getFirstHeader("Authorization").getValue()).isEqualTo("Basic "+token);
        assertThat(request.getFirstHeader("Content-Type").getValue()).isEqualTo("application/json");
        assertThat(request.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
    }

    @Test
    public void TestGETRequestwithExtraHeadersReturnsRequestWithExtraHeaders() {
        //Given ...
        String token = "getwithextraheaders";
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setenv("GALASA_RAS_TOKEN", token);
        HttpRequestFactory requestFactory = new HttpRequestFactory(mockEnv);
        String url = "http://example.com/get";
        String referer = "Galasa";
        String encoding = "gzip, deflate, br";

        //When ...
        HttpGet request = requestFactory.getHttpGetRequest(url);
        request.setHeader("Referer", referer);
        request.setHeader("Accept-Encoding", encoding);

        //Then ...
        assertThat(request.getURI().toString()).isEqualTo(url);
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getFirstHeader("Authorization").getValue()).isEqualTo("Basic "+token);
        assertThat(request.getFirstHeader("Content-Type").getValue()).isEqualTo("application/json");
        assertThat(request.getFirstHeader("Accept").getValue()).isEqualTo("application/json");
        assertThat(request.getFirstHeader("Referer").getValue()).isEqualTo(referer);
        assertThat(request.getFirstHeader("Accept-Encoding").getValue()).isEqualTo(encoding);
    }

    @Test
    public void TestPOSTRequestwithUpdatedHeadersReturnsRequestWithUpdatedHeaders() {
        //Given ...
        String token = "getwithextraheaders";
        MockEnvironment mockEnv = new MockEnvironment();
        mockEnv.setenv("GALASA_RAS_TOKEN", token);
        HttpRequestFactory requestFactory = new HttpRequestFactory(mockEnv);
        String url = "http://example.com/post";
        String accept = "application/xml";
        String contentType = "text/html";

        //When ...
        HttpPost request = requestFactory.getHttpPostRequest(url);
        request.setHeader("Accept", accept);
        request.setHeader("Content-Type", contentType);

        //Then ...
        assertThat(request.getURI().toString()).isEqualTo(url);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getFirstHeader("Authorization").getValue()).isEqualTo("Basic "+token);
        assertThat(request.getHeaders("Content-Type").length).isEqualTo(1);
        assertThat(request.getFirstHeader("Content-Type").getValue()).isEqualTo(contentType);
        assertThat(request.getHeaders("Accept").length).isEqualTo(1);
        assertThat(request.getFirstHeader("Accept").getValue()).isEqualTo(accept);

    }
}
