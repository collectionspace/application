/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.TeeInputStream;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionUtils {
	private static final Logger log=LoggerFactory.getLogger(ConnectionUtils.class);
	static InputStream serializetoXML(Document doc) throws IOException {
		return new ByteArrayInputStream(serializeToBytes(doc));
	}

	static byte[] serializeToBytes(Document doc) throws IOException {

		//what is the implications of changing this?
		//ok assuming all our xml comes from our shiny XmlJsonConversion then we can ignore the whole indent thing.
		/*
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		outformat.setEncoding("UTF-8");
		outformat.setExpandEmptyElements(true);
		outformat.setNewlines(false);
		outformat.setIndent(false);
		XMLWriter writer = new XMLWriter(out, outformat);
		
		writer.write(doc);
		writer.flush();
		out.close();
*/
return doc.asXML().getBytes();

//		return out.toByteArray();
	}
	
	static InputStream documentToStream(Document in) throws ConnectionException {
		if(in!=null) {
			try {
				return serializetoXML(in);
			} catch (IOException e) {
				throw new ConnectionException("Could not connect "+e.getLocalizedMessage(),e);
			}
		}
		return null;
	}

	static byte[] documentToBytes(Document in) throws ConnectionException {
		if(in!=null) {
			try {
				return serializeToBytes(in);
			} catch (IOException e) {
				throw new ConnectionException("Could not connect"+e.getLocalizedMessage(),e);
			}
		}
		return null;
	}
}
