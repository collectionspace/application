/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Relationship {

	private Map<String, String> allStrings = new HashMap<String, String>();
	private Map<String, Boolean> allBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allSets = new HashMap<String, Set<String>>();
	/* just used for documentation to retrieve defaults */
	private Map<String, String> allDefaultStrings = new HashMap<String, String>();
	private Map<String, Boolean> allDefaultBooleans = new HashMap<String, Boolean>();
	private Map<String, Set<String>> allDefaultSets = new HashMap<String, Set<String>>();
	

	Relationship(Spec spec,ReadOnlySection section) { 

		this.initStrings(section,"@id",null);
		this.initStrings(section,"childname",getString("@id"));
		this.initStrings(section,"displayName",getString("@id"));
		this.initStrings(section,"predicate",getString("@id"));
		this.initStrings(section,"showsiblings","");
		this.initStrings(section,"subject","n");
		this.initStrings(section,"object","n");
		this.initStrings(section,"inverseOf","");
		
		this.initBoolean(section,"directional",true);
		this.initSet(section,"sourceTypes",new String[] { "" });	
		this.initSet(section,"destinationTypes",new String[] { "" });	
		
	}

	public String getID() {	return  getString("@id");	}
	public String getChildName() { return getString("childname"); }
	public String getDisplayName() { return getString("displayName"); }
	public String getSubject() { return getString("subject"); }
	public String getObject() { return getString("object"); }
	public String getPredicate() { return getString("predicate"); }
	public String getInverse() { return getString("inverseOf"); }
	public Boolean hasInverse() { if(getString("inverseOf").equals("")){ return false; } else { return true; } }
	public Boolean isDirectional() { return getBoolean("directional"); }
	public Boolean showSiblings() { if(getString("showsiblings").equals("")){ return false; } else { return true; } }

	public String getSiblingParent() { return getString("showsiblings").split(":")[0]; }
	public String getSiblingChild() { return getString("showsiblings").split(":")[1]; }
	
	public String[] getAllSource(){
		return getSet("sourceTypes").toArray(new String[0]);
	}
	
	public String[] getAllDestination(){
		return getSet("destinationTypes").toArray(new String[0]);
	}
	
	public Boolean hasSourceType(String name){
		return getSet("sourceTypes").contains(name);
	}
	
	public Boolean hasDestinationType(String name){
		return getSet("destinationTypes").contains(name);
	}
	
	
	/** start generic functions **/
	protected Set<String> initSet(ReadOnlySection section, String name, String[] defaultval){
		Set<String> vard = Util.getSetOrDefault(section, "/"+name, defaultval);
		allDefaultSets.put(name,new HashSet<String>(Arrays.asList(defaultval)));
		allSets.put(name,vard);
		return vard;
	}
	protected String initStrings(ReadOnlySection section, String name, String defaultval){
		String vard = Util.getStringOrDefault(section, "/"+name, defaultval);
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
	protected String[] getAllString(){
		return allStrings.keySet().toArray(new String[0]);
	}
	protected String getString(String name){
		if(allStrings.containsKey(name)){
			return allStrings.get(name);
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

	protected String[] getAllSets(){
		return allSets.keySet().toArray(new String[0]);
	}
	
	protected Set<String> getSet(String name){
		if(allSets.containsKey(name)){
			return allSets.get(name);
		}
		return null;
	}
	/** end generic functions **/
}
