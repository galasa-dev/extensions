/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.ras.couchdb.internal.pojos;

public class Find {
	
	public Object     selector; // NOSONAR
//	public List<Sort> sort; // NOSONAR
	public Integer    limit; // NOSONAR
	public Integer    skip; // NOSONAR
	public Boolean    execution_stats; // NOSONAR
	public String     bookmark;// NOSONAR

}
