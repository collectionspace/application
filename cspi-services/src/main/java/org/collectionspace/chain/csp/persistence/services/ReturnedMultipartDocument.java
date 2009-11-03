/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;

/** Utility class returns documents and statuses */
public class ReturnedMultipartDocument {
	private int status;
	private Map<String,Document> docs=new HashMap<String,Document>();
	
	ReturnedMultipartDocument(int status) { this.status=status; }
	void addDocument(String name,Document doc) { docs.put(name,doc); }	
	public String[] listDocuments() { return docs.keySet().toArray(new String[0]); }
	public Document getDocument(String name) { return docs.get(name); }
	public int getStatus() { return status; }
}
