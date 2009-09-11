/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.bconfigutils.bootstrap;

import org.collectionspace.kludge.ConfigLoadFailedException;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** method to use tmpdir (a fallback) as value returned */
public class TmpdirConfigLoadMethod implements ConfigLoadMethod {
	private static final Logger log=LoggerFactory.getLogger(TmpdirConfigLoadMethod.class);
	
	public String getString(Element e) {
		String out=System.getProperty("java.io.tmpdir");
		log.warn("Warning: Defaulting to tmpdir for "+e.getName());
		return out;
	}

	public void init(BootstrapConfigController controller, Document root)
			throws ConfigLoadFailedException {}
}
