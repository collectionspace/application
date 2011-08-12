/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ReadOnlySection;
/**
 * 
 * @author caret
 * used when creating authority records e.g. person,organization
 * 
 */
public class Instance {
	private Record record;
	private Map<String, String> allStrings = new HashMap<String, String>();
	private Map<String, Boolean> allBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allSets = new HashMap<String, Set<String>>();
	/* just used for documentation to retrieve defaults */
	private Map<String, String> allDefaultStrings = new HashMap<String, String>();
	private Map<String, Boolean> allDefaultBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allDefaultSets = new HashMap<String, Set<String>>();
	
	private Set<String> option_default;
	private Map<String,Option> options=new HashMap<String,Option>();
	private List<Option> options_list=new ArrayList<Option>();

	public Instance(Record record, Map<String,String> data){
		this.record=record;
		this.initStrings(data.get("id"),"@id",null);
		this.initStrings(data.get("title"),"title", getString("@id"));
		this.initStrings(data.get("description"),"description", "");
		this.initStrings(data.get("title-ref"),"title-ref", getString("@id"));
		this.initStrings(data.get("web-url"),"web-url", getString("@id"));
		this.initStrings(data.get("ui-url"),"ui-url", getString("web-url") + ".html");
		this.initStrings(data.get("ui-type"),"@ui-type","plain");
		option_default = Util.getSetOrDefault(data.get("default"), "/@default", new String[]{""});
	}
	
	public Instance(Record record,ReadOnlySection section) {
		this.record=record;
		this.initStrings(section,"@id",null);
		this.initStrings(section,"title", getString("@id"));
		this.initStrings(section,"description", "");
		this.initStrings(section,"title-ref", getString("@id"));
		this.initStrings(section,"web-url", getString("@id"));
		this.initStrings(section,"ui-url", getString("web-url") + ".html");
		this.initStrings(section,"@ui-type","plain");
		option_default = Util.getSetOrDefault(section, "/@default", new String[]{""});
		
		
	}

	/** start generic functions **/
	protected Set<String> initSet(ReadOnlySection section, String name, String[] defaultval){
		Set<String> vard = Util.getSetOrDefault(section, "/"+name, defaultval);
		initSet(vard, name, defaultval);
		return vard;
	}
	protected Set<String> initSet(Set<String> vard, String name, String[] defaultval){
		allDefaultSets.put(name,new HashSet<String>(Arrays.asList(defaultval)));
		allSets.put(name,vard);
		return vard;
	}
	protected String initStrings(ReadOnlySection section, String name, String defaultval){
		String vard = Util.getStringOrDefault(section, "/"+name, defaultval);
		return initStrings(vard, name, defaultval);
	}
	protected String initStrings(String vard, String name, String defaultval){
		allDefaultStrings.put(name,defaultval);
		allStrings.put(name,vard);
		return vard;
		
	}
	protected Boolean initBoolean(ReadOnlySection section, String name, Boolean defaultval){
		Boolean vard = Util.getBooleanOrDefault(section, "/"+name, defaultval);
		initBoolean(vard, name, defaultval);
		return vard;
	}
	protected Boolean initBoolean(Boolean vard, String name, Boolean defaultval){
		allDefaultBooleans.put(name,defaultval);
		allBooleans.put(name,vard);
		return vard;
	}
	protected String[] getAllString(){
		return allStrings.keySet().toArray(new String[0]);
	}
	protected String getString(String name){
		if(allStrings.containsKey(name)){
			return allStrings.get(name);
		}
		return null;
	}

	protected String[] getAllBoolean(){
		return allBooleans.keySet().toArray(new String[0]);
	}
	protected Boolean getBoolean(String name){
		if(allBooleans.containsKey(name)){
			return allBooleans.get(name);
		}
		return null;
	}

	protected String[] getAllSets(){
		return allSets.keySet().toArray(new String[0]);
	}
	
	protected Set<String> getSet(String name){
		if(allSets.containsKey(name)){
			return allSets.get(name);
		}
		return null;
	}
	/** end generic functions **/
	
	public Record getRecord() { return record; }
	public String getID() { return getString("@id"); }
	public String getTitle() { return getString("title"); }
	public String getDesc() { return getString("description"); }
	public String getTitleRef() { return getString("title-ref"); }
	public String getWebURL() { return getString("web-url"); }
	public String getUIURL() { return getString("ui-url"); }

	public void addOption(String id,String name,String sample,boolean dfault) {
		if(id==null){
			id = name.replaceAll("\\W", "").toLowerCase();
		}
		Option opt=new Option(id,name,sample);
		if(dfault){
			opt.setDefault();
			option_default.add(name);
		}
		options.put(id,opt);
		options_list.add(opt);
		if("plain".equals(getString("@ui-type"))){
			allStrings.put("@ui-type", "dropdown");
		}
	}

	public void addOption(String id,String name,String sample,boolean dfault, String desc) {
		if(id==null){
			id = name.replaceAll("\\W", "").toLowerCase();
		}
		Option opt=new Option(id,name,sample,desc);
		if(dfault){
			opt.setDefault();
			option_default.add(name);
		}
		options.put(id,opt);
		options_list.add(opt);
		if("plain".equals(getString("@ui-type"))){
			allStrings.put("@ui-type", "dropdown");
		}
	}
	
	public void deleteOption(String id,String name,String sample,boolean dfault) {
		if(id==null){
			id = name.replaceAll("\\W", "").toLowerCase();
		}
		Option opt=options.get(id);
		if(dfault){
			//opt.setDefault();
			option_default.remove(name);
		}
		options_list.remove(opt);
		options.remove(id);
	}
	public Option getOption(String id) { return options.get(id); }
	public Option[] getAllOptions() { return options_list.toArray(new Option[0]); }
	public String getOptionDefault() { return StringUtils.join(option_default, ",");}
	
	public void config_finish(Spec spec) {}
}
