/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.bconfigutils.bootstrap;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.Element;

public class ClasspathConfigLoadMethod implements ConfigLoadMethod {

	public String getString(Element e) {
		try {
		String path=e.getText();
		InputStream in=Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if(in==null)
			return null;
		String data=IOUtils.toString(in);
		in.close();
		return data;
		} catch(IOException x) {
			// Skip. The below is not a hack.
			return null;
		}
	}

	public void init(BootstrapConfigController controller, Document root) throws BootstrapConfigLoadFailedException {}

}
