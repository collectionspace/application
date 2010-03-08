package org.collectionspace.chain.csp.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Record implements FieldParent {
	private String id;
	private Map<String,FieldSet> fields=new HashMap<String,FieldSet>();
	private Map<String,Instance> instances=new HashMap<String,Instance>();
	private Spec spec;
	private Field mini_summary,mini_number,display_name;
	private Set<String> type;
	
	/* UI Stuff */
	private String web_url,terms_used_url,number_selector,row_selector,list_key,ui_url,tab_url;
	private boolean in_findedit=false;
	
	/* Service stuff */
	private String services_url,services_list_path,services_record_path,in_tag,urn_syntax,services_instances_path;
		
	// XXX utility methods
	Record(Spec parent,ReadOnlySection section) {
		id=(String)section.getValue("/@id");
		web_url=(String)section.getValue("/web-url");
		if(web_url==null)
			web_url=id;
		type=Util.getSetOrDefault(section,"/@type",new String[]{"record"});
		terms_used_url=(String)section.getValue("/terms-used-url");
		if(terms_used_url==null)
			terms_used_url="nameAuthority";
		number_selector=(String)section.getValue("/number-selector");
		if(number_selector==null)
			number_selector=".csc-entry-number";
		row_selector=(String)section.getValue("/row-selector");
		if(row_selector==null)
			row_selector=".csc-"+id+"-record-list-row:";
		list_key=(String)section.getValue("/list-key");
		if(list_key==null)
			list_key="procedures"+id.substring(0,1).toUpperCase()+id.substring(1);
		ui_url=(String)section.getValue("/ui-url");
		if(ui_url==null)
			ui_url=web_url+".html";
		String findedit=(String)section.getValue("/@in-findedit");
		if(findedit!=null && ("yes".equals(findedit.toLowerCase()) || "1".equals(findedit.toLowerCase())))
			in_findedit=true;
		tab_url=(String)section.getValue("/tab-url");
		if(tab_url==null)
			tab_url=web_url+"-tab";
		services_url=Util.getStringOrDefault(section,"/services-url",id);
		services_list_path=Util.getStringOrDefault(section,"/services-list-path",services_url+"_common:"+services_url+"-common-list/"+services_url+"-list-item");
		services_record_path=Util.getStringOrDefault(section,"/services-record-path",
				services_url+"_common:http://collectionspace.org/services/"+services_url+","+services_url+"_common");
		in_tag=Util.getStringOrDefault(section,"/membership-tag","inAuthority");
		urn_syntax=Util.getStringOrDefault(section,"/urn-syntax","urn:cspace.org.collectionspace.demo."+id+":name({vocab}):"+id+":name({entry})'{display}'");
		services_instances_path=Util.getStringOrDefault(section,"/services-instances-path",
				services_url+"_common:http://collectionspace.org/services/"+services_url+","+services_url+"-common-list/"+services_url+"-list-item");
		spec=parent;
	}
	
	public String getID() { return id; }
	public String getWebURL() { return web_url; }
	public String getUIURL() { return ui_url; }
	public String getTabURL() { return tab_url; }
	public boolean isType(String k) { return type.contains(k); }
	public Spec getSpec() { return spec; }
	public FieldSet[] getAllFields() { return fields.values().toArray(new FieldSet[0]); }
	public String getTermsUsedURL() { return terms_used_url; }
	public String getNumberSelector() { return number_selector; }
	public String getRowSelector() { return row_selector; }
	public String getListKey() { return list_key; }
	public boolean isInFindEdit() { return in_findedit; }
	public String getInTag() { return in_tag; }
	public String getURNSyntax() { return urn_syntax; }
	
	public Instance[] getAllInstances() { return instances.values().toArray(new Instance[0]); }
	public Instance getInstance(String key) { return instances.get(key); }
	
	public String getServicesURL() { return services_url; }
	public String getServicesListPath() { return services_list_path; }
	public String getServicesRecordPath() { return services_record_path; }
	public String getServicesInstancesPath() { return services_instances_path; }
	
	void setMiniNumber(Field f) { mini_number=f; }
	void setMiniSummary(Field f) { mini_summary=f; }
	void setDisplayName(Field f) { display_name=f; }
	public Field getMiniNumber() { return mini_number; }
	public Field getMiniSummary() { return mini_summary; }
	public Field getDisplayNameField() { return display_name; }
	
	public void addField(FieldSet f) {
		fields.put(f.getID(),f);
	}
	
	public void addInstance(Instance n) {
		instances.put(n.getID(),n);
	}
	
	void dump(StringBuffer out) {
		out.append("  record id="+id+"\n");
		out.append("    web_url="+web_url+"\n");
		out.append("    type="+type+"\n");
	}

	public Record getRecord() { return this; }
}
