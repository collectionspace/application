/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import java.util.List;

public interface FieldSet {
	public String getID();
	public String[] getIDPath();

	public FieldParent getParent();
	public Record getRecord();
	public String getServicesTag();
	public String getSection();
	public Boolean isInServices();
	public boolean hasFieldPerm(String k);
	public String[] getAllFieldPerms();

	public void config_finish(Spec spec);
	
	//subrecords
	public String getSelectorAffix();
	public String getSelector();
	public Boolean usesRecord();
	public Record usesRecordId();
	
	//
	public Boolean hasAutocompleteInstance();
	public Boolean hasMergeData();
	public List<String> getAllMerge();
	public boolean isExpander();
}
