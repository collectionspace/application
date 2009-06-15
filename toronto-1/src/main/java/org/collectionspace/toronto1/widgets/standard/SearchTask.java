package org.collectionspace.toronto1.widgets.standard;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.collectionspace.toronto1.store.Store;
import org.collectionspace.toronto1.widgets.AJAXRequest;
import org.collectionspace.toronto1.widgets.Task;
import org.collectionspace.toronto1.widgets.TaskException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SearchTask implements Task {
	private static Store store=new Store(); // XXX eugh!

	public void preact(AJAXRequest data) throws TaskException {
		try {
			JSONObject a1=new JSONObject();
			a1.put("action","send-names");
			a1.put("code",data.getCode());
			data.addResult(a1);
		} catch (JSONException e) {
			throw new TaskException("Bad JSON",e);
		}
	}

	public void act(AJAXRequest request) throws TaskException {
		try {
			JSONObject action=new JSONObject();
			action.put("good","search complete");
			request.addResult(action);
		} catch (JSONException e) {
			throw new TaskException("Bad JSON",e);
		}
	}

	private void convertObjectToParameter(StringBuffer url,String key,String value) {
		if(StringUtils.isBlank(value))
			return;
		try {
			url.append(URLEncoder.encode(key,"UTF-8"));
			url.append('=');
			url.append(URLEncoder.encode(value,"UTF-8"));
			url.append('&');
		} catch (UnsupportedEncodingException e) {}
		
	}
	
	@SuppressWarnings("unchecked")
	private void convertObjectToParameters(StringBuffer url,JSONObject data) {
		Iterator t=data.keys();
		while(t.hasNext()) {
			String key=(String)t.next();
			try {
				Object vs=data.get(key);
				if(vs instanceof String) {
					convertObjectToParameter(url,key,(String)vs);
				} else if(vs instanceof JSONArray) {
					int len=((JSONArray)vs).length();
					for(int i=0;i<len;i++) {
						convertObjectToParameter(url,key,((JSONArray)vs).getString(i));
					}
				}
			} catch (JSONException e) {} // XXX handle
		}
	}
	
	public void postact(AJAXRequest request) throws TaskException {
		try {
			StringBuffer urlb=new StringBuffer();
			urlb.append(request.servletRelativePath("/main/results/"+request.getPage()+"?"));
			JSONObject data=new JSONObject((String)request.getUniqueParam("data"));
			convertObjectToParameters(urlb,data);
			String url=urlb.toString();
			if(urlb.length()>0 && urlb.charAt(urlb.length()-1)=='&')
				url=url.substring(0,urlb.length()-1);
			JSONObject go=new JSONObject();
			go.put("goto",url);
			request.addResult(go);
		} catch (JSONException e) {
			throw new TaskException("Bad JSON",e);
		}
	}
}
