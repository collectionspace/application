package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// XXX unentangle UI and SVC parts
public class Field implements FieldSet {
	private static final Logger log=LoggerFactory.getLogger(Field.class);
	private FieldParent parent;
	private String id;
	private Set<String> autocomplete_instance_ids;
	private Set<String> enum_default;

	private Map<String,Instance> instances=new HashMap<String,Instance>();
	
	
	/* UI */
	private String parentID,selector_affix,enum_blank,selector,type,autocomplete_selector,container_selector,title_selector,linktext_target,linktext,userecord;
	private boolean enum_hasblank=true, exists_in_service= true,in_title=false,display_name=false, has_container=true, xxx_ui_refactored = false ;
	private Stack<String> merged = new Stack<String>();
	private Map<String,Option> options=new HashMap<String,Option>();
	private List<Option> options_list=new ArrayList<Option>();

	/* Services */
	private String services_tag,services_section,services_filter_param;
		
	public Field(FieldParent record,ReadOnlySection section) {

		parentID = record.getRecord().getID();
		id=(String)section.getValue("/@id");
		autocomplete_instance_ids=Util.getSetOrDefault(section,"/@autocomplete",new String[]{""});
		has_container = Util.getBooleanOrDefault(section, "/@container", false);
		xxx_ui_refactored = Util.getBooleanOrDefault(section, "/@xxx_ui_refactored", true);

		selector=Util.getStringOrDefault(section,"/selector",".csc-"+parentID+"-"+id);
		userecord = Util.getStringOrDefault(section, "/@userecord", "");

		selector_affix = Util.getStringOrDefault(section, "/@selector-affix", "");
		linktext=Util.getStringOrDefault(section,"/linktext","${items.0.number}");
		linktext_target=Util.getStringOrDefault(section,"/linktext-target","${items.0.recordtype}.html?csid=${items.0.csid}");
		type=Util.getStringOrDefault(section,"/@ui-type","plain");
		autocomplete_selector=Util.getStringOrDefault(section,"/autocomplete-selector",selector+"-autocomplete");
		container_selector=Util.getStringOrDefault(section,"/container-selector",selector+"-container");
		title_selector=Util.getStringOrDefault(section,"/title-selector",selector+"-titlebar");
		

		in_title = Util.getBooleanOrDefault(section, "/@in-title", false);
		//no longer needed/used
		//in_tab = Util.getBooleanOrDefault(section, "/@in-tab", false);

		services_tag=Util.getStringOrDefault(section,"/services-tag",id);

		Set<String> minis=Util.getSetOrDefault(section,"/@mini",new String[]{""});
		if(minis.contains("number")){	record.getRecord().setMiniNumber(this);	}
		if(minis.contains("summary")){	record.getRecord().setMiniSummary(this);	}
		if(minis.contains("list")){	record.getRecord().addMiniSummaryList(this);	}
		for(String s : minis){
			record.getRecord().addMiniDataSet(this,s);
		}
		
		display_name=Util.getBooleanOrDefault(section,"/@display-name",false);
		if(display_name)
			record.getRecord().setDisplayName(this);
		this.parent=record;
		
		exists_in_service = Util.getBooleanOrDefault(section, "/@exists-in-services", true);
		enum_default = Util.getSetOrDefault(section, "/enum/default", new String[]{""});
		enum_hasblank = Util.getBooleanOrDefault(section, "/enum/@has-blank",true);
		enum_blank = Util.getStringOrDefault(section, "/enum/blank-value", "Please select an item");
		services_section=Util.getStringOrDefault(section,"/@section","common");
		services_filter_param=Util.getStringOrDefault(section,"/services-filter-param",null);
		if(services_filter_param!=null)
			record.getRecord().setServicesFilterParam(services_filter_param,this);
	}
	
	public String getID() { return id; }
	public String getAutocompleteSelector() { return autocomplete_selector; }
	public String getContainerSelector() { return container_selector; }
	public String getSelector() { return selector; }
	public String getLinkTextTarget() { return linktext_target; }
	public String getLinkText() { return linktext; }
	public String getUIType() { return type; }
	public boolean isInTitle() { return in_title; }
	//public boolean isInTab() { return in_tab; }
	public boolean hasContainer() {	return has_container;	}
	public boolean isInServices() {	return exists_in_service;	}
	public boolean isRefactored() { return xxx_ui_refactored; } //used until UI layer has moved all autocomplete to one container view
	public String getTitleSelector() { return title_selector; }
	public String getServicesFilterParam() { return services_filter_param; }
	public String getServicesTag() { return services_tag; }
	
	void setType(String in) { type=in; }
	//CSPACE-869
	void addMerge(String id, String rank){
		try{
			int index = Integer.parseInt(rank);
			if(merged.size() < index){	merged.setSize(index);	}
			merged.add(index, id);
		}
		catch(Exception e){
			// something wrong - could have been a non number
			log.error("Failed to add Merge field "+id+" into field "+this.id+ " at rank "+rank);
		}
		
	}
	public String getMerge(int index) { return merged.get(index); }
	public List<String> getAllMerge() {	return merged;	}
	public Boolean hasMergeData() {
		if(merged.isEmpty())
			return false;
		
		return true;
	}
	
	public String getSelectorAffix(){ return selector_affix; }
	public boolean usesRecord(){ if(userecord != null && !userecord.equals("")){ return true; } return false;}
	public Record usesRecordId(){ if(usesRecord()){ return this.getRecord().getSpec().getRecord(userecord); } return null; }
	
	void addOption(String id,String name,String sample,boolean dfault) {
		Option opt=new Option(id,name,sample);
		if(dfault)
			opt.setDefault();
		options.put(id,opt);
		options_list.add(opt);
		if("plain".equals(type))
			type="dropdown";
	}
	
	public Option getOption(String id) { return options.get(id); }
	public Option[] getAllOptions() { return options_list.toArray(new Option[0]); }

	public Record getRecord() { return parent.getRecord(); }

	public String[] getIDPath() { 
		if(parent instanceof Repeat) {
			String[] pre=((Repeat)parent).getIDPath();
			String[] out=new String[pre.length+1];
			System.arraycopy(pre,0,out,0,pre.length);
			out[pre.length]=id;
			return out;
		} else {
			return new String[]{id};
		}
	}
	
	public String getSection() { return services_section; }

	public Instance[] getAllAutocompleteInstances() { return instances.values().toArray(new Instance[0]); }
	//XXX hack so just returns the first autocomplete instance if multiple assigned
	public Instance getAutocompleteInstance() { 
		if(hasAutocompleteInstance()) {
			return getAllAutocompleteInstances()[0]; 
		}
		return null;
	}
	public boolean hasAutocompleteInstance(){ 
		if(autocomplete_instance_ids.size()>0){
			for (String autocomplete_instance_id : autocomplete_instance_ids){
				autocomplete_instance_id = autocomplete_instance_id.trim();
				if(!autocomplete_instance_id.equals("")){
					return true;
				}
			}
		}
		return false; 
	}
	public boolean hasEnumBlank(){ return enum_hasblank; }
	public String enumBlankValue(){ return enum_blank; }
	public boolean isEnumDefault(String name){
		if(enum_default.contains(name)){
			return true;
		}
		return false;
	}
	
	public void config_finish(Spec spec) {
		if(autocomplete_instance_ids.size()>0){
			for (String autocomplete_instance_id : autocomplete_instance_ids){
				autocomplete_instance_id = autocomplete_instance_id.trim();
				if(!autocomplete_instance_id.equals("")){
					instances.put(autocomplete_instance_id, spec.getInstance(autocomplete_instance_id));
				}
			}
		}
	}
}
