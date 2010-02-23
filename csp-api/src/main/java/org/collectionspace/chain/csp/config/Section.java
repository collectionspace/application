package org.collectionspace.chain.csp.config;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public interface Section extends ReadOnlySection {
	public void addValue(String key,Object value);
}
