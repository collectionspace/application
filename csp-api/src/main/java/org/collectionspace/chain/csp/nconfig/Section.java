package org.collectionspace.chain.csp.nconfig;

import org.collectionspace.chain.csp.nconfig.ReadOnlySection;

public interface Section extends ReadOnlySection {
	public void addValue(String key,Object value);
}
