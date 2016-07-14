/* Copyright 2010-2012 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ReadOnlySection;

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

	public final static String BLOB_SOURCE_URL = "blobUri"; // BlobClient.BLOB_URI_PARAM; // The 'blobUri' query param used to pass an external URL for the services to download data from
	public final static String BLOB_PURGE_ORIGINAL = "blobPurgeOrig"; // BlobClient.BLOB_PURGE_ORIGINAL;

	private static final String TYPE_AUTHORITY = "Authority";
	private static final String TYPE_AUTHORITY_LOWERCASE = TYPE_AUTHORITY.toLowerCase();

	public static final String SUPPORTS_LOCKING = "supportslocking";
	public static final String SUPPORTS_REPLICATING = "supportsReplicating";
	public static final String REMOTECLIENT_CONFIG_NAME = "remoteClientConfigName";
	public static final String REQUIRES_UNIQUE_SHORTID = "requiresUniqueShortId";
	public static final String SUPPORTS_VERSIONING = "supportsversioning";
	public static final String RANGE_START_SUFFIX = "Start";
	public static final String RANGE_END_SUFFIX = "End";
	
	public static final String COLLECTIONSPACE_CORE_PART_NAME = "collectionspace_core";
	public static final String COLLECTIONSPACE_COMMON_PART_NAME = "common";
	public static final String COLLECTIONSPACE_SYSTEM_PART_NAME = "system";
	public static final String COLLECTIONSPACE_SCHEMA_LOCATION_SYSTEM = "http://collectionspace.org/services/config/system http://collectionspace.org/services/config/system/system-response.xsd";
	public static final String COLLECTIONSPACE_NAMESPACE_URI_SYSTEM = "http://collectionspace.org/services/config/system";	

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
	private Map<String, Map<String, Map<String, FieldSet>>> servicefieldsbyoperation = new HashMap<String, Map<String, Map<String, FieldSet>>>();
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

	private Map<String, Instance> instances = new LinkedHashMap<String, Instance>();
	private Map<String, FieldSet> summarylist = new HashMap<String, FieldSet>();
	private Map<String, Map<String, FieldSet>> minidataset = new HashMap<String, Map<String, FieldSet>>();
	private Spec spec;
	private FieldSet mini_summary, mini_number, display_name;
	public String whoamI = ""; // Used for debugging purposes.
	private HashSet<String> authTypeTokenSet = new HashSet<String>();
	private Record lastAuthoriyProxy = null; // Used during Service binding generation.  Only the "baseAuthority" record ever uses this member.  Values would be things like PersonAuthority, OrgAuthority, and other authority records.

	/* Service stuff */
	private Map<String, String> services_record_paths = new HashMap<String, String>();
	private Map<String, String> services_instances_paths = new HashMap<String, String>();
	private Map<String, Field> services_filter_param = new HashMap<String, Field>();

	public Record getLastAuthorityProxy() {
		return this.lastAuthoriyProxy;
	}

	public void setLastAuthorityProxy(Record lastAuthoriyProxy) {
		this.lastAuthoriyProxy = lastAuthoriyProxy;
	}

	// XXX utility methods
	Record(Spec parent, ReadOnlySection section, Map<String, String> data) {
		//Map<String,String>data = (Map<String,String>)parent;
		/* parameters */
		// this is what the service layer id defaults to if not specified later
		// standard = singular form of the concept
		utils.initStrings(section, "@id", null);
		whoamI = utils.getString("@id");

		utils.initStrings(section, "@cms-type", "none");
		utils.initBoolean(section, "@generate-services-schema", true);
		utils.initBoolean(section, "@is-extension", false);
		utils.initBoolean(section, "@generate-if-authority", true); // REM: 12/2012 - The Contact service config file will set this value to false for schema generation -"false" since Contact is not a true authority from the Service's perspective.

		//
		// The name for used to create the Services XML Schema.
		//
		utils.initStrings(section, "@services-type", null);

		// record,authority,compute-displayname can have multiple types using
		// commas
		utils.initSet(section, "@type", new String[] { "record" });
		//
		// Service specific config for Nuxeo ECM platform - things added to the "doctype" definitions
		//
		utils.initSet(section, "@services-folder-subtypes", new String[] {});
		utils.initSet(section, "@services-workspace-subtypes", new String[] {});
		utils.initSet(section, "@services-prefetch-fields",
				new String[] { "<!-- Fields to be prefetch by the repository manager. -->" });

		utils.initStrings(section, "showin", "");

		// specified that it is included in the findedit uispec - probably not useful any more?
		//		utils.initBoolean(section,"@in-findedit",false);

		utils.initBoolean(section, "@in-recordlist", true);

		//Record differentiates between things like structureddates and procedures
		utils.initBoolean(section, "@separate-record", true);

		// config whether service layer needs call as multipart or not - authorization is not currently multipart
		utils.initBoolean(section, "is-multipart", true);

		// config whether record type has termsUsed or not (returns empty array
		// in Json if = false )
		utils.initBoolean(section, "terms-used", true);
		// config whether record type has relatedObj/procs or not (returns empty
		// array in Json if = false )
		utils.initBoolean(section, "refobj-used", true);

		// config the keyword to use for searching
		utils.initStrings(section, "services-search-keyword", "kw");

		// Used to differentiate between authority and vocabulary on create
		//utils.initStrings(section,"membership-tag","inAuthority");

		/* UI Layer helpers */
		// ui layer path
		utils.initStrings(section, "web-url", utils.getString("@id"));

		// specify url if not nameAuthority
		//utils.initStrings(section,"terms-used-url", "nameAuthority");

		utils.initStrings(section, "preselector", ".csc-");
		utils.initStrings(section, "decoratorselector", "cs-");

		// ui layer json row
		utils.initStrings(section, "number-selector", utils.getString("preselector") + "entry-number");

		// ui layer json used in list views
		utils.initStrings(section, "row-selector", utils.getString("preselector") + "recordList-row:");

		// ui layer path: defaults to web_url if not specified
		utils.initStrings(section, "ui-url", utils.getString("web-url") + ".html");

		// ui layer path
		utils.initStrings(section, "tab-url", utils.getString("web-url") + "-tab");
		utils.initStrings(section, "search-url", utils.getString("web-url") + "-search");

		utils.initStrings(section, "enum-blank", data.get("blank"));
		/* Service layer helpers */

		// path that the service layer uses to access this record
		utils.initStrings(section, "services-url", utils.getString("@id"));

		// authorization 
		utils.initStrings(section, "authorization-includes", utils.getString("services-url"));

		utils.initBoolean(section, "authorization-view", true);

		// service layer paths to list data for this record type
		utils.initStrings(section, "services-list-path",
				utils.getString("services-url") + "-common-list/" + utils.getString("services-url") + "-list-item");

		// This is relative to the services-list-path, in usage.
		utils.initStrings(section, "services-fields-path", "fieldsReturned");

		// used by service layer to construct authority names
		utils.initStrings(section, "authority-vocab-type", "");
		//
		utils.initStrings(
				section,
				"services-instances-path",
				utils.getString("services-url") + "_common:http://collectionspace.org/services/"
						+ utils.getString("services-url") + "," + utils.getString("services-url") + "-common-list/"
						+ utils.getString("services-url") + "-list-item");

		//
		utils.initStrings(
				section,
				"services-single-instance-path",
				utils.getString("services-url") + "_common:http://collectionspace.org/services/"
						+ utils.getString("services-url") + "," + utils.getString("services-url") + "-common");
		utils.initStrings(section, "primaryfield", "");
		utils.initBoolean(section, "hasdeletemethod", false);
		utils.initBoolean(section, "hassoftdelete", false);
		utils.initBoolean(section, SUPPORTS_LOCKING, false);
		utils.initBoolean(section, SUPPORTS_REPLICATING, false);
		utils.initStrings(section, REMOTECLIENT_CONFIG_NAME, "");
		utils.initBoolean(section, SUPPORTS_VERSIONING, false);

		//(17:06) The services singular tag should probably be "ServicesDocumentType"

		// The tenant's repository that the service will use.
		utils.initStrings(section, "services-repo-domain", utils.getString("services-repo-domain"));

		utils.initStrings(section, "services-tenant-singular", utils.getString("services-url"));
		utils.initStrings(section, "services-tenant-doctype", null);//, utils.getString("services-tenant-singular"));

		utils.initStrings(section, "services-tenant-plural", utils.getString("services-tenant-singular") + "s");
		utils.initStrings(section, "services-tenant-auth-singular", utils.getString("services-url"));
		utils.initStrings(section, "services-tenant-auth-plural", utils.getString("services-tenant-singular") + "s");

		utils.initStrings(section, "services-schema-location", "http://services.collectionspace.org");

		utils.initStrings(section, "services-abstract",
				"org.collectionspace.services." + utils.getString("services-tenant-singular").toLowerCase() + "."
						+ utils.getString("services-tenant-plural") + "CommonList");
		utils.initStrings(section, "services-common",
				utils.getString("services-abstract") + "$" + utils.getString("services-tenant-singular") + "ListItem");

		utils.initStrings(section, "services-dochandler", null);
		utils.initStrings(section, "services-validator", null);

		spec = parent;
	}

	public String getRecordName() {
		return this.whoamI;
	}

	public boolean isTrueRepeatField() {
		return false;
	}

	/** field functions **/
	//getPerm is now hasFieldByOperation(fieldId,operation)
	//tests if a specific field exists for a certain operation
	public Boolean hasFieldByOperation(String fieldId, String operation) {
		if (allfieldsbyoperation.containsKey(operation)) {
			if (allfieldsbyoperation.get(operation).containsKey(fieldId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * mark up the different types of repeats repeator are adv search only
	 * repeats repeatored are adv search and edit/view repeats repeatable are
	 * just edit/view repeats
	 * 
	 * @param f
	 */
	public void addSearchField(FieldSet f) {

		if (!(f.getSearchType().equals(""))) {
			FieldSet searchf = f;
			if (searchf.getSearchType().equals("repeatable")) {
				//need to makeup a repeatable field
				if (f.getParent() instanceof Repeat) {
					Repeat rs = (Repeat) f.getParent();
					//XXX this name is terrible - should be a better way
					rs.setSearchType("repeatored"); //need to set the type of repeat this is e.g. is it just for search or is is also for edit
					searchFieldFullList.put(rs.getID(), rs);
				} else {
					Repeat r = new Repeat(searchf.getRecord(), searchf.getID() + "s"); //UI wants 'plurals' for the fake repeat parents
					r.setSearchOnlyRepeat(true); // Our parent is not a "real" Repeat field.  It's just acting this way for searches.
					//XXX this name is terrible - should be a better way
					r.setSearchType("repeator");
					searchFieldFullList.put(r.getID(), r);

					searchf.setParent(r);
					r.addChild(searchf); // this is causing confusing later on... can we separate this some how?
				}
			} else if (searchf.getSearchType().equals("range")) {
				searchf.getRecord().addUISection("search", searchf.getID());
				//need to add label for main bit as well...
				Field fst = new Field(searchf.getRecord(), searchf.getID() + RANGE_START_SUFFIX);
				Field fed = new Field(searchf.getRecord(), searchf.getID() + RANGE_END_SUFFIX);

				fst.setSearchType("range");
				fed.setSearchType("range");
				fed.setType(searchf.getUIType());
				fst.setType(searchf.getUIType());
				searchFieldFullList.put(fst.getID(), fst);
				searchFieldFullList.put(fed.getID(), fed);
			} else {
				searchFieldFullList.put(searchf.getID(), searchf);
			}
		}
	}

	public void addNestedFieldList(String r) {
		nestedFieldList.put(r, r);
	}

	public void addField(FieldSet f) {
		String parentType = f.getParent().getClass().getSimpleName();
		fieldFullList.put(f.getID(), f);
		if (f.isInServices() || f.isServicesDerived()) {
			serviceFieldFullList.put(f.getID(), f);
		} else {
			log.trace(String.format("%s: isInServices() and isServicesDerived() methods returned false, so we wont add '%s' to the serviceFieldFullList.",
					f.getID(), f.getID()));
		}
		if (f.isASelfRenderer()) {
			addSelfRenderer(f);
		}

		//is this a search field?
		addSearchField(f);

		if (parentType.equals("Record")) { //toplevel field
			fieldTopLevel.put(f.getID(), f);
			if (f.isInServices() || f.isServicesDerived()) {
				serviceFieldTopLevel.put(f.getID(), f);
				//list fields by the operations they support
				for (String oper : f.getAllFieldOperations()) {
					hashutil(servicefieldsbyoperation, oper, f.getSection(), f);
					//subdivide section and then by GET POST PUT etc
				}
			} else {
				log.trace(String.format("%s: isInServices() and isServicesDerived() methods returned false, so we wont add '%s' to the serviceFieldTopLevel.",
						f.getID(), f.getID()));
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
		if (operation.equals("")) {//return everything
			return serviceFieldTopLevel.values().toArray(new FieldSet[0]);
		}
		if (servicefieldsbyoperation.containsKey(operation)) {
			if (servicefieldsbyoperation.get(operation).containsKey(section)) {
				return servicefieldsbyoperation.get(operation).get(section).values().toArray(new FieldSet[0]);
			}
		}
		return new FieldSet[0];
	}

	public FieldSet[] getAllFieldFullList() {
		return fieldFullList.values().toArray(new FieldSet[0]);
	}
	
	public FieldSet[] getAllFieldFullList(String operation) {
		if (allfieldsbyoperation.containsKey(operation)) {
			return allfieldsbyoperation.get(operation).values().toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}

	public FieldSet[] getAllFieldTopLevel(String operation) {
		if (operation.equals("")) { //return all fields
			return fieldTopLevel.values().toArray(new FieldSet[0]);
		}
		if (operation.equals("search")) {
			return searchFieldFullList.values().toArray(new FieldSet[0]);
		} else if (topfieldsbyoperation.containsKey(operation)) {
			return topfieldsbyoperation.get(operation).values().toArray(new FieldSet[0]);
		}
		return new FieldSet[0];
	}

	//merged fields are fields in the UI that take multiple service layer fields
	public FieldSet[] getAllMergedFields() {
		FieldSet[] merged = mergedfields.values().toArray(new FieldSet[0]);
		return merged;
	}

	public FieldSet getField(String id) {
		return this.fieldFullList.get(id);
	}

	public FieldSet getFieldTopLevel(String id) {
		return fieldTopLevel.get(id);
	}

	public FieldSet getFieldFullList(String id) {
		if (fieldFullList.get(id) != null) {
			return fieldFullList.get(id);
		}
		for (String r : nestedFieldList.values().toArray(new String[0])) {
			Record subitems = this.getSpec().getRecordByServicesUrl(r);
			if (subitems != null) {
				if (subitems.getFieldFullList(id) != null) {
					return subitems.getFieldFullList(id);
				}
			}
		}
		return null;
	}

	/*
	 * Returns a list of fields that have the "authref-in-services" attribute
	 * defined
	 */
	public ArrayList<FieldSet> getOtherServiceAuthRefFields() {
		ArrayList<FieldSet> result = new ArrayList<FieldSet>();

		for (String key : fieldFullList.keySet()) {
			FieldSet fieldSet = fieldFullList.get(key);
			if (fieldSet.isAuthRefInServices() == true) {
				result.add(fieldSet);
			}
		}

		return result;
	}

	public Map<String, String> getNestedFieldList() {
		return this.nestedFieldList;
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

	public void setMerged(Field f) {
		mergedfields.put(f.getID(), f);
	}

	public Boolean hasMerged() {
		if (mergedfields.isEmpty())
			return false;

		return true;
	}

	//generic function to simplify adding to hashmaps
	private void hashutil(Map<String, Map<String, Map<String, FieldSet>>> testfields, String level1, String level2,
			FieldSet fs) {
		if (!testfields.containsKey(level1)) {
			testfields.put(level1, new HashMap<String, Map<String, FieldSet>>());
		}
		if (!testfields.get(level1).containsKey(level2)) {
			testfields.get(level1).put(level2, new HashMap<String, FieldSet>());
		}
		testfields.get(level1).get(level2).put(fs.getID(), fs);
	}

	//generic function to simplify adding to hashmaps
	private void hashutil(Map<String, Map<String, FieldSet>> testfields, String level1, FieldSet fs) {
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

	public boolean isShowType(String k) {
		if (utils.getString("showin").equals("")) {
			return utils.getSet("@type").contains(k);
		} else {
			return utils.getString("showin").equals(k);
		}
	}

	public boolean isAuthorityItemType() {
		return isType(TYPE_AUTHORITY_LOWERCASE) && shouldGenerateAuthoritySchema();
	}

	public boolean isType(String k) {
		return utils.getSet("@type").contains(k);
	}

	public Set<String> getServicesFolderSubtypes() {
		return utils.getSet("@services-folder-subtypes");
	}

	public Set<String> getServicesWorkspaceSubtypes() {
		return utils.getSet("@services-workspace-subtypes");
	}

	public Set<String> getServicesPrefetchFields() {
		return utils.getSet("@services-prefetch-fields");
	}

	public Spec getSpec() {
		return spec;
	}

	@Override
	public FieldParent getParent() {
		return null;
	}

	public String getPreSelector() {
		return utils.getString("preselector");
	}

	public String getDecoratorSelector() {
		return utils.getString("decoratorselector");
	}

	public String getUIprefix() {
		return getPreSelector() + "";
	}

	public String getUILabel(String id) {
		return utils.getString("@id") + "-" + id + "Label";
	}

	public String getServicesType() {
		String result = utils.getString("@services-type");
		return result;
	}

	public String getServicesCmsType() {
		String result = utils.getString("@cms-type");
		return result;
	}

	private boolean shouldGenerateAuthoritySchema() {
		boolean result = utils.getBoolean("@generate-if-authority");
		return result;
	}

	public boolean isServicesExtension() {
		boolean result = utils.getBoolean("@is-extension");
		return result;
	}

	public boolean isGenerateServicesSchema() {
		boolean result = utils.getBoolean("@generate-services-schema");
		return result;
	}

	public String getUILabelSelector(String id) {
		return getPreSelector() + utils.getString("@id") + "-" + id + "-label";
	}

	public String getUILabelSelector() {
		return getUIprefix() + utils.getString("@id") + "-label";
	}

	public String[] getAllUISections(String section) {
		if (section.equals("")) {
			section = "base";
		}
		if (uisection.containsKey(section)) {
			return uisection.get(section).values().toArray(new String[0]);
		}
		return null;
	}

	public String getUISections(String section, String id) {
		if (section.equals("")) {
			section = "base";
		}
		if (uisection.containsKey(section)) {
			return uisection.get(section).get(id);
		}
		return null;
	}

	public String enumBlankValue() {
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
		if (!subrecordsperm.containsKey(perm)) {
			subrecordsperm.put(perm, new HashMap<String, FieldSet>());
			for (FieldSet fs : this.getAllFieldFullList(perm)) {
				if (fs.usesRecord()) {
					this.addSubRecord(fs, perm);
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
	
	public boolean supportsReplicating() {
		return utils.getBoolean(SUPPORTS_REPLICATING);
	}

	public String getRemoteClientConfigName() {
		return utils.getString(REMOTECLIENT_CONFIG_NAME);
	}

	public boolean supportsVersioning() {
            return utils.getBoolean(SUPPORTS_VERSIONING);
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

	/*
	 * By convention, the value from getServicesTenantSg() is the Nuxeo doctype
	 * name. However, if the record explicitly declares a doctype using the
	 * "services-tenant-doctype" element then that value is used. Also,
	 * Authorities are a special case, the doctype should be the value from
	 * getServicesTenantAuthSg for Authorites.
	 */
	public String getServicesTenantDoctype(boolean isAuthority) {
		String result = this.getServicesTenantSg();

		String elementVal = utils.getString("services-tenant-doctype");
		if (elementVal != null && elementVal.trim().isEmpty() == false) {
			result = elementVal;
		}
		//
		// Handle special case for Authorities
		// 
		if (isAuthority == true) {
			result = this.getServicesTenantAuthSg();
		}

		return result;
	}

	public String getServicesTenantAuthPl() {
		return utils.getString("services-tenant-auth-plural");
	}

	public String getServicesAbstractCommonList() {
		return utils.getString("services-abstract");
	}

	public String getServicesCommonList() {
		return utils.getString("services-common");
	}

	public String getServicesSchemaBaseLocation() {
		return utils.getString("services-schema-location");
	}

	/*
	 * The "<services-tenant-auth-singular>" element of the config record for
	 * authorities is used to create the Nuxeo doctype names. We also need to
	 * use it to generate the Service layer's document handler class name for
	 * the service bindings. Unfortunately, most of the existing authorities use
	 * something like "Personauthority" for the Nuxeo document name and
	 * "PersonAuthorityDocument..." for the document handler class name.
	 * Therefore, we need to convert the lowercase 'a' to an uppercase 'A' in
	 * the "Authority" substring. For example, we convert the substring
	 * "Personauthority" to "PersonAuthority".
	 */
	private String getAuthorityForm(String handlerName) {
		String result = handlerName;

		String servicesTenantSg = this.getServicesTenantSg();
		String servicesTenantAuthSg = this.getServicesTenantAuthSg();
		String authorityName = servicesTenantAuthSg.replace(TYPE_AUTHORITY_LOWERCASE, TYPE_AUTHORITY); // replace "authority" with "Authority"
		result = result.replace(servicesTenantSg, authorityName);

		return result;
	}

	/*
	 * Gets the last word at the end of a URL.
	 */
	private static String getURLTail(String url) {
		String result = null;
		
		Scanner scanner = null;
		try {
			scanner = new Scanner(url).useDelimiter("\\/+");
		    while (scanner.hasNext() == true) {
		    	result = scanner.next();
		    }	 
		} catch (Exception e) {
			// Ignore the exception or logger.trace(e);
		} finally {
			scanner.close();
		}

		return result;
	}
	
	/*
	 * Gets the Service's document model handler by using standard naming conventions.
	 */
	public String getServicesDocHandler(Boolean isAuthority) {
		String result = utils.getString("services-dochandler");
		
		if (result == null) {
			result = "org.collectionspace.services." + getURLTail(getServicesSchemaNameSpaceURI(COLLECTIONSPACE_COMMON_PART_NAME)) + ".nuxeo."
					+ utils.getString("services-tenant-singular") + "DocumentModelHandler";
		}

		if (isAuthority == true) {
			result = getAuthorityForm(result);
		}

		return result;
	}

	/*
	 * Gets the Service's document model validation handler by using standard naming conventions.
	 * For example, org.collectionspace.services.collectionobject.nuxeo.CollectionObjectValidatorHandler
	 */	
	public String getServicesValidatorHandler(Boolean isAuthority) {
		String result = utils.getString("services-validator");

		if (result == null) {
			result = "org.collectionspace.services." + getURLTail(getServicesSchemaNameSpaceURI(COLLECTIONSPACE_COMMON_PART_NAME)) + ".nuxeo."
					+ utils.getString("services-tenant-singular") + "ValidatorHandler";
		}
		
		if (isAuthority == true) {
			result = getAuthorityForm(result);
		}

		return result;
	}
	
	//
	// For example, org.collectionspace.services.client.CollectionObjectClient
	//
	public String getServicesClientHandler(Boolean isAuthority) {
		String result = utils.getString("services-client");

		if (result == null) {
			result = "org.collectionspace.services.client." + utils.getString("services-tenant-singular")
					+ "Client";
		}
		
		if (isAuthority == true) {
			result = getAuthorityForm(result);
		}

		return result;
	}
	

	public String getServicesRepositoryDomain() {
		return utils.getString("services-repo-domain");
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

	public String[] getServicesRecordPathKeys() {
		return services_record_paths.keySet().toArray(new String[0]);
	}

	public String[] getServicesInstancesPaths() {
		return services_instances_paths.keySet().toArray(new String[0]);
	}

	public String getServicesRecordPath(String name) {
		if (services_record_paths.get(name) != null){
			return services_record_paths.get(name).trim();
		}
		return null;
	}

	public String getServicesSchemaName(String sectionName) {
		String result = null;

		String servicesRecordPath = getServicesRecordPath(sectionName);
		if (servicesRecordPath != null) {
			result = servicesRecordPath.split(":", 2)[0];
		}

		return result;
	}

	public String getAuthoritySchemaName() {
		return getServicesSingleInstancePath().split(":", 2)[0];
	}

	public String getServicesSchemaNameSpaceURI(String sectionName) {
		String path = getServicesRecordPath(sectionName);
		String[] pathParts = path.split(":", 2);
		String schemaLocationCore = pathParts[1].split(",", 2)[0];

		return schemaLocationCore;
	}

	public String getXMLSchemaLocation(String sectionName) {
		String result;

		String schemaName = this.getServicesSchemaName(sectionName);
		String schemaNamespaceUri = this.getServicesSchemaNameSpaceURI(sectionName);
		String schemaXsdLocation = schemaNamespaceUri.replace(sectionName, schemaName);
		result = schemaNamespaceUri + " " + schemaXsdLocation + ".xsd";

		return result;
	}

	public String getServicesPartLabel(String sectionName) {
		String[] pathParts = getServicesRecordPath(sectionName).split(":", 2);
		String schemaPartLabel = pathParts[1].split(",", 2)[1];
		return schemaPartLabel;
	}

	public Boolean hasServicesRecordPath(String name) {
		if (services_record_paths.containsKey(name)) {
			return true;
		}
		return false;
	}

	public String getServicesInstancesPath(String name) {
		return services_instances_paths.get(name);
	}

	public Boolean hasServicesInstancesPath(String name) {
		if (services_instances_paths.containsKey(name)) {
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

	void setServicesInstancePath(String section, String path) {
		services_instances_paths.put(section, path);
	}

	void setServicesFilterParam(String param, Field field) {
		services_filter_param.put(param, field);
	}

	public FieldSet getMiniNumber() {
		return mini_number;
	}

	private void findMiniNumber() {
		if (mini_number == null) {
			// Try child expanders to see if the mini_number is there.
			// This handles case of mini_number for terms in the "preferred" termList sub-record
			for (FieldSet fs : selfRenderers) {
				Record subrecord = fs.getSelfRendererRecord();
				FieldSet miniNumberFS = subrecord.getMiniNumber();
				if (miniNumberFS != null) {
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
		if (mini_summary == null) {
			// Try child expanders to see if the mini_summary is there.
			// This handles case of mini_summary for terms in the "preferred" termList sub-record
			for (FieldSet fs : selfRenderers) {
				Record subrecord = fs.getSelfRendererRecord();
				FieldSet miniSummaryFS = subrecord.getMiniNumber();
				if (miniSummaryFS != null) {
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
		for (FieldSet fs : selfRenderers) {
			Record subrecord = fs.getSelfRendererRecord();
			if (!subrecord.summarylist.isEmpty())
				summarylist.putAll(subrecord.summarylist);
		}
	}

	/**
	 * For selfrenderer children, like the preferredTerm block in authorities,
	 * we need to gather up all the declared search fields into this parent, so
	 * that when we generate a UISpec or UISchema for search, we get those
	 * nested fields as well.
	 */
	private void mergeSearchLists() {
		for (FieldSet fs : selfRenderers) {
			Record subrecord = fs.getSelfRendererRecord();
			if (!subrecord.searchFieldFullList.isEmpty())
				searchFieldFullList.putAll(subrecord.searchFieldFullList);
		}
	}

	public FieldSet getDisplayNameField() {
		return display_name;
	}

	private void findDisplayNameField() {
		if (display_name == null) {
			// Try child expanders to see if the displayName is there.
			// This handles case of displayName for terms in the "preferred" termList sub-record
			for (FieldSet fs : selfRenderers) {
				Record subrecord = fs.getSelfRendererRecord();
				FieldSet displayNameFS = subrecord.getDisplayNameField();
				if (displayNameFS != null) {
					display_name = displayNameFS;
					return;
				}
			}
		}
	}

	private void buildAuthTypeTokenSet() {
		String authTypeTokens[] = getAuthorizationType().split("/");
		for (String token : authTypeTokens) {
			authTypeTokenSet.add(token);
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
		// This is too simplistic. We need to either match, 
		// or allow a set of tokens in a path to match, but
		// the contains model is broken. 
		String authType = getAuthorizationType();
		if (StringUtils.isEmpty(authType))
			return false;
		if (authType.equals(name))
			return true;
		// Okay, now we get fancy. Build a set of tokens, and match up
		String nameTokens[] = name.split("/");
		for (String nameToken : nameTokens) {
			// A missing token means we do not match
			if (!authTypeTokenSet.contains(nameToken))
				return false;
		}
		// If we get there, the auth type contained all the name tokens, so we return true.
		return true;
	}

	public void addUISection(String section, String id) {
		if (section.equals("")) {
			section = "base";
		}
		if (!uisection.containsKey(section)) {
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
		for (FieldSet fs : selfRenderers) {
			Record subrecord = fs.getSelfRendererRecord();
			for (String listName : subrecord.minidataset.keySet()) {
				Map<String, FieldSet> subRecordMiniList = subrecord.minidataset.get(listName);
				if (!subRecordMiniList.isEmpty()) {
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
		JSONObject RecordTable = new JSONObject();
		for (FieldSet fs : this.getAllFieldFullList("")) {
			if (allStrings == null) {
				allStrings = fs.getUtils().getAllString();
				allBooleans = fs.getUtils().getAllBoolean();
				allSets = fs.getUtils().getAllSets();

				for (String s : allStrings) {
					JSONObject data = new JSONObject();
					data.put("default", fs.getUtils().getDefaultString(s));
					RecordTable.put("String:" + s, data);
				}
				for (String s : allBooleans) {
					JSONObject data = new JSONObject();
					data.put("default", fs.getUtils().getDefaultBoolean(s));
					RecordTable.put("Boolean:" + s, data);
				}
				for (String s : allSets) {
					JSONObject data = new JSONObject();
					data.put("default", fs.getUtils().getDefaultSet(s));
					RecordTable.put("Set:" + s, data);
				}
			} else {

				for (String s : allStrings) {
					JSONObject data = RecordTable.getJSONObject("String:" + s);
					data.put(fs.getID(), fs.getUtils().getString(s));
					RecordTable.put("String:" + s, data);
				}
				for (String s : allBooleans) {
					JSONObject data = RecordTable.getJSONObject("Boolean:" + s);
					data.put(fs.getID(), fs.getUtils().getBoolean(s));
					RecordTable.put("Boolean:" + s, data);
				}
				for (String s : allSets) {
					JSONObject data = RecordTable.getJSONObject("Set:" + s);
					data.put(fs.getID(), fs.getUtils().getSet(s));
					RecordTable.put("Set:" + s, data);
				}
			}
		}

		out.put("allfields", RecordTable);
		out.put(this.getID(), record);
	}

	String dumpFields() throws JSONException {
		StringBuffer out = new StringBuffer();
		String[] allStrings = null;
		String[] allBooleans = null;
		String[] allSets = null;
		Map<String, StringBuffer> rout = new HashMap<String, StringBuffer>();

		allStrings = this.getAllFieldFullList("")[0].getUtils().getAllString();
		allBooleans = this.getAllFieldFullList("")[0].getUtils().getAllBoolean();
		allSets = this.getAllFieldFullList("")[0].getUtils().getAllSets();

		out.append("field" + ",");
		rout.put("default", new StringBuffer());
		for (FieldSet fs : this.getAllFieldFullList("")) {
			rout.put(fs.getID(), new StringBuffer());
		}

		for (String s : allStrings) {
			out.append(s + ",");
			rout.get("default").append(this.getAllFieldFullList("")[0].getUtils().getDefaultString(s) + ",");
			for (FieldSet fs : this.getAllFieldFullList("")) {
				rout.get(fs.getID()).append(fs.getUtils().getString(s) + ",");
			}
		}
		for (String s : allBooleans) {
			out.append(s + ",");
			rout.get("default").append(this.getAllFieldFullList("")[0].getUtils().getDefaultBoolean(s) + ",");
			for (FieldSet fs : this.getAllFieldFullList("")) {
				rout.get(fs.getID()).append(fs.getUtils().getBoolean(s) + ",");
			}
		}
		for (String s : allSets) {
			out.append(s + ",");
			rout.get("default").append(
					"\"" + this.getAllFieldFullList("")[0].getUtils().allDefaultSets.get(s) + "\"" + ",");
			for (FieldSet fs : this.getAllFieldFullList("")) {
				rout.get(fs.getID()).append("\"" + fs.getUtils().allSets.get(s) + "\"" + ",");
			}
		}

		for (Map.Entry<String, StringBuffer> entry : rout.entrySet()) {
			String key = entry.getKey();
			StringBuffer value = entry.getValue();
			out.append("\n");
			out.append(key + ",");
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
		buildAuthTypeTokenSet();
	}

	@Override
	public boolean isExpander() {
		return false;
	}
}
