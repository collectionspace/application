package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.collectionspace.chain.util.jtmpl.InvalidJTmplException;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.chain.util.jxj.JXJFile;
import org.collectionspace.chain.util.jxj.JXJTransformer;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.JSONObject;

// NOT USED. MOTHBALLED UNTIL IRRITATING ID ORDER BUG IS FIXED.

public class IdealServicesIntakeStorage implements Storage {
	private ServicesConnection conn;
	private JXJTransformer jxj;
	
	// XXX refactor into util
	private Document getDocument(String in) throws DocumentException, IOException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		SAXReader reader=new SAXReader();
		Document doc=reader.read(stream);
		stream.close();
		return doc;
	}
	
	public IdealServicesIntakeStorage(ServicesConnection conn) throws InvalidJXJException, DocumentException, IOException {
		this.conn=conn;
		JXJFile jxj_file=JXJFile.compile(getDocument("intake.jxj"));
		jxj=jxj_file.getTransformer("intake");
	}

	public String autocreateJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Document doc=jxj.json2xml(jsonObject);
			System.err.println(doc.asXML());
			ReturnedURL url = conn.getURL(RequestMethod.POST,"intakes/",doc);
			if(url.getStatus()>299 || url.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+url.getStatus());
			return url.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidXTmplException e) {
			throw new UnimplementedException("Error in template",e);
		}
	}

	public void createJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot create record with name, POST to directory instead of file");
	}
	
	public void deleteJSON(String filePath) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		try {
			int status=conn.getNone(RequestMethod.DELETE,"intakes/"+filePath,null);
			if(status>299 || status<200) // XXX CSPACE-73, should be 404
				throw new UnderlyingStorageException("Service layer exception status="+status);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}		
	}

	@SuppressWarnings("unchecked")
	public String[] getPaths(String rootPath) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			List<String> out=new ArrayList<String>();
			ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,"intakes/");			
			if(all.getStatus()!=200)
				throw new ConnectionException("Bad request during list: status not 200");
			List<Node> objects=(List<Node>)all.getDocument().selectNodes("intake-list/intake-list-item");
			for(Node object : objects) {
				String csid=object.selectSingleNode("csid").getText();
				int idx=csid.lastIndexOf("/");
				if(idx!=-1)
					csid=csid.substring(idx+1);
				out.add(csid);
			}
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	public JSONObject retrieveJSON(String filePath) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		try {
			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET,"intakes/"+filePath);
			if(doc.getStatus()==404)
				throw new ExistException("Not found");
			if((doc.getStatus()<200 || doc.getStatus()>299))
				throw new UnderlyingStorageException("Bad status = "+doc.getStatus());
			return jxj.xml2json(doc.getDocument());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidJTmplException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidJXJException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}

	public void updateJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Document data=jxj.json2xml(jsonObject);
			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.PUT,"intakes/"+filePath,data);
			if(doc.getStatus()==404)
				throw new ExistException("Not found: intakes/"+filePath);
			if(doc.getStatus()>299 || doc.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+doc.getStatus());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidXTmplException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		}
	}
}
