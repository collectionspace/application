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
import org.apache.commons.httpclient.HttpStatus;
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
	private static final Logger log = LoggerFactory.getLogger(AuthoritiesVocabulariesInitialize.class);
	private boolean append;
	private Instance n;
	private Record r;
	private Spec spec;
	private boolean modifyResponse = true;
	private int failedPosts = 0; // If this value is > 0 after the init is complete than the default authorities and term lists have not been properly initialized.
	
	public AuthoritiesVocabulariesInitialize(Instance n, Boolean append, boolean modifyResponse) {
		this.append = append;
		this.n = n;
		this.r = n.getRecord();
		this.modifyResponse = modifyResponse;
	}
	
	public AuthoritiesVocabulariesInitialize(Record r, Boolean append) {
		this.append = append;
		this.r = r;
		this.n = null;
	}
	
	public boolean success() {
		return failedPosts == 0;
	}
	
	private JSONObject getTermData(Storage storage,String auth_type,String inst_type,String csid) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		//should be using cached data (hopefully) from previous getPathsJson call
		JSONObject out=storage.retrieveJSON(auth_type+"/"+inst_type+"/"+csid+"/view", new JSONObject());
		return out;
	}

	private JSONObject list_vocab(JSONObject shortIdentifiers,Instance n,Storage storage,String param, Integer pageSize, Integer pageNum, Record thisr) throws ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
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
		// CSPACE-6371: When fetching existing vocabulary terms, include soft-deleted ones, so that terms
		// deleted through the UI are not re-added.
		restriction.put("deleted", true);
		String url = thisr.getID()+"/"+n.getTitleRef();
		JSONObject data = null;
		try {
			data = storage.getPathsJSON(url,restriction);
		} catch (UnderlyingStorageException x) {
			JSONObject fields=new JSONObject("{'displayName':'"+n.getTitle()+"','shortIdentifier':'"+n.getWebURL()+"'}");
			if(thisr.getFieldFullList("termStatus") instanceof Field){
				fields.put("termStatus", ((Field)thisr.getFieldFullList("termStatus")).getOptionDefault());
			}
			String base=thisr.getID();
			storage.autocreateJSON(base,fields, null);
			data = storage.getPathsJSON(url,restriction);
		}
		
		String[] results = (String[]) data.get("listItems");
		/* Get a view of each */
		for (String result : results) {
			//change csid into shortIdentifier
			JSONObject termData = getTermData(storage,thisr.getID(),n.getTitleRef(),result);
			shortIdentifiers.put(termData.getString("shortIdentifier"),result);
		}
		
		JSONObject alldata = new JSONObject();
		alldata.put("shortIdentifiers", shortIdentifiers);
		alldata.put("pagination",  data.getJSONObject("pagination"));
		
		return alldata;
	}
	
	public int createIfMissingAuthority(Storage storage, StringBuffer tty, Record record, Instance instance)
			throws ExistException, UnimplementedException, UIException, JSONException, UnderlyingStorageException {
		int result = HttpStatus.SC_OK;
		
		String url = record.getID() + "/" + instance.getTitleRef();
		try {
			storage.getPathsJSON(url,new JSONObject()).toString();
			if (tty != null) {
				log.info("--- Instance " + instance.getID() + " Exists.");
				tty.append("--- Instance " + instance.getID() + " Exists.\n");
			}
		} catch (UnderlyingStorageException e) {
			if (e.getStatus() == HttpStatus.SC_NOT_FOUND) {
				failedPosts++; // assume we're going to fail
				log.info("need to create instance " + n.getID());
				if (tty != null) {
					tty.append("need to create instance " + n.getID() + '\n');
				}
				JSONObject fields = new JSONObject("{'displayName':'" + instance.getTitle() + "', 'shortIdentifier':'" + instance.getWebURL() + "'}");
				String base = record.getID();
				storage.autocreateJSON(base, fields, null);
				failedPosts--; // We succeeded, so subtract our assumed failure
				log.info("Instance " + instance.getID() + " created.");
				if (tty != null) {
					tty.append("Instance " + instance.getID() + " created.\n");
				}
			} else if (e.getStatus() == HttpStatus.SC_FORBIDDEN) {
				result = -1; // We don't have the permissions needed to see if the instance exists.
			} else {
				throw e;
			}
		}
		
		return result;
	}
	
	private void initializeVocab(Storage storage, UIRequest request, String path) throws UIException {
		try {
			if (n == null) {
				// For now simply loop thr all the instances one after the other.
				for (Instance instance : r.getAllInstances()) {
					log.info(instance.getID());
					//does instance exist?
					if (createIfMissingAuthority(storage, null, this.r, instance) == -1) {
						log.warn(String.format("The currently authenticated user does not have sufficient permission to determine if the '%s' authority/term-list is properly initialized.",
								instance.getID()));
					}
					resetvocabdata(storage, request, instance);
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
		StringBuffer tty = new StringBuffer();

		tty.append("Initializing Vocab " + instance.getID() + '\n');
		//Where do we get the list from?
		//from Spec
		Option[] allOpts = instance.getAllOptions();
		
		//but first check: do we have a path?
		Set<String> args = request.getAllRequestArgument();
		if (args.contains("datapath")) {
			tty.append("Using Datapath \n");
			//remove all opts from instance as we have a path
			if (allOpts != null && allOpts.length > 0) {
				tty.append("Removing all opts from instance as we have a path\n");
				for (Option opt : allOpts) {
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
			try {
				tty.append("Getting data from path: " + value + '\n');
				String names = getResource(value);
				for (String line : names.split("\n")) {
					line = line.trim();
					String bits[] = line.split("\\|");
					if (bits.length > 1) {
						instance.addOption(bits[0], bits[1], null, false);
					} else {
						instance.addOption(null, line, null, false);
					}
				}
			} catch (IOException e) {
				throw new UIException("IOException",e);
			}
			allOpts = instance.getAllOptions();
		}

		fillVocab(storage, r, instance, tty, allOpts, this.append);
		
		if (this.modifyResponse == true) {
			TTYOutputter ttyOut = request.getTTYOutputter();
			ttyOut.line(tty.toString());
		}

	}
	
	public void fillVocab(Storage storage,
			Record thisr,
			Instance instance, 
			StringBuffer tty, 
			Option[] allOpts, 
			Boolean appendit) throws UIException, ExistException, UnimplementedException, UnderlyingStorageException, JSONException {
		//
		// step away if we have nothing to add
		//
		if (allOpts != null && allOpts.length > 0) {
			// get list from Service layer
			JSONObject results = new JSONObject();
			Integer pageNum = 0;
			Integer pageSize = 100;
			JSONObject fulldata = list_vocab(results, instance, storage, null, pageSize, pageNum, thisr);

			while (!fulldata.isNull("pagination")) {
				Integer total = fulldata.getJSONObject("pagination").getInt("totalItems");
				pageSize = fulldata.getJSONObject("pagination").getInt("pageSize");
				Integer itemsInPage = fulldata.getJSONObject("pagination").getInt("itemsInPage");
				pageNum = fulldata.getJSONObject("pagination").getInt("pageNum");
				results = fulldata.getJSONObject("shortIdentifiers");

				pageNum++;
				// are there more results
				if (total > (pageSize * (pageNum))) {
					fulldata = list_vocab(results, instance, storage, null, pageSize, pageNum, thisr);
				} else {
					break;
				}
			}

			// compare
			results = fulldata.getJSONObject("shortIdentifiers");

			for (Option opt : allOpts) {
				String name = opt.getName();
				String shortIdentifier = opt.getID();

				if (shortIdentifier == null || shortIdentifier.equals("")) {
					shortIdentifier = name.replaceAll("\\W", "").toLowerCase();
				}

				if (!results.has(shortIdentifier)) {
					if (tty != null) {
						tty.append("adding term " + name + '\n');
						log.info("adding term " + name);
					}

					// create it if term is not already present
					JSONObject data = new JSONObject();
					data.put("displayName", name);
					data.put("description", opt.getDesc());
					data.put("shortIdentifier", shortIdentifier);
					if (thisr.getFieldFullList("termStatus") instanceof Field) {
						data.put("termStatus", ((Field) thisr.getFieldFullList("termStatus")).getOptionDefault());
					}
					String url = thisr.getID() + "/" + instance.getTitleRef();

					failedPosts++; // assume we're going to fail and an exception will be thrown
					storage.autocreateJSON(url, data, null);
					failedPosts--; // we succeeded so we can remove our assumed failure
					
					results.remove(shortIdentifier);
				} else {
					// remove from results so can delete everything else if
					// necessary in next stage
					// though has issues with duplicates
					results.remove(shortIdentifier);
				}
			}

			if (!appendit) {
				// delete everything that is not in options
				Iterator<String> rit = results.keys();
				while (rit.hasNext()) {
					String key = rit.next();
					String csid = results.getString(key);
					
					failedPosts++; // assume we're going to fail and an exception will be thrown
					storage.deleteJSON(thisr.getID() + "/" + instance.getTitleRef() + "/" + csid);
					failedPosts--; // we succeeded so remove our assumed failure
					
					if (tty != null) {
						log.info("deleting term " + key);
						tty.append("deleting term " + key + '\n');
					}
				}
			}
		}
	}
	
	public void configure(WebUI ui, Spec spec) {
		this.spec = spec;
	}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		initializeVocab(q.getStorage(), q.getUIRequest(), StringUtils.join(tail,"/"));
	}
}
