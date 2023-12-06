/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal;

// The name of a cps flag, which controls the storage strategy for artifacts.
// TODO: This could be moved into framework so that that api code can access it also.
public enum FeatureFlag {
    ONE_ARTIFACT_PER_DOCUMENT("couchdb","one.artifact.per.document")
    ;

    private String namespace;
    private String propertyName ;

    private FeatureFlag(String namespace, String propertyName) {
        this.namespace = namespace;
        this.propertyName = propertyName;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getPropertyName() {
        return this.propertyName;
    }
}
