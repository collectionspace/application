package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnUnknown;
import org.collectionspace.chain.csp.persistence.services.connection.Returned;
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
	private static final String ORIGINAL_CONTENT = "Original";
	private static final String ORIGINALJPEG_CONTENT = "OriginalJpeg";
	private static final CharSequence PUBLISH_URL_SUFFIX = "publish";
	private static final long DERIVATIVE_TIMEOUT = 5 * 1000;  // Amount of time to wait for view/derivative before giving up.
	
	public BlobStorage(Record r,ServicesConnection conn) throws DocumentException, IOException {	
		super(r,conn);
		initializeGlean(r);
	}
	
	/*
	 * This method tries to get the "original" content, but if that content does not exist then it will
	 * try to get the ORIGINALJPEG_CONTENT view instead.  The original might be missing if the source of the
	 * image was an external URL.
	 */
	public JSONObject originalViewRetrieveImg(ContextualisedStorage storage,
			CSPRequestCredentials creds,
			CSPRequestCache cache,
			String inFilePath,
			String view,
			String extra,
			JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException, UnsupportedEncodingException {
		JSONObject result = null;
		
		try {
			result = viewRetrieveImg(storage, creds, cache, inFilePath, view, extra, restrictions);
		} catch (UnderlyingStorageException e) {
			// If we couldn't find the original, let's try to find the ORIGINALJPEG_CONTENT content instead.
			result = viewRetrieveImg(storage, creds, cache, inFilePath, ORIGINALJPEG_CONTENT, extra, restrictions);
			log.warn(String.format("Could not find the original content for '%s', so the '%s' view was returned instead.",
					inFilePath, ORIGINALJPEG_CONTENT));
		}
		
		return result;
	}
	
	private void sleep(long millisecondsToSleep) {
		try {
			Thread.sleep(millisecondsToSleep);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Try several times to get an image view.  Views (aka Derivatives) are created asynchronously by Nuxeo in the
	 * Services layer, so it might take a second or two for them to become available.
	 * 
	 * @param url
	 * @param creds
	 * @param cache
	 * @return
	 * @throws ConnectionException 
	 */
	private ReturnUnknown viewRetrieveImg(String view, String url, CSPRequestCredentials creds, CSPRequestCache cache) throws ConnectionException {
		ReturnUnknown result = null;
		int attempts = 0;
		long timeOutValue = System.currentTimeMillis() + DERIVATIVE_TIMEOUT;
		
		while (result == null && System.currentTimeMillis() < timeOutValue) {
			result = conn.getUnknownDocument(RequestMethod.GET, url, null, creds, cache);
			if (result.getStatus() < 200 || result.getStatus() >= 300) {
				sleep(500); // Go to sleep for 1/2 second while derivative gets created.
				result = null;
			}
			attempts++;
		}
		
		if (System.currentTimeMillis() > timeOutValue && result == null) {
			log.warn(String.format("Timed out after trying %s time(s) to retrieve '%s' view/derivative.", attempts, view));
		} else {
			log.debug(String.format("Successfully retrieved '%s' view/derivative after %s attempt(s).", view, attempts));
		}
				
		return result;
	}
	
	/*
	 * This method returns actual blob bits -either the original blob bits or those of a derivative
	 */
	public JSONObject viewRetrieveImg(ContextualisedStorage storage,
			CSPRequestCredentials creds,
			CSPRequestCache cache,
			String inFilePath,
			String view,
			String extra,
			JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException, UnsupportedEncodingException {
		JSONObject out = new JSONObject();
		String servicesurl = r.getServicesURL() + "/";
		try {
			String contentSuffix = "/content";
			String filePath = inFilePath;
			if (view.equalsIgnoreCase(ORIGINAL_CONTENT)) {
				filePath = filePath + contentSuffix;
			} else {
				filePath = filePath + "/derivatives/" + view + contentSuffix;
			}
			String softpath = filePath;
			if(r.hasSoftDeleteMethod()){
				softpath = softpath(filePath);
			}
			if(r.hasHierarchyUsed("screen")){
				softpath = hierarchicalpath(softpath);
			}
			
			//ReturnUnknown doc = conn.getUnknownDocument(RequestMethod.GET, servicesurl+softpath, null, creds, cache);
			ReturnUnknown doc = viewRetrieveImg(view, servicesurl+softpath, creds, cache);
			if (doc.getStatus() < 200 || doc.getStatus() >= 300) {
				throw new UnderlyingStorageException("Does not exist ", doc.getStatus(), softpath);
			}
			
			out.put("getByteBody", doc.getBytes()); // REM: We're returning an array of bytes here and we probably should be using a stream of bytes
			out.put("contenttype", doc.getContentType());
			out.put("contentdisposition", doc.getContentDisposition());

		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception " + e.getLocalizedMessage(), e.getStatus(), e.getUrl(), e);
		}
		
		return out;
	}	
	
	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject restrictions) throws ExistException,
	UnimplementedException, UnderlyingStorageException {
		JSONObject result = null;
		
		try {
			if (r.isType("report") == true) {
				Document doc = null;
				Map<String,Document> parts=new HashMap<String,Document>();
				for(String section : r.getServicesRecordPathKeys()) {
					String path=r.getServicesRecordPath(section);
					String[] record_path=path.split(":",2);
					doc=XmlJsonConversion.convertToXml(r,restrictions,section,"POST");
					if(doc!=null){
						parts.put(record_path[0],doc);
					}
				}
				
				Returned response = null;
				JSONObject out = new JSONObject();
				if (filePath.contains("/output") == true) {
					//
					// <Please document what this request returns>
					//
					response = conn.getReportDocument(RequestMethod.GET, "reports/"+filePath, null, creds, cache);			
				}
				else if (filePath.contains(PUBLISH_URL_SUFFIX) == true) {
					//
					// If they asked to publish the report then we return a URL to the publicitems service
					//
					response = conn.getPublishedReportDocumentURL(RequestMethod.POST, "reports/"+filePath, doc, creds, cache);
					ReturnedURL returnedURL = (ReturnedURL)response;
					out.put("Location", returnedURL.getURL());
				} else {
					//
					// This request returns the contents of the report.
					//
					response = conn.getReportDocument(RequestMethod.POST, "reports/"+filePath, doc, creds, cache);
					ReturnUnknown returnUnknown = (ReturnUnknown)response;
					out.put("getByteBody", returnUnknown.getBytes());
					out.put("contenttype", returnUnknown.getContentType());
					out.put("contentdisposition", returnUnknown.getContentDisposition());
				}

				int status = response.getStatus();
				if (status > 299 || status < 200) {
					throw new UnderlyingStorageException("Bad response ", status, r.getServicesURL() + "/");
				}
				
				result = out;
			} else {
				//
				// We're being asked for an image or some other attachment.
				//
				String[] parts=filePath.split("/");
				if (parts.length >= 2) {
					String imagefilePath = parts[0];
					String view = parts[1];
					String extra = "";
					if (parts.length == 3) {
						extra = parts[2];
					}
					if (view.equalsIgnoreCase(ORIGINAL_CONTENT)) {
						result = originalViewRetrieveImg(root, creds, cache, imagefilePath, view, extra, restrictions);
					}
					else {
						result = viewRetrieveImg(root, creds, cache, imagefilePath, view, extra, restrictions);
					}
				} else
					result = simpleRetrieveJSON(creds,cache,filePath);
			}
		} catch(JSONException x) {
			throw new UnderlyingStorageException("Error building JSON",x);
		} catch (UnsupportedEncodingException x) {
			throw new UnderlyingStorageException("Error UnsupportedEncodingException JSON",x);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		}
		
		return result;
	}
	
	@Override
	public String autocreateJSON(ContextualisedStorage root,
			CSPRequestCredentials creds, CSPRequestCache cache,
			String filePath, JSONObject jsonObject, JSONObject restrictions)
			throws ExistException, UnimplementedException,
			UnderlyingStorageException {

		ReturnedURL url = null;
		try {
			byte[] bitten = (byte[]) jsonObject.get("getbyteBody");
			String uploadname = jsonObject.getString("fileName");
			String type = jsonObject.getString("contentType");
			String path = r.getServicesURL();
			url = conn.getStringURL(RequestMethod.POST, path, bitten,
					uploadname, type, creds, cache);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException(e.getMessage(), e.getStatus(),
					e.getUrl(), e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException", e);
		}
		return conn.getBase() + url.getURL();
	}

}
