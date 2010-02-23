package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.chain.csp.config.ReadOnlySection;

// XXX unentangle UI and SVC parts
public class Field {
	private Record record;
	private String id,selector,type,autocomplete_selector,container_selector,title_selector;
	private boolean is_autocomplete=false,in_title=false;
	private Map<String,Option> options=new HashMap<String,Option>();
	private List<Option> options_list=new ArrayList<Option>();
	
	public Field(Record record,ReadOnlySection section) {
		id=(String)section.getValue("/@id");
		if(section.getValue("/@autocomplete")!=null)
			is_autocomplete=true;
		selector=(String)section.getValue("/selector");		
		if(selector==null)
			selector=".csc-"+id;
		type=(String)section.getValue("/ui-type");		
		if(type==null)
			type="plain";
		autocomplete_selector=(String)section.getValue("/autocomplete-selector");
		if(autocomplete_selector==null)
			autocomplete_selector=selector+"-autocomplete";
		container_selector=(String)section.getValue("/container-selector");
		if(container_selector==null)
			container_selector=selector+"-container";
		title_selector=(String)section.getValue("/title-selector");
		if(title_selector==null)
			title_selector=selector+"-titlebar";		
		String in_title=(String)section.getValue("/@in-title");
		if(in_title!=null && ("1".equals(in_title) || "yes".equals(in_title.toLowerCase())))
			this.in_title=true;
		this.record=record;
	}
	
	public Record getRecord() { return record; }
	public String getID() { return id; }
	public String getAutocompleteSelector() { return autocomplete_selector; }
	public String getContainerSelector() { return container_selector; }
	public String getSelector() { return selector; }
	public String getUIType() { return type; }
	public boolean isAutocomplete() { return is_autocomplete; }
	public boolean isInTitle() { return in_title; }
	public String getTitleSelector() { return title_selector; }
	
	void setType(String in) { type=in; }
	void addOption(String id,String name) { addOption(id,name,null); }
	void addOption(String id,String name,String sample) {
		Option opt=new Option(id,name,sample);
		options.put(id,opt);
		options_list.add(opt);
		if("plain".equals(type))
			type="dropdown";
	}
	
	public Option getOption(String id) { return options.get(id); }
	public Option[] getAllOptions() { return options_list.toArray(new Option[0]); }
}
