package org.collectionspace.chain.csp.schema;

public class Option {
	private String id,name,sample; 
	private boolean dfault=false;

	Option(String id,String name) { this.id=id; this.name=name; }
	Option(String id,String name,String sample) { this.id=id; this.name=name; this.sample=sample; }
	void setDefault() { dfault=true; }
	
	public String getID() { return id; }
	public String getName() { return name; }
	public String getSample() { return sample; }
	public boolean isDefault() { return dfault; }
}
