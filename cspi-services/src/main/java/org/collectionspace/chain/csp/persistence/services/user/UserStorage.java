/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services.user;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.collectionspace.chain.csp.persistence.services.GenericStorage;
import org.collectionspace.chain.csp.persistence.services.XmlJsonConversion;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.FieldSet;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import org.dom4j.Node;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements user service connection over service layer. Generally simply a
 * much simpler version of regular record storage, but also uses non-multiparts.
 * 
 * @author dan
 * 
 */

public class UserStorage extends GenericStorage {
	private static final Logger log = LoggerFactory
			.getLogger(UserStorage.class);
	
	public UserStorage(Record r, ServicesConnection conn)
			throws DocumentException, IOException {
		super(r, conn);
		initializeGlean(r);
	}

	private JSONObject correctPassword(JSONObject in) throws JSONException,
			UnderlyingStorageException {
		try {
			if (in.has("password")) {
				String password = in.getString("password");
				in.remove("password");
				password = new String(Base64.encodeBase64(password.getBytes("UTF-8")),"UTF-8");
				while (password.endsWith("\n") || password.endsWith("\r"))
					password = password.substring(0, password.length() - 1);
				in.put("password", password);
			}
			return in;
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Error generating Base 64", e);
		}
	}

	/* XXX FIXME in here until we fix the UI layer to pass the data correctly */
	private JSONObject correctScreenName(JSONObject in) throws JSONException,
			UnderlyingStorageException {
		if (in.has("userName") && !in.has("screenName")) {
			String username = in.getString("userName");
			in.remove("userName");
			in.put("screenName", username);
		}
		return in;
	}

	/* XXX FIXME in here until we fix the UI layer to pass the data correctly */
	private JSONObject correctUserId(JSONObject in) throws JSONException,
			UnderlyingStorageException {
		if (!in.has("userId")) {
			String userId = in.getString("email");
			in.remove("userId");
			in.put("userId", userId);
		}
		return in;
	}



	protected ReturnedURL autoCreateSub(CSPRequestCredentials creds,
			CSPRequestCache cache, JSONObject jsonObject, Document doc, String savePrefix, Record r)
			throws JSONException, UnderlyingStorageException,
			ConnectionException {
		ReturnedURL url;
		

		jsonObject = correctPassword(jsonObject);
		jsonObject = correctScreenName(jsonObject);
		jsonObject = correctUserId(jsonObject);
		doc = XmlJsonConversion.convertToXml(r, jsonObject,
				"common","POST");
		url = conn.getURL(RequestMethod.POST, r.getServicesURL() + "/",
				doc, creds, cache);
		if(url.getStatus()>299 || url.getStatus()<200)
			throw new UnderlyingStorageException("Bad response ",url.getStatus(),r.getServicesURL()+"/");
		return url;
	}
	


	public void deleteJSON(ContextualisedStorage root,
			CSPRequestCredentials creds, CSPRequestCache cache, String filePath)
			throws ExistException, UnimplementedException,
			UnderlyingStorageException {

		String[] parts = filePath.split("/");
		if (parts.length > 2) {
			for (FieldSet allfs : r.getAllSubRecords("DELETE")) {
				Record allr = allfs.usesRecordId();
				if (allr.getID().equals(parts[2])) {
					filePath = parts[0] + "/" + allr.getServicesURL() + "/"
							+ parts[2];
				}
			}
		}

		//String serviceurl = r.getServicesURL() + "/";
		deleteJSON(root,creds,cache,filePath,r);
		
	}

	@SuppressWarnings("unchecked")
	public JSONObject getPathsJSON(ContextualisedStorage root,
			CSPRequestCredentials creds, CSPRequestCache cache,
			String rootPath, JSONObject restrictions) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		try {
			JSONObject out = new JSONObject();
			List<String> listitems = new ArrayList<String>();
			Iterator rit = restrictions.keys();
			StringBuffer args = new StringBuffer();
			while (rit.hasNext()) {
				String key = (String) rit.next();
				FieldSet fs = r.getFieldTopLevel(key);
				if (!(fs instanceof Field))
					continue;
				String filter = ((Field) fs).getServicesFilterParam();
				if (filter == null)
					continue;
				args.append('&');
				args.append(filter);
				args.append('=');
				args.append(URLEncoder.encode(restrictions.getString(key),
						"UTF-8"));
			}
			// pagination
			String tail = args.toString();
			
			String path = getRestrictedPath(r.getServicesURL(), restrictions, r.getServicesSearchKeyword(), tail, false, "");

			if(r.hasSoftDeleteMethod()){
				path = softpath(path);
			}
			if(r.hasHierarchyUsed("screen")){
				path = hierarchicalpath(path);
			}
			
			
			JSONObject data = getListView(creds,cache,path,r.getServicesListPath(),"csid",false,r);
			
			return data;
			

		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"
					+ e.getLocalizedMessage(), e.getStatus(), e.getUrl(), e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Exception building query"
					+ e.getLocalizedMessage(), e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Exception building query"
					+ e.getLocalizedMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public String[] getPaths(ContextualisedStorage root,
			CSPRequestCredentials creds, CSPRequestCache cache,
			String rootPath, JSONObject restrictions) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		try {
			List<String> out = new ArrayList<String>();
			Iterator rit = restrictions.keys();
			StringBuffer args = new StringBuffer();
			while (rit.hasNext()) {
				String key = (String) rit.next();
				FieldSet fs = r.getFieldTopLevel(key);
				if (!(fs instanceof Field))
					continue;
				String filter = ((Field) fs).getServicesFilterParam();
				if (filter == null)
					continue;
				args.append('&');
				args.append(filter);
				args.append('=');
				args.append(URLEncoder.encode(restrictions.getString(key),
						"UTF-8"));
			}
			// pagination

			String tail = args.toString();
			String path = getRestrictedPath(r.getServicesURL(), restrictions, r.getServicesSearchKeyword(), tail, false, "");
			

			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET, path, null, creds, cache);
			if (doc.getStatus() < 200 || doc.getStatus() > 399)
				throw new UnderlyingStorageException(
						"Cannot retrieve account list", doc.getStatus(), path);
			Document list = doc.getDocument();
			List<Node> objects = list.selectNodes(r.getServicesListPath());
			for (Node object : objects) {
				List<Node> fields = object.selectNodes("*");
				String csid = object.selectSingleNode("csid").getText();
				for (Node field : fields) {
					if ("csid".equals(field.getName())) {
						int idx = csid.lastIndexOf("/");
						if (idx != -1)
							csid = csid.substring(idx + 1);
						out.add(csid);
					} else if ("uri".equals(field.getName())) {
						// Skip!
					} else {
						String json_name = view_map.get(field.getName());
						if (json_name != null) {
							String value = field.getText();
							// XXX hack to cope with multi values
							if (value == null || "".equals(value)) {
								List<Node> inners = field.selectNodes("*");
								for (Node n : inners) {
									value += n.getText();
								}
							}
							setGleanedValue(cache, r.getServicesURL() + "/"
									+ csid, json_name, value);
						}
					}
				}
			}
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"
					+ e.getLocalizedMessage(), e.getStatus(), e.getUrl(), e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Exception building query"
					+ e.getLocalizedMessage(), e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Exception building query"
					+ e.getLocalizedMessage(), e);
		}
	}

	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String filePath, JSONObject restrictions)
			throws ExistException, UnimplementedException,
			UnderlyingStorageException {
		try {
			Boolean isUserRole = false;
			String[] parts = filePath.split("/");
			if (parts.length >= 2 && parts[1].equals("userrole")) {
				isUserRole = true;
				String path = r.getSpec().getRecord("userrole")
						.getServicesURL();
				int len = parts.length - 1;
				int i = 0;
				for (i = 0; i < len; i++) {
					path = path.replace("*", parts[i]);
					i++;
				}
				if (len >= i) {
					path = path + "/" + parts[len];
				} else {
					path = path + "/";
				}
				filePath = path;
			} else {
				filePath = r.getServicesURL() + "/" + filePath;
			}
			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.GET,
					filePath, null, creds, cache);
			JSONObject out = new JSONObject();
			Document xml = null;
			xml = doc.getDocument();
			if ((doc.getStatus() < 200 || doc.getStatus() >= 300))
				throw new UnderlyingStorageException("Does not exist ", doc
						.getStatus(), filePath);
			if (isUserRole)
				out = XmlJsonConversion.convertToJson(r.getSpec().getRecord(
						"userrole"), xml,"GET","common","","");// XXX hardwired common section :(
			else
				out = XmlJsonConversion.convertToJson(r, xml,"GET","common","","");// XXX hardwired common section :(
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"
					+ e.getLocalizedMessage(), e.getStatus(), e.getUrl(), e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception"
					+ e.getLocalizedMessage(), e);
		}
	}

	public void updateJSON(ContextualisedStorage root,
			CSPRequestCredentials creds, CSPRequestCache cache,
			String filePath, JSONObject jsonObject, JSONObject restrictions) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		try {
			// XXX when CSPACE-1458 is fixed, remove the call to
			// xxx_cspace1458_fix, and just pass jsonObject as this arg. (fao
			// Chris or somoeone else at CARET).
			jsonObject = correctPassword(jsonObject);
			Document in = XmlJsonConversion.convertToXml(r, jsonObject,
					"common","PUT");
			// Document
			// in=XmlJsonConversion.convertToXml(r,xxx_cspace1458_fix(filePath,jsonObject,creds,cache),"common");
			//log.info("Sending: " + in.asXML());
			ReturnedDocument doc = conn.getXMLDocument(RequestMethod.PUT, r
					.getServicesURL()
					+ "/" + filePath, in, creds, cache);
			// if(doc.getStatus()==404)
			// throw new
			// ExistException("Not found: "+r.getServicesURL()+"/"+filePath);
			if (doc.getStatus() > 299 || doc.getStatus() < 200)
				throw new UnderlyingStorageException("Bad response ", doc
						.getStatus(), r.getServicesURL() + "/" + filePath);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"
					+ e.getLocalizedMessage(), e.getStatus(), e.getUrl(), e);
		} catch (JSONException e) {
			throw new UnimplementedException("JSONException"
					+ e.getLocalizedMessage(), e);
		}
	}

}