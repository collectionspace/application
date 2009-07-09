/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.config;

import org.dom4j.Document;
import org.dom4j.Element;

/** method to just return the string provided (a fallback) */
public class DefaultConfigLoadMethod implements ConfigLoadMethod {

	public String getString(Element e) { return e.getTextTrim(); }

	public void init(ConfigLoadController controller, Document root)
			throws ConfigLoadFailedException {}
}
