/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.ras.couchdb.internal.pojos;

public class Find {

    public Object  selector;        // NOSONAR
//	public List<Sort> sort; // NOSONAR
    public Integer limit;           // NOSONAR
    public Integer skip;            // NOSONAR
    public Boolean execution_stats; // NOSONAR
    public String  bookmark;        // NOSONAR

}
