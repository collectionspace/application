package org.collectionspace.chain.storage.services;

import java.io.IOException;
import java.io.InputStream;

import org.collectionspace.chain.storage.ExistException;
import org.collectionspace.chain.storage.Storage;
import org.collectionspace.chain.storage.UnderlyingStorageException;
import org.collectionspace.chain.storage.UnimplementedException;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.RequestMethod;
import org.collectionspace.chain.util.jxj.InvalidJXJException;
import org.collectionspace.chain.util.jxj.JXJFile;
import org.collectionspace.chain.util.jxj.JXJTransformer;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ServicesCollectionObjectStorage implements Storage {
	private ServicesConnection conn;
	private JXJTransformer jxj;
	private ServicesIdentifierMap cspace_264_hack;
	
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
		cspace_264_hack=new ServicesIdentifierMap(conn);
		JXJFile jxj_file=JXJFile.compile(getDocument("collectionobject.jxj"));
		jxj=jxj_file.getTransformer("collection-object");
		if(jxj==null)
			throw new InvalidJXJException("Missing collection-object transform.");
	}
	
	private JSONObject cspace264Hack(JSONObject in,String path) {
		// 1. Copy accession number into other number
		JSONArray ons=new JSONArray();
		String accnum;
		try {
			accnum = in.getString("objectnumber");
		} catch (JSONException e1) {
			 // XXX should never happen: log it
			return in;
		}		
		try {
			ons=in.getJSONArray("othernumber");
		} catch (JSONException e) {}
		ons.put("_accnum:"+accnum);
		in.remove("othernumber");
		try {
			in.put("othernumber",ons);
		} catch (JSONException e) {
			 // XXX should never happen: log it
		}
		// 2. Copy path into accession number
		in.remove("objectnumber");
		try {
			in.put("objectnumber",path);
		} catch (JSONException e) {
			 // XXX should never happen: log it
		}
		return in;
	}
	
	public void createJSON(String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		// XXX Here's what we do because of CSPACE-264
		System.err.println(jsonObject);
		jsonObject=cspace264Hack(jsonObject,filePath);
		autocreateJSON("",jsonObject);
		// XXX End of here's what we do because of CSPACE-264		
		// Here's what we should do ->
		// throw new UnimplementedException("Cannot create collectionobject at known path, use autocreateJSON");
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

	// XXX cannot test until CSPACE-264 is fixed.
	public String autocreateJSON(String filePath, JSONObject jsonObject) throws ExistException, UnderlyingStorageException, UnimplementedException {
		try {			
			ReturnedURL url = conn.getURL(RequestMethod.POST,"collectionobjects/",jxj.json2xml(jsonObject));
			if(url.getStatus()>299 || url.getStatus()<200)
				throw new UnderlyingStorageException("Bad response "+url.getStatus());
			return url.getURLTail();
		} catch (BadRequestException e) {
			throw new UnderlyingStorageException("Service layer exception",e);
		} catch (InvalidXTmplException e) {
			throw new UnimplementedException("Error in template",e);
		}
	}
}
