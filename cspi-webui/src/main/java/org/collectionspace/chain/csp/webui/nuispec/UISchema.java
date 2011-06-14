/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.nuispec;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Schemas;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.UISpecRunContext;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UISchema extends UISpec {
	private static final Logger log = LoggerFactory.getLogger(UISchema.class);
	protected JSONObject controlledCache;
	private Spec spec;
	private Schemas schema;


	public UISchema(Record record, String structureview) {
		super(record, structureview);
	}
	public UISchema(Spec spec) {
		super();
		this.spec = spec;
		this.record = null;
	}
	public UISchema(Spec spec, Schemas s) {
		super();
		this.schema = s;
		this.spec = spec;
		this.record = null;
	}

	protected JSONObject generateTermsUsed(UISpecRunContext affix) throws JSONException {
		return generateSchemaObject("array", new JSONArray(), null, null);
	}
	protected void generateUploaderEntry(JSONObject out, FieldSet f, UISpecRunContext affix) throws JSONException{
	}
	protected void generateHierarchyEntry(JSONObject out, FieldSet f, UISpecRunContext affix) throws JSONException{
	}
	

	private JSONObject generateRelations() throws JSONException {
		return generateSchemaObject("object", new JSONObject(), null, null);
	}
	
	private JSONObject generateFields(Record r,UISpecRunContext context) throws JSONException {
		return generateSchemaObject("object", null,
				generateDataEntrySection(context,r), null);
	}

	private JSONObject generateFields(UISpecRunContext context) throws JSONException {
		return generateSchemaObject("object", null,
				generateDataEntrySection(context), null);
	}

	protected String getSelector(FieldSet fs, UISpecRunContext context) {
		return fs.getID();
	}

	protected Object generateENUMField(Field f,UISpecRunContext context) throws JSONException {
		String type = "string";
		String defaultval = f.getEnumDefault();
		return generateSchemaObject(type, defaultval, null, null);
	}
	
	protected Object generateGroupField(FieldSet f,UISpecRunContext context)
			throws JSONException {
		//JSONObject out = new JSONObject();
		JSONObject items = new JSONObject();

		String parts[] = f.getUIType().split("/");
		Record subitems = f.getRecord().getSpec().getRecordByServicesUrl(parts[1]);

		String selector = getSelector(f,context);
		JSONObject protoTree = new JSONObject();
		for(FieldSet fs2 : subitems.getAllFields("")) {
			generateDataEntry(protoTree, fs2,context);
		}
		protoTree.put("_primary", generateSchemaObject("boolean", null,
					null, null));
		
		items = generateSchemaObject("object", null, protoTree, null);

		//out.put(selector, items);
		return items;
	}
	

	protected Object generateOptionField(Field f,UISpecRunContext context) throws JSONException {
		String type = "string";
		String defaultval = f.getOptionDefault();
		return generateSchemaObject(type, defaultval, null, null);
	}

	protected void generateFieldDataEntry_notrefactored(JSONObject out,
			UISpecRunContext affix, Field f) throws JSONException {
		generateFieldDataEntry_refactored(out, affix, f);
	}

	protected void generateExpanderDataEntry(JSONObject out, UISpecRunContext context,
			Field f) throws JSONException {
		out.put(getSelector(f,context), generateOptionField(f,context));
	}

	protected JSONObject generateChooser(Field f,UISpecRunContext context) throws JSONException {
		return generateSchemaObject("string", null, null, null);
	}

	protected JSONObject generateAutocomplete(Field f,UISpecRunContext context) throws JSONException {
		return generateSchemaObject("string", null, null, null);
	}

	private void repeatItem(JSONObject out, Repeat r, UISpecRunContext context)
			throws JSONException {
		JSONObject items = new JSONObject();

		String selector = getSelector(r,context);
		JSONObject protoTree = new JSONObject();
		for (FieldSet child : r.getChildren("")) {
			generateDataEntry(protoTree, child, context);
		}
		if (r.hasPrimary()) {
			protoTree.put("_primary", generateSchemaObject("boolean", null,
					null, null));
		}
		items = generateSchemaObject("object", null, protoTree, null);

		out.put(selector, generateSchemaObject("array", null, null, items));
	}

	protected void repeatNonSibling(JSONObject out, FieldSet fs, UISpecRunContext context,
			Repeat r) throws JSONException {
		repeatItem(out, r, context);
	}

	protected void repeatSibling(JSONObject out, UISpecRunContext context, Repeat r,
			JSONObject row, JSONArray children) throws JSONException {
		repeatItem(out, r, context);
	}

	protected JSONObject generateDate(Field f,UISpecRunContext context) throws JSONException {
		String type = "date";
		return generateSchemaObject(type, null, null, null);
	}

	protected Object generateDataEntryField(Field f,UISpecRunContext context) throws JSONException {
		if ("plain".equals(f.getUIType())) {
			return generateSchemaObject("string", null, null, null);
		} else if ("list".equals(f.getUIType())) {
			return generateSchemaObject("object", new JSONObject(), null, null);
		} else if ("linktext".equals(f.getUIType())) {
			return generateSchemaObject("object", new JSONObject(), null, null);
		} else if ("dropdown".equals(f.getUIType())) {
			return generateOptionField(f,context);
		} else if ("enum".equals(f.getUIType())) {
			return generateENUMField(f,context);
		} else if(f.getUIType().startsWith("groupfield")) {
			return generateGroupField(f,context);
		}
		//ignore ui-type uploader
		return generateSchemaObject("string", null, null, null);
	}

	private JSONObject generateSchemaObject(String type, Object defaultobj,
			JSONObject properties, JSONObject items) throws JSONException {
		JSONObject out = new JSONObject();
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
		return out;
	}

	private JSONObject uiotherschema(Storage storage, String params) throws UIException {
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
					if(rc.isType("record")||rc.isType("authority")||rc.isType("procedure")){
						if(rc.isInRecordList()){
							recrds.put(rc.getWebURL());
						}
					}
				}
				schema.put("type", "array");
				schema.put("default", recrds);
				out.put(sectionname, schema);
			}
			else if(sectionid.toLowerCase().equals("recordtypes")){

				
				JSONObject pschema = new JSONObject();
				JSONObject aschema = new JSONObject();
				JSONObject cschema = new JSONObject();

				JSONArray precrds = new JSONArray();
				JSONArray arecrds = new JSONArray();
				JSONArray crecrds = new JSONArray();
				/**
				 * { "procedures": { "type": "array", "default": ["loanout",
				 * "movement", ...] }, "vocabularies": { "type": "array",
				 * "default": ["person", "organization", ...] }, "cataloging": {
				 * "type": "array", "default": ["cataloging"] } }
				 */
				for (Record rc : this.spec.getAllRecords()) {
					if (rc.isInRecordList()) {
						if (rc.isType("procedure")) {
							precrds.put(rc.getWebURL());
						} else if (rc.isType("authority")) {
							arecrds.put(rc.getWebURL());
						} else if (rc.isType("record")) {
							crecrds.put(rc.getWebURL());
						}
					}
				}

				pschema.put("type", "array");
				pschema.put("default", precrds);
				aschema.put("type", "array");
				aschema.put("default", arecrds);
				cschema.put("type", "array");
				cschema.put("default", crecrds);

				JSONObject rtypes = new JSONObject();
				JSONObject ptypes = new JSONObject();

				ptypes.put("procedures", pschema);
				ptypes.put("vocabularies", aschema);
				ptypes.put("cataloging", cschema);
				rtypes.put("type","object");
				rtypes.put("properties",ptypes);

				out.put("recordtypes", rtypes);
				
			}
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException", e);
		}
		return out;
	}
	
	private JSONObject uirecordschema(Storage storage,Record record) throws UIException {
		UISpecRunContext context = new UISpecRunContext();
		this.storage = storage;
		this.record = record;
		this.tenantname = this.record.getSpec().getAdminData().getTenantName();
		if(this.tenantname.equals("")){
			this.tenantname = "html";
		}
		try {
			JSONObject out = new JSONObject();
			JSONObject details = new JSONObject();
			JSONObject properties = new JSONObject();

			if (record.hasTermsUsed()) {
				properties.put("termsUsed", generateTermsUsed(context));
			}
			if (record.hasRefObjUsed()) {
				properties.put("relations", generateRelations());
			}
			JSONObject fields = generateFields(context);
			JSONObject prop = fields.getJSONObject("properties");
			

			if(record.hasHierarchyUsed("screen")){
				JSONObject temp = generateFields(record.getSpec().getRecord("hierarchy"),context);
				JSONObject prop2 = temp.getJSONObject("properties");

				Iterator rit=prop2.keys();
				while(rit.hasNext()) {
					String key=(String)rit.next();
					Object value = prop2.get(key);
					prop.put(key,value);
				}
			}
			fields.put("properties", prop);
			properties.put("fields", fields);
			
			properties.put("csid", generateSchemaObject("string", null, null,
					null));
			details = generateSchemaObject("object", null, properties, null);

			out.put(record.getWebURL(), details);
			/*
			 * { "cataloging": { "type": "object", "properties": {
			 */

			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException", e);
		}
	}

	public void configure() throws ConfigException {
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q = (Request) in;
		JSONObject out;
		if(this.record != null){
			out = uirecordschema(q.getStorage(),this.record);
		}
		else{
			out = uiotherschema(q.getStorage(),StringUtils.join(tail,"/"));
		}
		q.getUIRequest().sendJSONResponse(out);
	}

	public void configure(WebUI ui, Spec spec) {
	}
}
