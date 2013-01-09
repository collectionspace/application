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

	protected SchemaUtils utils = new SchemaUtils();
	

	Relationship(Spec spec,ReadOnlySection section) { 

		utils.initStrings(section,"@id",null);
		utils.initStrings(section,"childname",utils.getString("@id"));
		utils.initStrings(section,"displayName",utils.getString("@id"));
		utils.initStrings(section,"predicate",utils.getString("@id"));
		utils.initStrings(section,"metaTypeField","");
		utils.initStrings(section,"showsiblings","");
		utils.initStrings(section,"subject","n");
		utils.initStrings(section,"object","n");
		utils.initStrings(section,"inverseOf","");

		utils.initBoolean(section,"showinui",false);
		utils.initBoolean(section,"directional",true);
		utils.initSet(section,"sourceTypes",new String[] { "" });	
		utils.initSet(section,"destinationTypes",new String[] { "" });	
		
	}

	public String getID() {	return  utils.getString("@id");	}
	public String getChildName() { return utils.getString("childname"); }
	public String getDisplayName() { return utils.getString("displayName"); }
	public String getSubject() { return utils.getString("subject"); }
	public String getObject() { return utils.getString("object"); }
	public String getPredicate() { return utils.getString("predicate"); }
	public String getMetaTypeField() { return utils.getString("metaTypeField"); }
	public String getInverse() { return utils.getString("inverseOf"); }
	public Boolean hasInverse() { if(utils.getString("inverseOf").equals("")){ return false; } else { return true; } }
	public Boolean isDirectional() { return utils.getBoolean("directional"); }
	public Boolean showSiblings() { if(utils.getString("showsiblings").equals("")){ return false; } else { return true; } }
	public Boolean mustExistInSpec() { return utils.getBoolean("showinui"); }

	public String getSiblingParent() { return utils.getString("showsiblings").split(":")[0]; }
	public String getSiblingChild() { return utils.getString("showsiblings").split(":")[1]; }
	
	public String[] getAllSource(){
		return utils.getSet("sourceTypes").toArray(new String[0]);
	}
	
	public String[] getAllDestination(){
		return utils.getSet("destinationTypes").toArray(new String[0]);
	}
	
	public Boolean hasSourceType(String name){
		return utils.getSet("sourceTypes").contains(name);
	}
	
	public Boolean hasDestinationType(String name){
		return utils.getSet("destinationTypes").contains(name);
	}
	
}
