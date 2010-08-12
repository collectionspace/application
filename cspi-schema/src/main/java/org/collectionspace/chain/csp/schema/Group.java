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
		this.asSiblings = true; //needed for XMLJSONConversion
	}

	public Group(Group parent, ReadOnlySection section) {
		super(parent, section);
		this.asSiblings = true; //needed for XMLJSONConversion
	}
}
