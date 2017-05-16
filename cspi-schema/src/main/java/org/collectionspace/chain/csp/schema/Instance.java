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
	protected SchemaUtils utils = new SchemaUtils();
	
	private Set<String> option_default;
	private Map<String,Option> options=new HashMap<String,Option>();
	private List<Option> options_list=new ArrayList<Option>();
	
	private final static String NPT_ALLOWED = "nptAllowed";

	public Instance(Record record, Map<String,String> data){
		this.record=record;
		utils.initStrings(data.get("id"),"@id",null);
		utils.initStrings(data.get("title"),"title", utils.getString("@id"));
		utils.initStrings(data.get("description"),"description", "");
		utils.initStrings(data.get("title-ref"),"title-ref", utils.getString("@id"));
		utils.initStrings(data.get("web-url"),"web-url", utils.getString("@id"));
		utils.initStrings(data.get("ui-url"),"ui-url", utils.getString("web-url") + ".html");
		utils.initStrings(data.get("ui-type"),"@ui-type","plain");
		String createUnreferencedSpec = data.get("create-unreferenced");
		Boolean createUnreferenced = (createUnreferencedSpec == null) || Boolean.parseBoolean(createUnreferencedSpec);
		utils.initBoolean(createUnreferenced, "@create-unreferenced", false);
		String nptSpec = data.get(NPT_ALLOWED);
		Boolean nptAllowed = (nptSpec==null) || Boolean.parseBoolean(nptSpec);
		utils.initBoolean(nptAllowed,NPT_ALLOWED,true);
		option_default = Util.getSetOrDefault(data.get("default"), "/@default", new String[]{""});
	}
	
	public Instance(Record record,ReadOnlySection section) {
		this.record=record;
		utils.initStrings(section,"@id",null);
		utils.initStrings(section,"title", utils.getString("@id"));
		utils.initStrings(section,"description", "");
		utils.initStrings(section,"title-ref", utils.getString("@id"));
		utils.initStrings(section,"web-url", utils.getString("@id"));
		utils.initStrings(section,"ui-url", utils.getString("web-url") + ".html");
		utils.initStrings(section,"@ui-type","plain");
		utils.initBoolean(section, "@create-unreferenced", false);
		utils.initBoolean(section,NPT_ALLOWED,true);
		option_default = Util.getSetOrDefault(section, "/@default", new String[]{""});
		
		
	}

	
	public Record getRecord() { return record; }
	public String getID() { return utils.getString("@id"); }
	public String getTitle() { return utils.getString("title"); }
	public String getDesc() { return utils.getString("description"); }
	public String getTitleRef() { return utils.getString("title-ref"); }
	public String getWebURL() { return utils.getString("web-url"); }
	public String getUIURL() { return utils.getString("ui-url"); }
	public boolean getNPTAllowed() { return utils.getBoolean(NPT_ALLOWED); }
	public boolean getCreateUnreferenced() { return utils.getBoolean("@create-unreferenced"); }
	
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
		if("plain".equals(utils.getString("@ui-type"))){
			utils.setString("@ui-type", "dropdown");
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
		if("plain".equals(utils.getString("@ui-type"))){
			utils.setString("@ui-type", "dropdown");
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
