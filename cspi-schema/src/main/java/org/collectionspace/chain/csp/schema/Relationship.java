package org.collectionspace.chain.csp.schema;

import java.util.Set;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Relationship {

	private String id,name; 
	private boolean directional = false;

	private Set<String> source,destination;

	Relationship(Spec spec,ReadOnlySection section) { 
		this.id=(String)section.getValue("/@id");
		this.name=Util.getStringOrDefault(section,"/displayName",id);
		this.directional=Util.getBooleanOrDefault(section,"/directional",true);
		this.source=Util.getSetOrDefault(section,"/sourceTypes",new String[]{""});
		this.destination=Util.getSetOrDefault(section,"/destinationTypes",new String[]{""});
	}
	
	
	public String getID() { return id; }
	public String getDisplayName() { return name; }
	public Boolean isDirectional() { return this.directional; }
	
	public String[] getAllSource(){
		return source.toArray(new String[0]);
	}
	
	public String[] getAllDestination(){
		return destination.toArray(new String[0]);
	}
	
	public Boolean hasSourceType(String name){
		return source.contains(name);
	}
	
	public Boolean hasDestinationType(String name){
		return destination.contains(name);
	}
	

}
