/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.bconfigutils.bootstrap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dom4j.Document;
import org.dom4j.Element;

/** Expects a URL, but will only return it if the service layer is working at this URL */
// XXX CSPACE-302
public class ServicesRespondingConfigLoadMethod implements ConfigLoadMethod {
	private boolean pings(String url) {
		HttpClient client=new HttpClient();
		HttpMethod method=new GetMethod(url);
		try {
			int response=client.executeMethod(method);
			return (response>199 && response<300) || response==401;
		} catch(Exception e) {
			return false;
		} finally {
			method.releaseConnection();
		}
	}

	public String getString(Element e) {
		String url=e.getTextTrim();
		String suffix=e.attributeValue("suffix");
		if(suffix==null)
			suffix="";
		if(pings(url+suffix))
			return url;
		return null;
	}

	public void init(BootstrapConfigController controller, Document root) throws BootstrapConfigLoadFailedException {}
}
