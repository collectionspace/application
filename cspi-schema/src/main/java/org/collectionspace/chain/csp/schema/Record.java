package org.collectionspace.chain.csp.schema;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Record implements FieldParent {
	private String id,web_url,terms_used_url;
	private String type;
	private Spec spec;
	private Map<String,FieldSet> fields=new HashMap<String,FieldSet>();
	
	Record(Spec parent,ReadOnlySection section) {
		id=(String)section.getValue("/@id");
		web_url=(String)section.getValue("/web-url");
		if(web_url==null)
			web_url=id;
		type=(String)section.getValue("/@type");
		if(type==null)
			type="record";
		terms_used_url=(String)section.getValue("/terms-used-url");
		if(terms_used_url==null)
			terms_used_url="nameAuthority";
		spec=parent;
	}
	
	public String getID() { return id; }
	public String getWebURL() { return web_url; }
	public String getType() { return type; }
	public Spec getSpec() { return spec; }
	public FieldSet[] getAllFields() { return fields.values().toArray(new FieldSet[0]); }
	public String getTermsUsedURL() { return terms_used_url; }
	
	public void addField(FieldSet f) {
		fields.put(f.getID(),f);
	}
	
	void dump(StringBuffer out) {
		out.append("  record id="+id+"\n");
		out.append("    web_url="+web_url+"\n");
		out.append("    type="+type+"\n");
	}

	public Record getRecord() { return this; }
}
