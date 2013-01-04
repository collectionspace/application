/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;

/**
 * Groups is a repeat which just doesn't repeat
 * XXX technically repeat should be a repeating group
 * @author csm22
 *
 */
public class Group extends Repeat {

	public Group(Record record, ReadOnlySection section) {
		super(record, section);
		utils.setBoolean("@asSibling",true);
	}

	public Group(Repeat parent, ReadOnlySection section) {
		super(parent, section);
		utils.setBoolean("@asSibling",true);
	}
	public Group(Group parent, ReadOnlySection section) {
		super(parent, section);
		utils.setBoolean("@asSibling",true);
	}
		
}
