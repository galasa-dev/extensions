/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.extensions.common.couchdb.pojos;

import com.google.gson.annotations.SerializedName;

public class IdRev {

    @SerializedName("_id")
    public String id;

    @SerializedName("_rev")
    public String rev;

    public String getId() {
        return id;
    }

    public String getRev() {
        return rev;
    }
}