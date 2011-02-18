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
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// XXX unentangle UI and SVC parts
public class Field implements FieldSet {
	private static final Logger log = LoggerFactory.getLogger(Field.class);
	private Map<String, String> allStrings = new HashMap<String, String>();
	private Map<String, Boolean> allBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allSets = new HashMap<String, Set<String>>();
	/* just used for documentation to retrieve defaults */
	private Map<String, String> allDefaultStrings = new HashMap<String, String>();
	private Map<String, Boolean> allDefaultBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allDefaultSets = new HashMap<String, Set<String>>();
	
	private FieldParent parent;

	private Map<String, Instance> instances = new HashMap<String, Instance>();

	/* UI */
	private Boolean seperate_default = false;
	private Stack<String> merged = new Stack<String>();
	private Map<String, Option> options = new HashMap<String, Option>();
	private List<Option> options_list = new ArrayList<Option>();

	/* Services */

	public Field(FieldParent record, ReadOnlySection section) {

		allStrings.put("parentID", record.getRecord().getID());
		this.setRepeatSubRecord(false);
		this.initStrings(section,"@id",null);
		
		this.initSet(section,"@autocomplete",new String[] { "" });
		this.initBoolean(section,"@container",false);
		this.initBoolean(section,"@xxx_ui_refactored",true);

		this.initStrings(section,"selector", ".csc-" + getString("parentID") + "-" +  getString("@id"));
		this.initStrings(section,"label", "" + getString("parentID") + "-" +  getString("@id")+"Label");
		this.initStrings(section,"@userecord", "");
		this.initStrings(section,"@selector-affix", "");
		this.initStrings(section,"@label-affix", "-label");
		
		this.initStrings(section,"linktext", "${items.0.number}");
		
		this.initStrings(section,"linktext-target", "${items.0.recordtype}.html?csid=${items.0.csid}");

		this.initStrings(section,"@ui-type", "plain");
		if (getString("@ui-type").equals("date")) {
			seperate_default = true;
		}
		
		this.initBoolean(section,"@seperate_ui_container",seperate_default);
		
		this.initStrings(section,"autocomplete-selector", getString("selector") + "-autocomplete");
		this.initStrings(section,"container-selector", getString("selector") + "-container");
		this.initStrings(section,"title-selector", getString("selector") + "-titlebar");
		// used by uispec to create new structure
		this.initBoolean(section,"@as-expander",false);
		
		
		this.initBoolean(section,"@in-title",false);
		// no longer needed/used
		// in_tab = Util.getBooleanOrDefault(section, "/@in-tab", false);
		
		this.initStrings(section,"services-tag", getString("@id"));

		Set<String> minis = Util.getSetOrDefault(section, "/@mini",
				new String[] { "" });
		if (minis.contains("number")) {
			record.getRecord().setMiniNumber(this);
		}
		if (minis.contains("summary")) {
			record.getRecord().setMiniSummary(this);
		}
		if (minis.contains("list")) {
			record.getRecord().addMiniSummaryList(this);
		}
		for (String s : minis) {
			record.getRecord().addMiniDataSet(this, s);
		}

		this.initBoolean(section,"@display-name",false);
		if (getBoolean("@display-name"))
			record.getRecord().setDisplayName(this);
		this.parent = record;
		
		this.initBoolean(section,"@exists-in-services",true);
		
		this.initSet(section,"enum/default",new String[] { "" });		
		this.initBoolean(section,"enum/@has-blank",true);
		this.initStrings(section,"enum/blank-value", "Please select a value");
		
		this.initSet(section,"@default",new String[] { "" });
		this.initStrings(section,"@section", "common");
		this.initStrings(section,"services-filter-param", null);
		if (getString("services-filter-param") != null)
			record.getRecord().setServicesFilterParam(getString("services-filter-param"),this);
		
		this.initStrings(section,"@datatype", "");
		
		this.initSet(section,"@attributes",new String[] {"GET","PUT","POST","DELETE"});
	}


	/** start generic functions **/
	protected Set<String> initSet(ReadOnlySection section, String name, String[] defaultval){
		Set<String> vard = Util.getSetOrDefault(section, "/"+name, defaultval);
		allDefaultSets.put(name,new HashSet<String>(Arrays.asList(defaultval)));
		allSets.put(name,vard);
		return vard;
	}
	protected String initStrings(ReadOnlySection section, String name, String defaultval){
		String vard = Util.getStringOrDefault(section, "/"+name, defaultval);
		allDefaultStrings.put(name,defaultval);
		allStrings.put(name,vard);
		return vard;
	}
	protected Boolean initBoolean(ReadOnlySection section, String name, Boolean defaultval){
		Boolean vard = Util.getBooleanOrDefault(section, "/"+name, defaultval);
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
	public String getID() {
		return  getString("@id");
	}

	public String getAutocompleteSelector() {
		return getString("autocomplete-selector");
	}

	public String getContainerSelector() {
		return getString("container-selector");
	}

	public String getSelector() {
		return getString("selector");
	}
	public String getLabel() {
		return getString("label");
	}

	public String getLinkTextTarget() {
		return getString("linktext-target");
	}

	public String getLinkText() {
		return getString("linktext");
	}

	public String getUIType() {
		return getString("@ui-type");
	}

	public Boolean isInTitle() {
		return getBoolean("@in-title");
	}

	// public boolean isInTab() { return in_tab; }
	public Boolean hasContainer() {
		return getBoolean("@container");
	}

	public Boolean isInServices() {
		return getBoolean("@exists-in-services");
	}

	public boolean isExpander() {
		return getBoolean("@as-expander");
	}

	public boolean isRepeatSubRecord() {
		return getBoolean("@is-subrecord");
	}
	
	public void setRepeatSubRecord(Boolean var) {
		allBooleans.put("@is-subrecord",var);
	}

	public Boolean isRefactored() {
		return !getBoolean("@seperate_ui_container");
	} // used until UI layer has moved all autocomplete to one container view

	public String getTitleSelector() {
		return getString("title-selector");
	}

	public String getServicesFilterParam() {
		return getString("services-filter-param");
	}

	public String getServicesTag() {
		return getString("services-tag");
	}
	
	//XXX could be used for validation at the app layer
	public String getDataType(){
		return getString("@datatype");
	}

	void setType(String in) {
		allStrings.put("@ui-type",in);
	}

	// CSPACE-869
	void addMerge(String mid, String rank) {
		try {
			int index = Integer.parseInt(rank);
			if (merged.size() < index) {
				merged.setSize(index);
			}
			merged.add(index, mid);
			parent.getRecord().setMerged(this);
		} catch (Exception e) {
			// something wrong - could have been a non number
			log.error("Failed to add Merge field " +  getString("@id") + " into field "
					+  getString("@id") + " at rank " + rank);
		}

	}

	public String getMerge(int index) {
		return merged.get(index);
	}

	public List<String> getAllMerge() {
		return merged;
	}

	public Boolean hasMergeData() {
		if (merged.isEmpty())
			return false;

		return true;
	}

	public String getLabelAffix() {
		return getString("@label-affix");
	}
	public String getSelectorAffix() {
		return getString("@selector-affix");
	}

	public Boolean usesRecord() {
		if (getString("@userecord") != null && !getString("@userecord").equals("")) {
			return true;
		}
		return false;
	}

	public Record usesRecordId() {
		if (usesRecord()) {
			return this.getRecord().getSpec().getRecord(getString("@userecord"));
		}
		return null;
	}

	void addOption(String id, String name, String sample, boolean dfault) {
		Option opt = new Option(id, name, sample);
		if (dfault) {
			opt.setDefault();
			getSet("@default").add(opt.getID());
		}
		options.put(id, opt);
		options_list.add(opt);
		if ("plain".equals(getString("@ui-type")))
			setType("dropdown");
	}

	public Option getOption(String id) {
		return options.get(id);
	}

	public Option[] getAllOptions() {
		return options_list.toArray(new Option[0]);
	}
	
	public String[] getAllFieldPerms(){
		return getSet("@attributes").toArray(new String[0]);
	}

	public boolean hasFieldPerm(String perm){
		return getSet("@attributes").contains(perm);
	}
	
	public String getOptionDefault() {
		getSet("@default").remove("");
		return StringUtils.join(getSet("@default"), ",");
	}

	public FieldParent getParent() {
		return this.parent;
	}

	public Record getRecord() {
		return parent.getRecord();
	}

	public String[] getIDPath() {
		if (parent instanceof Repeat) {
			String[] pre = ((Repeat) parent).getIDPath();
			String[] out = new String[pre.length + 1];
			System.arraycopy(pre, 0, out, 0, pre.length);
			out[pre.length] =  getString("@id");
			return out;
		} else {
			return new String[] {  getString("@id") };
		}
	}

	public String getSection() {
		return getString("@section");
	}

	public Instance[] getAllAutocompleteInstances() {
		return instances.values().toArray(new Instance[0]);
	}

	// XXX hack so just returns the first autocomplete instance if multiple
	// assigned
	public Instance getAutocompleteInstance() {
		if (hasAutocompleteInstance()) {
			return getAllAutocompleteInstances()[0];
		}
		return null;
	}

	public Boolean hasAutocompleteInstance() {
		if (getSet("@autocomplete").size() > 0) {
			for (String autocomplete_instance_id : getSet("@autocomplete")) {
				autocomplete_instance_id = autocomplete_instance_id.trim();
				if (!autocomplete_instance_id.equals("")) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasEnumBlank() {
		return getBoolean("enum/@has-blank");
	}

	public String enumBlankValue() {
		return getString("enum/blank-value");
	}

	public String getEnumDefault() {
		return StringUtils.join(getSet("enum/default"), ",");
	}

	public boolean isEnumDefault(String name) {
		if (getSet("enum/default").contains(name)) {
			return true;
		}
		return false;
	}

	public void config_finish(Spec spec) {
		if (getSet("@autocomplete").size() > 0) {
			for (String autocomplete_instance_id : getSet("@autocomplete")) {
				autocomplete_instance_id = autocomplete_instance_id.trim();
				if (!autocomplete_instance_id.equals("")) {
					instances.put(autocomplete_instance_id, spec
							.getInstance(autocomplete_instance_id));
				}
			}
		}
	}
	
	void dump(StringBuffer out) {
		out.append("  id=" + this.getID() + "\n");
		out.append("    type=" + getSet("@type") + "\n");

		for(String s: this.getAllString()){
			out.append("String,"+ s);
			out.append(",Value,"+ this.allStrings.get(s));
			out.append(",Default,"+ this.allDefaultStrings.get(s));
			out.append("\n");
		}
		for(String s: this.getAllBoolean()){
			out.append("Boolean,"+ s);
			out.append(",Value,"+ this.allBooleans.get(s));
			out.append(",Default,"+ this.allDefaultBooleans.get(s));
			out.append("\n");
		}
		for(String s: this.getAllSets()){
			out.append("Set,"+ s);
			out.append(",Value,"+ this.allSets.get(s));
			out.append(",Default,"+ this.allDefaultSets.get(s));
			out.append("\n");
		}
	}
}