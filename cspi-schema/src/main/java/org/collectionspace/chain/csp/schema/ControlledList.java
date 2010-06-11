package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlledList {
	private static final Logger log=LoggerFactory.getLogger(ControlledList.class);
	private String id, name;
	
	// XXX utility methods
	ControlledList(Spec parent,ReadOnlySection section) {
		id=(String)section.getValue("/@id");
		name = Util.getStringOrDefault(section,"/name",id);
	
	}

	public String getID() { return id; }
	public String getName() { return name; }
	
	public ControlledList getControlledList() { return this; }
	
	public void config_finish(Spec spec) {
		//for(FieldSet fs : fields.values())
		//	fs.config_finish(spec);
	}
}
