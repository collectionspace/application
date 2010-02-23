package org.collectionspace.chain.csp.schema;

import java.util.HashMap;
import java.util.Map;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Record {
	private String id,web_url;
	private String type;
	private Spec spec;
	private Map<String,Field> fields=new HashMap<String,Field>();
	
	Record(Spec parent,ReadOnlySection section) {
		id=(String)section.getValue("/@id");
		web_url=(String)section.getValue("/web-url");
		if(web_url==null)
			web_url=id;
		type=(String)section.getValue("/@type");
		if(type==null)
			type="record";
		spec=parent;
	}
	
	public String getID() { return id; }
	public String getWebURL() { return web_url; }
	public String getType() { return type; }
	public Spec getSpec() { return spec; }
	public Field[] getAllFields() { return fields.values().toArray(new Field[0]); }
	
	public void addField(Field f) {
		fields.put(f.getID(),f);
	}
	
	void dump(StringBuffer out) {
		out.append("  record id="+id+"\n");
		out.append("    web_url="+web_url+"\n");
		out.append("    type="+type+"\n");
	}
}
