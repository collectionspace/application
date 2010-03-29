package org.collectionspace.chain.csp.schema;

public interface FieldSet {
	public String getID();
	public String[] getIDPath();
	public Record getRecord();
	public String getServicesTag();
	public String getSection();
	public void config_finish(Spec spec);
}
