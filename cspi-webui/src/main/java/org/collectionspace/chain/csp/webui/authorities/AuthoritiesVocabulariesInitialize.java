/* Copyright 2010 University of Cambridge
 * Licensed under the Educational Community License (ECL), Version 2.0. You may not use this file except in 
 * compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at https://source.collectionspace.org/collection-space/LICENSE.txt
 */
package org.collectionspace.chain.csp.webui.authorities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.schema.Field;
import org.collectionspace.chain.csp.schema.Instance;
import org.collectionspace.chain.csp.schema.Option;
import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
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

/**
 * checks content of Vocabulary against a list and adds anything missing
 * eventually the service layer will have a function to do this so we wont have to do all the checking
 * 
 * @author caret
 *
 */
public class AuthoritiesVocabulariesInitialize implements WebMethod  {
	private static final Logger log=LoggerFactory.getLogger(AuthoritiesVocabulariesInitialize.class);
	private boolean append;
	private Instance n;
	private Record r;
	

	public AuthoritiesVocabulariesInitialize(Instance n, Boolean append) {
		this.append = append;
		this.n = n;
		this.r = n.getRecord();
	}
	public AuthoritiesVocabulariesInitialize(Record r, Boolean append) {
		this.append = append;
		this.r = r;
		this.n = null;
	}
	
	private JSONObject getDisplayNameList(Storage storage,String auth_type,String inst_type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		//should be using cached data (hopefully) from previous getPathsJson call
		JSONObject out=storage.retrieveJSON(auth_type+"/"+inst_type+"/"+csid+"/view", new JSONObject());
		return out;
	}
		
	
	
	private JSONObject list_vocab(JSONObject displayNames,Instance n,Storage storage,String param, Integer pageSize, Integer pageNum, Record thisr) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		JSONObject restriction=new JSONObject();
		if(param!=null){
			restriction.put(n.getRecord().getDisplayNameField().getID(),param);
		}
		if(pageNum!=null){
			restriction.put("pageNum",pageNum);
		}
		if(pageSize!=null){
			restriction.put("pageSize",pageSize);
		}
		String url = thisr.getID()+"/"+n.getTitleRef();
		JSONObject data = null;
		try{
			data = storage.getPathsJSON(url,restriction);
		}
		catch (UnderlyingStorageException x) {

			JSONObject fields=new JSONObject("{'displayName':'"+n.getTitle()+"','shortIdentifier':'"+n.getWebURL()+"'}");
			if(thisr.getFieldFullList("termStatus") instanceof Field){
				fields.put("termStatus", ((Field)thisr.getFieldFullList("termStatus")).getOptionDefault());
			}
			String base=thisr.getID();
			storage.autocreateJSON(base,fields, null);
			data = storage.getPathsJSON(url,restriction);
		}
		//if(data.has("isError")&& data.getBoolean("isError")){
			/*
			 *                     <instance id="vocab-languages">
                        <web-url>languages</web-url>
                        <title-ref>languages</title-ref>
                        <title>Languages</title>
                       */
			//create the vocab
				//create the vocab
		//	JSONObject fields=new JSONObject("{'displayName':'"+n.getTitle()+"','shortIdentifier':'"+n.getWebURL()+"'}");

		//	String base=thisr.getID();
		//	storage.autocreateJSON(base,fields);
		//	
		//}
		
		String[] results = (String[]) data.get("listItems");
		/* Get a view of each */
		for(String result : results) {
			//change csid into displayName
			JSONObject datanames = getDisplayNameList(storage,thisr.getID(),n.getTitleRef(),result);
			
			displayNames.put(datanames.getString("displayName"),result);
		}
		JSONObject alldata = new JSONObject();
		alldata.put("displayName", displayNames);
		alldata.put("pagination",  data.getJSONObject("pagination"));
		return alldata;
	}
	
	public void createIfMissingAuthority(Storage storage, TTYOutputter tty, Record r1, Instance n1) throws ExistException, UnimplementedException, UIException, JSONException, UnderlyingStorageException{
		
		String url = r1.getID()+"/"+n1.getTitleRef();
		try{
			storage.getPathsJSON(url,new JSONObject()).toString();
			if(tty != null){
				log.info("Instance " + n1.getID()+ " Exists");
				tty.line("Instance " + n1.getID()+ " Exists");
			}
		}
		catch (UnderlyingStorageException x) {
			if(tty != null){
				log.info("need to create Instance " + n.getID());
				tty.line("need to create Instance " + n.getID());
			}
			JSONObject fields=new JSONObject("{'displayName':'"+n1.getTitle()+"','shortIdentifier':'"+n1.getWebURL()+"'}");
			String base=r1.getID();
			storage.autocreateJSON(base, fields, null);
			if(tty != null){
				log.info("Instance " + n1.getID() + " Created");
				tty.line("Instance " + n1.getID() + " Created");
			}
		}	
				
	}
	
	private void initializeVocab(Storage storage,UIRequest request,String path) throws UIException {
		try{
			if(n==null) {
				// For now simply loop thr all the instances one after the other.
				for(Instance n2 : r.getAllInstances()) {
					log.info(n2.getID());
					//does instance exist?
					createIfMissingAuthority(storage,null, this.r, n2);
					resetvocabdata(storage, request, n2);
				}
			} else {
				log.info(n.getID());
				resetvocabdata(storage, request, this.n);
			}
		} catch (JSONException e) {
			throw new UIException("Cannot generate JSON",e);
		} catch (ExistException e) {
			throw new UIException("Exist exception",e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented exception",e);
		} catch (UnderlyingStorageException x) {
			UIException uiexception =  new UIException(x.getMessage(),x.getStatus(),x.getUrl(),x);
			request.sendJSONResponse(uiexception.getJSON());
		}
	}
	
	private JSONObject getJSONResource(String in) throws IOException, JSONException {	
		return new JSONObject(getResource(in));
	}

	private String getResource(String in) throws IOException {
		File file=new File(in);
		if(!file.exists())
			return null;
		InputStream stream= new FileInputStream(file);
		String data=IOUtils.toString(stream);
		stream.close();		
		return data;
	}
	
	
	public void doreset(Storage storage, Instance ins, JSONArray terms ) throws JSONException, UIException, ExistException, UnimplementedException, UnderlyingStorageException{

		Option[] allOpts = ins.getAllOptions();

		//remove all opts from instance
		if(allOpts != null && allOpts.length > 0){
			for(Option opt : allOpts){
				String name = opt.getName();
				String shortIdentifier = opt.getID();
				String sample = opt.getSample();
				Boolean dfault = opt.isDefault();
				ins.deleteOption(shortIdentifier, name, sample, dfault);
			}
		}

		for(int i=0;i<terms.length();i++){
			JSONObject element = terms.getJSONObject(i);
			if(element.has("description"))
				ins.addOption(element.getString("shortIdentifier"), element.getString("displayName"), null, false, element.getString("description"));
			else
				ins.addOption(element.getString("shortIdentifier"), element.getString("displayName"), null, false);
				
		}
		

		allOpts = ins.getAllOptions();
		fillVocab(storage, ins.getRecord(), ins, null, allOpts, false);
	}
	
	private void resetvocabdata(Storage storage,UIRequest request, Instance instance) throws UIException, ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		TTYOutputter tty=request.getTTYOutputter();

		tty.line("Initializing Vocab "+instance.getID());
		//Where do we get the list from?
		//from Spec
		Option[] allOpts = instance.getAllOptions();
		
		//but first check: do we have a path?
		Set<String> args = request.getAllRequestArgument();
		if(args.contains("datapath")){
			tty.line("Using Datapath ");
			//remove all opts from instance as we have a path
			if(allOpts != null && allOpts.length > 0){
				tty.line("Removing all opts from instance as we have a path");
				for(Option opt : allOpts){
					String name = opt.getName();
					String shortIdentifier = opt.getID();
					String sample = opt.getSample();
					Boolean dfault = opt.isDefault();
					instance.deleteOption(shortIdentifier, name, sample, dfault);
				}
			}
			
			//add from path
			String value = request.getRequestArgument("datapath");
			//log.info("getting data from path: "+value);
			try{
				tty.line("Getting data from path: "+value);
				String names = getResource(value);
				for (String line : names.split("\n")) {
					line = line.trim();
					String bits[] = line.split("\\|");
					if(bits.length>1){
						instance.addOption(bits[0], bits[1], null, false);
					}
					else{
						instance.addOption(null, line, null, false);
					}
				}
			} catch (IOException e) {
				throw new UIException("IOException",e);
			}
			allOpts = instance.getAllOptions();
		}

		fillVocab(storage, r, instance, tty, allOpts, this.append);

	}
	
	public void fillVocab(Storage storage, Record thisr,
			Instance instance, TTYOutputter tty, Option[] allOpts, Boolean appendit)
			throws UIException, ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		//step away if we have nothing
		if(allOpts != null && allOpts.length > 0){

			if(tty!= null){
				tty.line("get list from Service layer");
			}
			//get list from Service layer
			JSONObject results = new JSONObject();
				Integer pageNum = 0;
				Integer pageSize = 100;
				JSONObject fulldata= list_vocab(results,instance,storage,null, pageSize,pageNum, thisr);

				while(!fulldata.isNull("pagination")){
					Integer total = fulldata.getJSONObject("pagination").getInt("totalItems");
					pageSize = fulldata.getJSONObject("pagination").getInt("pageSize");
					Integer itemsInPage = fulldata.getJSONObject("pagination").getInt("itemsInPage");
					pageNum = fulldata.getJSONObject("pagination").getInt("pageNum");
					results=fulldata.getJSONObject("displayName");
					
					pageNum++;
					//are there more results
					if(total > (pageSize * (pageNum))){
						fulldata= list_vocab(results, instance, storage, null, pageSize, pageNum, thisr);
					}
					else{
						break;
					}
				}

				//compare
				results= fulldata.getJSONObject("displayName");

				//only add if term is not already present
				if(tty!= null){
					tty.line("only add if term is not already present");
				}
				for(Option opt : allOpts){
					String name = opt.getName();
					String shortIdentifier = opt.getID();
					//create it if term is not already present
					JSONObject data=new JSONObject("{'displayName':'"+name+"'}");
					if(opt.getID() == null || opt.getID().equals("")){
						//XXX here until the service layer does this
						shortIdentifier = name.replaceAll("\\W", "").toLowerCase();
					}
					data.put("description", opt.getDesc());
					data.put("shortIdentifier", shortIdentifier);
					if(thisr.getFieldFullList("termStatus") instanceof Field){
						data.put("termStatus", ((Field)thisr.getFieldFullList("termStatus")).getOptionDefault());
					}
					String url = thisr.getID()+"/"+instance.getTitleRef();
					
					if(!results.has(name)){
						if(tty!= null){
							tty.line("adding term "+name);
							log.info("adding term "+name);
						}
						storage.autocreateJSON(url,data,null);
						results.remove(name);
					}
					else{
						//update term
						storage.updateJSON(url+"/"+results.get(name), data);
						
						if(tty!= null){
							tty.line("removing term "+name);
						}
						//remove from results so can delete everything else if necessary in next stage
						//tho has issues with duplicates
						results.remove(name);
					}
				}
				if(!appendit){
					//delete everything that is not in options
					Iterator<String> rit=results.keys();
					while(rit.hasNext()) {
						String key=rit.next();
						String csid = results.getString(key);
						storage.deleteJSON(thisr.getID()+"/"+instance.getTitleRef()+"/"+csid);
						if(tty!= null){
							log.info("deleting term "+key);
							tty.line("deleting term "+key);
						}
					}
				}

		}
	}
	
	public void configure(WebUI ui, Spec spec) {}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		initializeVocab(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}
}
