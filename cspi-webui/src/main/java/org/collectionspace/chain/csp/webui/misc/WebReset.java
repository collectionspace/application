package org.collectionspace.chain.csp.webui.misc;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.chain.csp.config.ConfigException;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebReset implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebReset.class);
	private boolean quick;

	public WebReset(boolean in) { quick=in; }	

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

	private void reset(Storage storage,UIRequest request,String path) throws UIException { 
		//remember to log into the fornt end before trying to run this
		JSONObject data = new JSONObject();
		// Temporary hack to reset db
		try {
			TTYOutputter tty=request.getTTYOutputter();
			if(!path.equals("nodelete")){
				// Delete existing records
				data = storage.getPathsJSON("/",null);
				String[] paths = (String[]) data.get("listItems");
				for(String dir : paths) {
					// XXX yuck!
					// ignore authorities
					if("place".equals(dir) || "vocab".equals(dir) || "contact".equals(dir) || "location".equals(dir) || "person".equals(dir) || "organization".equals(dir)){
						continue;
					}
					
					// ignore authorization
					if("rolePermission".equals(dir) || "accountrole".equals(dir)  || "userperm".equals(dir)|| "permrole".equals(dir) || "permission".equals(dir) || "role".equals(dir)|| "userrole".equals(dir) || "users".equals(dir) ){
						continue;
					}
					
					// ignore other - tho we do need to clean these up
					if("relations".equals(dir) || "direct".equals(dir) || "id".equals(dir) )
						continue;
					
					
					tty.line("dir : "+dir);
					data = storage.getPathsJSON(dir,null);
					paths = (String[]) data.get("listItems");
					for(int i=0;i<paths.length;i++) {
						tty.line("path : "+dir+"/"+paths[i]);
						try {
							storage.deleteJSON(dir+"/"+paths[i]);
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
			log.info("this might take some time, go get a cup of tea and be patient");
			tty.line("this might take some time, go get a cup of tea and be patient");
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
			// Delete existing vocab entries

			JSONObject myjs = new JSONObject();
			myjs.put("pageSize", "100");
			myjs.put("pageNum", "0");
			int resultsize=1;
			int check = 0;
			String checkpagination = "";

			log.info("Delete existing vocab entries");
			tty.line("Delete existing vocab entries");
			

			while(resultsize >0){
				myjs.put("pageNum", check);
				//check++;
				//don't increment page num as need to call page 0 as 
				//once you delete a page worth the next page is now the current page
				data = storage.getPathsJSON("/person/person",myjs);
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
						storage.deleteJSON("/person/person/"+urn);
						tty.line("Deleting Person "+urn);
						log.info("Deleting Person "+urn);
					} catch(Exception e) { /* Sometimes records are wdged */ }
					tty.flush();
					
				}
			}
			
			resultsize=1;
			check = 0;
			checkpagination = "";
			while(resultsize >0){
				myjs.put("pageNum", check);
				//check++;
				//don't increment page num as need to call page 0 as 
				//once you delete a page worth the next page is now the current page
				data = storage.getPathsJSON("/organization/organization",myjs);
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
						storage.deleteJSON("/organization/organization/"+urn);
						tty.line("Deleting Organization "+urn);
						log.info("Deleting Organization "+urn);
					} catch(Exception e) { /* Sometimes records are wdged */ }
					tty.flush();
					
				}
			}
			
			tty.line("Creating");
			tty.flush();
			// Create vocab entries
			String names=getResource("names.txt");
			int i=0;
			for(String line : names.split("\n")) {
				i++;
				JSONObject name=new JSONObject();
				name.put("displayName",line);
				String shortID = line.replaceAll("\\W", "").toLowerCase();
				name.put("shortIdentifier",shortID);
				storage.autocreateJSON("/person/person",name);
				tty.line("Created Person "+name);
				log.info("Created Person "+name);
				tty.flush();
				if(quick && i>20)
					break;
			}
			// Create vocab entries
			String orgs=getResource("orgs.txt");
			i=0;
			for(String line : orgs.split("\n")) {
				i++;
				JSONObject name=new JSONObject();
				name.put("displayName",line);
				String shortID = line.replaceAll("\\W", "").toLowerCase();
				name.put("shortIdentifier",shortID);
				storage.autocreateJSON("/organization/organization",name);
				tty.line("Created Organisation "+line);
				log.info("Created Organisation "+line);
				tty.flush();
				if(quick && i>20)
					break;
			}
			

			tty.line("done");
			log.info("done");
		} catch (ExistException e) {
			throw new UIException("Existence problem",e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented ",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Problem storing",e);
		} catch (JSONException e) {
			throw new UIException("Invalid JSON",e);
		} catch (IOException e) {
			throw new UIException("IOException",e);
		}
	}

	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		reset(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}

	public void configure() throws ConfigException {}
	public void configure(WebUI ui,Spec spec) {}
}
