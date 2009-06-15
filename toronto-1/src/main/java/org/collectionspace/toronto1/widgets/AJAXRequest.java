package org.collectionspace.toronto1.widgets;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;

public class AJAXRequest {
	private Map<String,String[]> params=new HashMap<String,String[]>();
	private String type,code,page;
	private JSONArray result=new JSONArray();
	private Map<String,Object> attrs=new HashMap<String,Object>();
	private boolean destination=false;
	private HttpServletRequest request;
	
	public AJAXRequest(HttpServletRequest request,String page,String type,String code,Map<String,String[]> params) {
		this.page=page;
		this.type=type;
		this.code=code;
		this.params=params;
		this.request=request;
	}

	public void addResult(JSONObject data) {
		result.put(data);
	}
	
	public String getUniqueParam(String key) {
		String[] values=params.get(key);
		if(values==null || values.length==0)
			return null;
		return values[0];
	}
	public void setAttr(String key,String value) { attrs.put(key,value); }
	public Object getAttr(String key) { return attrs.get(key); }
	public void deleteAttr(String key) { attrs.remove(key); }
	public void setDestination() { destination=true; }
	public boolean haveDestination() { return destination; }
	
	public String getPage() { return page; }
	public String getType() { return type; }
	public String getCode() { return code; }
	public JSONArray getResult() { return result; }
	
	public String servletRelativePath(String in) { // XXX configurable
		if(!in.startsWith("/"))
			in="/"+in;
		return request.getContextPath()+in;
	}
}
