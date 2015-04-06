/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Repeat extends FieldSetImpl implements FieldParent {

	protected String[] services_parent;
	protected FieldParent parent;
	protected Stack<String> merged = new Stack<String>();
	protected List<FieldSet> children = new ArrayList<FieldSet>();
	protected Map<String, List<FieldSet>> childrenperm = new HashMap<String, List<FieldSet>>();
	protected boolean searchOnlyRepeat = false;

	/* Services */

	@Override
	public boolean isTrueRepeatField() {
		boolean result = false;

		if (this instanceof Repeat && ((Repeat) this).isSearchOnlyRepeat() == false) {
			result = true;
		}

		return result;
	}

	public void setSearchOnlyRepeat(boolean flag) {
		this.searchOnlyRepeat = flag;
	}

	public boolean isSearchOnlyRepeat() {
		return this.searchOnlyRepeat;
	}

	public Repeat(Record record, String id) {
		this.parent = record;
		utils.setString("parentID", record.getID());
		this.initialiseVariables(null, id);
	}

	public Repeat(Record record, ReadOnlySection section) {
		this.parent = record;
		utils.setString("parentID", record.getID());
		this.initialiseVariables(section, null);
	}

	public Repeat(Structure structure, ReadOnlySection section) {
		this.parent = structure;
		utils.setString("parentID", structure.getID());
		this.initialiseVariables(section, null);
	}

	public Repeat(Group group, ReadOnlySection section) {
		this.parent = group;
		utils.setString("parentID", group.getID());
		this.initialiseVariables(section, null);
	}

	public Repeat(Repeat repeat, ReadOnlySection section) {
		this.parent = repeat;
		String parentid = repeat.getID();
		if (repeat.getSearchType().equals("repeator")) {
			parentid = parent.getParent().getID();
			this.parent = parent.getParent();
		}
		utils.setString("parentID", parentid);
		this.initialiseVariables(section, null);
	}

	/**
	 * all constructors get variables initialised in the same way
	 * 
	 * @param section
	 */
	protected void initialiseVariables(ReadOnlySection section, String tempid) {
		super.initialiseVariables(section, tempid);

		utils.initStrings(section, "@id", tempid);
		this.setRepeatSubRecord(false);
		utils.setString("fullid", utils.getString("@id"));
		utils.initStrings(section, "@label-affix", "-label");

		utils.initBoolean(section, "@show", true);
		utils.initBoolean(section, "@xxx-services-no-repeat", false); //Mismatch between Services and UI - repeat in UI not in Services
		utils.initBoolean(section, "@xxx-ui-no-repeat", false); //Mismatch between Services and UI - repeat in Services not in UI
		utils.initBoolean(section, "@asSibling", false); //show repeatables as siblings rather than proper repeat - used in roles and permissions
		utils.initStrings(section, "@section", "common"); //Service section that this field exists in
		utils.initStrings(section, "@service-field-alias", null);
		utils.initBoolean(section, "@exists-in-services", true); //in case you want to totally hide something from the services
		utils.initBoolean(section, "@authref-in-services", false); //
		utils.initBoolean(section, "@services-type-anonymous", true); //in case you want to totally hide something from the services
		// should this field allow a primary flag in the UI repeat spec and schema
		utils.initBoolean(section, "@has-primary", true);
		// used when want to override default grouped behaviour e.g. blobs in media
		utils.initBoolean(section, "@showgrouped", true);
		utils.initStrings(section, "@userecord", ""); //this is a mark of a subrecord e.g contact in person, blob in media
		utils.initStrings(section, "@onlyifexists", ""); //
		utils.initStrings(section, "@ui-spec-prefix", "");
		utils.initBoolean(section, "@ui-spec-inherit", false);
		utils.initBoolean(section, "@services-readonly", false);
		utils.initBoolean(section, "@ui-readonly", utils.getBoolean("@services-readonly"));
		utils.initStrings(section, "@with-csid", null);
		// used by uispec to create new structure
		utils.initBoolean(section, "@as-expander", false);
		utils.initBoolean(section, "@as-conditional-expander", false);

		utils.initBoolean(section, "@xxx-hack-authorization", false); //authorization is weird in the Service layer - one day it might be less weird
		utils.initStrings(section, "@serviceurl", null); //Url that the service layer uses for this thing.

		//this is all about repeatable groups and how there is one level in the UI but 2 in teh services
		//tho in roles and permissions the groups are only single level in the services
		utils.setBoolean("has_services_parent", false);

		String[] idparts = utils.getString("@id").split("/");
		if (idparts.length > 1) {
			int len = idparts.length - 1;
			utils.setBoolean("has_services_parent", true);

			utils.setString("@id", idparts[len]);
			idparts[len] = null;
			if (!utils.getBoolean("@xxx-hack-authorization")) {
				this.services_parent = idparts;
			} else {
				this.services_parent = new String[0];
			}
			utils.setBoolean("@asSibling", true);
		}

		utils.initStrings(section, "selectorID", utils.getString("parentID"));
		utils.initStrings(section, "selector", utils.getString("selectorID") + "-" + utils.getString("@id"));
		utils.initStrings(section, "preselector", ".csc-");
		utils.initStrings(section, "decoratorselector", "cs-");

		utils.initStrings(section, "container-selector", utils.getString("selector") + "-container");
		utils.initStrings(section, "precontainer-selector", utils.getString("preselector"));
		utils.initStrings(section, "pretitle-selector", utils.getString("preselector"));
		utils.initStrings(section, "title-selector", utils.getString("selector") + "-titlebar");

		utils.initStrings(section, "@primarykey", utils.getString("selector"));

		utils.initStrings(section, "@selector-affix", "");

		utils.initSet(section, "enum/default", new String[] { "" });
		utils.initBoolean(section, "enum/@has-blank", true);
		utils.initStrings(section, "enum/blank-value", "Please select a value");

		Set<String> minis = Util.getSetOrDefault(section, "/@mini", new String[] { "" });
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

		utils.initStrings(section, "@ui-type", "plain");
		utils.initStrings(section, "@ui-func", "");

		utils.initStrings(section, "@ui-search", "");
		utils.initStrings(section, "@query-behavior", QUERY_BEHAVIOR_NORMAL);
		utils.initStrings(section, "label", "" + utils.getString("selectorID") + "-" + utils.getString("@id") + "Label");

		utils.initStrings(section, "services-tag", utils.getString("@id"));
		utils.initStrings(section, "services-schema", null);

		//define the operations that the Service layer allows for this item
		utils.initSet(section, "@attributes", new String[] { "GET", "PUT", "POST", "DELETE" });

	}

	@Override
	public SchemaUtils getUtils() {
		return utils;
	}

	public String getfullID() {
		return utils.getString("fullid");
	}

	@Override
	public String getUISpecPrefix() {
		return utils.getString("@ui-spec-prefix");
	}

	//this affects the depth of nesting in the things like the elPaths e.g. false: "elPath": "fields..0.telephoneNumberGroup" vs true: "elPath": "fields.telephoneNumberGroup"
	@Override
	public Boolean getUISpecInherit() {
		return utils.getBoolean("@ui-spec-inherit");
	}

	@Override
	public boolean hasServicesParent() {
		return utils.getBoolean("has_services_parent");
	}

	/*
	 * Not sure why, but apparently a Repeat instance with 1 child that is a group is
	 * considered to have orphans -go figure.
	 */
	public boolean hasOrphans() {
		Boolean result = utils.getBoolean("@hasOrphans");
		
		if (result == null) {
			result = false; // let's assume no orphans
			FieldSet[] children = this.getChildren(null);
			if (children.length == 1) {
				if (children[0].isAGroupField() == true) {
					result = true;
					utils.setBoolean("@hasOrphans", result);
				}
			}
		}

		return result;
	}

	@Override
	public String[] getServicesParent() {
		return services_parent;
	}

	void addChild(FieldSet f) {
		children.add(f);

		for (String perm : f.getAllFieldOperations()) {
			if (!childrenperm.containsKey(perm)) {
				childrenperm.put(perm, new ArrayList<FieldSet>());
			}
			childrenperm.get(perm).add(f);
		}
	}

	//	public FieldSet[] getChildren() {
	//		return children.toArray(new FieldSet[0]);
	//	}
	public FieldSet[] getChildren(String perm) {
		if (perm == null || perm.equals("")) {
			return children.toArray(new FieldSet[0]);
		}
		if (childrenperm.containsKey(perm)) {
			return childrenperm.get(perm).toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}

	@Override
	public FieldParent getParent() {
		return this.parent;
	}

	@Override
	public void setParent(FieldParent fp) {
		this.parent = fp;
	}

	@Override
	public Record getRecord() {
		return parent.getRecord();
	}

	@Override
	public String getContainerSelector() {
		return utils.getString("container-selector");
	}

	@Override
	public String getPreContainerSelector() {
		return utils.getString("precontainer-selector");
	}

	@Override
	public String getPreSelector() {
		return utils.getString("preselector");
	}

	@Override
	public String getDecoratorSelector() {
		return utils.getString("decoratorselector");
	}

	@Override
	public String getSelector() {
		return utils.getString("selector");
	}

	@Override
	public String getTitleSelector() {
		return utils.getString("title-selector");
	}

	@Override
	public String getPreTitleSelector() {
		return utils.getString("pretitle-selector");
	}

	@Override
	public String getLabel() {
		return utils.getString("label");
	}

	public String getUIprefix() {
		return getPreSelector() + utils.getString("parentID") + "-";
	}

	public String getUILabelSelector(String id) {
		return getUIprefix() + id + "-label";
	}

	@Override
	public String getUILabelSelector() {
		return getUIprefix() + utils.getString("@id") + "-label";
	}

	public String getUILabel(String id) {
		return utils.getString("@id") + "-" + id + "Label";
	}

	@Override
	public String getUIFunc() {
		return utils.getString("@ui-func");
	}

	@Override
	public String getUIType() {
		return utils.getString("@ui-type");
	}

	@Override
	public String getSearchType() {
		return utils.getString("@ui-search");
	}

	@Override
	public String getQueryBehavior() {
		return utils.getString("@query-behavior");
	}

	public void setSearchType(String val) {
		utils.setString("@ui-search", val);
	}

	@Override
	public String getServicesTag() {
		return utils.getString("services-tag");
	}

	@Override
	public String getServicesUrl() {
		return utils.getString("@serviceurl");
	}

	public Boolean getXxxServicesNoRepeat() {
		return utils.getBoolean("@xxx-services-no-repeat");
	}

	public Boolean getXxxUiNoRepeat() {
		return utils.getBoolean("@xxx-ui-no-repeat");
	}

	public Boolean isVisible() {
		return utils.getBoolean("@show");
	}

	public Boolean asSibling() {
		return utils.getBoolean("@asSibling");
	}

	//used in generateGroupField in uispec for elpaths
	@Override
	public String getPrimaryKey() {
		return utils.getString("@primarykey");
	}

	public Boolean hasPrimary() {
		return utils.getBoolean("@has-primary");
	}

	@Override
	public boolean isExpander() {
		return utils.getBoolean("@as-expander");
	}

	@Override
	public boolean isConditionExpander() {
		return utils.getBoolean("@as-conditional-expander");
	}

	@Override
	public boolean isRepeatSubRecord() {
		return utils.getBoolean("@is-subrecord");
	}

	@Override
	public void setRepeatSubRecord(Boolean var) {
		utils.setBoolean("@is-subrecord", var);
	}

	@Override
	public String getSection() {
		return utils.getString("@section");
	}

	@Override
	public String getLabelAffix() {
		return utils.getString("@label-affix");
	}

	@Override
	public String getSelectorAffix() {
		return utils.getString("@selector-affix");
	}

	@Override
	public boolean isReadOnly() {
		return utils.getBoolean("@ui-readonly");
	}

	@Override
	public boolean isServicesReadOnly() {
		return utils.getBoolean("@services-readonly");
	}
	
	public Boolean isGrouped() {
		return utils.getBoolean("@showgrouped");
	}

	@Override
	public Boolean usesRecord() {
		if (utils.getString("@userecord") != null && !utils.getString("@userecord").equals("")) {
			return true;
		}
		return false;
	}

	@Override
	public String usesRecordValidator() {
		if (utils.getString("@onlyifexists") != null && !utils.getString("@onlyifexists").equals("")) {
			return utils.getString("@onlyifexists");
		}
		return null;
	}

	@Override
	public Record usesRecordId() {
		if (usesRecord()) {
			return this.getRecord().getSpec().getRecord(utils.getString("@userecord"));
		}
		return null;
	}

	@Override
	public String[] getIDPath() {
		if (utils.getBoolean("@xxx-ui-no-repeat")) {
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
				out[pre.length] = utils.getString("@id");
				return out;
			} else {
				return new String[] { utils.getString("@id") };
			}
		}
	}

	/**
	 * UI specific marking: YURA said: these are renderer decorators that do
	 * their own rendering so need some sub nesting Generally, should only be a
	 * field, but does not hurt to have same logic here as from Field.
	 * 
	 * @param fs
	 * @return
	 */
	@Override
	public boolean isASelfRenderer() {
		return getUIType().contains(SELFRENDERER);
	}

	@Override
	public Record getSelfRendererRecord() {
		String parts[] = getUIType().split("/");
		if (parts.length != 3 || !SELFRENDERER.equals(parts[2]))
			return null;
		Record subrecord = getRecord().getSpec().getRecordByServicesUrl(parts[1]);
		return subrecord;
	}

	@Override
	public Boolean hasAutocompleteInstance() {
		return false;
	}

	@Override
	public Boolean hasMergeData() {
		return false;
	}

	@Override
	public List<String> getAllMerge() {
		return merged;
	}

	//getAllFieldPerms now getAllFieldOperations
	@Override
	public String[] getAllFieldOperations() {
		return utils.getSet("@attributes").toArray(new String[0]);
	}

	@Override
	public boolean hasFieldPerm(String perm) {
		return utils.getSet("@attributes").contains(perm);
	}

	public boolean hasEnumBlank() {
		return utils.getBoolean("enum/@has-blank");
	}

	@Override
	public String enumBlankValue() {
		return utils.getString("enum/blank-value");
	}

	@Override
	public String getWithCSID() {
		return utils.getString("@with-csid");
	}

	public String getEnumDefault() {
		return StringUtils.join(utils.getSet("enum/default"), ",");
	}

	public Boolean isEnumDefault(String name) {
		if (utils.getSet("enum/default").contains(name)) {
			return true;
		}
		return false;
	}

	@Override
	public void config_finish(Spec spec) {
		for (FieldSet child : children)
			child.config_finish(spec);
	}

	void dump(StringBuffer out) {
		out.append("  id=" + this.getID() + "\n");
		out.append("    type=" + utils.getSet("@type") + "\n");
		utils.dump(out);
	}
}
