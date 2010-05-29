package org.collectionspace.chain.csp.webui.misc;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
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

	private void reset(Storage storage,UIRequest request) throws UIException { 
		//remember to log into the fornt end before trying to run this
		// Temporary hack to reset db
		try {
			TTYOutputter tty=request.getTTYOutputter();
			// Delete existing records
			JSONObject data = storage.getPathsJSON("/",null);
			String[] paths = (String[]) data.get("listItems");
			for(String dir : paths) {
				// XXX yuck!
				// ignore authorities
				if("place".equals(dir) || "vocab".equals(dir) || "person".equals(dir) || "organization".equals(dir)){
					continue;
				}
				
				// ignore authorization
				if("rolePermission".equals(dir) || "accountrole".equals(dir)  || "permrole".equals(dir) || "permission".equals(dir) || "role".equals(dir) || "users".equals(dir) ){
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
						// Never mind
						tty.line("ux");
					} catch (UnderlyingStorageException e) {
						tty.line("UnderlyingStorageEception");
					}
					tty.line("ok");
					tty.flush();
				}					
			}
			// Create records anew
			tty.line("Create records anew");
			String schedule=getResource("reset.txt");
			for(String line : schedule.split("\n")) {
				String[] parts=line.split(" +",2);
				tty.line("Creating "+parts[0]);
				storage.autocreateJSON(parts[0],getJSONResource(parts[1]));
				tty.flush();
			}
			// Delete existing vocab entries

			JSONObject myjs = new JSONObject();
			myjs.put("pageSize", "100");
			myjs.put("pageNum", "0");
			int resultsize=1;
			int check = 0;
			String checkpagination = "";
			
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
					} catch(Exception e) { /* Sometimes records are wdged */ }
					tty.flush();
					
				}
			}

			while(resultsize >0){
				myjs.put("pageNum", check);
				//check++;
				//don't increment page num as need to call page 0 as 
				//once you delete a page worth the next page is now the current page
				data = storage.getPathsJSON("/organization/organization",myjs);
				String[] res = (String[]) data.get("listItems");

				if(res.length==0 || checkpagination.equals(res[0])){
					resultsize=0;
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
				storage.autocreateJSON("/person/person",name);
				tty.line("Created Person "+name);
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
				storage.autocreateJSON("/organization/organization",name);
				tty.line("Created Organisation "+line);
				tty.flush();
				if(quick && i>20)
					break;
			}
			
			
			tty.line("done");
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
		reset(q.getStorage(),q.getUIRequest());
	}

	public void configure() throws ConfigException {}
	public void configure(WebUI ui,Spec spec) {}
}
