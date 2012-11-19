package org.collectionspace.chain.csp.schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SchemaUtils {

	private Map<String, String> allStrings = new HashMap<String, String>();
	private Map<String, Boolean> allBooleans = new HashMap<String, Boolean>();
	protected Map<String, Set<String>> allSets = new HashMap<String, Set<String>>();
	/* just used for documentation to retrieve defaults */
	private Map<String, String> allDefaultStrings = new HashMap<String, String>();
	private Map<String, Boolean> allDefaultBooleans = new HashMap<String, Boolean>();
	protected Map<String, Set<String>> allDefaultSets = new HashMap<String, Set<String>>();
	
	public SchemaUtils() {
	}

	/** start generic functions **/
	protected Set<String> initSet(ReadOnlySection section, String name, String[] defaultval){
		Set<String> vard = Util.getSetOrDefault(section, "/"+name, defaultval);
		allDefaultSets.put(name,new LinkedHashSet<String>(Arrays.asList(defaultval)));
		allSets.put(name,vard);
		return vard;
	}
	protected Set<String> initSet(Set<String> vard, String name, String[] defaultval){
		allDefaultSets.put(name,new LinkedHashSet<String>(Arrays.asList(defaultval)));
		allSets.put(name,vard);
		return vard;
	}
	protected String initStrings(ReadOnlySection section, String name, String defaultval){
		String vard = Util.getStringOrDefault(section, "/"+name, defaultval);
		allDefaultStrings.put(name,defaultval);
		allStrings.put(name,vard);
		return vard;
	}

	protected String initStrings(String vard, String name, String defaultval){
		allDefaultStrings.put(name,defaultval);
		allStrings.put(name,vard);
		return vard;
	}
	protected Boolean initBoolean(ReadOnlySection section, String name, Boolean defaultval){
		Boolean vard = Util.getBooleanOrDefault(section, "/"+name, defaultval);
		allDefaultBooleans.put(name,defaultval);
		allBooleans.put(name,vard);
		return vard;
	}
	protected Boolean initBoolean(Boolean vard, String name, Boolean defaultval){
		allDefaultBooleans.put(name,defaultval);
		allBooleans.put(name,vard);
		return vard;
	}
	protected String[] getAllString(){
		return allStrings.keySet().toArray(new String[0]);
	}
	protected void setString(String name,String value){
		allStrings.put(name,value);
	}
	protected void setBoolean(String name,Boolean value){
		allBooleans.put(name,value);
	}
	protected void setSet(String name,Set<String> value){
		allSets.put(name,value);
	}
	protected String getString(String name){
		if(allStrings.containsKey(name)){
			return allStrings.get(name);
		}
		return null;
	}
	protected String getDefaultString(String name){
		if(allDefaultStrings.containsKey(name)){
			return allDefaultStrings.get(name);
		}
		return null;
	}

	protected String[] getAllBoolean(){
		return allBooleans.keySet().toArray(new String[0]);
	}
	protected Boolean getBoolean(String name){
		if(allBooleans.containsKey(name)){
			return allBooleans.get(name);
		}
		return null;
	}
	protected Boolean getDefaultBoolean(String name){
		if(allDefaultBooleans.containsKey(name)){
			return allDefaultBooleans.get(name);
		}
		return null;
	}

	protected String[] getAllSets(){
		return allSets.keySet().toArray(new String[0]);
	}

	protected Set<String> getSet(String name){
		if(allSets.containsKey(name)){
			return allSets.get(name);
		}
		return null;
	}
	protected Set<String> getDefaultSet(String name){
		if(allDefaultSets.containsKey(name)){
			return allDefaultSets.get(name);
		}
		return null;
	}
	/** end generic functions **/
	
	void dump(StringBuffer out) {
		for(String s: this.getAllString()){
			out.append("String,"+ s);
			out.append(",Value,"+ this.allStrings.get(s));
			out.append(",Default,"+ this.allDefaultStrings.get(s));
			out.append("\n");
		}
		for(String s: this.getAllBoolean()){
			out.append("Boolean,"+ s);
			out.append(",Value,"+ this.allBooleans.get(s));
			out.append(",Default,"+ this.allDefaultBooleans.get(s));
			out.append("\n");
		}
		for(String s: this.getAllSets()){
			out.append("Set,"+ s);
			out.append(",Value,"+ this.allSets.get(s));
			out.append(",Default,"+ this.allDefaultSets.get(s));
			out.append("\n");
		}
	}


	void dumpJson(JSONObject fieldsObj, String name) throws JSONException {
		JSONArray fields = new JSONArray();
		
		for(String s: this.getAllString()){
			JSONObject data = new JSONObject();
			data.put("type", "String");
			data.put("name", s);
			data.put("value", this.allStrings.get(s));
			data.put("default",  this.allDefaultStrings.get(s));
			fields.put(data);
		}
		for(String s: this.getAllBoolean()){
			JSONObject data = new JSONObject();
			data.put("type", "Boolean");
			data.put("name", s);
			data.put("value", this.allBooleans.get(s));
			data.put("default",  this.allDefaultBooleans.get(s));
			fields.put(data);
		}
		for(String s: this.getAllSets()){
			JSONObject data = new JSONObject();
			data.put("type", "Set");
			data.put("name", s);
			data.put("value", this.allSets.get(s));
			data.put("default",  this.allDefaultSets.get(s));
			fields.put(data);
		}
		fieldsObj.put(name, fields);
	}

}
