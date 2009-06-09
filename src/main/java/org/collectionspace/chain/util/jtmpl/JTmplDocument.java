package org.collectionspace.chain.util.jtmpl;

import java.util.Map;

import org.collectionspace.chain.util.JSONClone;
import org.collectionspace.chain.util.jpath.JPathPath;
import org.json.JSONException;
import org.json.JSONObject;

public class JTmplDocument {
	private JSONObject document;
	private Map<String,JPathPath> attach;

	JTmplDocument(JSONObject template,Map<String,JPathPath> attach) throws JSONException {
		this.document=(JSONObject)JSONClone.clone(template);
		this.attach=attach;
	}
	
	public void set(String key,Object value) throws InvalidJTmplException {
		try {
			JPathPath path=attach.get(key);
			if(path==null)
				return;
			path.set(document,value);
		} catch (JSONException e) {
			throw new InvalidJTmplException("Bad JSON",e);
		}
	}
	
	public JSONObject getJSON() {
		return document;
	}
}
