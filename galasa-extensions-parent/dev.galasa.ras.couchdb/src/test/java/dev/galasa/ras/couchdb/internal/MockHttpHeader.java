package dev.galasa.ras.couchdb.internal;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.ParseException;

public class MockHttpHeader implements Header {

    private String name ;
    private String value ;
    private HeaderElement[] elements ;

    public MockHttpHeader(String name, String value, HeaderElement[] elements) {
        this.name = name ;
        this.value = value;
        this.elements = elements;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public HeaderElement[] getElements() throws ParseException {
        HeaderElement[] elements = new HeaderElement[1];

        elements[0] = new MockHeaderElement(this.name, this.value);
        return elements;
    }

}
