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

// XXX only one level of repetition at the moment. Should only be a matter of type furtling.
public class Repeat implements FieldSet, FieldParent {
	private Map<String, String> allStrings = new HashMap<String, String>();
	protected Map<String, Boolean> allBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allSets = new HashMap<String, Set<String>>();
	/* just used for documentation to retrieve defaults */
	private Map<String, String> allDefaultStrings = new HashMap<String, String>();
	private Map<String, Boolean> allDefaultBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allDefaultSets = new HashMap<String, Set<String>>();
	
	
	protected String[] services_parent;
	protected FieldParent parent;
	protected Stack<String> merged = new Stack<String>();
	protected List<FieldSet> children = new ArrayList<FieldSet>();
	protected Map<String,List<FieldSet>> childrenperm = new HashMap<String, List<FieldSet>>();

	/* Services */

	public Repeat(Record record, ReadOnlySection section) {
		this.parent = record;
		allStrings.put("parentID", record.getID());
		this.initialiseVariables(section);
	}

	public Repeat(Structure structure, ReadOnlySection section) {
		this.parent = structure;
		allStrings.put("parentID", structure.getID());
		this.initialiseVariables(section);
	}

	public Repeat(Group group, ReadOnlySection section) {
		this.parent = group;
		allStrings.put("parentID", group.getID());
		this.initialiseVariables(section);
	}

	public Repeat(Repeat repeat, ReadOnlySection section) {
		this.parent = repeat;
		allStrings.put("parentID", repeat.getID());
		this.initialiseVariables(section);
	}

	public Repeat(Subrecord subrecord, ReadOnlySection section) {
		this.parent = subrecord;
		allStrings.put("parentID", subrecord.getID());
		this.initialiseVariables(section);
	}

	/**
	 * all constructors get variables initialised in the same way
	 * 
	 * @param section
	 */
	protected void initialiseVariables(ReadOnlySection section) {
		this.initStrings(section,"@id",null);
		this.setRepeatSubRecord(false);
		allStrings.put("fullid",getString("@id"));
		this.initStrings(section,"@label-affix", "-label");

		this.initBoolean(section,"@show",true);
		this.initBoolean(section,"@xxx-services-no-repeat",false);
		this.initBoolean(section,"@xxx-ui-no-repeat",false);
		this.initBoolean(section,"@asSibling",false);
		this.initStrings(section,"@section","common");
		this.initBoolean(section,"@exists-in-services",true);
		// should this field allow a primary flag
		this.initBoolean(section,"@has-primary",true);
		this.initStrings(section,"@userecord","");
		// used by uispec to create new structure
		this.initBoolean(section,"@as-expander",false);
		this.initBoolean(section,"@as-conditional-expander",false);
		
		this.initBoolean(section,"@xxx-hack-authorization",false);
		allBooleans.put("has_services_parent",false);

		String[] idparts = getString("@id").split("/");
		if (idparts.length > 1) {
			int len = idparts.length - 1;
			allBooleans.put("has_services_parent",true);

			allStrings.put("@id",idparts[len]);
			idparts[len] = null;
			if (!getBoolean("@xxx-hack-authorization")) {
				this.services_parent = idparts;
			} else {
				this.services_parent = new String[0];
			}
			allBooleans.put("@asSibling",true);
		}
		this.initStrings(section,"selector",".csc-"
				+ getString("parentID") + "-" + getString("@id"));
		this.initStrings(section,"@selector-affix","");


		this.initSet(section,"enum/default",new String[] { "" });		
		this.initBoolean(section,"enum/@has-blank",true);
		this.initStrings(section,"enum/blank-value", "Please select a value");
		

		Set<String> minis = Util.getSetOrDefault(section, "/@mini",
				new String[] { "" });
		if (minis.contains("number")) {
			this.parent.getRecord().setMiniNumber(this);
		}
		if (minis.contains("summary")) {
			this.parent.getRecord().setMiniSummary(this);
		}
		if (minis.contains("list")) {
			this.parent.getRecord().addMiniSummaryList(this);
		}
		for (String s : minis) {
			this.parent.getRecord().addMiniDataSet(this, s);
		}

		if(this.parent instanceof Record){
			this.initStrings(section,"label", ((Record) this.parent).getUILabel(getString("@id")));
		}
		this.initStrings(section,"services-tag",getString("@id"));
		this.initSet(section, "@attributes", new String[] {"GET","PUT","POST","DELETE"});
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
		return getString("@id");
	}

	public String getfullID() {
		return getString("fullid");
	}

	public boolean hasServicesParent() {
		return getBoolean("has_services_parent");
	}

	public String[] getServicesParent() {
		return services_parent;
	}

	void addChild(FieldSet f) {
		children.add(f);

		for(String perm : f.getAllFieldPerms()){
			if(!childrenperm.containsKey(perm)){
				childrenperm.put(perm, new ArrayList<FieldSet>());
			}
			childrenperm.get(perm).add(f);
		}
	}

//	public FieldSet[] getChildren() {
//		return children.toArray(new FieldSet[0]);
//	}
	public FieldSet[] getChildren(String perm) {
		if(perm.equals("")){
			return children.toArray(new FieldSet[0]);
		}
		if(childrenperm.containsKey(perm)){
			return childrenperm.get(perm).toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}

	public FieldParent getParent() {
		return this.parent;
	}

	public Record getRecord() {
		return parent.getRecord();
	}

	public String getSelector() {
		return getString("selector");
	}
	public String getLabel() {
		return getString("label");
	}

	public String getServicesTag() {
		return getString("services-tag");
	}

	public Boolean isInServices() {
		return getBoolean("@exists-in-services");
	}

	public Boolean getXxxServicesNoRepeat() {
		return getBoolean("@xxx-services-no-repeat");
	}

	public Boolean getXxxUiNoRepeat() {
		return getBoolean("@xxx-ui-no-repeat");
	}

	public Boolean isVisible() {
		return getBoolean("@show");
	}

	public Boolean asSibling() {
		return getBoolean("@asSibling");
	}

	public Boolean hasPrimary() {
		return getBoolean("@has-primary");
	}

	public boolean isExpander() {
		return getBoolean("@as-expander");
	}
	
	public boolean isConditionExpander(){
		return getBoolean("@as-conditional-expander");
	}

	public boolean isRepeatSubRecord() {
		return getBoolean("@is-subrecord");
	}
	
	public void setRepeatSubRecord(Boolean var) {
		allBooleans.put("@is-subrecord",var);
	}
	
	public String getSection() {
		return getString("@section");
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

	public String[] getIDPath() {
		if (getBoolean("@xxx-ui-no-repeat")) {
			if (parent instanceof Repeat) {
				return ((Repeat) parent).getIDPath();
			} else {
				return new String[] {};
			}
		} else {
			if (parent instanceof Repeat) {
				String[] pre = ((Repeat) parent).getIDPath();
				String[] out = new String[pre.length + 1];
				System.arraycopy(pre, 0, out, 0, pre.length);
				out[pre.length] = getString("@id");
				return out;
			} else {
				return new String[] { getString("@id") };
			}
		}
	}

	public Boolean hasAutocompleteInstance() {
		return false;
	}

	public Boolean hasMergeData() {
		return false;
	}

	public List<String> getAllMerge() {
		return merged;
	}

	public String[] getAllFieldPerms(){
		return getSet("@attributes").toArray(new String[0]);
	}

	public boolean hasFieldPerm(String perm){
		return getSet("@attributes").contains(perm);
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

	public Boolean isEnumDefault(String name) {
		if (getSet("enum/default").contains(name)) {
			return true;
		}
		return false;
	}

	public void config_finish(Spec spec) {
		for (FieldSet child : children)
			child.config_finish(spec);
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
