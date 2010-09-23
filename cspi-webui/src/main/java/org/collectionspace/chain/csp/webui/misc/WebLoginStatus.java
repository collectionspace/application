package org.collectionspace.chain.csp.webui.misc;

import org.collectionspace.chain.csp.schema.Record;
import org.collectionspace.chain.csp.schema.Spec;
import org.collectionspace.chain.csp.webui.main.Request;
import org.collectionspace.chain.csp.webui.main.WebMethod;
import org.collectionspace.chain.csp.webui.main.WebUI;
import org.collectionspace.chain.csp.webui.record.RecordSearchList;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;
import org.collectionspace.csp.api.ui.UISession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebLoginStatus  implements WebMethod {
	private static final Logger log=LoggerFactory.getLogger(WebLoginStatus.class);
	private Spec spec;
	
	public WebLoginStatus(Spec spec) {
		this.spec = spec;
	}

	private JSONObject getPermissions(Storage storage) throws JSONException, UIException, ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject data = new JSONObject();
		JSONObject perms = new JSONObject();

		String permbase = "accountperms";//spec.getRecordByWebUrl("userperm").getID();
		String base = spec.getRecordByWebUrl("userperm").getID();
		JSONObject activePermissions = storage.retrieveJSON(base + "/0/");

		//we are ignoring pagination so this will return the first 40 permissions only
		//UI doesn't know what it wants to do about pagination etc

		if(activePermissions.has("account"))
		{
			JSONObject account = activePermissions.getJSONObject("account");
			String csid = account.getString("accountId");
			data.put("csid",csid);
		}
		if(activePermissions.has("permission"))
		{
			JSONArray active = activePermissions.getJSONArray("permission");
			for(int j=0;j<active.length();j++){
				String resourceName = Generic.ResourceName(spec,active.getJSONObject(j).getString("resourceName"));
				JSONArray permissions = Generic.PermissionLevelArray(active.getJSONObject(j).getString("actionGroup"));
				perms.put(resourceName, permissions);
			}
		}
		data.put("permissions",perms);
		return data;
	}
	
	public void testlogin(Request in) throws UIException {
		try{
			Storage storage = in.getStorage();
			JSONObject output= new JSONObject();
			UIRequest request=in.getUIRequest();
			if(request.getSession() != null && request.getSession().getValue(UISession.USERID) != null ){
				if(request.getSession().getValue(UISession.USERID).equals("")){
					output.put("login", false);
				}				
				else{
					JSONObject perms = getPermissions(storage);
					output.put("permissions",perms.getJSONObject("permissions"));
					output.put("csid",perms.getString("csid"));
					output.put("login", true);
				}
			}
			else{
				output.put("login", false);
			}
			request.sendJSONResponse(output);
		} catch (JSONException x) {
			throw new UIException("Failed to parse json: "+x,x);
		} catch (ExistException x) {
			throw new UIException("Existence exception: "+x,x);
		} catch (UnimplementedException x) {
			throw new UIException("Unimplemented exception: "+x,x);
		} catch (UnderlyingStorageException x) {
			throw new UIException("Problem storing: "+x,x);
		}
		
		
	}
	
	public void run(Object in,String[] tail) throws UIException {
		testlogin((Request)in);
	}

	public void configure(WebUI ui, Spec spec) {
		
	}
}
