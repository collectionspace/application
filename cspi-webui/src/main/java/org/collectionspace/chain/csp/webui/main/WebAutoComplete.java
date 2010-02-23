package org.collectionspace.chain.csp.webui.main;

import java.util.ArrayList;
import java.util.List;

import org.collectionspace.chain.csp.nconfig.ReadOnlySection;
import org.collectionspace.chain.csp.nconfig.Rules;
import org.collectionspace.chain.csp.nconfig.impl.main.NConfigException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.TTYOutputter;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class WebAutoComplete implements WebMethod {

	// XXX we just assume name for now
	private String[] doAutocomplete(CSPRequestCache cache,Storage storage,String start) throws JSONException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject restriction=new JSONObject();
		restriction.put("name",start);
		List<String> out=new ArrayList<String>();
		for(String urn : storage.getPaths("person/person",restriction)) {
			JSONObject data=storage.retrieveJSON("person/person/"+urn);
			JSONObject entry=new JSONObject();
			entry.put("urn",urn);
			entry.put("label",data.getString("name"));
			out.add(entry.toString());
		}
		return out.toArray(new String[0]);
	}
	
	private void autocomplete(CSPRequestCache cache,Storage storage,UIRequest request) throws UIException {
		try {
			TTYOutputter tty=request.getTTYOutputter();
			for(String v : doAutocomplete(cache,storage,request.getRequestArgument("q"))) {
				tty.line(v);
			}
			tty.flush();
		} catch (JSONException e) {
			throw new UIException("JSONException during autocompletion",e);
		} catch (ExistException e) {
			throw new UIException("ExistException during autocompletion",e);
		} catch (UnimplementedException e) {
			throw new UIException("UnimplementedException during autocompletion",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("UnderlyingStorageException during autocompletion",e);
		}
	}
	
	public void run(Object in,String[] tail) throws UIException {
		Request q=(Request)in;
		autocomplete(q.getCache(),q.getStorage(),q.getUIRequest());
	}

	public void configure(ReadOnlySection config) throws NConfigException {}
	
	public void configure_finish() {}
}
