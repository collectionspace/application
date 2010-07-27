package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author caret
 * 
 *
 */
public class Record implements FieldParent {
	private static final Logger log=LoggerFactory.getLogger(Record.class);
	private String id;
	private Map<String,Structure> structure=new HashMap<String,Structure>();
	private Map<String,Record> subrecords=new HashMap<String,Record>();
	private Map<String,FieldSet> fields=new HashMap<String,FieldSet>();
	private Map<String,FieldSet> servicefields=new HashMap<String,FieldSet>();
	private Map<String,FieldSet> repeatfields=new HashMap<String,FieldSet>();
	
	private Map<String,Instance> instances=new HashMap<String,Instance>();
	private Map<String,FieldSet> summarylist=new HashMap<String,FieldSet>();
	private Map<String, Map<String, FieldSet>> minidataset=new HashMap<String, Map<String,FieldSet>>();
	private Spec spec;
	private FieldSet mini_summary,mini_number,display_name;
	private Set<String> type;
	
	/* UI Stuff */
	private String web_url,terms_used_url,number_selector,row_selector,list_key,ui_url,tab_url;
	private boolean in_findedit=false;
	private boolean is_multipart=false;
	private boolean has_terms_used = false;
	private boolean has_refobj_used = true;
	private String services_search_keyword = "kw";
	
	/* Service stuff */
	private String authorization_name,services_url,services_list_path,in_tag,urn_syntax,authority_vocab_type,services_instances_path,services_single_instance_path;
	private Set<String> authorization_includes;
	private Map<String,String> services_record_paths=new HashMap<String,String>();
	private Map<String,Field> services_filter_param=new HashMap<String,Field>();
	
	// XXX utility methods
	Record(Spec parent,ReadOnlySection section) {
		/* parameters */
		// this is what the service layer id defaults to if not specified later
		// standard = singular form of the concept
		id=(String)section.getValue("/@id");

		// record,authority,compute-displayname can have multiple types using commas
		type=Util.getSetOrDefault(section,"/@type",new String[]{"record"});
		
		//specified that it is included in the findedit uispec
		in_findedit=Util.getBooleanOrDefault(section,"/@in-findedit",false);
		
		//config whether service layer needs call as multipart or not
		is_multipart=Util.getBooleanOrDefault(section,"/is-multipart",true);

		//config whether record type has termsUsed or not (returns empty array in Json if = false )
		has_terms_used = Util.getBooleanOrDefault(section,"/terms-used",true);
		//config whether record type has relatedObj/procs or not (returns empty array in Json if = false )
		has_refobj_used = Util.getBooleanOrDefault(section,"/refobj-used",true);
		
		//config the keyword to use for searching
		services_search_keyword = Util.getStringOrDefault(section,"/services-search-keyword","kw");
		
		// Used to differentiate between authority and vocabulary on create
		in_tag=Util.getStringOrDefault(section,"/membership-tag","inAuthority");
		
		/* UI Layer helpers */
		//ui layer path
		web_url=Util.getStringOrDefault(section,"/web-url",id);
		
		// specify url if not nameAuthority
		terms_used_url=Util.getStringOrDefault(section,"/terms-used-url","nameAuthority");
		
		//ui layer json row
		number_selector=Util.getStringOrDefault(section,"/number-selector",".csc-entry-number");
		
		//ui layer json used in list views
		row_selector=Util.getStringOrDefault(section,"/row-selector",".csc-recordList-row:");
		
		//
		list_key=Util.getStringOrDefault(section,"/list-key","procedures"+id.substring(0,1).toUpperCase()+id.substring(1));
		
		//ui layer path: defaults to web_url if not specified
		ui_url=Util.getStringOrDefault(section,"/ui-url",web_url+".html");
		
		//ui layer path
		tab_url=Util.getStringOrDefault(section,"/tab-url",web_url+"-tab");
		
		/* Service layer helpers */

		//path that the service layer uses to access this record
		services_url=Util.getStringOrDefault(section,"/services-url",id);
		
		//authorization name

		authorization_name = Util.getStringOrDefault(section,"/authorization-name",id);
		if(type.contains("record")){
			authorization_includes = Util.getSetOrDefault(section,"/authorization-includes",new String[]{services_url, "/"+services_url+"/*/authorityrefs/"});
		}
		else if(type.contains("authority")){
			authorization_includes = Util.getSetOrDefault(section,"/authorization-includes",new String[]{services_url,services_url+"/*/items/", "/"+services_url+"/*/items/*/authorityrefs/", "/"+services_url+"/*/items/*/refobjs/", "/"+services_url+"/*/items/*/contacts/"});
		}
		else if(type.contains("id")){
			authorization_includes = Util.getSetOrDefault(section,"/authorization-includes",new String[]{id,"idgenerators", "/idgenerators/*/ids/"});
		}
		else {
			authorization_includes = Util.getSetOrDefault(section,"/authorization-includes",new String[]{});
		}
		
		//service layer paths to list data for this record type
		services_list_path=Util.getStringOrDefault(section,"/services-list-path",services_url+"-common-list/"+services_url+"-list-item");
		
		
		//used by service layer to construct authority names
		urn_syntax=Util.getStringOrDefault(section,"/urn-syntax","urn:cspace.org.collectionspace.demo."+id+":name({vocab}):"+id+":name({entry})'{display}'");
		authority_vocab_type = Util.getStringOrDefault(section, "/authority-vocab-type", "PersonAuthority");
		//
		services_instances_path=Util.getStringOrDefault(section,"/services-instances-path",
				services_url+"_common:http://collectionspace.org/services/"+services_url+","+services_url+"-common-list/"+services_url+"-list-item");
		
		//
		services_single_instance_path=Util.getStringOrDefault(section,"/services-single-instance-path",
				services_url+"_common:http://collectionspace.org/services/"+services_url+","+services_url+"-common");
		
		spec=parent;
	}
	
	public String getID() { return id; }
	public String getWebURL() { return web_url; }
	public String getUIURL() { return ui_url; }
	public String getTabURL() { return tab_url; }
	public boolean isType(String k) { return type.contains(k); }
	public Spec getSpec() { return spec; }
	public FieldSet[] getAllServiceFields() { return servicefields.values().toArray(new FieldSet[0]); }
	public FieldSet[] getAllFields() { return fields.values().toArray(new FieldSet[0]); }
	public FieldSet getField(String id) { return fields.get(id); }
	public FieldSet[] getAllRepeatFields() { return repeatfields.values().toArray(new FieldSet[0]); }
	/*
	 * includes all the fields that are children of repeats as well as top level fields.
	 */
	public FieldSet getRepeatField(String id) { return repeatfields.get(id); }
	public Structure getStructure(String id) { return structure.get(id); }
	public Record getSubrecord(String id) { return subrecords.get(id); }
	public Record[] getAllSubRecords(){ 
		if(subrecords.values().isEmpty()){
			for(FieldSet fs : this.getAllRepeatFields()){
				if(fs.usesRecord()){
					this.addSubRecord(fs.usesRecordId());
				}
			}
		}
		return subrecords.values().toArray(new Record[0]); 
	}
	
	public String getTermsUsedURL() { return terms_used_url; }
	public String getNumberSelector() { return number_selector; }
	public String getRowSelector() { return row_selector; }
	public String getListKey() { return list_key; }
	
	public boolean isInFindEdit() { return in_findedit; }
	public boolean isMultipart() { return is_multipart; }
	public boolean hasTermsUsed() { return has_terms_used; }
	public boolean hasRefObjUsed() { return has_refobj_used; }

	public String getServicesSearchKeyword(){ return services_search_keyword; }
	public String getInTag() { return in_tag; }
	public String getURNSyntax() { return urn_syntax; }
	public String getVocabType() {return authority_vocab_type; }

	public Instance[] getAllInstances() { return instances.values().toArray(new Instance[0]); }
	public Instance getInstance(String key) { return instances.get(key); }
	
	public String getServicesURL() { return services_url; }
	public String getServicesListPath() { return services_list_path; }
	public String getServicesInstancesPath() { return services_instances_path; }
	public String getServicesSingleInstancePath() { return services_single_instance_path; }
	public String[] getServicesRecordPaths() { return services_record_paths.keySet().toArray(new String[0]); }
	public String getServicesRecordPath(String name) { return services_record_paths.get(name); }

	void setMiniNumber(Field f) { mini_number=f; }
	void setMiniSummary(Field f) { mini_summary=f; }
	void setMiniNumber(Repeat f) { mini_number=f; }
	void setMiniSummary(Repeat f) { mini_summary=f; }
	void setDisplayName(Field f) { display_name=f; }
	void setServicesRecordPath(String section,String path) { services_record_paths.put(section,path); }
	void setServicesFilterParam(String param,Field field) { services_filter_param.put(param,field); }
	
	public FieldSet getMiniNumber() { return mini_number; }
	public FieldSet getMiniSummary() { return mini_summary; }
	public FieldSet[] getAllMiniSummaryList() { return summarylist.values().toArray(new FieldSet[0]); }
	public FieldSet getMiniSummaryList(String key) { return summarylist.get(key); }
	public FieldSet getDisplayNameField() { return display_name; }
	public Field getFieldByServicesFilterParam(String param) { return services_filter_param.get(param); }
	
	//authorization
	public String getAuthorizationName(){	return authorization_name; }
	public String[] getAllAuthorizationTypes(){ return authorization_includes.toArray(new String[0]) ;	}
	public Boolean isAuthorizationType(String name){
		return authorization_includes.contains(name);
	}
	

	public void addField(FieldSet f) {
		fields.put(f.getID(),f);
		if(f.isInServices()){
			servicefields.put(f.getID(),f);
		}
	}
	public void addAllField(FieldSet f) {
		repeatfields.put(f.getID(),f);
	}
	public void addStructure(Structure s) {
		structure.put(s.getID(),s);
	}
	public void addSubRecord(Record r) {
		subrecords.put(r.getID(),r);
	}
	
	public void addInstance(Instance n) {
		instances.put(n.getID(),n);
		spec.addInstance(n);
	}
	//obsolete?
	public void addMiniSummaryList(FieldSet f){
		summarylist.put(f.getID(), f);
	}
	public void addMiniDataSet(FieldSet f, String s){
		//s:{ name: field, name: field, name: field }
		if(!minidataset.containsKey(s)){
			Map<String,FieldSet> subdata=new HashMap<String,FieldSet>();
			minidataset.put(s, subdata);
		}
		minidataset.get(s).put(f.getID(), f);
	}
	public void addMiniDataSet(Repeat r, String s){
		//s:{ name: field, name: field, name: field }
		if(!minidataset.containsKey(s)){
			Map<String,FieldSet> subdata=new HashMap<String,FieldSet>();
			minidataset.put(s, subdata);
		}
		minidataset.get(s).put(r.getID(), r);
	}
	public FieldSet[] getMiniDataSetByName(String s){
		if(minidataset.containsKey(s)){
			return minidataset.get(s).values().toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}
	public String[] getAllMiniDataSets(){
		if(minidataset.isEmpty()){
			return new String[0];
		}
		return minidataset.keySet().toArray(new String[0]);
	}
	
	void dump(StringBuffer out) {
		out.append("  record id="+id+"\n");
		out.append("    web_url="+web_url+"\n");
		out.append("    type="+type+"\n");
	}

	public Record getRecord() { return this; }
	
	public void config_finish(Spec spec) {
		for(Instance n : instances.values())
			n.config_finish(spec);
		for(FieldSet fs : fields.values())
			fs.config_finish(spec);
	}
}
