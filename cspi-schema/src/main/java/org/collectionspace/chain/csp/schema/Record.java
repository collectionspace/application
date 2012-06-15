/* Copyright 2010-2012 University of Cambridge
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
	
	public static final String SUPPORTS_LOCKING = "supportslocking";
	
	private static final Logger log = LoggerFactory.getLogger(Record.class);
	protected SchemaUtils utils = new SchemaUtils();
	
	private Map<String, Structure> structure = new HashMap<String, Structure>();
	private Map<String, Map<String, String>> uisection = new HashMap<String, Map<String, String>>();
	private List<FieldSet> selfRenderers = new ArrayList<FieldSet>();
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
	
	//list of all 'record' e.g. structuredDates, dimensions etc that are included
	private Map<String, String> nestedFieldList = new HashMap<String, String>();
	
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
		utils.initStrings(section,"@id",null);
		whoamI = utils.getString("@id");
		// record,authority,compute-displayname can have multiple types using
		// commas
		utils.initSet(section,"@type",new String[] { "record" });
		utils.initStrings(section,"showin","");

		// specified that it is included in the findedit uispec - probably not useful any more?
//		utils.initBoolean(section,"@in-findedit",false);

		utils.initBoolean(section,"@in-recordlist",true);

		//Record differentiates between things like structureddates and procedures
		utils.initBoolean(section,"@separate-record",true);
		
		
		
		// config whether service layer needs call as multipart or not - authorization is not currently multipart
		utils.initBoolean(section,"is-multipart",true);

		// config whether record type has termsUsed or not (returns empty array
		// in Json if = false )
		utils.initBoolean(section,"terms-used",true);
		// config whether record type has relatedObj/procs or not (returns empty
		// array in Json if = false )
		utils.initBoolean(section,"refobj-used",true);
		
		// config the keyword to use for searching
		utils.initStrings(section,"services-search-keyword","kw");

		// Used to differentiate between authority and vocabulary on create
		//utils.initStrings(section,"membership-tag","inAuthority");

		/* UI Layer helpers */
		// ui layer path
		utils.initStrings(section,"web-url", utils.getString("@id"));

		// specify url if not nameAuthority
		//utils.initStrings(section,"terms-used-url", "nameAuthority");

		utils.initStrings(section,"preselector", ".csc-" );
		utils.initStrings(section,"decoratorselector", "cs-" );
		
		// ui layer json row
		utils.initStrings(section,"number-selector", utils.getString("preselector")+"entry-number");

		
		// ui layer json used in list views
		utils.initStrings(section,"row-selector",utils.getString("preselector")+"recordList-row:");

		// ui layer path: defaults to web_url if not specified
		utils.initStrings(section,"ui-url", utils.getString("web-url") + ".html");

		// ui layer path
		utils.initStrings(section,"tab-url", utils.getString("web-url") + "-tab");
		utils.initStrings(section,"search-url", utils.getString("web-url") + "-search");

		utils.initStrings(section,"enum-blank", data.get("blank"));
		/* Service layer helpers */

		// path that the service layer uses to access this record
		utils.initStrings(section,"services-url", utils.getString("@id"));

		// authorization 
		utils.initStrings(section,"authorization-includes", utils.getString("services-url"));

		utils.initBoolean(section,"authorization-view",true);

		// service layer paths to list data for this record type
		utils.initStrings(section,"services-list-path", utils.getString("services-url") + "-common-list/"
				+ utils.getString("services-url") + "-list-item");

		// This is relative to the services-list-path, in usage.
		utils.initStrings(section,"services-fields-path", "fieldsReturned");

		// used by service layer to construct authority names
		utils.initStrings(section,"authority-vocab-type","");
		//
		utils.initStrings(section,"services-instances-path", utils.getString("services-url")
						+ "_common:http://collectionspace.org/services/"
						+ utils.getString("services-url") + "," + utils.getString("services-url") + "-common-list/"
						+ utils.getString("services-url") + "-list-item");

		//
		utils.initStrings(section,"services-single-instance-path", utils.getString("services-url")
						+ "_common:http://collectionspace.org/services/"
						+ utils.getString("services-url") + "," + utils.getString("services-url") + "-common");
		utils.initStrings(section,"primaryfield", "");
		utils.initBoolean(section,"hasdeletemethod",false);
		utils.initBoolean(section,"hassoftdelete",false);
		utils.initBoolean(section,SUPPORTS_LOCKING,false);
		
//(17:06) The services singular tag should probably be "ServicesDocumentType"


		utils.initStrings(section,"services-tenant-singular", utils.getString("services-url"));
		utils.initStrings(section,"services-tenant-plural", utils.getString("services-tenant-singular")+"s");
		utils.initStrings(section,"services-tenant-auth-singular", utils.getString("services-url"));
		utils.initStrings(section,"services-tenant-auth-plural", utils.getString("services-tenant-singular")+"s");

		utils.initStrings(section,"services-schemalocation", "http://services.collectionspace.org");
		
		utils.initStrings(section,"services-dochandler","org.collectionspace.services."+ utils.getString("services-tenant-singular").toLowerCase() +".nuxeo."+ utils.getString("services-tenant-singular")+"DocumentModelHandler");
		utils.initStrings(section,"services-abstract","org.collectionspace.services."+utils.getString("services-tenant-singular").toLowerCase()+"."+ utils.getString("services-tenant-plural") +"CommonList");
		utils.initStrings(section,"services-common", utils.getString("services-abstract") + "$"+utils.getString("services-tenant-singular")+"ListItem");
		utils.initStrings(section,"services-validator","org.collectionspace.services."+ utils.getString("services-tenant-singular").toLowerCase() +".nuxeo."+ utils.getString("services-tenant-singular")+"ValidatorHandler");

		spec = parent;
	}


	
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
	
	/**
	 * mark up the different types of repeats
	 * repeator are adv search only repeats
	 * repeatored are adv search and edit/view repeats
	 * repeatable are just edit/view repeats
	 * @param f
	 */
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
	

	public void addNestedFieldList(String r){
		nestedFieldList.put(r,r);
	}
	
	public void addField(FieldSet f){
		String parentType = f.getParent().getClass().getSimpleName();
		fieldFullList.put(f.getID(),f);
		if(f.isInServices()){
			serviceFieldFullList.put(f.getID(),f);
		}
		if(f.isASelfRenderer()) {
			addSelfRenderer(f);
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
		
	public FieldSet[] getAllServiceFieldTopLevel(String operation, String section) { //section should be an array?
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
		if(fieldFullList.get(id)!=null){
			return fieldFullList.get(id);
		}
		for(String r: nestedFieldList.values().toArray(new String[0])){
			Record subitems = this.getSpec().getRecordByServicesUrl(r);
			if(subitems!=null){
				if(subitems.getFieldFullList(id)!=null){
					return subitems.getFieldFullList(id);
				}
			}
		}
		return null;
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
		return utils.getString("@id");
	}

	public String getWebURL() {
		return utils.getString("web-url");
	}
//used in tenantUIServlet for overlaying stuff and testing if I am showing the right page
	public String getUIURL() {
		return utils.getString("ui-url");
	}

	public String getTabURL() {
		return utils.getString("tab-url");
	}

	public String getSearchURL() {
		return utils.getString("search-url");
	}

	public boolean isShowType(String k){
		if(utils.getString("showin").equals("")){
			return utils.getSet("@type").contains(k);
		}
		else{
			return utils.getString("showin").equals(k);
		}
	}
	public boolean isType(String k) {
		return utils.getSet("@type").contains(k);
	}

	public Spec getSpec() {
		return spec;
	}
	public FieldParent getParent() {
		return null;
	}
	public String getPreSelector() {
		return utils.getString("preselector");
	}
	public String getDecoratorSelector() {
		return utils.getString("decoratorselector");
	}
	public String getUIprefix(){
		return getPreSelector()  + "";
	}
	public String getUILabel(String id){
		return utils.getString("@id") + "-" + id + "Label";
	}
	public String getUILabelSelector(String id){
		return getPreSelector()  + utils.getString("@id") + "-" +  id + "-label";
	}
	public String getUILabelSelector() {
		return getUIprefix() +  utils.getString("@id") + "-label";
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
		return utils.getString("enum-blank");
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
		return utils.getString("primaryfield");
	};

	public Boolean hasPrimaryField() {
		if (getPrimaryField().equals("")) {
			return false;
		} else {
			return true;
		}
	}

	public String getNumberSelector() {
		return utils.getString("number-selector");
	}

	public String getRowSelector() {
		return utils.getString("row-selector");
	}

	public boolean isInRecordList() {
		return utils.getBoolean("@in-recordlist");
	}
	public boolean isRealRecord() {
		return utils.getBoolean("@separate-record");
	}
	
	public boolean isMultipart() {
		return utils.getBoolean("is-multipart");
	}

	public boolean hasTermsUsed() {
		return utils.getBoolean("terms-used");
	}

	public boolean hasRefObjUsed() {
		return utils.getBoolean("refobj-used");
	}
	public boolean hasHierarchyUsed(String type) {
		return this.getStructure(type).showHierarchySection();
	}

	public boolean hasDeleteMethod() {
		return utils.getBoolean("hasdeletemethod");
	}

	public boolean hasSoftDeleteMethod() {
		return utils.getBoolean("hassoftdelete");
	}
	
	public boolean supportsLocking() {
		return utils.getBoolean(SUPPORTS_LOCKING);
	}
	
	public String getServicesSearchKeyword() {
		return utils.getString("services-search-keyword");
	}

	public String getVocabType() {
		return utils.getString("authority-vocab-type");
	}

	public Instance[] getAllInstances() {
		return instances.values().toArray(new Instance[0]);
	}

	public int getNumInstances() {
		return instances.size();
	}

	public Instance getInstance(String key) {
		return instances.get(key);
	}
	public Boolean hasInstance(String key) {
		return instances.containsKey(key);
	}

	public String getServicesURL() {
		return utils.getString("services-url");
	}

	public String getServicesTenantSg() {
		return utils.getString("services-tenant-singular");
	}

	public String getServicesTenantPl() {
		return utils.getString("services-tenant-plural");
	}

	public String getServicesTenantAuthSg() {
		return utils.getString("services-tenant-auth-singular");
	}

	public String getServicesTenantAuthPl() {
		return utils.getString("services-tenant-auth-plural");
	}
	
	public String getServicesAbstractCommonList(){
		return utils.getString("services-abstract");
	}
	public String getServicesValidatorHandler(){
		return utils.getString("services-validator");
		
	}
	public String getServicesCommonList(){
		return utils.getString("services-common");
	}

	public String getServicesSchemaBaseLocation(){
		return utils.getString("schema-location");
	}
	
	public String getServicesDocHandler(){
		return utils.getString("services-dochandler");
	}
	
	public String getServicesListPath() {
		return utils.getString("services-list-path");
	}

	public String getServicesFieldsPath() {
		return utils.getString("services-fields-path");
	}

	public String getServicesInstancesPath() {
		return utils.getString("services-instances-path");
	}

	public String getServicesSingleInstancePath() {
		return utils.getString("services-single-instance-path");
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

	private void findMiniNumber() {
		if(mini_number==null) {
			// Try child expanders to see if the mini_number is there.
			// This handles case of mini_number for terms in the "preferred" termList sub-record
			for(FieldSet fs : selfRenderers) {
				Record subrecord = fs.getSelfRendererRecord();
				FieldSet miniNumberFS = subrecord.getMiniNumber();
				if(miniNumberFS != null) {
					mini_number = miniNumberFS;
					return;
				}
			}
		}
	}

	public FieldSet getMiniSummary() {
		return mini_summary;
	}

	private void findMiniSummary() {
		if(mini_summary==null) {
			// Try child expanders to see if the mini_summary is there.
			// This handles case of mini_summary for terms in the "preferred" termList sub-record
			for(FieldSet fs : selfRenderers) {
				Record subrecord = fs.getSelfRendererRecord();
				FieldSet miniSummaryFS = subrecord.getMiniNumber();
				if(miniSummaryFS != null) {
					mini_summary = miniSummaryFS;
					return;
				}
			}
		}
	}

	public FieldSet[] getAllMiniSummaryList() {
		return summarylist.values().toArray(new FieldSet[0]);
	}

	public FieldSet getMiniSummaryList(String key) {
		return summarylist.get(key);
	}

	// obsolete?
	public void addMiniSummaryList(FieldSet f) {
		summarylist.put(f.getID(), f);
	}

	private void mergeNestedSummaryLists() {
		for(FieldSet fs : selfRenderers) {
			Record subrecord = fs.getSelfRendererRecord();
			if(!subrecord.summarylist.isEmpty())
				summarylist.putAll(subrecord.summarylist);
		}
	}
	
	/**
	 * For selfrenderer children, like the preferredTerm block in authorities, we need to gather
	 * up all the declared search fields into this parent, so that when we generate a UISpec
	 * or UISchema for search, we get those nested fields as well.
	 */
	private void mergeSearchLists() {
		for(FieldSet fs : selfRenderers) {
			Record subrecord = fs.getSelfRendererRecord();
			if(!subrecord.searchFieldFullList.isEmpty())
				searchFieldFullList.putAll(subrecord.searchFieldFullList);
		}
	}
	

	public FieldSet getDisplayNameField() {
		return display_name;
	}

	private void findDisplayNameField() {
		if(display_name==null) {
			// Try child expanders to see if the displayName is there.
			// This handles case of displayName for terms in the "preferred" termList sub-record
			for(FieldSet fs : selfRenderers) {
				Record subrecord = fs.getSelfRendererRecord();
				FieldSet displayNameFS = subrecord.getDisplayNameField();
				if(displayNameFS != null) {
					display_name = displayNameFS;
					return;
				}
			}
		}
	}



	// authorization
	public Boolean getAuthorizationView() {
		return utils.getBoolean("authorization-view");
	}


	public String getAuthorizationType() {
		return utils.getString("authorization-includes");
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
	
	protected void addSelfRenderer(FieldSet fs) {
		selfRenderers.add(fs);
	}

	public void addInstance(Instance n) {
		instances.put(n.getID(), n);
		spec.addInstance(n);
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
	
	private void mergeNestedMiniLists() {
		for(FieldSet fs : selfRenderers) {
			Record subrecord = fs.getSelfRendererRecord();
			for(String listName:subrecord.minidataset.keySet()) {
				Map<String, FieldSet> subRecordMiniList = subrecord.minidataset.get(listName);
				if(!subRecordMiniList.isEmpty()) {
					Map<String, FieldSet> fieldsForList;
					if (!minidataset.containsKey(listName)) {
						fieldsForList = new HashMap<String, FieldSet>();
						minidataset.put(listName, fieldsForList);
					} else {
						fieldsForList = minidataset.get(listName);
					}
					fieldsForList.putAll(subRecordMiniList);
				}
			}
		}
	}

	void dump(StringBuffer out) {
		out.append("  record id=" + this.getID() + "\n");
		out.append("    web_url=" + getWebURL() + "\n");
		out.append("    type=" + utils.getSet("@type") + "\n");
		utils.dump(out);
	}

	void dumpJson(JSONObject out) throws JSONException {
		JSONObject record = new JSONObject();
		utils.dumpJson(record, "attributes");
		record.put("id", this.getID());
		record.put("web_url", getWebURL());
		record.put("type", utils.getSet("@type"));
		out.put(this.getID(), record);
	}

	void dumpJsonFields(JSONObject out) throws JSONException {
		JSONObject record = new JSONObject();
		String[] allStrings = null;
		String[] allBooleans = null;
		String[] allSets = null;
		JSONObject RecordTable=new JSONObject();
		for(FieldSet fs : this.getAllFieldFullList("")){
			if(allStrings == null){
				allStrings = fs.getUtils().getAllString();
				allBooleans = fs.getUtils().getAllBoolean();
				allSets = fs.getUtils().getAllSets();

				for(String s: allStrings){
					JSONObject data = new JSONObject();
					data.put("default",  fs.getUtils().getDefaultString(s));
					RecordTable.put("String:"+s, data);
				}
				for(String s: allBooleans){
					JSONObject data = new JSONObject();
					data.put("default",  fs.getUtils().getDefaultBoolean(s));
					RecordTable.put("Boolean:"+s,data);
				}
				for(String s:allSets){
					JSONObject data = new JSONObject();
					data.put("default",  fs.getUtils().getDefaultSet(s));
					RecordTable.put("Set:"+s,data);
				}
			}
			else{

				for(String s: allStrings){
					JSONObject data = RecordTable.getJSONObject("String:"+s);
					data.put(fs.getID(), fs.getUtils().getString(s));
					RecordTable.put("String:"+s, data);
				}
				for(String s: allBooleans){
					JSONObject data = RecordTable.getJSONObject("Boolean:"+s);
					data.put(fs.getID(), fs.getUtils().getBoolean(s));
					RecordTable.put("Boolean:"+s,data);
				}
				for(String s:allSets){
					JSONObject data = RecordTable.getJSONObject("Set:"+s);
					data.put(fs.getID(), fs.getUtils().getSet(s));
					RecordTable.put("Set:"+s,data);
				}
			}
		}

		out.put("allfields", RecordTable);
		out.put(this.getID(), record);
	}
	
	String dumpFields() throws JSONException {
		StringBuffer out=new StringBuffer();
		String[] allStrings = null;
		String[] allBooleans = null;
		String[] allSets = null;
		Map <String, StringBuffer> rout = new HashMap<String, StringBuffer>();
		

		allStrings = this.getAllFieldFullList("")[0].getUtils().getAllString();
		allBooleans = this.getAllFieldFullList("")[0].getUtils().getAllBoolean();
		allSets = this.getAllFieldFullList("")[0].getUtils().getAllSets();

		out.append("field" + ",");
		rout.put("default", new StringBuffer());
		for(FieldSet fs : this.getAllFieldFullList("")){
			rout.put(fs.getID(), new StringBuffer());
		}

		for(String s: allStrings){
			out.append(s + ",");
			rout.get("default").append(this.getAllFieldFullList("")[0].getUtils().getDefaultString(s) + ",");
			for(FieldSet fs : this.getAllFieldFullList("")){
				rout.get(fs.getID()).append(fs.getUtils().getString(s) + ",");
			}
		}
		for(String s: allBooleans){
			out.append(s + ",");
			rout.get("default").append(this.getAllFieldFullList("")[0].getUtils().getDefaultBoolean(s) + ",");
			for(FieldSet fs : this.getAllFieldFullList("")){
				rout.get(fs.getID()).append(fs.getUtils().getBoolean(s) + ",");
			}
		}
		for(String s: allSets){
			out.append(s + ",");
			rout.get("default").append("\"" + this.getAllFieldFullList("")[0].getUtils().allDefaultSets.get(s) + "\"" + ",");
			for(FieldSet fs : this.getAllFieldFullList("")){
				rout.get(fs.getID()).append("\"" + fs.getUtils().allSets.get(s) + "\"" + ",");
			}
		}

		for (Map.Entry<String, StringBuffer> entry : rout.entrySet()) {
		    String key = entry.getKey();
		    StringBuffer value = entry.getValue();
		    out.append("\n");
		    out.append(key+",");
		    out.append(value);
		    // ...
		}
		return out.toString();
		//out.put("allfields", RecordTable);
		//out.put(this.getID(), record);
	}

	public Record getRecord() {
		return this;
	}

	public void config_finish(Spec spec) {
		for (Instance n : instances.values())
			n.config_finish(spec);
		for (FieldSet fs : fieldTopLevel.values())
			fs.config_finish(spec);
		// Handle nested self-renderers, to harvest things from nested like displayName, 
		// mini lists, etc. 
		findDisplayNameField();
		findMiniNumber();
		findMiniSummary();
		mergeNestedSummaryLists();
		mergeNestedMiniLists();
		mergeSearchLists();
	}

	@Override
	public boolean isExpander() {
		return false;
	}
}
