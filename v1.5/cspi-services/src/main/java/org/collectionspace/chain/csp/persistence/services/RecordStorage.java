/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.persistence.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.Document;
import org.dom4j.DocumentException;

import org.dom4j.Node;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordStorage extends GenericStorage {
	private static final Logger log=LoggerFactory.getLogger(RecordStorage.class);
	private ServicesConnection conn;
	private Record r;
	
	public RecordStorage(Record r,ServicesConnection conn) throws DocumentException, IOException {	
		super(r,conn);
		initializeGlean(r);
	}


	/**
	 * Gets a list of csids of a certain type of record
	 * 
	 * XXX should not be used...
	 */
	@SuppressWarnings("unchecked")
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache,String rootPath,JSONObject restrictions) throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			Document list=null;
			List<String> out=new ArrayList<String>();

			String path = getRestrictedPath(  r.getServicesURL(),  restrictions, r.getServicesSearchKeyword(), "", false, "");
			
			
			ReturnedDocument all = conn.getXMLDocument(RequestMethod.GET,path,null,creds,cache);
			if(all.getStatus()!=200){
				throw new ConnectionException("Bad request during identifier cache map update: status not 200");
			}
			list=all.getDocument();
			List<Node> objects=list.selectNodes(r.getServicesListPath());
			if(r.getServicesListPath().equals("roles_list/*")){
				//XXX hack to deal with roles being inconsistent
				// XXX CSPACE-1887 workaround
				for(Node object : objects) {
					String csid = object.valueOf( "@csid" );
					out.add(csid);
				}
				
			}
			else{
				for(Node object : objects) {
					List<Node> fields=object.selectNodes("*");
					String csid=object.selectSingleNode("csid").getText();
					for(Node field : fields) {
						if("csid".equals(field.getName())) {
							int idx=csid.lastIndexOf("/");
							if(idx!=-1)
								csid=csid.substring(idx+1);
							out.add(csid);						
						} else if("uri".equals(field.getName())) {
							// Skip!
						} else {
							String json_name=view_map.get(field.getName());
							if(json_name!=null) {
								String value=field.getText();
								// XXX hack to cope with multi values		
								if(value==null || "".equals(value)) {
									List<Node> inners=field.selectNodes("*");
									for(Node n : inners) {
										value+=n.getText();
									}
								}
								setGleanedValue(cache,r.getServicesURL()+"/"+csid,json_name,value);
							}
						}
					}
				}
			}
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Service layer exception"+e.getLocalizedMessage(),e.getStatus(),e.getUrl(),e);
		} catch (UnsupportedEncodingException e) {
			throw new UnderlyingStorageException("Service layer exception:UnsupportedEncodingException",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Service layer exception:JSONException",e);
		}
	}



}
