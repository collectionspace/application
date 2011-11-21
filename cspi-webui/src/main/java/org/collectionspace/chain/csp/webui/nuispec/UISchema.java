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
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Instance;
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
	public UISchema(Spec spec, CacheTermList ctl) {
		super(spec);
		this.spec = spec;
		this.record = null;
	}
	public UISchema(Spec spec, Schemas s) {
		super(spec);
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
	protected void generateTrueTree(JSONObject out, JSONObject trueTreeBits) throws JSONException{
	}
	protected JSONObject generateMessageKeys(UISpecRunContext affix, JSONObject temp, Record r) throws JSONException {
		return temp;
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

	protected Object generateBooleanField(Field f, UISpecRunContext context) throws JSONException {
		String type = "boolean";
		String defaultval = f.getDefault();
		Boolean defval = false;
		if("1".equals(defaultval) || "yes".equals(defaultval.toLowerCase()) || "true".equals(defaultval.toLowerCase())){
			defval = true;
		}
		if( (this.spectype.equals("search") && !f.getSearchType().equals(""))){
			defval = null;
		}
		return generateSchemaObject(type, defval, null, null);
	}
	
	protected Object generateENUMField(Field f,UISpecRunContext context) throws JSONException {
		String type = "string";
		String defaultval = f.getEnumDefault();
		if( (this.spectype.equals("search") && !f.getSearchType().equals(""))){
			defaultval = "";
		}
		return generateSchemaObject(type, defaultval, null, null);
	}
	
	protected Object generateGroupField(FieldSet f,UISpecRunContext context)
			throws JSONException {
		String parts[] = f.getUIType().split("/");

		JSONObject items = new JSONObject();

		Record subitems = f.getRecord().getSpec().getRecordByServicesUrl(parts[1]);

		String selector = getSelector(f,context);
		JSONObject protoTree = new JSONObject();
		for(FieldSet fs2 : subitems.getAllFieldTopLevel("")) {
			generateDataEntry(protoTree, fs2,context);
		}
		
		if(parts.length>=3 && parts[2].equals("selfrenderer")){
			return protoTree;
		}
		
		protoTree.put("_primary", generateSchemaObject("boolean", true,
					null, null));
		
		items = generateSchemaObject("object", null, protoTree, null);

		//out.put(selector, items);
		return items;
	}
	

	protected Object generateOptionField(Field f,UISpecRunContext context) throws JSONException {
		String type = "string";
		String defaultval = f.getOptionDefault();
		if( (this.spectype.equals("search") && !f.getSearchType().equals(""))){
			defaultval = "";
		}
		return generateSchemaObject(type, defaultval, null, null);
	}

	protected void generateFieldDataEntry_notrefactored(JSONObject out,
			UISpecRunContext affix, Field f) throws JSONException {
		generateFieldDataEntry_refactored(out, affix, f);
	}
	
	protected void generateFieldDataEntry_refactored(JSONObject out, UISpecRunContext context, Field f)
	throws JSONException {
		if(f.hasAutocompleteInstance()) {
			makeAuthorities(out, context, f);
		}
		else if("chooser".equals(f.getUIType()) && !this.spectype.equals("search")) {
			out.put(getSelector(f,context),generateChooser(f,context));
		}
		else if("date".equals(f.getUIType())) {
			out.put(getSelector(f,context),generateDate(f,context));
		}
		else if("validated".equals(f.getUIType())){
			out.put(getSelector(f,context),generateDataTypeValidator(f,context));
		}
		else if("sidebar".equals(f.getUIType())) {
			//Won't work now if uncommented
			//out.put(getSelector(f)+affix,generateSideBar(f));
		}
		else if(f.getUIType().contains("selfrenderer")){
			Object temp = generateDataEntryField(f,context);
			if(temp instanceof JSONObject){
				JSONObject tempo = (JSONObject)temp;

				Iterator rit=tempo.keys();
				while(rit.hasNext()) {
					String key=(String)rit.next();
					out.put(key, tempo.get(key));
				}
				
			}
			else{
				out.put(getSelector(f,context),generateDataEntryField(f,context));
			}
				
		}
		else{
			out.put(getSelector(f,context),generateDataEntryField(f,context));	
		}
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
		Boolean isASelfrendererStructuredDate = false;
		JSONObject structuredate = new JSONObject();

		String selector = getSelector(r,context);
		JSONObject protoTree = new JSONObject();
		if(r.usesRecord() ){
			if(!r.getUISpecInherit()){
				UISpecRunContext sub = context.createChild();
				if(!getSelectorAffix(r).equals("")){
					if(!context.equals("")){
						sub.setUIAffix(getSelectorAffix(r));
					}
					else{
						sub.setUIAffix(getSelectorAffix(r));
					}
				}
				String sp=r.getUISpecPrefix();
				if(sp!=null)
					sub.setUIPrefix(sp);
				generateSubRecord(protoTree, r,sub, null);
			}
			else{
				generateSubRecord(protoTree, r,context, null);
			}
		}
		else{
			for (FieldSet child : r.getChildren("")) {
				if(!this.spectype.equals("search") || (this.spectype.equals("search") && !child.getSearchType().equals(""))){
					generateDataEntry(protoTree, child, context);
				}
				if(child.getUIType().startsWith("groupfield") && child.getUIType().contains("structureddate")){
					structuredate = protoTree.getJSONObject(getSelector(child,context)).getJSONObject("properties");
					protoTree.remove(getSelector(child,context));

					Iterator rit=structuredate.keys();
					while(rit.hasNext()) {
						String key=(String)rit.next();
						protoTree.put(key, structuredate.get(key));
					}
				}
			}
		}
		if (r.hasPrimary()) {
			protoTree.put("_primary", generateSchemaObject("boolean", true,
					null, null));
		}
		items = generateSchemaObject("object", null, protoTree, null);

		out.put(selector, generateSchemaObject("array", null, null, items));
	}

	protected void repeatNonSibling(JSONObject out,  UISpecRunContext context,
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

	protected JSONObject generateDataTypeValidator(Field f, UISpecRunContext context) throws JSONException{
		String datatype = f.getDataType();
		if(datatype.equals("")){datatype="string";}
		return generateSchemaObject(datatype, null, null, null);
	}
	protected Object generateDataEntryField(Field f,UISpecRunContext context) throws JSONException {
		if ("plain".equals(f.getUIType())) {
			if("boolean".equals(f.getDataType())){
				return generateBooleanField(f,context);
			}
			else{
				return generateSchemaObject("string", null, null, null);
			}
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
		String datatype = f.getDataType();
		if(datatype.equals("")){	datatype="string";	}
		if(datatype.equals("boolean")){	return generateBooleanField(f,context);	}
		
		//ignore ui-type uploader
		return generateSchemaObject(datatype, null, null, null);
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
	
	protected void generateSubRecord(JSONObject out, FieldSet fs, UISpecRunContext context, JSONObject parent) throws JSONException {
		Record subrecord = fs.usesRecordId();
		Boolean repeated = false;
		if(fs.getParent() instanceof Repeat ||( fs instanceof Repeat && !(fs instanceof Group))){
			repeated = true;
		}
		if( parent == null){
			parent = out;
		}
		if(fs instanceof Group){
			Group gp = (Group)fs;
			if(gp.isGrouped()){
				JSONObject schemaprop = new JSONObject();
				generateSubRecord(subrecord, schemaprop,  repeated,  context, parent);

				JSONObject schemaadd = new JSONObject();
				schemaadd.put("type", "object");
				schemaadd.put("properties", schemaprop);
				out.put(gp.getID(), schemaadd);
				return;
			}
		}
		generateSubRecord(subrecord, out,  repeated,  context, parent);
		
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
			else if(sectionid.toLowerCase().equals("namespaces")){
				JSONObject namespaces = new JSONObject();
				JSONObject properties = new JSONObject();

				for (Record rc : this.spec.getAllRecords()) {
					if (rc.isInRecordList()) {
						if (rc.isShowType("authority")) {
							JSONArray insta = new JSONArray();
							for(Instance ins : rc.getAllInstances()){
								insta.put(ins.getWebURL());
							}
							JSONObject insd = new JSONObject();
							insd.put("default", insta);
							insd.put("type", "array");
							properties.put(rc.getWebURL(), insd);
						}
					}
				}
				namespaces.put("type", "object");
				namespaces.put("properties", properties);
				out.put("namespaces", namespaces);
				/**
				 * {
    "namespaces": {
        "type": "object",
        "properties": {
            "person": {
                "default": [
                    "persontest1",
                    "persontest2"
                ],
                "type": "array"
            },
            "organization": {
                "default": [
                    "organizationtest1",
                    "organizationtest2"
                ],
                "type": "array"
            }
        }
    }
}
				 */
			}
			else if(sectionid.toLowerCase().equals("recordtypes")){

				
				JSONObject pschema = new JSONObject();
				JSONObject aschema = new JSONObject();
				JSONObject adschema = new JSONObject();
				JSONObject cschema = new JSONObject();

				JSONArray precrds = new JSONArray();
				JSONArray arecrds = new JSONArray();
				JSONArray adrecrds = new JSONArray();
				JSONArray crecrds = new JSONArray();
				/**
				 * { "procedures": { "type": "array", "default": ["loanout",
				 * "movement", ...] }, "vocabularies": { "type": "array",
				 * "default": ["person", "organization", ...] }, "cataloging": {
				 * "type": "array", "default": ["cataloging"] } }
				 */
				for (Record rc : this.spec.getAllRecords()) {
					if (rc.isInRecordList()) {
						if (rc.isShowType("procedure")) {
							precrds.put(rc.getWebURL());
						} else if (rc.isShowType("authority")) {
							for(Instance ins : rc.getAllInstances()){
								arecrds.put(ins.getWebURL());
							}
						} else if (rc.isShowType("record")) {
							crecrds.put(rc.getWebURL());
						} else if(rc.isShowType("authorizationdata") || rc.isShowType("userdata")){
							adrecrds.put(rc.getWebURL());
						}
					}
				}

				pschema.put("type", "array");
				pschema.put("default", precrds);
				aschema.put("type", "array");
				aschema.put("default", arecrds);
				adschema.put("type", "array");
				adschema.put("default", adrecrds);
				cschema.put("type", "array");
				cschema.put("default", crecrds);

				JSONObject rtypes = new JSONObject();
				JSONObject ptypes = new JSONObject();

				ptypes.put("procedures", pschema);
				ptypes.put("vocabularies", aschema);
				ptypes.put("cataloging", cschema);
				ptypes.put("administration", adschema);
				rtypes.put("type","object");
				rtypes.put("properties",ptypes);

				out.put("recordtypes", rtypes);
				
			}
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException", e);
		}
		return out;
	}
	private JSONObject uisearchschema(Storage storage, Record record) throws UIException {
		UISpecRunContext context = new UISpecRunContext();
		this.storage = storage;
		this.record = record;
		this.tenantname = this.record.getSpec().getAdminData().getTenantName();

		try {
			JSONObject out = new JSONObject();

			JSONObject fields = generateFields(context);
			JSONObject prop = fields.getJSONObject("properties");
			fields.put("properties", prop);
			out.put(record.getWebURL(), fields);
			return out;
		} catch (JSONException e) {
			throw new UIException("Cannot generate UISpec due to JSONException", e);
		}
	}
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
			if(this.spectype.equals("search")){
				out = uisearchschema(q.getStorage(),this.record);
			}
			else{
				out = uirecordschema(q.getStorage(),this.record);
			}
		}
		else{
			out = uiotherschema(q.getStorage(),StringUtils.join(tail,"/"));
		}
		q.getUIRequest().sendJSONResponse(out);
	}

	public void configure(WebUI ui, Spec spec) {
	}
}
