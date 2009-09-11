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

/** the interface implemented by methods */
public interface ConfigLoadMethod {	
	public void init(BootstrapConfigController controller,Document root) throws ConfigLoadFailedException;
		
	public String getString(Element e);
}
