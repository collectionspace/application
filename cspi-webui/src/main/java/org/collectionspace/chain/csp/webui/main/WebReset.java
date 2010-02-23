package org.collectionspace.chain.csp.webui.main;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.csp.config.ConfigException;
import org.collectionspace.chain.csp.config.ReadOnlySection;
import org.collectionspace.chain.csp.config.Rules;
import org.collectionspace.chain.pathtrie.TrieMethod;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class WebReset implements WebMethod {
	private boolean quick;
	
	WebReset(boolean in) { quick=in; }	
	
	// XXX refactor
	private JSONObject getJSONResource(String in) throws IOException, JSONException {	
		return new JSONObject(getResource(in));
	}

	// XXX refactor
	private String getResource(String in) throws IOException, JSONException {
		String path=getClass().getPackage().getName().replaceAll("\\.","/");
		InputStream stream=Thread.currentThread().getContextClassLoader().getResourceAsStream(path+"/"+in);
		System.err.println(path);
		String data=IOUtils.toString(stream);
		stream.close();		
		return data;
	}
	
	private void reset(Storage storage,UIRequest request) throws UIException { // Temporary hack to reset db
		try {
			TTYOutputter tty=request.getTTYOutputter();
			// Delete existing records
			for(String dir : storage.getPaths("/",null)) {
				if("relations".equals(dir) || "vocab".equals(dir) || "person".equals(dir))
					continue;
				tty.line("dir : "+dir);
				String[] paths=storage.getPaths(dir,null);
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
			String schedule=getResource("reset.txt");
			for(String line : schedule.split("\n")) {
				String[] parts=line.split(" +",2);
				tty.line("Creating "+parts[0]);
				storage.autocreateJSON(parts[0],getJSONResource(parts[1]));
				tty.flush();
			}
			// Delete existing vocab entries
			for(String urn : storage.getPaths("/person/person",null)) {
				tty.line("Deleting "+urn);
				storage.deleteJSON("/person/person/"+urn);
				tty.flush();
			}
			tty.line("Creating");
			tty.flush();
			// Create vocab entries
			String names=getResource("names.txt");
			int i=0;
			for(String line : names.split("\n")) {
				i++;
				JSONObject name=new JSONObject();
				name.put("name",line);
				storage.autocreateJSON("/person/person",name);
				tty.line("Created "+name);
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

	public void configure(ReadOnlySection config) throws ConfigException {}
	public void configure_finish() {}
}
