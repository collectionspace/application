/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.misc;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Option;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.authorities.AuthoritiesVocabulariesInitialize;
import org.collectionspace.chain.csp.webui.authorities.VocabulariesRead;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebReset implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebReset.class);
	private boolean quick;
	private boolean populate;
	private Spec spec;
	private  AuthoritiesVocabulariesInitialize avi;
	
	// HACK! This should not build services logic in this way!!!
	private static final String PERSON_TERMLIST_ELEMENT = "personTermGroup";
	private static final String ORG_TERMLIST_ELEMENT = "orgTermGroup";
	private static final String TERM_DISPLAYNAME_ELEMENT = "termDisplayName";

	public WebReset(boolean in, boolean populate) { quick=in; this.populate = populate; }	

	// XXX refactor
	private JSONObject getJSONResource(String in) throws IOException, JSONException {	
		return new JSONObject(getResource(in));
	}

	// XXX refactor
	private String getResource(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		log.debug(path);
		String data=IOUtils.toString(stream);
		stream.close();		
		return data;
	}

	private void initialiseAll(Storage storage,UIRequest request,String path) throws UIException { 
		TTYOutputter tty=request.getTTYOutputter();
		try{
			log.info("Initialise vocab/auth entries");
			tty.line("Initialise vocab/auth entries");
			// Delete existing vocab entries
			JSONObject myjs = new JSONObject();
			myjs.put("pageSize", "10");
			myjs.put("pageNum", "0");
			JSONObject data = storage.getPathsJSON("/",null);
			String[] paths = (String[]) data.get("listItems");
			for(String dir : paths) {
				try{
					if(this.spec.hasRecord(dir)){
						Record r = this.spec.getRecord(dir);
						if(r.isType("authority")){
							log.info("testing Authority " +dir);
							tty.line("testing Authority  " + dir);
							for(Instance n : r.getAllInstances()) {

								avi = new AuthoritiesVocabulariesInitialize(n, populate);
								Option[] allOpts = n.getAllOptions();

								avi.createIfMissingAuthority(storage,tty, r, n);
								avi.fillVocab(storage, r, n, tty, allOpts, true);
								//avi.initializeVocab(storage,request,path);
								
								
								/*
								String url = r.getID()+"/"+n.getTitleRef();
								try{
									storage.getPathsJSON(url,new JSONObject()).toString();
									log.info("Instance " + n.getID()+ " Exists");
									tty.line("Instance " + n.getID()+ " Exists");
								}
								catch (UnderlyingStorageException x) {
		
									log.info("need to create Instance " + n.getID());
									tty.line("need to create Instance " + n.getID());
									JSONObject fields=new JSONObject("{'displayName':'"+n.getTitle()+"','shortIdentifier':'"+n.getWebURL()+"'}");
									String base=r.getID();
									storage.autocreateJSON(base,fields);
									log.info("Instance " + n.getID() + " Created");
									tty.line("Instance " + n.getID() + " Created");
								}	
								*/						
							}
						}
					}
				}
				catch(Exception e){
					tty.line("that was weird but probably not a problem " + e.getMessage());
					log.warn("initialiseAll() exception: " + e.getMessage());
				}
			}

		
		} catch (ExistException e) {
			log.info("ExistException "+ e.getLocalizedMessage());
			tty.line("ExistException "+ e.getLocalizedMessage());
			throw new UIException("Existence problem",e);
		} catch (UnimplementedException e) {
			log.info("UnimplementedException "+ e.getLocalizedMessage());
			tty.line("UnimplementedException "+ e.getLocalizedMessage());
			throw new UIException("Unimplemented ",e);
		} catch (UnderlyingStorageException x) {
			log.info("UnderlyingStorageException "+ x.getLocalizedMessage());
			tty.line("UnderlyingStorageException "+ x.getLocalizedMessage());
			throw new UIException("Problem storing"+x.getLocalizedMessage(),x.getStatus(),x.getUrl(),x);
		} catch (JSONException e) {
			log.info("JSONException "+ e.getLocalizedMessage());
			tty.line("JSONException "+ e.getLocalizedMessage());
			throw new UIException("Invalid JSON",e);
		} 
		
		
	}
	
	private static JSONObject createTrivialAuthItem(String termGroup, String name) throws JSONException {
		JSONObject item=new JSONObject();
		JSONArray termInfoArray = new JSONArray();
		JSONObject termInfo = new JSONObject();
		termInfo.put(TERM_DISPLAYNAME_ELEMENT, name);
		termInfoArray.put(termInfo);
		item.put(termGroup, termInfoArray);
		return item;
	}
	
	private void reset(Storage storage,UIRequest request,String path) throws UIException { 
		//remember to log into the front end before trying to run this
		JSONObject data = new JSONObject();
		TTYOutputter tty=request.getTTYOutputter();
		// Temporary hack to reset db
		try {
			data = storage.getPathsJSON("/",null);
			String[] paths = (String[]) data.get("listItems");
			if(!path.equals("nodelete")){
				// Delete existing records
				for(String dir : paths) {
					Record r = null;
					log.info(dir);
					if("direct".equals(dir)||"relations".equals(dir))
						continue;
					try{
						r = this.spec.getRecord(dir);
					}
					catch(Exception e){
						continue;
					}
					if(r.isType("procedure")){
						if("termlistitem".equals(dir) ||"termlist".equals(dir))
							continue;
						// Nothing to do for the pseudo-records
						if(r.isType("searchall")) {
							continue;
						}
					}
					else if(r.isType("authority")){
						continue;
					}
					else if(r.isType("record")){
						if("hierarchy".equals(dir) || !r.isRealRecord())	// Filter out self-renderers, etc
							continue;
						log.info("S");
					}
					else if(r.isType("authorizationdata")){
						continue;
					}
					else if(r.isType("userdata")){
						continue;
					}
					else{
						//ignore - have no idea what it is
						continue;
					}
					
					//if("place".equals(dir) || "vocab".equals(dir) || "contact".equals(dir) || "location".equals(dir) || "person".equals(dir) || "organization".equals(dir) || "taxon".equals(dir)){
					//	continue;
					//}
					
					// ignore authorization
					//if("rolePermission".equals(dir) || "accountrole".equals(dir) || "accountroles".equals(dir)  || "userperm".equals(dir)|| "permrole".equals(dir) || "permission".equals(dir) || "role".equals(dir)|| "userrole".equals(dir) || "users".equals(dir) ){
					//	continue;
					//}

					// ignore other - tho we do need to clean these up
					//if("termlistitem".equals(dir) ||"termlist".equals(dir) || "reports".equals(dir) || "reporting".equals(dir) || "output".equals(dir)  )
					//	continue;
					//// ignore other - tho we do need to clean these up
					//if("hierarchy".equals(dir) || "dimension".equals(dir) ||"structureddate".equals(dir)  ||"blobs".equals(dir) ||"relations".equals(dir) || "direct".equals(dir) || "id".equals(dir) )
					//	continue;
					

					log.info("Deleteing data associated with : "+dir);
					tty.line("Deleteing data associated with : "+dir);
					JSONObject data2 = storage.getPathsJSON(dir,null);
					String[] paths2 = (String[]) data2.get("listItems");
					for(int i=0;i<paths2.length;i++) {
						tty.line("path : "+dir+"/"+paths2[i]);
						try {
							storage.deleteJSON(dir+"/"+paths2[i]);
						} catch (UnimplementedException e) {
							tty.line("UnimplementedException"+e);
							tty.line("ux");
						} catch (UnderlyingStorageException e) {
							tty.line("UnderlyingStorageEception"+e);
						}
						tty.line("ok");
						tty.flush();
					}					
				}
			}
			
			log.info("Creating records and procedures: this might take some time, go get a cup of tea and be patient");
			tty.line("Creating records and procedures: this might take some time, go get a cup of tea and be patient");
			// Create records anew
			tty.line("Create records anew");
			String schedule=getResource("reset.txt");
			for(String line : schedule.split("\n")) {
				String[] parts=line.split(" +",2);
				if(!parts[0].equals("")){
					tty.line("Creating "+parts[0]);
					log.info("Creating "+parts[0]);
					storage.autocreateJSON(parts[0],getJSONResource(parts[1]));
					tty.flush();
				}
			}

			log.info("Delete existing vocab/auth entries");
			tty.line("Delete existing vocab/auth entries");
			// Delete existing vocab entries
			JSONObject myjs = new JSONObject();
			myjs.put("pageSize", "10");
			myjs.put("pageNum", "0");
			for(String dir : paths) {
				try{
					if(this.spec.hasRecord(dir)){
						Record r = this.spec.getRecord(dir);
						if(r.isType("authority")){
							for(Instance n : r.getAllInstances()) {
								String url = r.getID()+"/"+n.getTitleRef();
								try{
									storage.getPathsJSON(url,new JSONObject()).toString();
								}
								catch (UnderlyingStorageException x) {
	
									log.info("need to create Instance " + n.getID());
									tty.line("need to create Instance " + n.getID());
									JSONObject fields=new JSONObject("{'displayName':'"+n.getTitle()+"','shortIdentifier':'"+n.getWebURL()+"'}");
									String base=r.getID();
									storage.autocreateJSON(base,fields);
									log.info("Instance " + n.getID() + " Created");
									tty.line("Instance " + n.getID() + " Created");
								}
	
								deletall(n,r,url,"Deleting "+ url, storage, data, tty, myjs);
								
							}
						}
					}
				}
				catch(Exception e){
					log.info("that was weird but probably not an issue " + e.getMessage());
				}
			}
			
			log.info("Creating Dummy data");
			tty.line("Creating Dummy data");
			tty.flush();
			// Create vocab entries
			
			String names=getResource("names.txt");
			int i=0;
			for(String nextName : names.split("\n")) {
				i++;
				JSONObject entry=createTrivialAuthItem(PERSON_TERMLIST_ELEMENT, nextName);
				storage.autocreateJSON("/person/person",entry);
				tty.line("Created Person "+entry);
				log.info("Created Person "+entry);
				tty.flush();
				if(quick && i>20)
					break;
			}
			// Create vocab entries
			String orgs=getResource("orgs.txt");
			i=0;
			for(String nextName : orgs.split("\n")) {
				i++;
				JSONObject entry=createTrivialAuthItem(ORG_TERMLIST_ELEMENT, nextName);
				storage.autocreateJSON("/organization/organization",entry);
				tty.line("Created Organisation "+nextName);
				log.info("Created Organisation "+nextName);
				tty.flush();
				if(quick && i>20)
					break;
			}
			
			

			tty.line("done");
			log.info("done");
		} catch (ExistException e) {
			log.info("ExistException "+ e.getLocalizedMessage());
			tty.line("ExistException "+ e.getLocalizedMessage());
			throw new UIException("Existence problem",e);
		} catch (UnimplementedException e) {
			log.info("UnimplementedException "+ e.getLocalizedMessage());
			tty.line("UnimplementedException "+ e.getLocalizedMessage());
			throw new UIException("Unimplemented ",e);
		} catch (UnderlyingStorageException x) {
			log.info("UnderlyingStorageException "+ x.getLocalizedMessage());
			tty.line("UnderlyingStorageException "+ x.getLocalizedMessage());
			throw new UIException("Problem storing"+x.getLocalizedMessage(),x.getStatus(),x.getUrl(),x);
		} catch (JSONException e) {
			log.info("JSONException "+ e.getLocalizedMessage());
			tty.line("JSONException "+ e.getLocalizedMessage());
			throw new UIException("Invalid JSON",e);
		} catch (IOException e) {
			log.info("IOException "+ e.getLocalizedMessage());
			tty.line("IOException "+ e.getLocalizedMessage());
			throw new UIException("IOException",e);
		}
	}

	private JSONObject deletall(Instance n,Record thisr,  String path, String msg, Storage storage, JSONObject data,
			TTYOutputter tty, JSONObject myjs) throws JSONException,
			ExistException, UnimplementedException, UnderlyingStorageException,
			UIException {
		
		int resultsize;
		int check;
		String checkpagination;
		resultsize=1;
		check = 0;
		checkpagination = "";
		while(resultsize >0){
			myjs.put("pageNum", check);
			//check++;
			//don't increment page num as need to call page 0 as 
			//once you delete a page worth the next page is now the current page
			//String url = thisr.getID()+"/"+n.getTitleRef();
			if(thisr== null || n==null){
				String[] bits = path.split("/");
				thisr = this.spec.getRecordByWebUrl(bits[1]);
				n = thisr.getInstance(bits[1]+"-"+bits[2]);
			}
			try{
				data = storage.getPathsJSON(path,myjs);
			}
			catch (UnderlyingStorageException x) {

				JSONObject fields=new JSONObject("{'displayName':'"+n.getTitle()+"','shortIdentifier':'"+n.getWebURL()+"'}");
				if(thisr.getFieldFullList("termStatus") instanceof Field){
					fields.put("termStatus", ((Field)thisr.getFieldFullList("termStatus")).getOptionDefault());
				}
				
				String base=thisr.getID();
				storage.autocreateJSON(base,fields);
				//data = storage.getPathsJSON(url,restriction);
			}
			String[] res = (String[]) data.get("listItems");

			if(res.length==0 || checkpagination.equals(res[0])){
				resultsize=0;
				break;
				//testing whether we have actually returned the same page or the next page - all csid returned should be unique
			}
			else{
				checkpagination = res[0];
			}
			resultsize=res.length;
			for(String urn : res) {
				try {
					storage.deleteJSON(path+"/"+urn);
					tty.line(msg+urn);
					log.info(msg+urn);
				} catch(Exception e) { 
					/* Sometimes records are wdged */ 
				}
				tty.flush();
				
			}
		}
		return data;
	}

	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		initialiseAll(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));

		if(this.populate){
			reset(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
		}
	}

	public void configure() throws ConfigException {}
	public void configure(WebUI ui,Spec spec) {
		this.spec = spec;
	}
}
