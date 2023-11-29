/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.properties;

import dev.galasa.framework.spi.cps.CpsProperties;
import dev.galasa.ras.couchdb.internal.CouchdbRasException;

/**
 * CouchDB Document Type CPS Property
 * 
 * @galasa.cps.property
 * 
 * @galasa.name couchdb.document.type
 * 
 * @galasa.description Describes whether a shared CouchDB document should be created with each
 * test artifact attached to it or if a single document should be created for each test artifact 
 * 
 * @galasa.required No
 * 
 * @galasa.default shared
 * 
 * @galasa.valid_values shared or single
 * 
 * @galasa.examples 
 * <code>couchdb.document.type=shared<br>
 * couchdb.document.type=single</code>
 * 
 */
public class CouchdbDocumentType extends CpsProperties {

    public static String get() throws CouchdbRasException {
        return getStringWithDefault(CouchdbPropertiesSingleton.cps(), "single", "document", "type");
    }
    
}
