package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Instance {
	private Record record;
	private String id,title,title_ref;
	
	public Instance(Record record,ReadOnlySection section) {
		this.record=record;
		id=(String)section.getValue("/@id");
		title=Util.getStringOrDefault(section,"/title",id);
		title_ref=Util.getStringOrDefault(section,"/title-ref",id);		
	}
	
	public Record getRecord() { return record; }
	public String getID() { return id; }
	public String getTitle() { return title; }
	public String getTitleRef() { return title_ref; }
}
