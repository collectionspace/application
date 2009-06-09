package org.collectionspace.chain.util.jtmpl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.collectionspace.chain.util.JSONClone;
import org.collectionspace.chain.util.jpath.InvalidJPathException;
import org.collectionspace.chain.util.jpath.JPathPath;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JTmplTmpl {
	private JSONObject template;
	private Map<String,JPathPath> attach=new HashMap<String,JPathPath>();
	
	public static JTmplTmpl compile(JSONObject template) throws InvalidJTmplException {
		return new JTmplTmpl(template);
	}
	
	@SuppressWarnings("unchecked")
	private void build_template(String path,Object in,Object root) throws JSONException, InvalidJPathException {
		if(in instanceof JSONObject) {
			Iterator ks=((JSONObject)in).keys();
			while(ks.hasNext()) {
				String k=(String)ks.next();
				build_template(path+JPathPath.component(k),((JSONObject)in).get(k),root);
			}
		} else if(in instanceof JSONArray) {
			int len=((JSONArray)in).length();
			for(int i=0;i<len;i++)
				build_template(path+"["+i+"]",((JSONArray)in).get(i),root);
		} else if(in instanceof String) {
			if(((String) in).length()>0 && ((String)in).charAt(0)=='\0') {
				String key=((String)in).substring(1);
				if(key.length()>0) { 
					JPathPath jpath=JPathPath.compile(path);
					if(key.charAt(0)!='\0') {
						attach.put(key,jpath);
					} else {
						jpath.set(root,key); // remove leading \0
					}
				}
			}
		}
	}
	
	private JTmplTmpl(JSONObject template) throws InvalidJTmplException {
		try {
			build_template("",template,template);
			this.template=(JSONObject)JSONClone.clone(template);
			System.err.println(template);
		} catch (JSONException e) {
			throw new InvalidJTmplException("Bad JSON template",e);
		} catch (InvalidJPathException e) {
			throw new InvalidJTmplException("Bad JPath",e);
		}
	}
	
	public JTmplDocument makeDocument() throws InvalidJTmplException {
		try {
			return new JTmplDocument(template,attach);
		} catch (JSONException e) {
			throw new InvalidJTmplException("Bad JSON template",e);
		}
	}
}
