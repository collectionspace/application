package org.collectionspace.chain.config.main;

import org.collectionspace.kludge.ConfigLoadFailedException;
import org.xml.sax.InputSource;

public interface ConfigFactory {	
	public ConfigRoot parseConfig(InputSource in,String url) throws ConfigLoadFailedException;
}
