/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.nuispec;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Repeat;
import org.collectionspace.chain.csp.schema.Spec;
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


	public UISchema(Record record, String structureview) {
		super(record, structureview);
	}
	public UISchema(Spec spec) {
		super();
		this.spec = spec;
		this.record = null;
	}

	protected JSONObject generateTermsUsed() throws JSONException {
		return generateSchemaObject("array", new JSONArray(), null, null);
	}

	protected JSONObject generateRelations() throws JSONException {
		return generateSchemaObject("object", new JSONObject(), null, null);
	}

	protected JSONObject generateFields() throws JSONException {
		return generateSchemaObject("object", null,
				generateDataEntrySection(""), null);
	}

	protected String getSelector(FieldSet fs) {
		return fs.getID();
	}

	protected Object generateENUMField(Field f) throws JSONException {
		String type = "string";
		String defaultval = f.getEnumDefault();
		return generateSchemaObject(type, defaultval, null, null);
	}

	protected Object generateOptionField(Field f) throws JSONException {
		String type = "array";
		String defaultval = f.getOptionDefault();
		return generateSchemaObject(type, defaultval, null, null);
	}

	protected void generateFieldDataEntry_notrefactored(JSONObject out,
			String affix, Field f) throws JSONException {
		generateFieldDataEntry_refactored(out, affix, f);
	}

	protected void generateExpanderDataEntry(JSONObject out, String affix,
			Field f) throws JSONException {
		out.put(getSelector(f), generateOptionField(f));
	}

	protected JSONObject generateChooser(Field f) throws JSONException {
		return generateSchemaObject("string", null, null, null);
	}

	protected JSONObject generateAutocomplete(Field f) throws JSONException {
		return generateSchemaObject("string", null, null, null);
	}

	private void repeatItem(JSONObject out, Repeat r, String affix)
			throws JSONException {
		JSONObject items = new JSONObject();

		String selector = getSelector(r);
		JSONObject protoTree = new JSONObject();
		for (FieldSet child : r.getChildren()) {
			generateDataEntry(protoTree, child, affix);
		}
		if (r.hasPrimary()) {
			protoTree.put("_primary", generateSchemaObject("boolean", null,
					null, null));
		}
		items = generateSchemaObject("object", null, protoTree, null);

		out.put(selector, generateSchemaObject("array", null, null, items));
	}

	protected void repeatNonSibling(JSONObject out, FieldSet fs, String affix,
			Repeat r) throws JSONException {
		repeatItem(out, r, affix);
	}

	protected void repeatSibling(JSONObject out, String affix, Repeat r,
			JSONObject row, JSONArray children) throws JSONException {
		repeatItem(out, r, affix);
	}

	protected JSONObject generateDate(Field f) throws JSONException {
		String type = "date";
		return generateSchemaObject(type, null, null, null);
	}

	protected Object generateDataEntryField(Field f) throws JSONException {
		if ("plain".equals(f.getUIType())) {
			return generateSchemaObject("string", null, null, null);
		} else if ("list".equals(f.getUIType())) {
			return generateSchemaObject("object", new JSONObject(), null, null);
		} else if ("linktext".equals(f.getUIType())) {
			return generateSchemaObject("object", new JSONObject(), null, null);
		} else if ("dropdown".equals(f.getUIType())) {
			return generateOptionField(f);
		} else if ("enum".equals(f.getUIType())) {
			return generateENUMField(f);
		}
		return plain(f);
	}

	protected JSONObject generateSchemaObject(String type, Object defaultobj,
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

		try {
			if (params.equals("recordlist")) {
				JSONObject schema = new JSONObject();
				JSONArray recrds = new JSONArray();
				for(Record rc : this.spec.getAllRecords()){
					if(rc.isType("record")||rc.isType("authority")||rc.isType("procedure")){
						if(!rc.getID().equals("vocab")){ // vocab is weird - ignore for the moment
							recrds.put(rc.getWebURL());
						}
					}
				}
				schema.put("type", "array");
				schema.put("default", recrds);
				out.put(params, schema);
			}
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException", e);
		}
		return out;
	}
	
	private JSONObject uirecordschema(Storage storage,Record record) throws UIException {
		this.storage = storage;
		this.record = record;
		try {
			JSONObject out = new JSONObject();
			JSONObject details = new JSONObject();
			JSONObject properties = new JSONObject();

			if (record.hasTermsUsed()) {
				properties.put("termsUsed", generateTermsUsed());
			}
			if (record.hasRefObjUsed()) {
				properties.put("relations", generateRelations());

			}
			properties.put("fields", generateFields());
			properties.put("csid", generateSchemaObject("string", null, null,
					null));
			details = generateSchemaObject("object", null, properties, null);

			out.put(record.getWebURL(), details);
			/*
			 * { "objects": { "type": "object", "properties": {
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
