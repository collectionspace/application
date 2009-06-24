package org.collectionspace.chain.storage.services;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.collectionspace.chain.storage.ExistException;
import org.collectionspace.chain.storage.Storage;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.chain.util.jxj.JXJFile;
import org.collectionspace.chain.util.jxj.JXJTransformer;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.json.JSONObject;

// XXX we assume it's an object for now: we need an abstraction for other kinds of things, in due course.
public class ServicesCollectionObjectStorage implements Storage {
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
	
	public ServicesCollectionObjectStorage(ServicesConnection conn) throws InvalidJXJException, DocumentException, IOException {
		this.conn=conn;		
		JXJFile jxj_file=JXJFile.compile(getDocument("collectionobject.jxj"));
		jxj=jxj_file.getTransformer("collection-object");
		if(jxj==null)
			throw new InvalidJXJException("Missing collection-object transform.");
	}
	
	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException {
		// TODO Auto-generated method stub
	}

	public String[] getPaths() {
		// TODO Auto-generated method stub
		return null;
	}

	public String retrieveJSON(String filePath) throws ExistException {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateJSON(String filePath, JSONObject jsonObject) throws ExistException {
		// TODO Auto-generated method stub
	}
}
