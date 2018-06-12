/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.chain.csp.schema.AdminData;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Option;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.schema.Structure;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VocabRedirector implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(VocabRedirector.class);
	private Record r;
	private AdminData adminData = null;
	
	public VocabRedirector(Record r) { this.r=r; }
	
	public void configure(WebUI ui, Spec spec) {
		adminData = spec.getAdminData();
	}

	//old style
	private String pathFor(String in) {
		Field fd = (Field) r.getFieldFullList(in);
		String weburl = fd.getAutocompleteInstance().getWebURL();
		
		return "/vocabularies/"+weburl; 
	}
	
	/**
	 * Returns information on the vocabularies (Authority namespaces) that are configured
	 * for autocomplete use for the passed field, in the context record. 
	 * If the record is itself an Authority term editor, hierarchy fields should
	 * be constrained to the vocabulary named by vocabConstraint 
	 * @param fieldname The name of the field for which to return the info
	 * @param vocabConstraint The vocabulary when the field is hierarchic and the record is a term.
	 * @return Information on the configured vocabularies
	 * @throws JSONException
	 */
	private JSONArray pathForAll(String fieldname, String vocabConstraint) throws JSONException{
		JSONArray out = new JSONArray();

		FieldSet fd = r.getFieldFullList(fieldname);

		Instance[] allInstances = null;
		if(fd == null || !(fd instanceof Field)){
			if(r.hasHierarchyUsed("screen")){
				Structure s = r.getStructure("screen");	// Configures the hierarchy section.
				if(s.hasOption(fieldname)){				// This is one of the hierarchy fields
					if(vocabConstraint!=null) {
						allInstances = new Instance[1];
						String fullname = r.getID()+"-"+vocabConstraint;
						allInstances[0] = r.getSpec().getInstance(fullname);
					} else {
						Option a = s.getOption(fieldname);
						String[] data = a.getName().split(",");
						allInstances = new Instance[data.length];
						for(int i=0; i<data.length; i++){
							allInstances[i] = (r.getSpec().getInstance(data[i]));
						}
					}
				} else{
					FieldSet fs = r.getSpec().getRecord("hierarchy").getFieldFullList(fieldname);
					if(fs instanceof Field){ 	
						allInstances = ((Field)fs).getAllAutocompleteInstances();
					}
				}
			}
		}
		else{
			allInstances = ((Field)fd).getAllAutocompleteInstances();
		}
		
		for(Instance autoc : allInstances) {
			if (autoc != null) {
				JSONObject instance = new JSONObject();
				instance.put("url","/vocabularies/"+autoc.getWebURL());
				instance.put("type",autoc.getID());
				instance.put("fullName",autoc.getTitle());
				out.put(instance);
			} else {
				log.debug(String.format("A vocab/authority instance for autocompleting the '%s' field was null or missing.",
						fieldname));
			}
		}
		
		return out;
	}
	
	private void redirect(CSPRequestCache cache,Storage storage,UIRequest request,String[] tail) throws UIException {
		try {
			JSONArray out = new JSONArray();
			String vocabConstraint = request.getRequestArgument(CONSTRAIN_VOCAB_PARAM);
			out = pathForAll(tail[0], vocabConstraint);
			request.sendJSONResponse(out);
			int cacheMaxAgeSeconds = adminData.getAutocompleteListCacheAge();
			if(cacheMaxAgeSeconds > 0) {
				request.setCacheMaxAgeSeconds(cacheMaxAgeSeconds);
			}
			
		} catch (JSONException e) {
			throw new UIException("JSON building failed",e);
		}
	}
	
	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		redirect(q.getCache(),q.getStorage(),q.getUIRequest(),tail);
	}

}
