package org.collectionspace.chain.csp.schema;

public class Option {
	private String id,name,sample; 

	public Option(String id,String name) { this.id=id; this.name=name; }
	public Option(String id,String name,String sample) { this.id=id; this.name=name; this.sample=sample; }
	
	public String getID() { return id; }
	public String getName() { return name; }
	public String getSample() { return sample; }
}
