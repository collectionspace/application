package org.collectionspace.chain.config.main;

import org.collectionspace.chain.config.api.ConfigLoadFailedException;
import org.xml.sax.InputSource;

public interface MainConfigFactory {	
	public MainConfig parseConfig(InputSource in,String url) throws ConfigLoadFailedException;
}
