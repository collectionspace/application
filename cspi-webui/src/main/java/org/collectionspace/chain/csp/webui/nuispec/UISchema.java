/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.nuispec;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;

import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Schemas;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.UISpecRunContext;
import org.collectionspace.chain.csp.webui.authorities.VocabulariesRead;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnauthorizedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UISchema extends SchemaStructure implements WebMethod {
	private static final Logger log = LoggerFactory.getLogger(UISchema.class);
	protected CacheTermList ctl;
	protected Record record;
	protected Storage storage;
	private Schemas schema;
	private Map<String, String> workflowStateCache = new HashMap<String, String>();

	public UISchema(Record r, String sview) {
		super(r, sview);
		this.record = r;
	}
	public UISchema(Spec spec, String sview, String stype) {
		super(spec, sview, stype);
	}
	public UISchema(Spec spec, Schemas s) {
		super(spec,"screen");
		this.schema = s;
		this.spec = spec;
		this.record = null;
	}
	
	
	@Override
	public void configure(WebUI ui, Spec spec) {
		// TODO Auto-generated method stub
	}

	@Override
	public void run(Object in, String[] tail) throws UIException, UnauthorizedException {
		Request q = (Request) in;
		JSONObject out;
		if (this.record != null) {
			if (this.spectype.equals("search")) {
				out = uisearchschema(q.getStorage(), this.record);
			} else {
				out = uirecordschema(q.getStorage(), this.record);
			}
		} else {
			UISession session = q.getUIRequest().getSession();
			out = uiotherschema(session, q.getStorage(), StringUtils.join(tail, "/"));
		}
		
		UIRequest uir = q.getUIRequest();
		uir.sendJSONResponse(out);
		int cacheMaxAgeSeconds = spec.getAdminData().getUiSpecSchemaCacheAge();
		if (cacheMaxAgeSeconds > 0) {
			uir.setCacheMaxAgeSeconds(cacheMaxAgeSeconds);
		}
	}

	@Override
	protected void actualValidatedField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String datatype = ((Field)fs).getDataType();
		if(datatype.equals("")){datatype="string";}
		JSONObject validator = new JSONObject();
		actualSchemaObject(datatype, null, null, null, validator);
		out.put(getSelector(fs,context),validator);
	}
	
	@Override
	protected void actualWorkflowStateField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String datatype = "string";
		JSONObject workflowStateField = new JSONObject();
		actualSchemaObject(datatype, null, null, null, workflowStateField);
		out.put(getSelector(fs,context),workflowStateField);
	}
	
	@Override
	protected void actualExternalURLField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String datatype = "string";
		JSONObject urlfield = new JSONObject();
		actualSchemaObject(datatype, null, null, null, urlfield);
		out.put(getSelector(fs,context),urlfield);
	}
        
        @Override
		protected void actualDeURNedField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String datatype = "string";
		JSONObject deurnedfield = new JSONObject();
		actualSchemaObject(datatype, null, null, null, deurnedfield);
		out.put(getSelector(fs,context),deurnedfield);
	}
	
	@Override
	protected void actualSubRecordField(JSONObject out, FieldSet fs, UISpecRunContext context, Record subr, Boolean repeated, JSONObject parent) throws JSONException{

		if(fs instanceof Group){
			Group gp = (Group)fs;
			if(!gp.isGrouped()){
				makeASubRecord(subr, out,  repeated,  context, parent);
				return;
			}
		}
		
		
		JSONObject insidebit = new JSONObject();
		makeASubRecord(subr, insidebit,  repeated,  context, parent);

		JSONObject chooser = new JSONObject();
		actualSchemaObject("object", null, insidebit, null, chooser);
		out.put(getSelector(fs,context),chooser);
	}
	@Override
	protected void actualChooserField(JSONObject out, FieldSet fs, UISpecRunContext context, Boolean useContainer) throws JSONException{
		JSONObject chooser = new JSONObject();
		actualSchemaObject("string", null, null, null, chooser);
		out.put(getSelector(fs,context),chooser);
	}

	@Override
	protected void actualDateField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		String type = "date";
		JSONObject datefield = new JSONObject();
		actualSchemaObject(type, null, null, null, datefield);
		out.put(getSelector(fs,context),datefield);
	}

	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param context
	 * @param f
	 * @throws JSONException
	 */
	@Override
	protected void actualAuthorities(JSONObject out, FieldSet fs, UISpecRunContext context)
			throws JSONException {
		if("enum".equals(fs.getUIType())){
			out.put(getSelector(fs,context),actualFieldEntry(fs,context));
		}
		else{
			out.put(getSelector(fs,context),actualAutocomplete(fs,context));
		}
	}
	
	/**
	 * 
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	protected JSONObject actualAutocomplete(FieldSet fs,UISpecRunContext context) throws JSONException {
		JSONObject out = new JSONObject();
		actualSchemaObject("string", null, null, null, out);
		return out;
	}

	/**
	 * Do the same in UISchema with refactored and not refactored fields
	 * @param out
	 * @param affix
	 * @param f
	 * @throws JSONException
	 */
	@Override
	protected void actualFieldNotRefactored(JSONObject out,FieldSet fs, UISpecRunContext context) throws JSONException{
		actualFieldRefactored(out, fs, context);
	}
	@Override
	protected void 	actualGroupEntry(FieldSet fs, JSONObject out, UISpecRunContext context, JSONObject contents) throws JSONException{
		if(contents.has("type")){
			out.put("type", contents.get("type"));// array|object|string
		}
		if (contents.has("default")) {
			out.put("default", contents.get("default"));
		}
		if (contents.has("properties")) {
			out.put("properties", contents.get("properties"));
		}
		if (contents.has("items")) {
			out.put("items", contents.get("items"));
		}
	}
	
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param f
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	@Override
	protected Object actualOptionField(Field f,UISpecRunContext context) throws JSONException {
		String type = "string";
		String defaultval = f.getOptionDefault();
		if( (this.spectype.equals("search") && !f.getSearchType().equals(""))){
			defaultval = "";
		}
		JSONObject out = new JSONObject();
		actualSchemaObject(type, defaultval, null, null, out);
		return out;
	}
	

	/**
	 * specific UISchema realization of this item
	 */
	@Override
	protected void actualSelfRendererField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		Object temp = actualFieldEntry(fs,context);
		if(temp instanceof JSONObject){
			JSONObject tempo = (JSONObject)temp;

			Iterator<?> rit=tempo.keys();
			while(rit.hasNext()) {
				String key=(String)rit.next();
				out.put(key, tempo.get(key));
			}
			
		}
		else{
			out.put(getSelector(fs,context),actualFieldEntry(fs,context));
		}
	}
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param context
	 * @param f
	 * @throws JSONException
	 */
	protected void actualFieldExpanderEntry(JSONObject out, UISpecRunContext context,
			Field f) throws JSONException {
		out.put(getSelector(f,context), actualOptionField(f,context));
	}
	
	/**
	 * Overwrite with output you need for this thing you are doing
	 * @param out
	 * @param fs
	 * @param context
	 * @throws JSONException 
	 */
	@Override
	protected void actualField(JSONObject out, FieldSet fs, UISpecRunContext context) throws JSONException{
		out.put(getSelector(fs,context),actualFieldEntry(fs,context));
	}

	@Override
	protected Object displayAsplain(Field f,UISpecRunContext context) throws JSONException {
		JSONObject output = new JSONObject();
		actualSchemaObject("string", null, null, null, output);
		return output;
	}

	@Override
	protected Object displayAsplainlist(Field f) throws JSONException {
		JSONObject output = new JSONObject();
		actualSchemaObject("object", new JSONObject(), null, null, output);
		return output;
	}
	
	@Override
	protected void makeAStructureDate(FieldSet fs, JSONObject out,
			JSONObject subexpander, JSONObject options, Record subitems,
			UISpecRunContext sub, UISpecRunContext mainContext) throws JSONException {
		// We map structured-date range searches onto a range search on the scalar date fields. 
		// We continue to refer to the structured date field so that the search builder can
		// do the right thing, but here in the search schema, we act as though the structured
		// date is really a scalar date, so the UI code will build the right UI
		// Note that at this point, the fs is actually a synthetic copy of the original
		// with the id changed to append "Start" or "End"
		if( (this.spectype.equals("search") && fs.getSearchType().equals("range"))){
			actualDateField(out, fs, mainContext);
		} else {
			makeAOtherGroup(fs,out,subexpander, options, subitems, sub);
		}
	}	
	
	@Override
	protected void makeAOtherGroup(FieldSet fs, JSONObject out,
			JSONObject subexpander, JSONObject options, Record subitems,
			UISpecRunContext sub) throws JSONException {
		JSONObject protoTree = new JSONObject();
		for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {
			whatIsThisFieldSet(protoTree, fs2, sub);
		}
		JSONObject output = new JSONObject();
		actualSchemaObject("boolean", true, null, null, output);
		protoTree.put("_primary",output);
	
		actualSchemaObject("object", null, protoTree, null, out);
	}
	
	/**
	 * return the affix for this selector
	 * @param fs
	 * @return
	 */
	protected String getSelectorAffix(FieldSet fs){
		return fs.getSelectorAffix();
	}
	/**
	 * UISchema specific idea of a repeatable item
	 * @param out
	 * @param r
	 * @param context
	 * @throws JSONException
	 */
	private void UISchemaRepeatItem(JSONObject out, Repeat r, UISpecRunContext context)
			throws JSONException {
		JSONObject items = new JSONObject();
		JSONObject structuredate = new JSONObject();

		String selector = getSelector(r,context);
		JSONObject preProtoTree = new JSONObject();

		if(isARepeatingSubRecord(r)){
			makeASubRecordEntry(preProtoTree, r, context, out);
			preProtoTree = preProtoTree.getJSONObject(selector).getJSONObject("properties");
		}
		else{
			Integer numChild = r.getChildren("").length;
			for(FieldSet child :r.getChildren("")) {
				whatIsThisFieldSet(preProtoTree, child, context);
				if(isAStructureDate(child)){
					Boolean truerepeat = isATrueRepeat(r);
					//should we use structured dates in line rather than nested
					
					if(!truerepeat){
						structuredate = preProtoTree.getJSONObject(getSelector(child,context)).getJSONObject("properties");
						preProtoTree.remove(getSelector(child,context));
						Iterator<?> rit=structuredate.keys();
						while(rit.hasNext()) {
							String key=(String)rit.next();
							preProtoTree.put(key, structuredate.get(key));
						}
					}
					else if(numChild != 1 && preProtoTree.has("properties")){

						structuredate.put("properties",preProtoTree.getJSONObject("properties"));
						if(preProtoTree.has("type")){
							structuredate.put("type",preProtoTree.getString("type"));
							preProtoTree.remove("type");
						}
						else{
							structuredate.put("type","object");
						}
						preProtoTree.remove("properties");
						preProtoTree.put(getSelector(child,context), structuredate);
					}
				}
			}
		}
		//actualRepeatNonSiblingEntry(out, r, context, preProtoTree);//XXX ??? do I need this?
		if(preProtoTree.has("properties")){
			preProtoTree = preProtoTree.getJSONObject("properties");
		}
		if (r.hasPrimary()) {
			JSONObject output = new JSONObject();
			actualSchemaObject("boolean", true, null, null, output);
			preProtoTree.put("_primary", output);
		}
		actualSchemaObject("object", null, preProtoTree, null, items);
		JSONObject output = new JSONObject();
		actualSchemaObject("array", null, null, items, output);
		out.put(selector, output);
	}
	
	
	@Override
	protected void makeARepeatSiblingEntry(JSONObject out, Repeat r, UISpecRunContext context) throws JSONException {
		UISchemaRepeatItem(out, r, context);
	}
	@Override
	protected void makeARepeatNonSiblingEntry(JSONObject out, Repeat r, UISpecRunContext context) throws JSONException{
		makeARepeatSiblingEntry(out, r, context);
	}
	@Override
	protected void makeASelfRenderer(FieldSet fs, UISpecRunContext context,
			JSONObject out, JSONObject subexpander, JSONObject options,
			Record subitems, UISpecRunContext sub) throws JSONException {
		for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {
			whatIsThisFieldSet(out, fs2, context);
		}
	}
	
	@Override
	protected Object actualENUMField(Field f,UISpecRunContext context) throws JSONException {
		String type = "string";
		String defaultval = f.getEnumDefault();
		if( (this.spectype.equals("search") && !f.getSearchType().equals(""))){
			defaultval = "";
		}
		JSONObject output = new JSONObject();
		actualSchemaObject(type, defaultval, null, null, output);
		return output;
	}
	
	@Override
	protected Object actualBooleanField(Field f,UISpecRunContext context) throws JSONException {
		String type = "boolean";
		String defaultval = f.getDefault();
		Boolean defval = false;
		if("1".equals(defaultval) || "yes".equals(defaultval.toLowerCase()) || "true".equals(defaultval.toLowerCase())){
			defval = true;
		}
		if( (this.spectype.equals("search") && !f.getSearchType().equals(""))){
			defval = null;
		}
		JSONObject output = new JSONObject();
		actualSchemaObject(type, defval, null, null, output);
		return output;
	}
	/**
	 * Selector is just the ID in the UISchema
	 * @param fs
	 * @param context
	 * @return
	 */
	protected String getSelector(FieldSet fs, UISpecRunContext context) {
		return fs.getID();
	}
	
	
	/**
	 * Abstraction for creating the UISchema structure for items
	 * @param r
	 * @param context
	 * @return
	 * @throws JSONException
	 */
	private JSONObject actualSchemaFields(Record r,UISpecRunContext context) throws JSONException {
		JSONObject out = generateDataEntrySection(context, r, this.spectype);
		JSONObject output = new JSONObject();
		actualSchemaObject("object", null, out, null, output);
		return output;
	}
	/**
	 * Generate the UISpec Relations bit
	 * @return
	 * @throws JSONException
	 */
	private JSONObject actualSchemaRelations() throws JSONException {
		JSONObject output = new JSONObject();
		actualSchemaObject("object", new JSONObject(), null, null, output);
		return output;
	}
	/**
	 * Generate the UISchema Termsused section
	 * @param affix
	 * @return
	 * @throws JSONException
	 */
	private JSONObject actualSchemaTermsUsed(UISpecRunContext affix) throws JSONException {
		JSONObject output = new JSONObject();
		actualSchemaObject("array", new JSONArray(), null, null, output);
		return output;
	}
	
	/**
	 * Nuts and bolts of the UISchema obj structure
	 * @param type
	 * @param defaultobj
	 * @param properties
	 * @param items
	 * @return 
	 * @return
	 * @throws JSONException
	 */
	private void actualSchemaObject(String type, Object defaultobj,
			JSONObject properties, JSONObject items, JSONObject out) throws JSONException {
		out.put("type", type);// array|object|string
		if (defaultobj != null) {
			out.put("default", defaultobj);
		}
		if (properties != null) {
			out.put("properties", properties);
		}
		if (items != null) {
			out.put("items", items);
		}
	}
	/**
	 * Create all the other weird schemas that the UI wants
	 * @param storage
	 * @param params
	 * @return
	 * @throws UIException
	 * @throws UnauthorizedException 
	 */
	private JSONObject uiotherschema(UISession session, Storage storage, String params) throws UIException, UnauthorizedException {
		JSONObject out = new JSONObject();
		String sectionname = "";
		String sectionid = params.toLowerCase();
		if(schema !=null){
			sectionid = schema.getID();
			sectionname = schema.getWebURL();
		}
		try {
			if (sectionid.toLowerCase().equals("recordlist")) {
				JSONObject schema = new JSONObject();
				JSONArray recrds = new JSONArray();
				for(Record rc : this.spec.getAllRecords()){
					if(rc.isShowType("authority")){
						if(rc.isInRecordList()){
							for(Instance ins : rc.getAllInstances()){
								recrds.put(ins.getWebURL());
							}
						}
					}
					else if(rc.isShowType("record")||rc.isShowType("procedure")){
						if(rc.isInRecordList()){
							recrds.put(rc.getWebURL());
						}
					}
				}
				schema.put("type", "array");
				schema.put("default", recrds);
				out.put(sectionname, schema);
			}
			else if (sectionid.toLowerCase().equals("namespaces")) {
				assertLoginStatus(session);
				JSONObject namespaces = new JSONObject();
				JSONObject namespacesProps = new JSONObject();

				for (Record rc : this.spec.getAllRecords()) {
					if (rc.isInRecordList()) {
						if (rc.isShowType("authority")) {
							JSONObject authInfoProps = new JSONObject();
							int cardinal = 0;
							for(Instance ins : rc.getAllInstances()){
								JSONObject instanceInfo = new JSONObject();
								JSONObject instanceProps = new JSONObject();
								JSONObject nptAllowed = new JSONObject();
								nptAllowed.put("type", "boolean");
								nptAllowed.put("default", ins.getNPTAllowed());
								instanceProps.put("nptAllowed", nptAllowed);
								// Preserve the order of the namespaces
								JSONObject orderProp = new JSONObject();
								orderProp.put("type", "integer");
								orderProp.put("default", cardinal);
								instanceProps.put("order", orderProp);
								JSONObject workflowState = new JSONObject();
								workflowState.put("type", "string");
								workflowState.put("default", getWorkflowState(storage, ins));
								instanceProps.put("workflowState", workflowState);
								instanceInfo.put("type", "object");
								instanceInfo.put("properties", instanceProps);
								authInfoProps.put(ins.getWebURL(), instanceInfo);
								cardinal++;
							}
							JSONObject authorityInfo = new JSONObject();
							authorityInfo.put("type", "object");
							authorityInfo.put("properties", authInfoProps);
							namespacesProps.put(rc.getWebURL(), authorityInfo);
						}
					}
				}
				namespaces.put("type", "object");
				namespaces.put("properties", namespacesProps);
				out.put("namespaces", namespaces);
				/**
				 *
{
    "namespaces": {
        "type": "object",
        "properties": {
            "person": {
                "type": "object"
                "properties": {
                    "person" : {
                        "type": "object"
                        "properties": {
                            "nptAllowed": {
                                "type": "boolean",
                                "default": true
                            }
                        }
                    },
                    "persontest" : {
                        "type": "object"
                        "properties": {
                            "nptAllowed": {
                                "type": "boolean",
                                "default": true
                            }
                        }
                    }
                }
            },
            "organization": {
                "type": "object"
                "properties": {
                    "organization" : {
                        "type": "object"
                        "properties": {
                            "nptAllowed": {
                                "type": "boolean",
                                "default": true
                            }
                        }
                    },
                    "organizationtest" : {
                        "type": "object"
                        "properties": {
                            "nptAllowed": {
                                "type": "boolean",
                                "default": true
                            }
                        }
                    }
                }
            },
            ...
        }
    }
}
				 */
			}
			else if(sectionid.toLowerCase().equals("recordtypes")){

				
				JSONObject procedures_schema = new JSONObject();
				JSONObject authorities_schema = new JSONObject();
				JSONObject admin_schema = new JSONObject();
				JSONObject cataloging_schema = new JSONObject();
				JSONObject searchAll_schema = new JSONObject();

				JSONArray procedures_records = new JSONArray();
				JSONArray authorities_records = new JSONArray();
				JSONArray admin_records = new JSONArray();
				JSONArray cataloging_records = new JSONArray();
				JSONArray searchAll_records = new JSONArray();
				/**
				 * { "procedures": { "type": "array", "default": ["loanout",
				 * "movement", ...] }, "vocabularies": { "type": "array",
				 * "default": ["person", "organization", ...] }, "cataloging": {
				 * "type": "array", "default": ["cataloging"] } }
				 */
				for (Record rc : this.spec.getAllRecords()) {
					if (rc.isInRecordList()) {
						if (rc.isShowType("procedure")) {
							procedures_records.put(rc.getWebURL());
						} else if (rc.isShowType("authority")) {
							for(Instance ins : rc.getAllInstances()){
								authorities_records.put(ins.getWebURL());
							}
						} else if (rc.isShowType("record")) {
							// FIXME Assumes that "records" are either 
							// procedures, authorities, or cataloging.
							// Should instead have a type "cataloging"
							cataloging_records.put(rc.getWebURL());
						} else if (rc.isShowType("searchall")) {
								searchAll_records.put(rc.getWebURL());
						} else if(rc.isShowType("authorizationdata") || rc.isShowType("userdata")){
							admin_records.put(rc.getWebURL());
						}
					}
				}

				procedures_schema.put("type", "array");
				procedures_schema.put("default", procedures_records);
				authorities_schema.put("type", "array");
				authorities_schema.put("default", authorities_records);
				admin_schema.put("type", "array");
				admin_schema.put("default", admin_records);
				cataloging_schema.put("type", "array");
				cataloging_schema.put("default", cataloging_records);
				searchAll_schema.put("type", "array");
				searchAll_schema.put("default", searchAll_records);

				JSONObject record_types = new JSONObject();
				JSONObject types_list = new JSONObject();

				types_list.put("procedures", procedures_schema);
				types_list.put("vocabularies", authorities_schema);
				types_list.put("cataloging", cataloging_schema);
				types_list.put("all", searchAll_schema);
				types_list.put("administration", admin_schema);
				record_types.put("type","object");
				record_types.put("properties",types_list);

				out.put("recordtypes", record_types);
				
			}
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException", e);
		}
		return out;
	}
	
	private void assertLoginStatus(UISession uiSession) throws UnauthorizedException {		
		if (uiSession == null || uiSession.getValue(UISession.USERID) == null) {
			throw new UnauthorizedException("User must be authenticated to access this resource.", HttpStatus.SC_UNAUTHORIZED, "/namespaces");
		}		
	}
	
	private String getWorkflowState(Storage storage, Instance authorityInstance) throws UIException {
		String cacheKey = authorityInstance.getWebURL();
		
		// Cache workflow states by instance id. The app layer already expects instance ids to be
		// unique across authorities in other places.
		
		if (workflowStateCache.containsKey(cacheKey)) {
			return workflowStateCache.get(cacheKey);
		}
		
		VocabulariesRead vocabulariesRead = new VocabulariesRead(authorityInstance, VocabulariesRead.GET_BASIC_INFO);
		JSONObject instanceData = vocabulariesRead.getInstance(storage);
		
		String workflowState = null;
		
		try {
			if (!instanceData.has("csid") || (instanceData.has("isError") && instanceData.getBoolean("isError"))) {
				return null;
			}
		}
		catch(JSONException e) {
		}

		try {
			workflowState = instanceData.getJSONObject("fields").getString("workflow");
		}
		catch(JSONException e) {
		}

		workflowStateCache.put(cacheKey, workflowState);
		
		return workflowState;
	}
	
	/**
	 * Create the search uischemas
	 * @param storage
	 * @param record
	 * @return
	 * @throws UIException
	 */
	private JSONObject uisearchschema(Storage storage, Record record) throws UIException {
		UISpecRunContext context = new UISpecRunContext();
		this.storage = storage;
		this.record = record;
		this.tenantname = this.record.getSpec().getAdminData().getTenantName();

		try {
			JSONObject out = new JSONObject();

			JSONObject fields = actualSchemaFields(record, context);
			JSONObject prop = fields.getJSONObject("properties");
			fields.put("properties", prop);
			out.put(record.getWebURL(), fields);
			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException", e);
		}
	}
	
	/**
	 * Create the generic UI record schemas
	 * @param storage
	 * @param record
	 * @return
	 * @throws UIException
	 */
	private JSONObject uirecordschema(Storage storage,Record record) throws UIException {
		UISpecRunContext context = new UISpecRunContext();
		this.storage = storage;
		this.record = record;
		this.tenantname = this.record.getSpec().getAdminData().getTenantName();
		try {
			JSONObject out = new JSONObject();
			JSONObject details = new JSONObject();
			JSONObject properties = new JSONObject();

			if (record.hasTermsUsed()) {
				properties.put("termsUsed", actualSchemaTermsUsed(context));
			}
			if (record.hasRefObjUsed()) {
				properties.put("relations", actualSchemaRelations());
			}
			JSONObject fields = actualSchemaFields(record, context);
			JSONObject prop = fields.getJSONObject("properties");
			
			if(record.hasHierarchyUsed("screen")){
				JSONObject temp = actualSchemaFields(record.getSpec().getRecord("hierarchy"),context);
				JSONObject prop2 = temp.getJSONObject("properties");

				Iterator<?> rit=prop2.keys();
				while(rit.hasNext()) {
					String key=(String)rit.next();
					Object value = prop2.get(key);
					prop.put(key,value);
				}
			}
			fields.put("properties", prop);
			properties.put("fields", fields);

			JSONObject output = new JSONObject();
			actualSchemaObject("string", null, null, null, output);
			properties.put("csid", output);
			actualSchemaObject("object", null, properties, null, details);

			out.put(record.getWebURL(), details);
			/*
			 * { "cataloging": { "type": "object", "properties": {
			 */

			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException", e);
		}
	}
}