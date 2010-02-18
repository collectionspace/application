package org.collectionspace.chain.csp.persistence.services.connection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class ConnectionUtils {
	static InputStream serializetoXML(Document doc) throws IOException {
		return new ByteArrayInputStream(serializeToBytes(doc));
	}

	static byte[] serializeToBytes(Document doc) throws IOException {
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
		return out.toByteArray();
	}
	
	static InputStream documentToStream(Document in) throws ConnectionException {
		if(in!=null) {
			try {
				return serializetoXML(in);
			} catch (IOException e) {
				throw new ConnectionException("Could not connect",e);
			}
		}
		return null;
	}

	static byte[] documentToBytes(Document in) throws ConnectionException {
		if(in!=null) {
			try {
				return serializeToBytes(in);
			} catch (IOException e) {
				throw new ConnectionException("Could not connect",e);
			}
		}
		return null;
	}
}
