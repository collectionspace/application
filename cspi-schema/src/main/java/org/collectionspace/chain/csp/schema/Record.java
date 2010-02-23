package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class Record {
	private String id;
	
	Record(ReadOnlySection section) {
		id=(String)section.getValue("/record/@id");
	}
	
	public String getID() { return id; }
}
