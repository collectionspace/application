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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
	private Map<String, Map<String, String>> uisection = new HashMap<String, Map<String, String>>();
	private Map<String, FieldSet> subrecords = new HashMap<String, FieldSet>();
	private Map<String, Map<String, FieldSet>> subrecordsperm = new HashMap<String, Map<String, FieldSet>>();
	
	//operation - GET POST PUT SEARCH - as some fields are not available for all operations	
	//genpermfields becomes (servicefieldsbyoperation) - topfieldsbyoperation
	private Map<String, Map<String, FieldSet>> topfieldsbyoperation = new HashMap<String, Map<String, FieldSet>>();
	//allgenpermfields becomes allfieldsbyoperation includes fields that don't existing the service layer
	private Map<String, Map<String, FieldSet>> allfieldsbyoperation = new HashMap<String, Map<String, FieldSet>>();
	//servicepermfields becomes servicefieldsbyoperation
	private Map<String, Map<String, Map<String, FieldSet>>> servicefieldsbyoperation = new HashMap<String, Map<String,Map<String, FieldSet>>>();
	// don't thinkg this is used
	// private Map<String, Map<String, FieldSet>> mergedpermfields = new HashMap<String, Map<String, FieldSet>>();
	//servicefields becomes serviceFieldTopLevel
	private Map<String, FieldSet> serviceFieldTopLevel = new HashMap<String, FieldSet>();
	//fields become fieldTopLevel
	private Map<String, FieldSet> fieldTopLevel = new HashMap<String, FieldSet>();

	//this is actually a list of all fields flattened out rather than hierarchical so includes all children
	//repeatfields become (messageKeyFields) becomes fieldFullList
	private Map<String, FieldSet> fieldFullList = new HashMap<String, FieldSet>();
	//serviceFieldFullList: not sure if this is needed - but is all fields in the service layer
	private Map<String, FieldSet> serviceFieldFullList = new HashMap<String, FieldSet>(); 
	//searchFieldFullList: all fields used in the advanced search UI
	private Map<String, FieldSet> searchFieldFullList = new HashMap<String, FieldSet>(); 

	//merged fields are fields in the UI that take multiple service layer fields
	private Map<String, FieldSet> mergedfields = new HashMap<String, FieldSet>(); 
	
	private Map<String, Instance> instances = new HashMap<String, Instance>();
	private Map<String, FieldSet> summarylist = new HashMap<String, FieldSet>();
	private Map<String, Map<String, FieldSet>> minidataset = new HashMap<String, Map<String, FieldSet>>();
	private Spec spec;
	private FieldSet mini_summary, mini_number, display_name;
	private String whoamI = "";

	/* Service stuff */
	private Map<String, String> services_record_paths = new HashMap<String, String>();
	private Map<String, String> services_instances_paths = new HashMap<String, String>();
	private Map<String, Field> services_filter_param = new HashMap<String, Field>();

	// XXX utility methods
	Record(Spec parent, ReadOnlySection section, Map<String,String> data) {
		//Map<String,String>data = (Map<String,String>)parent;
		/* parameters */
		// this is what the service layer id defaults to if not specified later
		// standard = singular form of the concept
		this.initStrings(section,"@id",null);
		whoamI = getString("@id");
		// record,authority,compute-displayname can have multiple types using
		// commas
		this.initSet(section,"@type",new String[] { "record" });
		this.initStrings(section,"showin","");

		// specified that it is included in the findedit uispec - probably not useful any more?
		this.initBoolean(section,"@in-findedit",false);

		this.initBoolean(section,"@in-recordlist",true);

		//Record differentiates between things like structureddates and procedures
		this.initBoolean(section,"@separate-record",true);
		
		
		
		// config whether service layer needs call as multipart or not - authorization is not currently multipart
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
		this.initStrings(section,"search-url", getString("web-url") + "-search");

		this.initStrings(section,"enum-blank", data.get("blank"));
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
	
	/** field functions **/
	//getPerm is now hasFieldByOperation(fieldId,operation)
	//tests if a specific field exists for a certain operation
	public Boolean hasFieldByOperation(String fieldId, String operation){
		if(allfieldsbyoperation.containsKey(operation)){
			if(allfieldsbyoperation.get(operation).containsKey(fieldId)){
				return true;
			}
		}
		return false;
	}
	
	public void addSearchField(FieldSet f){
		
		if(!(f.getSearchType().equals(""))){
			FieldSet searchf = f;
			if(searchf.getSearchType().equals("repeatable")){
				//need to makeup a repeatable field
				if(f.getParent() instanceof Repeat){
					Repeat rs = (Repeat)f.getParent();
					//XXX this name is terrible - should be a better way
					rs.setSearchType("repeatored"); //need to set the type of repeat this is e.g. is it just for search or is is also for edit
					searchFieldFullList.put(rs.getID(),rs);
				}
				else{
					Repeat r = new Repeat(searchf.getRecord(),searchf.getID()+"s"); //UI wants 'plurals' for the fake repeat parents
					//XXX this name is terrible - should be a better way
					r.setSearchType("repeator");
					searchFieldFullList.put(r.getID(),r);
	
					searchf.setParent(r);
					r.addChild(searchf); // this is causing confusing later on... can we separate this some how?
				}
				
			}
			else if(searchf.getSearchType().equals("range")){
				searchf.getRecord().addUISection("search", searchf.getID());
//need to add label for main bit as well...
				Field fst = new Field(searchf.getRecord(),searchf.getID()+"Start");
				Field fed = new Field(searchf.getRecord(),searchf.getID()+"End");

				fst.setSearchType("range");
				fed.setSearchType("range");
				fed.setType(searchf.getUIType());
				fst.setType(searchf.getUIType());
				searchFieldFullList.put(fst.getID(),fst);
				searchFieldFullList.put(fed.getID(),fed);
			}
			else{
				searchFieldFullList.put(searchf.getID(),searchf);
			}
		}
	}
	

	public void addField(FieldSet f){
		String parentType = f.getParent().getClass().getSimpleName();
		fieldFullList.put(f.getID(),f);
		if(f.isInServices()){
			serviceFieldFullList.put(f.getID(),f);
		}
		
		//is this a search field?
		addSearchField(f);
			
		if(parentType.equals("Record")){ //toplevel field
			fieldTopLevel.put(f.getID(),f);
			if(f.isInServices()){
				serviceFieldTopLevel.put(f.getID(),f);
				//list fields by the operations they support
				for (String oper : f.getAllFieldOperations()) {
					hashutil(servicefieldsbyoperation, oper,f.getSection(), f); 
					//subdivide section and then by GET POST PUT etc
				}
			}
			//list fields by the operations they support
			for (String oper : f.getAllFieldOperations()) {
				hashutil(topfieldsbyoperation, oper, f); //subdivide by GET POST PUT etc
			}
			hashutil(topfieldsbyoperation, "", f); //ALL function
		}
		//list fields by the operations they support
		for (String oper : f.getAllFieldOperations()) {
			hashutil(allfieldsbyoperation, oper, f); //subdivide by GET POST PUT etc
		}
		hashutil(allfieldsbyoperation, "", f); //ALL function
	}
		
	public FieldSet[] getAllServiceFieldTopLevel(String operation, String section) {
		if(operation.equals("")){//return everything
			return serviceFieldTopLevel.values().toArray(new FieldSet[0]);
		}
		if(servicefieldsbyoperation.containsKey(operation)){
			if(servicefieldsbyoperation.get(operation).containsKey(section)){
				return servicefieldsbyoperation.get(operation).get(section).values().toArray(new FieldSet[0]);
			}
		}
		return new FieldSet[0];
	}
	public FieldSet[] getAllFieldFullList(String operation) {
		if(allfieldsbyoperation.containsKey(operation)){
			return allfieldsbyoperation.get(operation).values().toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}
	
	public FieldSet[] getAllFieldTopLevel(String operation) {
		if(operation.equals("")){ //return all fields
			return fieldTopLevel.values().toArray(new FieldSet[0]);
		}
		if(operation.equals("search")){
			return searchFieldFullList.values().toArray(new FieldSet[0]);
		}
		else if(topfieldsbyoperation.containsKey(operation)){
			return topfieldsbyoperation.get(operation).values().toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}
	//merged fields are fields in the UI that take multiple service layer fields
	public FieldSet[]  getAllMergedFields(){
		FieldSet[] merged = mergedfields.values().toArray(new FieldSet[0]);
		return merged;
	}
	
	public FieldSet getFieldTopLevel(String id) {
		return fieldTopLevel.get(id);
	}
	public FieldSet getFieldFullList(String id) {
		return fieldFullList.get(id);
	}
	public FieldSet getServiceFieldFullList(String id) {
		return serviceFieldFullList.get(id);
	}
	public FieldSet getSearchFieldFullList(String id) {
		return searchFieldFullList.get(id);
	}
	public Boolean hasSearchField(String id) {
		return searchFieldFullList.containsKey(id);
	}
	public void setMerged(Field f){
		mergedfields.put(f.getID(), f);
	}
	public Boolean hasMerged() {
		if (mergedfields.isEmpty())
			return false;

		return true;
	}
	//generic function to simplify adding to hashmaps
	private void hashutil(Map<String, Map<String, Map<String, FieldSet>>> testfields, String level1, String level2, FieldSet fs){
		if (!testfields.containsKey(level1)) {
			testfields.put(level1, new HashMap<String, Map<String, FieldSet>>());
		}
		if (!testfields.get(level1).containsKey(level2)) {
			testfields.get(level1).put(level2,new HashMap<String, FieldSet>());
		}
		testfields.get(level1).get(level2).put(fs.getID(), fs);
	}
	//generic function to simplify adding to hashmaps
	private void hashutil(Map<String, Map<String, FieldSet>> testfields, String level1, FieldSet fs){
		if (!testfields.containsKey(level1)) {
			testfields.put(level1, new HashMap<String, FieldSet>());
		}
		testfields.get(level1).put(fs.getID(), fs);
		
	}
	/** end field functions **/
	
	
	
	
	
	
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

	public String getSearchURL() {
		return getString("search-url");
	}

	public boolean isShowType(String k){
		if(getString("showin").equals("")){
			return getSet("@type").contains(k);
		}
		else{
			return getString("showin").equals(k);
		}
	}
	public boolean isType(String k) {
		return getSet("@type").contains(k);
	}

	public Spec getSpec() {
		return spec;
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
	public String[] getAllUISections(String section){
		if(section.equals("")){section = "base";}
		if(uisection.containsKey(section)){
			return uisection.get(section).values().toArray(new String[0]);
		}
		return null;
	}
	public String getUISections(String section, String id){
		if(section.equals("")){section = "base";}
		if(uisection.containsKey(section)){
			return uisection.get(section).get(id);
		}
		return null;
	}



	public String enumBlankValue(){
		return getString("enum-blank");
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
			for (FieldSet fs : this.getAllFieldFullList(perm)) {
				if (fs.usesRecord()) {
					this.addSubRecord(fs,perm);
				}
			}
		}
		return subrecordsperm.get(perm).values().toArray(new FieldSet[0]);
	}	


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
	public boolean isRealRecord() {
		return getBoolean("@separate-record");
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
	
	public String[] getServicesInstancesPaths(){
		return services_instances_paths.keySet().toArray(new String[0]);
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
	public String getServicesInstancesPath(String name) {
		return services_instances_paths.get(name);
	}
	
	public Boolean hasServicesInstancesPath(String name) {
		if(services_instances_paths.containsKey(name)){
			return true;
		}
		return false;
	}

	
	void setMiniNumber(FieldSet f) {
		mini_number = f;
	}

	void setMiniSummary(FieldSet f) {
		mini_summary = f;
	}

	void setDisplayName(Field f) {
		display_name = f;
	}

	void setServicesRecordPath(String section, String path) {
		services_record_paths.put(section, path);
	}
	void setServicesInstancePath(String section, String path){
		services_instances_paths.put(section, path);
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


	public void addUISection(String section, String id){
		if(section.equals("")){section = "base";}
		if(!uisection.containsKey(section)){
			uisection.put(section, new HashMap<String, String>());
		}
		uisection.get(section).put(id, id);
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

	void dumpJson(JSONObject out) throws JSONException {
		JSONObject record = new JSONObject();
		record.put("id", this.getID());
		record.put("web_url", getWebURL());
		record.put("type", getSet("@type"));
		JSONArray fields = new JSONArray();
		
		for(String s: this.getAllString()){
			JSONObject data = new JSONObject();
			data.put("type", "String");
			data.put("name", s);
			data.put("value", this.allStrings.get(s));
			data.put("default",  this.allDefaultStrings.get(s));
			fields.put(data);
		}
		for(String s: this.getAllBoolean()){
			JSONObject data = new JSONObject();
			data.put("type", "Boolean");
			data.put("name", s);
			data.put("value", this.allBooleans.get(s));
			data.put("default",  this.allDefaultBooleans.get(s));
			fields.put(data);
		}
		for(String s: this.getAllSets()){
			JSONObject data = new JSONObject();
			data.put("type", "Set");
			data.put("name", s);
			data.put("value", this.allSets.get(s));
			data.put("default",  this.allDefaultSets.get(s));
			fields.put(data);
		}
		record.put("fields", fields);
		out.put(this.getID(), record);
	}

	public Record getRecord() {
		return this;
	}

	public void config_finish(Spec spec) {
		for (Instance n : instances.values())
			n.config_finish(spec);
		for (FieldSet fs : fieldTopLevel.values())
			fs.config_finish(spec);
	}

	@Override
	public boolean isExpander() {
		return false;
	}
}
