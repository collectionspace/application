/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
	private static final Logger log = LoggerFactory.getLogger(Record.class);
	private Map<String, String> allStrings = new HashMap<String, String>();
	private Map<String, Boolean> allBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allSets = new HashMap<String, Set<String>>();
	/* just used for documentation to retrieve defaults */
	private Map<String, String> allDefaultStrings = new HashMap<String, String>();
	private Map<String, Boolean> allDefaultBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allDefaultSets = new HashMap<String, Set<String>>();
	
	private Map<String, Structure> structure = new HashMap<String, Structure>();
	private Map<String, String> uisection = new HashMap<String, String>();
	private Map<String, FieldSet> subrecords = new HashMap<String, FieldSet>();
	private Map<String, Map<String, FieldSet>> subrecordsperm = new HashMap<String, Map<String, FieldSet>>();
	private Map<String, FieldSet> fields = new HashMap<String, FieldSet>();
	private Map<String, FieldSet> servicefields = new HashMap<String, FieldSet>();
	private Map<String, FieldSet> mergedfields = new HashMap<String, FieldSet>();
	
	private Map<String, Map<String, Map<String, FieldSet>>> servicepermfields = new HashMap<String, Map<String,Map<String, FieldSet>>>();
	private Map<String, Map<String, FieldSet>> genpermfields = new HashMap<String, Map<String, FieldSet>>();
	private Map<String, Map<String, FieldSet>> allgenpermfields = new HashMap<String, Map<String, FieldSet>>();
	private Map<String, Map<String, FieldSet>> mergedpermfields = new HashMap<String, Map<String, FieldSet>>();
	private Map<String, Map<String, FieldSet>> repeatpermfields = new HashMap<String, Map<String, FieldSet>>();
	
	
	private Map<String, FieldSet> repeatfields = new HashMap<String, FieldSet>();

	private Map<String, Instance> instances = new HashMap<String, Instance>();
	private Map<String, FieldSet> summarylist = new HashMap<String, FieldSet>();
	private Map<String, Map<String, FieldSet>> minidataset = new HashMap<String, Map<String, FieldSet>>();
	private Spec spec;
	private FieldSet mini_summary, mini_number, display_name;

	/* Service stuff */
	private Map<String, String> services_record_paths = new HashMap<String, String>();
	private Map<String, Field> services_filter_param = new HashMap<String, Field>();

	// XXX utility methods
	Record(Spec parent, ReadOnlySection section) {
		/* parameters */
		// this is what the service layer id defaults to if not specified later
		// standard = singular form of the concept
		this.initStrings(section,"@id",null);

		// record,authority,compute-displayname can have multiple types using
		// commas
		this.initSet(section,"@type",new String[] { "record" });

		// specified that it is included in the findedit uispec
		this.initBoolean(section,"@in-findedit",false);

		this.initBoolean(section,"@in-recordlist",true);
		
		// config whether service layer needs call as multipart or not
		this.initBoolean(section,"is-multipart",true);

		// config whether record type has termsUsed or not (returns empty array
		// in Json if = false )
		this.initBoolean(section,"terms-used",true);
		// config whether record type has relatedObj/procs or not (returns empty
		// array in Json if = false )
		this.initBoolean(section,"refobj-used",true);
		
		// config the keyword to use for searching
		this.initStrings(section,"services-search-keyword","kw");

		// Used to differentiate between authority and vocabulary on create
		this.initStrings(section,"membership-tag","inAuthority");

		/* UI Layer helpers */
		// ui layer path
		this.initStrings(section,"web-url", getString("@id"));

		// specify url if not nameAuthority
		this.initStrings(section,"terms-used-url", "nameAuthority");

		// ui layer json row
		this.initStrings(section,"number-selector", ".csc-entry-number");

		// ui layer json used in list views
		this.initStrings(section,"row-selector",".csc-recordList-row:");

		// ui layer path: defaults to web_url if not specified
		this.initStrings(section,"ui-url", getString("web-url") + ".html");

		// ui layer path
		this.initStrings(section,"tab-url", getString("web-url") + "-tab");

		/* Service layer helpers */

		// path that the service layer uses to access this record
		this.initStrings(section,"services-url", getString("@id"));

		// authorization 
		this.initStrings(section,"authorization-includes", getString("services-url"));

		this.initBoolean(section,"authorization-view",true);

		// service layer paths to list data for this record type
		this.initStrings(section,"services-list-path", getString("services-url") + "-common-list/"
				+ getString("services-url") + "-list-item");

		this.initStrings(section,"services-fields-path", getString("services-url")
						+ "-common-list/fieldsReturned");

		// used by service layer to construct authority names
		this.initStrings(section,"urn-syntax","urn:cspace.org.collectionspace.demo." + getString("@id") + ":name({vocab}):"
						+ getString("@id") + ":name({entry})'{display}'");
		this.initStrings(section,"vocab-syntax","urn:cspace:name");
		this.initStrings(section,"authority-vocab-type","PersonAuthority");
		//
		this.initStrings(section,"services-instances-path", getString("services-url")
						+ "_common:http://collectionspace.org/services/"
						+ getString("services-url") + "," + getString("services-url") + "-common-list/"
						+ getString("services-url") + "-list-item");

		//
		this.initStrings(section,"services-single-instance-path", getString("services-url")
						+ "_common:http://collectionspace.org/services/"
						+ getString("services-url") + "," + getString("services-url") + "-common");
		this.initStrings(section,"primaryfield", "");
		this.initBoolean(section,"hasdeletemethod",false);
		this.initBoolean(section,"hassoftdelete",false);

		this.initStrings(section,"services-tenant-singular", getString("services-url"));
		this.initStrings(section,"services-tenant-plural", getString("services-tenant-singular")+"s");
		this.initStrings(section,"services-tenant-auth-singular", getString("services-url"));
		this.initStrings(section,"services-tenant-auth-plural", getString("services-tenant-singular")+"s");

		this.initStrings(section,"services-schemalocation", "http://services.collectionspace.org");
		
		this.initStrings(section,"services-dochandler","org.collectionspace.services."+ getString("services-tenant-singular").toLowerCase() +".nuxeo."+ getString("services-tenant-singular")+"DocumentModelHandler");
		this.initStrings(section,"services-abstract","org.collectionspace.services."+getString("services-tenant-singular").toLowerCase()+"."+ getString("services-tenant-plural") +"CommonList");
		this.initStrings(section,"services-common", getString("services-abstract") + "$"+getString("services-tenant-singular")+"ListItem");
		this.initStrings(section,"services-validator","org.collectionspace.services."+ getString("services-tenant-singular").toLowerCase() +".nuxeo."+ getString("services-tenant-singular")+"ValidatorHandler");

		spec = parent;
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

	public String getWebURL() {
		return getString("web-url");
	}

	public String getUIURL() {
		return getString("ui-url");
	}

	public String getTabURL() {
		return getString("tab-url");
	}

	public boolean isType(String k) {
		return getSet("@type").contains(k);
	}

	public Spec getSpec() {
		return spec;
	}

	public FieldSet[]  getAllMergedFields(){
		FieldSet[] merged = mergedfields.values().toArray(new FieldSet[0]);
		return merged;
	}
	public FieldSet[]  getAllGenFields(String perm){
		if(allgenpermfields.containsKey(perm)){
			return allgenpermfields.get(perm).values().toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}
	
	public Boolean getPerm(String fieldId, String perm){
		if(allgenpermfields.containsKey(perm)){
			if(allgenpermfields.get(perm).containsKey(fieldId)){
				return true;
			}
		}
		return false;
	}
	
	
	public FieldSet[] getAllServiceFields(String perm, String section) {
		if(perm.equals("")){//return everything
			return servicefields.values().toArray(new FieldSet[0]);
		}
		if(servicepermfields.containsKey(perm)){
			if(servicepermfields.get(perm).containsKey(section)){
				return servicepermfields.get(perm).get(section).values().toArray(new FieldSet[0]);
			}
		}
		return new FieldSet[0];
	}
	
//	public FieldSet[] getAllServiceFields() {
//		return servicefields.values().toArray(new FieldSet[0]);
//	}
	public String getUIprefix(){
		return ".csc-" + getString("@id") + "-";
	}
	public String getUILabel(String id){
		return getString("@id") + "-" + id + "Label";
	}
	public String getUILabelSelector(String id){
		return getUIprefix() +  id + "-label";
	}
	public String[] getAllUISections(){
		return uisection.values().toArray(new String[0]);
	}
	public String getUISections(String id){
		return uisection.get(id);
	}

	public FieldSet[] getAllFields(String perm) {
		if(perm.equals("")){
			return fields.values().toArray(new FieldSet[0]);
		}
		if(genpermfields.containsKey(perm)){
			return genpermfields.get(perm).values().toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}
//	public FieldSet[] getAllFields() {
//		return fields.values().toArray(new FieldSet[0]);
//	}

	public FieldSet getField(String id) {
		return fields.get(id);
	}

	//public FieldSet[] getAllRepeatFields() {
	//	return repeatfields.values().toArray(new FieldSet[0]);
	//}
	public FieldSet[] getAllRepeatFields(String perm) {
		if(perm.equals("")){
			return repeatfields.values().toArray(new FieldSet[0]);
		}
		if(repeatpermfields.containsKey(perm)){
			return repeatpermfields.get(perm).values().toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}

	/*
	 * includes all the fields that are children of repeats as well as top level
	 * fields.
	 */
	public FieldSet getRepeatField(String id) {
		return repeatfields.get(id);
	}

	public Structure getStructure(String id) {
		// fall back if structure isn't defined used defaults
		if (!structure.containsKey(id)) {
			Structure s = new Structure(this, id);
			structure.put(id, s);
		}
		return structure.get(id);
	}

	public Record getSubrecord(String fieldid) {
		return subrecords.get(fieldid).usesRecordId();
	}

	public FieldSet[] getAllSubRecords(String perm) {
		if(!subrecordsperm.containsKey(perm)){
			subrecordsperm.put(perm, new HashMap<String, FieldSet>());
			for (FieldSet fs : this.getAllRepeatFields(perm)) {
				if (fs.usesRecord()) {
					this.addSubRecord(fs,perm);
				}
			}
		}
		return subrecordsperm.get(perm).values().toArray(new FieldSet[0]);
	}	
	/*
	public FieldSet[] getAllSubRecords() {
		if (subrecords.values().isEmpty()) {
			for (FieldSet fs : this.getAllRepeatFields("")) {
				if (fs.usesRecord()) {
					this.addSubRecord(fs);
				}
			}
		}
		return subrecords.values().toArray(new FieldSet[0]);
	}
	*/

	public String getPrimaryField() {
		return getString("primaryfield");
	};

	public Boolean hasPrimaryField() {
		if (getPrimaryField().equals("")) {
			return false;
		} else {
			return true;
		}
	}

	public Boolean hasMergeData() {
		if (mergedfields.isEmpty())
			return false;

		return true;
	}
	public String getTermsUsedURL() {
		return getString("terms-used-url");
	}

	public String getNumberSelector() {
		return getString("number-selector");
	}

	public String getRowSelector() {
		return getString("row-selector");
	}

	public boolean isInFindEdit() {
		return getBoolean("@in-findedit");
	}
	public boolean isInRecordList() {
		return getBoolean("@in-recordlist");
	}

	public boolean isMultipart() {
		return getBoolean("is-multipart");
	}

	public boolean hasTermsUsed() {
		return getBoolean("terms-used");
	}

	public boolean hasRefObjUsed() {
		return getBoolean("refobj-used");
	}
	public boolean hasHierarchyUsed(String type) {
		return this.getStructure(type).showHierarchySection();
	}

	public boolean hasDeleteMethod() {
		return getBoolean("hasdeletemethod");
	}

	public boolean hasSoftDeleteMethod() {
		return getBoolean("hassoftdelete");
	}
	
	public String getServicesSearchKeyword() {
		return getString("services-search-keyword");
	}

	public String getInTag() {
		return getString("membership-tag");
	}

	public String getURNSyntax() {
		return getString("urn-syntax");
	}

	public String getURNVocab() {
		return getString("vocab-syntax");
	}

	public String getVocabType() {
		return getString("authority-vocab-type");
	}

	public Instance[] getAllInstances() {
		return instances.values().toArray(new Instance[0]);
	}

	public Instance getInstance(String key) {
		return instances.get(key);
	}

	public String getServicesURL() {
		return getString("services-url");
	}

	public String getServicesTenantSg() {
		return getString("services-tenant-singular");
	}

	public String getServicesTenantPl() {
		return getString("services-tenant-plural");
	}

	public String getServicesTenantAuthSg() {
		return getString("services-tenant-auth-singular");
	}

	public String getServicesTenantAuthPl() {
		return getString("services-tenant-auth-plural");
	}
	
	public String getServicesAbstractCommonList(){
		return getString("services-abstract");
	}
	public String getServicesValidatorHandler(){
		return getString("services-validator");
		
	}
	public String getServicesCommonList(){
		return getString("services-common");
	}

	public String getServicesSchemaBaseLocation(){
		return getString("schema-location");
	}
	
	public String getServicesDocHandler(){
		return getString("services-dochandler");
	}
	
	public String getServicesListPath() {
		return getString("services-list-path");
	}

	public String getServicesFieldsPath() {
		return getString("services-fields-path");
	}

	public String getServicesInstancesPath() {
		return getString("services-instances-path");
	}

	public String getServicesSingleInstancePath() {
		return getString("services-single-instance-path");
	}

	public String[] getServicesRecordPaths() {
		return services_record_paths.keySet().toArray(new String[0]);
	}

	public String getServicesRecordPath(String name) {
		return services_record_paths.get(name);
	}
	public Boolean hasServicesRecordPath(String name) {
		if(services_record_paths.containsKey(name)){
			return true;
		}
		return false;
	}
	void setMerged(Field f){
		mergedfields.put(f.getID(), f);
	}
	
	void setMiniNumber(Field f) {
		mini_number = f;
	}

	void setMiniSummary(Field f) {
		mini_summary = f;
	}

	void setMiniNumber(Repeat f) {
		mini_number = f;
	}

	void setMiniSummary(Repeat f) {
		mini_summary = f;
	}

	void setDisplayName(Field f) {
		display_name = f;
	}

	void setServicesRecordPath(String section, String path) {
		services_record_paths.put(section, path);
	}

	void setServicesFilterParam(String param, Field field) {
		services_filter_param.put(param, field);
	}

	public FieldSet getMiniNumber() {
		return mini_number;
	}

	public FieldSet getMiniSummary() {
		return mini_summary;
	}

	public FieldSet[] getAllMiniSummaryList() {
		return summarylist.values().toArray(new FieldSet[0]);
	}

	public FieldSet getMiniSummaryList(String key) {
		return summarylist.get(key);
	}

	public FieldSet getDisplayNameField() {
		return display_name;
	}

	public Field getFieldByServicesFilterParam(String param) {
		return services_filter_param.get(param);
	}

	// authorization
	public Boolean getAuthorizationView() {
		return getBoolean("authorization-view");
	}


	public String getAuthorizationType() {
		return getString("authorization-includes");
	}

	public Boolean isAuthorizationType(String name) {
		return getAuthorizationType().contains(name);
	}

	public void addField(FieldSet f , Boolean plusServices){
		if (plusServices) {
			fields.put(f.getID(), f);
			if (f.isInServices()) {
				servicefields.put(f.getID(), f);
				for (String perm : f.getAllFieldPerms()) {
					if (!servicepermfields.containsKey(perm)) {
						servicepermfields.put(perm,
								new HashMap<String, Map<String, FieldSet>>());
					}
					if (!servicepermfields.get(perm)
							.containsKey(f.getSection())) {
						servicepermfields.get(perm).put(f.getSection(),
								new HashMap<String, FieldSet>());
					}
					servicepermfields.get(perm).get(f.getSection()).put(
							f.getID(), f);
				}
			}
			for (String perm : f.getAllFieldPerms()) {
				if (!genpermfields.containsKey(perm)) {
					genpermfields.put(perm, new HashMap<String, FieldSet>());
				}
				genpermfields.get(perm).put(f.getID(), f);
			}
			if (!genpermfields.containsKey("")) {
				genpermfields.put("", new HashMap<String, FieldSet>());
			}
			genpermfields.get("").put(f.getID(), f);
		} 

		for (String perm : f.getAllFieldPerms()) {
			if (!allgenpermfields.containsKey(perm)) {
				allgenpermfields.put(perm, new HashMap<String, FieldSet>());
			}
			allgenpermfields.get(perm).put(f.getID(), f);
		}
		if (!allgenpermfields.containsKey("")) {
			allgenpermfields.put("", new HashMap<String, FieldSet>());
		}
		allgenpermfields.get("").put(f.getID(), f);
	}
	public void addField(FieldSet f) {
		addField(f,true);
	}

	public void addAllField(FieldSet f) {
		repeatfields.put(f.getID(), f);
		for(String perm : f.getAllFieldPerms()){
			if(!repeatpermfields.containsKey(perm)){
				repeatpermfields.put(perm, new HashMap<String, FieldSet>());
			}
			repeatpermfields.get(perm).put(f.getID(), f);
		}
	}

	public void addUISection(String id){
		uisection.put(id,id);
	}
	public void addStructure(Structure s) {
		structure.put(s.getID(), s);
	}

	public void addSubRecord(FieldSet fs, String perm) {
		subrecordsperm.get(perm).put(fs.getID(), fs);
	}

	public void addInstance(Instance n) {
		instances.put(n.getID(), n);
		spec.addInstance(n);
	}

	// obsolete?
	public void addMiniSummaryList(FieldSet f) {
		summarylist.put(f.getID(), f);
	}

	public void addMiniDataSet(FieldSet f, String s) {
		// s:{ name: field, name: field, name: field }
		if (!minidataset.containsKey(s)) {
			Map<String, FieldSet> subdata = new HashMap<String, FieldSet>();
			minidataset.put(s, subdata);
		}
		minidataset.get(s).put(f.getID(), f);
	}

	public void addMiniDataSet(Repeat r, String s) {
		// s:{ name: field, name: field, name: field }
		if (!minidataset.containsKey(s)) {
			Map<String, FieldSet> subdata = new HashMap<String, FieldSet>();
			minidataset.put(s, subdata);
		}
		minidataset.get(s).put(r.getID(), r);
	}

	public FieldSet[] getMiniDataSetByName(String s) {
		if (minidataset.containsKey(s)) {
			return minidataset.get(s).values().toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}

	public String[] getAllMiniDataSets() {
		if (minidataset.isEmpty()) {
			return new String[0];
		}
		return minidataset.keySet().toArray(new String[0]);
	}

	void dump(StringBuffer out) {
		out.append("  record id=" + this.getID() + "\n");
		out.append("    web_url=" + getWebURL() + "\n");
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

	public Record getRecord() {
		return this;
	}

	public void config_finish(Spec spec) {
		for (Instance n : instances.values())
			n.config_finish(spec);
		for (FieldSet fs : fields.values())
			fs.config_finish(spec);
	}

	@Override
	public boolean isExpander() {
		return false;
	}
}
