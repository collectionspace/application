package org.collectionspace.chain.csp.schema;

import java.util.List;

public interface FieldSet {
	public String getID();
	public String[] getIDPath();
	public Record getRecord();
	public String getServicesTag();
	public String getSection();
	public boolean isInServices();
	public void config_finish(Spec spec);
	
	//subrecords
	public String getSelectorAffix();
	public boolean usesRecord();
	public Record usesRecordId();
	
	//
	public boolean hasAutocompleteInstance();
	public Boolean hasMergeData();
	public List<String> getAllMerge();
}
