/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.config;

import org.dom4j.Document;
import org.dom4j.Element;

/** abstract base type for many methods, probably a bit useless now that it's so simple after refactors of the API */
public abstract class StringReadingConfigLoadMethod implements ConfigLoadMethod {
	protected String value;
	
	protected abstract String string_get(String value);

	public void init(ConfigLoadController controller,Document root) throws ConfigLoadFailedException {}
	
	public final String getString(Element e) {
		return string_get(e.getTextTrim());
	}
}
