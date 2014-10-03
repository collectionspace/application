package org.collectionspace.chain.csp.webui.misc;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Group;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.nuispec.CacheTermList;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebTermList implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebTermList.class);
	private Spec spec;
	private Record r;
	protected CacheTermList ctl;

	public WebTermList(Record r) { this.r=r; }
	
	private void termlist(CSPRequestCache cache,Storage storage,UIRequest request, String path) throws UIException {
		try {
			//{tenant}/{tenantname}/{recordType}/termList/{termListType}
			// needs to be {tenant}/{tenantname}/{recordType}/termList/{fieldname}
			//as blanks etc are on a field basis not a vocab basis
			String[] bits = path.split("/");
			Record vb = this.spec.getRecord("vocab");
			Field f = (Field)r.getFieldTopLevel(bits[0]);
			if(f == null){
				f = (Field)r.getFieldFullList(bits[0]);
			}
			// If the field isn't in this record, look for it in subrecords (e.g. contacts).
			if (f == null) {
				FieldSet[] subRecordFields = r.getAllSubRecords("GET");
				
				for (int i=0; i<subRecordFields.length; i++) {
					FieldSet subRecordField = subRecordFields[i];
					Group group = (Group) subRecordField;
					
					if (group.usesRecord()) {
						Record subRecord = group.usesRecordId();
						f = (Field) subRecord.getFieldTopLevel(bits[0]);
						
						if (f == null) {
							f = (Field) subRecord.getFieldFullList(bits[0]);
						}
						
						if (f != null) {
							break;
						}
					}
				}
			}
			JSONArray result = new JSONArray();
			for(Instance ins : f.getAllAutocompleteInstances()){
				JSONArray getallnames = ctl.get(storage, ins.getTitleRef(), vb);
				for (int i = 0; i < getallnames.length(); i++) {
			        result.put(getallnames.get(i));
			    }
			}
			JSONObject out =  generateENUMField(storage, f, result, false);
			request.sendJSONResponse(out);
			int cacheMaxAgeSeconds = spec.getAdminData().getTermListCacheAge();
			if(cacheMaxAgeSeconds > 0) {
				request.setCacheMaxAgeSeconds(cacheMaxAgeSeconds);
			}
		} catch (JSONException e) {
			throw new UIException("JSONException during autocompletion",e);
		} 
	}
	
	protected JSONObject generateENUMField(Storage storage, Field f, JSONArray getallnames, Boolean showshortID) throws JSONException {
		JSONArray ids=new JSONArray();
		JSONArray names=new JSONArray();
		JSONArray activestatus = new JSONArray();
		int dfault = -1;
		int spacer =0;
		if(f.hasEnumBlank()){
			ids.put("");
			activestatus.put("");
			names.put(f.enumBlankValue());
			spacer = 1;
		}
		for(int i=0;i<getallnames.length();i++) {
			JSONObject namedata = getallnames.getJSONObject(i);
			String name = namedata.getString("displayName");
			String status = namedata.getString("termStatus");
			activestatus.put(status);
			String shortId="";
			String refname=namedata.getString("refid");
			if(namedata.has("shortIdentifier") && !namedata.getString("shortIdentifier").equals("")){
				shortId = namedata.getString("shortIdentifier");
			}
			else{
				shortId = name.replaceAll("\\W","");					
			}
			//currently only supports single select dropdowns and not multiselect
			if(f.isEnumDefault(name)){
				dfault = i + spacer;
			}
			if(showshortID){
				ids.put(shortId.toLowerCase());
			}
			else{
				ids.put(refname);
			}
			
			names.put(name);
		}
		// Dropdown entry pulled from service layer data
		JSONObject out=new JSONObject();


		if(dfault!=-1)
			out.put("default",dfault+"");
		out.put("optionlist",ids);
		out.put("optionnames",names);	
		out.put("activestatus",activestatus);	
		return out;
	}
	

	@Override
	public void configure(WebUI ui, Spec spec) {
		this.spec = spec;
		
	}

	@Override
	public void run(Object in, String[] tail) throws UIException {

		Request q=(Request)in;
		ctl = new CacheTermList(q.getCache());
		termlist(q.getCache(),q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
		
	}
	
	/**
	 * {
	 * "optionnames": ["Please select a value", "Value 1", "Value 2"],
	 * "optionlist": ["", "/vocabularies/{csid}/items/{csid}", "value2"]
	 * "activestatus": ["", "inactive", "active"]
	 * }
	 */
}
