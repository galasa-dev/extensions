/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.extensions.mocks;

import static org.assertj.core.api.Assertions.*;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;

public abstract class BaseHttpInteraction implements HttpInteraction {

    private String expectedBaseUri ;
    private String returnedDocument;

    public BaseHttpInteraction(String expectedBaseUri, String returnedDocument ) {
        this.expectedBaseUri = expectedBaseUri;
        this.returnedDocument = returnedDocument;
    }

    public String getExpectedBaseUri() {
        return this.expectedBaseUri;
    }

    public String getReturnedDocument() {
        return this.returnedDocument;
    }

    public String getExpectedHttpContentType() {
        return "application/json";
    }

    @Override
    public void validateRequest(HttpHost host, HttpRequest request) throws RuntimeException {

        String uri = request.getRequestLine().getUri();
        assertThat(uri).isEqualTo(this.expectedBaseUri);

        validateRequestContentType(request);
    }

    public void validateRequestContentType(HttpRequest request) {
        assertThat(request.containsHeader("Content-Type")).as("Missing Content-Type header!").isTrue();
        assertThat(request.getHeaders("Content-Type")[0].getValue()).isEqualTo(getExpectedHttpContentType());
    }
}