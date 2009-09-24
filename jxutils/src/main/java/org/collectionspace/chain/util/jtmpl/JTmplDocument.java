/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.util.jtmpl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.util.jpath.JPathPath;
import org.collectionspace.chain.util.misc.JSON;
import org.json.JSONException;
import org.json.JSONObject;

/** A template instance which can be filled in with values. Once done, you can then retrieve the result.
 * 
 */
public class JTmplDocument {
	private JSONObject document;
	private Map<String,JPathPath> attach;
	private Set<String> unused;

	JTmplDocument(JSONObject template,Map<String,JPathPath> attach) throws JSONException {
		this.document=(JSONObject)JSON.clone(template);
		this.attach=attach;
		unused=new HashSet<String>(attach.keySet());
	}
	
	public void set(String key,Object value) throws InvalidJTmplException {
		try {
			JPathPath path=attach.get(key);
			if(path==null)
				return;
			path.set(document,value);
			unused.remove(key);
		} catch (JSONException e) {
			throw new InvalidJTmplException("Bad JSON",e);
		}
	}
	
	public JSONObject getJSON() {
		for(String key : unused) {
			try {
				set(key,"");
			} catch (InvalidJTmplException e) {} // Should never happen
		}
		return document;
	}
}
