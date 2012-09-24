package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnUnknown;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.DocumentException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlobStorage extends GenericStorage {
	private static final Logger log=LoggerFactory.getLogger(RecordStorage.class);
	
	public BlobStorage(Record r,ServicesConnection conn) throws DocumentException, IOException {	
		super(r,conn);
		initializeGlean(r);
	}
	
	public JSONObject viewRetrieveImg(ContextualisedStorage storage,CSPRequestCredentials creds,CSPRequestCache cache,String filePath,String view, String extra, JSONObject restrictions) throws ExistException,UnimplementedException, UnderlyingStorageException, JSONException, UnsupportedEncodingException {
		JSONObject out=new JSONObject();
		String servicesurl = r.getServicesURL()+"/";
		try {
			filePath = filePath +"/derivatives/"+view+"/content";
			String softpath = filePath;
			if(r.hasSoftDeleteMethod()){
				softpath = softpath(filePath);
			}
			if(r.hasHierarchyUsed("screen")){
				softpath = hierarchicalpath(softpath);
			}
			
			
			if(r.isMultipart()){
				ReturnUnknown doc = conn.getUnknownDocument(RequestMethod.GET, servicesurl+softpath, null, creds, cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new UnderlyingStorageException("Does not exist ",doc.getStatus(),softpath);
				out.put("getByteBody", doc.getBytes());
				out.put("contenttype", doc.getContentType());
				
			}
			else{
				ReturnUnknown doc = conn.getUnknownDocument(RequestMethod.GET, servicesurl+softpath, null, creds, cache);
				if((doc.getStatus()<200 || doc.getStatus()>=300))
					throw new UnderlyingStorageException("Does not exist ",doc.getStatus(),softpath);

				out.put("getByteBody", doc.getBytes());
				out.put("contenttype", doc.getContentType());
			}

		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
		return out;
	}
	
	
	
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject restrictions) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		try {
			if(r.isType("report")){
				Document doc = null;
				Map<String,Document> parts=new HashMap<String,Document>();
				for(String section : r.getServicesRecordPaths()) {
					String path=r.getServicesRecordPath(section);
					String[] record_path=path.split(":",2);
					doc=XmlJsonConversion.convertToXml(r,restrictions,section,"POST");
					if(doc!=null){
						parts.put(record_path[0],doc);
					}
				}
				ReturnUnknown doc2 = null;
				if(filePath.contains("/output")){
					doc2 = conn.getReportDocument(RequestMethod.GET, "reports/"+filePath, null, creds, cache);
					
				}
				else{
					doc2 = conn.getReportDocument(RequestMethod.POST, "reports/"+filePath, doc, creds, cache);
					
				}
				if(doc2.getStatus()>299 || doc2.getStatus()<200)
					throw new UnderlyingStorageException("Bad response ", doc2.getStatus(), r.getServicesURL()+"/");
				
				JSONObject out = new JSONObject();
				out.put("getByteBody", doc2.getBytes());
				out.put("contenttype", doc2.getContentType());
				return out;
			}
			else if(r.isType("batch")){
				Document doc = null;
				Map<String,Document> parts=new HashMap<String,Document>();
				for(String section : r.getServicesRecordPaths()) {
					String path=r.getServicesRecordPath(section);
					String[] record_path=path.split(":",2);
					doc=XmlJsonConversion.convertToXml(r,restrictions,section,"POST");
					if(doc!=null){
						parts.put(record_path[0],doc);
					}
				}
				ReturnedDocument doc2 = null;
				if(filePath.contains("/output")){
					doc2 = conn.getBatchDocument(RequestMethod.GET, "batch/"+filePath, null, creds, cache);
					
				}
				else{
					doc2 = conn.getBatchDocument(RequestMethod.POST, "batch/"+filePath, doc, creds, cache);
					
				}
				if(doc2.getStatus()>299 || doc2.getStatus()<200)
					throw new UnderlyingStorageException("Bad response ", doc2.getStatus(), r.getServicesURL()+"/");
				
				JSONObject out = new JSONObject();
				this.convertToJson(out, doc2.getDocument(), r.getSpec().getRecord("invocationresults"), "", "invocationResults", filePath);
				return out;
			}
			else{
				String[] parts=filePath.split("/");
				if(parts.length>=2) {
					String extra = "";
					if(parts.length==3){
						extra = parts[2];
					}
					return viewRetrieveImg(root,creds,cache,parts[0],parts[1],extra, restrictions);
				} else
					return simpleRetrieveJSON(creds,cache,filePath);
			}
			
			
		} catch(JSONException x) {
			throw new UnderlyingStorageException("Error building JSON",x);
		} catch (UnsupportedEncodingException x) {
			throw new UnderlyingStorageException("Error UnsupportedEncodingException JSON",x);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
	}
	
	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds, CSPRequestCache cache, String filePath, JSONObject jsonObject) throws ExistException, UnimplementedException, UnderlyingStorageException {
		
		ReturnedURL url = null;
		try {
		byte[] bitten = (byte[]) jsonObject.get("getbyteBody");
		String uploadname = jsonObject.getString("fileName");
		String type = jsonObject.getString("contentType");
		String path = r.getServicesURL();
			url = conn.getStringURL(RequestMethod.POST, path, bitten, uploadname, type, creds, cache);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException(e.getMessage(),e.getStatus(), e.getUrl(),e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException",e);
		}
		return conn.getBase()+url.getURL();
	}

}
