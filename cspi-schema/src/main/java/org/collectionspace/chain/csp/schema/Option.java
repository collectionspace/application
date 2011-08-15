/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

public class Option {
	private String id,name,sample;
	private String description="";
	private boolean dfault=false;

	Option(String id,String name) { this.id=id; this.name=name; }
	Option(String id,String name,String sample) { this.id=id; this.name=name; this.sample=sample; }
	Option(String id,String name,String sample, String desc) { this.id=id; this.name=name; this.sample=sample; this.description=desc; }
	void setDefault() { dfault=true; }
	
	public String getID() { return id; }
	public String getName() { return name; }
	public String getDesc() { return description; }
	public String getSample() { return sample; }
	public boolean isDefault() { return dfault; }
}
