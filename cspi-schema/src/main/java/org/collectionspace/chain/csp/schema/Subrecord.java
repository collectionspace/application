/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Subrecord extends Record implements FieldParent{

	private Record record;
	private String id;
	private Map<String,FieldSet> fields=new HashMap<String,FieldSet>();
	
	public Subrecord(Record record, ReadOnlySection section){
		super(record.getSpec(), section);
		this.record = record;
		id=(String)section.getValue("/@id");
	}
	
	@Override
	public Record getRecord() { return record; }
	public String getID() { return id; }
	
	public void addField(FieldSet f) {
		fields.put(f.getID(),f);
	}
	
	public FieldSet[] getAllFields() { return fields.values().toArray(new FieldSet[0]); }
	public FieldSet getField(String id) { return fields.get(id); }
	
	public void config_finish(Spec spec) {}
}
