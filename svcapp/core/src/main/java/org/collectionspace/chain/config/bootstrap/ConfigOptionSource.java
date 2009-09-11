/* Copyright 2009 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.config.bootstrap;

import org.dom4j.Element;

/** [package scope] internal use, a part of the control file */
class ConfigOptionSource {
	private Element element;
	private ConfigLoadMethod method;
	private String value;
	
	ConfigOptionSource(ConfigLoadMethod method,Element element) {
		this.method=method;
		this.element=element;
	}
	
	public String getString() {
		if(value==null) {
			synchronized(this) {
				if(value==null)
					value=method.getString(element);
			}
		}
		return value;
	}
}
